package app.revanced.integrations.patches.misc;

import static android.text.TextUtils.isEmpty;
import static app.revanced.integrations.patches.misc.requests.LiveStreamRendererRequester.getLiveStreamRenderer;
import static app.revanced.integrations.utils.ReVancedUtils.submitOnBackgroundThread;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import app.revanced.integrations.patches.misc.requests.PlayerRoutes;
import app.revanced.integrations.patches.misc.requests.PlayerRoutes.ClientType;
import app.revanced.integrations.settings.SettingsEnum;
import app.revanced.integrations.utils.LogHelper;

/**
 * @noinspection ALL
 */
public class SpoofClientPatch {
    private static final boolean SPOOF_CLIENT_ENABLED = SettingsEnum.SPOOF_CLIENT.getBoolean();

    /**
     * Clips or Shorts Parameters.
     */
    private static final String CLIP_PARAMETER_PREFIX = "kAIB";
    private static final String SHORTS_PARAMETER_PREFIX = "8AEB";

    /**
     * The client of the Shorts / Clips setting is used for Clips or Shorts.
     */
    private static volatile boolean isShortsOrClips;

    /**
     * Any unreachable ip address.  Used to intentionally fail requests.
     */
    private static final String UNREACHABLE_HOST_URI_STRING = "https://127.0.0.0";
    private static final Uri UNREACHABLE_HOST_URI = Uri.parse(UNREACHABLE_HOST_URI_STRING);

    /**
     * Client id of the plain Android client, which is the client this app itself is.
     */
    private static final int APP_OWN_CLIENT_ID = 3;

    /**
     * Last spoofed client type.
     */
    private static volatile ClientType lastSpoofedClientType;

    /**
     * Whether the last player request was made for a live stream (used for the headers of the stream requests).
     */
    private static volatile boolean lastRequestWasLive;

    /**
     * Last video id loaded. Used to prevent reloading the same spec multiple times.
     */
    @NonNull
    private static volatile String lastPlayerResponseVideoId = "";

    @Nullable
    private static volatile Future<LiveStreamRenderer> rendererFuture;

    /**
     * Injection point.
     * Blocks /get_watch requests by returning a localhost URI.
     *
     * @param playerRequestUri The URI of the player request.
     * @return Localhost URI if the request is a /get_watch request, otherwise the original URI.
     */
    public static Uri blockGetWatchRequest(Uri playerRequestUri) {
        if (SPOOF_CLIENT_ENABLED) {
            try {
                String path = playerRequestUri.getPath();

                if (path != null && path.contains("get_watch")) {
                    // Logger.printDebug(() -> "Blocking: " + playerRequestUri + " by returning: " + UNREACHABLE_HOST_URI_STRING);

                    return UNREACHABLE_HOST_URI;
                }
            } catch (Exception ex) {
                // Logger.printException(() -> "blockGetWatchRequest failure", ex);
            }
        }

        return playerRequestUri;
    }

    /**
     * A player request that is sent like the official ReVanced patches send it (see
     * {@link ClientType#officialStyleRequest}). Decided once per request by {@link #beginPlayerRequest} and used by the
     * next two hooks of the same request (same thread, called one after the other).
     */
    private static final class OfficialStyleRequest {
        final String url;
        final byte[] body;

        OfficialStyleRequest(String url, byte[] body) {
            this.url = url;
            this.body = body;
        }
    }

    private static final ThreadLocal<OfficialStyleRequest> pendingOfficialStyleRequest = new ThreadLocal<>();

    private static volatile String lastLoggedOfficialStyleRequest = "";

    /**
     * Injection point: the Volley request of the app ({@code yyi.a()}), after the app has put the headers and the body
     * of the request together and right before the network request is created. Every Volley request of the app passes
     * here, the {@code /player} request is the one that matters.
     * <p>
     * The player request of the app is a protobuf body signed as the YouTube app (key and signature in the URL, the
     * "Spatula" header, all headers of the app, client 3). When the body declares another client (Android VR, Android TV,
     * ...) the server answers it with HTTP 400 "Request contains an invalid argument" (the player shows "There was a
     * problem with the server [400]"), while the same client is accepted when the request is sent the way the official
     * ReVanced patches send it (confirmed on device). So for these clients (see {@link ClientType#officialStyleRequest})
     * the whole request is replaced: headers (here, in place), URL ({@code ?alt=proto}: the answer is still the protobuf the app reads, see
     * {@link #overridePlayerRequestUrl}) and body (JSON with the client fields and the video id, see
     * {@link #overridePlayerRequestBody}).
     *
     * @param headers headers of the request (changed in place).
     * @param body    body of the request as the app made it.
     * @param url     URL of the request as the app made it.
     */
    public static void beginPlayerRequest(Map<String, String> headers, byte[] body, String url) {
        pendingOfficialStyleRequest.set(null);

        if (!SPOOF_CLIENT_ENABLED || headers == null || url == null) {
            return;
        }

        try {
            Uri uri = Uri.parse(url);
            String path = uri.getPath();
            if (path == null || !path.endsWith("/youtubei/v1/player")) {
                return;
            }

            // The visitor id of the app (sent with the playability check, see PlayerRoutes.visitorId).
            rememberVisitorId(headers);

            if (body == null) {
                return;
            }

            ClientType client = getSpoofClientType();
            if (!client.officialStyleRequest) {
                return;
            }

            String videoId = findVideoId(body);
            if (videoId == null) {
                LogHelper.info(SpoofClientPatch.class, "Player request not rewritten: no video id in the body ("
                        + body.length + " bytes)");
                return;
            }

            String newUrl = uri.buildUpon().clearQuery().appendQueryParameter("alt", "proto").build().toString();
            byte[] newBody = client.innerTubeBody.replace("%s", videoId).getBytes(StandardCharsets.UTF_8);

            applyOfficialStyleHeaders(headers, client);
            pendingOfficialStyleRequest.set(new OfficialStyleRequest(newUrl, newBody));

            logRequestHeaderNames(headers);

            String text = client.name() + "|" + videoId;
            if (!text.equals(lastLoggedOfficialStyleRequest)) {
                lastLoggedOfficialStyleRequest = text;
                LogHelper.info(SpoofClientPatch.class, "Player request sent like the official patches: client="
                        + client.name() + " video=" + videoId + " body=" + newBody.length + " bytes (app body "
                        + body.length + " bytes), url=" + newUrl);
            }
        } catch (Exception ex) {
            pendingOfficialStyleRequest.set(null);
            LogHelper.info(SpoofClientPatch.class, "Player request not rewritten: " + ex);
        }
    }

    /**
     * Injection point: the URL of the request (after {@link #beginPlayerRequest}).
     */
    public static String overridePlayerRequestUrl(String original) {
        OfficialStyleRequest pending = pendingOfficialStyleRequest.get();
        return pending == null ? original : pending.url;
    }

    /**
     * Injection point: the body of the request (after {@link #overridePlayerRequestUrl}).
     */
    public static byte[] overridePlayerRequestBody(byte[] original) {
        OfficialStyleRequest pending = pendingOfficialStyleRequest.get();
        if (pending == null) {
            return original;
        }

        pendingOfficialStyleRequest.set(null);
        return pending.body;
    }

    /**
     * The video id of a player request: the video of the last request the app made, or (a request of another video, for
     * example a prefetch) the 11 character string field of the protobuf body.
     */
    @Nullable
    private static String findVideoId(byte[] body) {
        String videoId = currentVideoId;
        if (isVideoId(videoId) && containsAscii(body, videoId)) {
            return videoId;
        }

        String found = null;
        int[] pos = {0};
        while (pos[0] < body.length) {
            long tag = readVarint(body, pos);
            if (tag < 0) return found;

            int wireType = (int) (tag & 7);
            long field = tag >>> 3;
            if (wireType == 0) {
                if (readVarint(body, pos) < 0) return found;
            } else if (wireType == 1) {
                pos[0] += 8;
            } else if (wireType == 5) {
                pos[0] += 4;
            } else if (wireType == 2) {
                long length = readVarint(body, pos);
                if (length < 0 || length > body.length - pos[0]) return found;

                if (length == 11) {
                    String text = new String(body, pos[0], 11, StandardCharsets.US_ASCII);
                    if (isVideoId(text)) {
                        if (field == 2) return text;
                        if (found == null) found = text;
                    }
                }
                pos[0] += (int) length;
            } else {
                return found;
            }
        }

        return found;
    }

    /**
     * Reads a protobuf varint at {@code pos[0]} (moves it), -1 when the data ends or the value is not valid.
     */
    private static long readVarint(byte[] data, int[] pos) {
        long result = 0;
        int shift = 0;
        while (pos[0] < data.length && shift < 64) {
            int b = data[pos[0]++] & 0xFF;
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result < 0 ? -1 : result;
            }
            shift += 7;
        }
        return -1;
    }

    private static boolean isVideoId(String text) {
        if (text == null || text.length() != 11) return false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean valid = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_';
            if (!valid) return false;
        }
        return true;
    }

    private static boolean containsAscii(byte[] data, String text) {
        int length = text.length();
        outer:
        for (int start = 0; start + length <= data.length; start++) {
            for (int i = 0; i < length; i++) {
                if (data[start + i] != (byte) text.charAt(i)) continue outer;
            }
            return true;
        }
        return false;
    }

    /**
     * The headers of an official style request: what the client sends plus the visitor id (and the API format version) of
     * the app. Nothing else of the app (signature, key, language, ...) is sent.
     */
    private static void applyOfficialStyleHeaders(Map<String, String> headers, ClientType client) {
        String visitorId = null;
        String apiFormatVersion = null;
        String authorization = null;

        for (Map.Entry<String, String> header : headers.entrySet()) {
            String name = header.getKey();
            if (name == null) continue;

            if (name.equalsIgnoreCase("X-Goog-Visitor-Id")) {
                visitorId = header.getValue();
            } else if (name.equalsIgnoreCase("X-GOOG-API-FORMAT-VERSION")) {
                apiFormatVersion = header.getValue();
            } else if (name.equalsIgnoreCase("Authorization")) {
                authorization = header.getValue();
            }
        }

        headers.clear();
        headers.put("Content-Type", "application/json");
        headers.put("User-Agent", client.userAgent);
        headers.put("X-YouTube-Client-Name", String.valueOf(client.id));
        headers.put("X-YouTube-Client-Version", client.version);

        if (visitorId != null && !visitorId.isEmpty()) {
            headers.put("X-Goog-Visitor-Id", visitorId);
        }
        if (apiFormatVersion != null && !apiFormatVersion.isEmpty()) {
            headers.put("X-GOOG-API-FORMAT-VERSION", apiFormatVersion);
        }
        if (client.useAuth && authorization != null && !authorization.isEmpty()) {
            headers.put("Authorization", authorization);
        }
    }

    /**
     * Injection point.
     *
     * Blocks /initplayback requests.
     */
    public static String blockInitPlaybackRequest(String originalUrlString) {
        if (SPOOF_CLIENT_ENABLED) {
            try {
                Uri originalUri = Uri.parse(originalUrlString);
                String path = originalUri.getPath();

                if (path != null && path.contains("initplayback")) {
                    // None of the listed clients needs the /initplayback request (the old ANDROID_TESTSUITE client that
                    // did was removed).
                    String replacementUriString = UNREACHABLE_HOST_URI_STRING;

                    // Logger.printDebug(() -> "Blocking: " + originalUrlString + " by returning: " + replacementUriString);

                    return replacementUriString;
                }
            } catch (Exception ex) {
                // Logger.printException(() -> "blockInitPlaybackRequest failure", ex);
            }
        }

        return originalUrlString;
    }

    private static ClientType getSpoofClientType() {
        boolean live = false;
        ClientType type;
        String reason;

        if (isShortsOrClips) {
            type = getClientTypeFromSettingsEnum(SettingsEnum.SPOOF_CLIENT_SHORTS);
            reason = "shorts";
        } else {
            LiveStreamRenderer renderer = getRenderer(false);
            if (renderer != null && renderer.isLive) {
                type = getClientTypeFromSettingsEnum(SettingsEnum.SPOOF_CLIENT_LIVESTREAM);
                live = true;
                reason = "live";
            } else if (renderer != null && !renderer.playabilityOk) {
                type = getClientTypeFromSettingsEnum(SettingsEnum.SPOOF_CLIENT_FALLBACK);
                reason = "unplayable";
            } else {
                type = getClientTypeFromSettingsEnum(SettingsEnum.SPOOF_CLIENT_GENERAL);
                reason = renderer == null ? "general (renderer unknown)" : "general";
            }
        }

        lastRequestWasLive = live;
        lastSpoofedClientType = type;
        logClientChoice(type, reason);
        return type;
    }

    /**
     * Logs the client used for a video once per change (logcat tag "revanced"), so a failing video can be traced.
     */
    private static volatile String lastLoggedChoice = "";

    /**
     * Video id of the last player request (for the log only).
     */
    private static volatile String currentVideoId = "";

    private static void logClientChoice(ClientType type, String reason) {
        try {
            String choice = currentVideoId + "|" + type.name() + "|" + reason;
            if (!choice.equals(lastLoggedChoice)) {
                lastLoggedChoice = choice;
                LogHelper.info(SpoofClientPatch.class, "Spoof client: " + type.name() + " (" + reason + ") video="
                        + currentVideoId + " id=" + type.id + " version=" + type.version);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Injection point.
     */
    public static int getClientTypeId(int originalClientTypeId) {
        if (SPOOF_CLIENT_ENABLED) {
            return getSpoofClientType().id;
        }

        return originalClientTypeId;
    }

    /**
     * Injection point.
     */
    public static String getClientVersion(String originalClientVersion) {
        if (SPOOF_CLIENT_ENABLED) {
            return getSpoofClientType().version;
        }

        return originalClientVersion;
    }

    /**
     * Injection point.
     */
    public static String getClientModel(String originalClientModel) {
        if (SPOOF_CLIENT_ENABLED) {
            String model = getSpoofClientType().model;
            return model != null ? model : originalClientModel;
        }

        return originalClientModel;
    }

    /**
     * Injection point.
     */
    public static String getClientDeviceMake(String originalClientDeviceMake) {
        if (SPOOF_CLIENT_ENABLED) {
            String make = getSpoofClientType().make;
            return make != null ? make : originalClientDeviceMake;
        }

        return originalClientDeviceMake;
    }

    /**
     * Injection point.
     */
    public static String getClientOsVersion(String originalClientOsVersion) {
        if (SPOOF_CLIENT_ENABLED) {
            String osVersion = getSpoofClientType().osVersion;
            return osVersion != null ? osVersion : originalClientOsVersion;
        }

        return originalClientOsVersion;
    }

    /**
     * Injection point.
     */
    public static int getClientAndroidSdkVersion(int originalClientAndroidSdkVersion) {
        if (SPOOF_CLIENT_ENABLED) {
            int sdk = getSpoofClientType().sdk;
            if (sdk >= 0) {
                return sdk; // 0 = not applicable (Apple clients).
            }
        }

        return originalClientAndroidSdkVersion;
    }

    /**
     * Injection point.
     * The OS name of the client (only the Apple clients change it).
     */
    public static String getClientOsName(String originalClientOsName) {
        if (SPOOF_CLIENT_ENABLED) {
            String osName = getSpoofClientType().osName;
            if (osName != null && !osName.equals("Android")) {
                return osName;
            }
        }

        return originalClientOsName;
    }

    /**
     * Injection point.
     *
     * Called with the headers of every request that is made by the data source of the app, right before they are
     * added to the request. The headers of the player request are changed to match the spoofed client.
     */
    public static void overrideRequestHeaders(Map<String, String> headers, Uri uri) {
        if (!SPOOF_CLIENT_ENABLED || headers == null || uri == null) {
            return;
        }

        try {
            rememberVisitorId(headers);

            String host = uri.getHost();
            if (host != null && host.endsWith("googlevideo.com")) {
                // Requests of the streams (segments, HLS/DASH manifests). The stream URLs of a client carry its name
                // ("c=<client>"). For live streams the server can refuse them when the user agent is not the one of
                // that client (HTTP 400), so the user agent of the client is used there when the setting is on.
                ClientType last = lastSpoofedClientType;
                if (lastRequestWasLive && last != null && last.userAgent != null
                        && SettingsEnum.SPOOF_CLIENT_LIVE_USER_AGENT.getBoolean()) {
                    putHeader(headers, "User-Agent", last.userAgent);
                }
                return;
            }

            String path = uri.getPath();
            if (path == null || !path.endsWith("/youtubei/v1/player")) {
                return;
            }

            ClientType client = getSpoofClientType();

            putHeader(headers, "User-Agent", client.userAgent);
            putHeader(headers, "X-YouTube-Client-Name", String.valueOf(client.id));
            putHeader(headers, "X-YouTube-Client-Version", client.version);

            if (!client.useAuth) {
                // These clients can only be used when logged out.
                removeHeader(headers, "Authorization");
                removeHeader(headers, "X-Goog-PageId");
                removeHeader(headers, "X-Goog-AuthUser");
            }

            if (client.id != APP_OWN_CLIENT_ID) {
                // The app signs its requests as the YouTube app ("Spatula"). That fits the client the app itself is,
                // but it contradicts any other client (for example Android VR), and the official ReVanced patches
                // send none of the headers of the app for these clients.
                removeHeader(headers, "X-Goog-YTSpatula");
            }

            logRequestHeaderNames(headers);
        } catch (Exception ex) {
            // Logger.printException(() -> "overrideRequestHeaders failure", ex);
        }
    }

    private static volatile String lastLoggedHeaderNames = "";

    /**
     * Diagnostics: the NAMES (never the values) of the headers of the player request, once per change.
     */
    private static void logRequestHeaderNames(Map<String, String> headers) {
        java.util.TreeSet<String> names = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String name : headers.keySet()) {
            if (name != null) names.add(name);
        }

        String text = names.toString();
        if (!text.equals(lastLoggedHeaderNames)) {
            lastLoggedHeaderNames = text;
            LogHelper.info(SpoofClientPatch.class, "Player request headers: " + text);
        }
    }

    /**
     * The visitor id of the app is sent with the playability check ({@link PlayerRoutes#visitorId}).
     */
    private static void rememberVisitorId(Map<String, String> headers) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
            String name = header.getKey();
            if (name != null && name.equalsIgnoreCase("X-Goog-Visitor-Id")) {
                String value = header.getValue();
                if (value != null && !value.isEmpty()) {
                    PlayerRoutes.visitorId = value;
                }
                return;
            }
        }
    }

    private static void removeHeader(Map<String, String> headers, String name) {
        // Header names are case insensitive.
        Iterator<String> iterator = headers.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (key != null && key.equalsIgnoreCase(name)) {
                iterator.remove();
            }
        }
    }

    private static void putHeader(Map<String, String> headers, String name, String value) {
        removeHeader(headers, name);
        headers.put(name, value);
    }

    /**
     * Injection point.
     */
    public static boolean isClientSpoofingEnabled() {
        return SPOOF_CLIENT_ENABLED;
    }

    /**
     * Injection point.
     */
    public static boolean enablePlayerGesture(boolean original) {
        return SPOOF_CLIENT_ENABLED || original;
    }

    /**
     * Injection point.
     * When spoofing the client, the playback speed menu can be missing from the player response.
     * Return true to force create the playback speed menu.
     */
    public static boolean forceCreatePlaybackSpeedMenu(boolean original) {
        if (SPOOF_CLIENT_ENABLED && lastSpoofedClientType != null) {
            return true;
        }

        return original;
    }

    /**
     * Injection point.
     */
    public static String appendSpoofedClient(String videoFormat) {
        try {
            if (SPOOF_CLIENT_ENABLED && SettingsEnum.SPOOF_CLIENT_STATS_FOR_NERDS.getBoolean()
                    && !isEmpty(videoFormat)) {
                // Force LTR layout, to match the same LTR video time/length layout YouTube uses for all languages
                ClientType client = lastSpoofedClientType;
                if (client == null) {
                    return videoFormat;
                }
                return "\u202D" + videoFormat + String.format("\u2009(%s)", client.friendlyName); // u202D = left to right override
            }
        } catch (Exception ex) {
            // Logger.printException(() -> "appendSpoofedClient failure", ex);
        }

        return videoFormat;
    }

    /**
     * Injection point.
     */
    public static String setPlayerResponseVideoId(@NonNull String videoId, @Nullable String parameters) {
        if (SPOOF_CLIENT_ENABLED) {
            currentVideoId = videoId;
            isShortsOrClips = parameters != null &&
                    (parameters.startsWith(CLIP_PARAMETER_PREFIX) || parameters.startsWith(SHORTS_PARAMETER_PREFIX));

            if (isShortsOrClips && SettingsEnum.SPOOF_CLIENT_SHORTS_AS_VIDEO.getBoolean()) {
                // The Shorts parameters make some clients answer with an error or without streams. The request is
                // sent like the one of a normal video (the client of the Shorts setting is still used).
                return "";
            }

            if (!isShortsOrClips) {
                ClientType generalClientType = getClientTypeFromSettingsEnum(SettingsEnum.SPOOF_CLIENT_GENERAL);
                // The request is made without the authorization of the app, so a client that requires login
                // cannot be used to check the playability. Use a client that works logged out.
                fetchLiveStreamRenderer(videoId, generalClientType.useAuth ? ClientType.ANDROID_VR : generalClientType);
            }
        }

        return parameters; // Return the original value since we are observing and not modifying.
    }

    private static void fetchLiveStreamRenderer(@NonNull String videoId, @NonNull ClientType clientType) {
        if (!videoId.equals(lastPlayerResponseVideoId)) {
            rendererFuture = submitOnBackgroundThread(() -> getLiveStreamRenderer(videoId, clientType));
            lastPlayerResponseVideoId = videoId;
        }
        // Block until the renderer fetch completes.
        // This is desired because if this returns without finishing the fetch
        // then video will start playback but the storyboard is not ready yet.
        getRenderer(true);
    }

    @Nullable
    private static LiveStreamRenderer getRenderer(boolean waitForCompletion) {
        Future<LiveStreamRenderer> future = rendererFuture;
        if (future != null) {
            try {
                if (waitForCompletion || future.isDone()) {
                    return future.get(20000, TimeUnit.MILLISECONDS); // Any arbitrarily large timeout.
                } // else, return null.
            } catch (TimeoutException ex) {
                // Logger.printDebug(() -> "Could not get renderer (get timed out)");
            } catch (ExecutionException | InterruptedException ex) {
                // Should never happen.
                // Logger.printException(() -> "Could not get renderer", ex);
            }
        }
        return null;
    }

    private static ClientType getClientTypeFromSettingsEnum(@NonNull SettingsEnum setting) {
        try {
            return ClientType.valueOf(setting.getString());
        } catch (IllegalArgumentException e) {
            // if the ClientType does not exist, reset the setting
            setting.saveValue(setting.getDefaultValue());
            return ClientType.valueOf((String) setting.getDefaultValue());
        }
    }
}