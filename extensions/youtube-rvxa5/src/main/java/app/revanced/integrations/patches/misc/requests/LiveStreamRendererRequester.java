package app.revanced.integrations.patches.misc.requests;

import static app.revanced.integrations.patches.misc.requests.PlayerRoutes.GET_LIVE_STREAM_RENDERER;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

import app.revanced.integrations.patches.misc.LiveStreamRenderer;
import app.revanced.integrations.patches.misc.requests.PlayerRoutes.ClientType;
import app.revanced.integrations.requests.Requester;
import app.revanced.integrations.utils.ReVancedUtils;

public class LiveStreamRendererRequester {

    private LiveStreamRendererRequester() {
    }

    @Nullable
    private static JSONObject fetchPlayerResponse(@NonNull String requestBody,
                                                  @NonNull ClientType clientType) {
        // final long startTime = System.currentTimeMillis();
        try {
            ReVancedUtils.verifyOffMainThread();
            Objects.requireNonNull(requestBody);

            final byte[] innerTubeBody = requestBody.getBytes(StandardCharsets.UTF_8);

            HttpURLConnection connection = PlayerRoutes.getPlayerResponseConnectionFromRoute(GET_LIVE_STREAM_RENDERER, clientType);
            connection.getOutputStream().write(innerTubeBody, 0, innerTubeBody.length);

            final int responseCode = connection.getResponseCode();
            if (responseCode == 200) return Requester.parseJSONObject(connection);

            // A non 200 response: the check is unavailable (nothing is shown to the user).
            connection.disconnect();
        } catch (SocketTimeoutException ex) {
            // The check is temporarily unavailable (API timed out).
        } catch (IOException ex) {
            // The check is temporarily unavailable.
        } catch (Exception ex) {
            // Logger.printException(() -> "Fetch livestreams failed", ex); // Should never happen.
        } finally {
            // Logger.printDebug(() -> "Request took: " + (System.currentTimeMillis() - startTime) + "ms");
        }

        return null;
    }

    private static boolean isPlayabilityStatusOk(@NonNull JSONObject playerResponse) {
        try {
            return playerResponse.getJSONObject("playabilityStatus").getString("status").equals("OK");
        } catch (JSONException e) {
            // Logger.printDebug(() -> "Failed to get playabilityStatus for response: " + playerResponse);
        }

        return false;
    }

    private static boolean isLiveStream(@NonNull JSONObject playerResponse) {
        try {
            return playerResponse.getJSONObject("videoDetails").getBoolean("isLive");
        } catch (JSONException e) {
            // Logger.printDebug(() -> "Failed to get videoDetails for response: " + playerResponse);
        }

        return false;
    }

    /**
     * The visitor id the server created for this request ({@code responseContext.visitorData}), or null.
     */
    @Nullable
    private static String readVisitorData(@NonNull JSONObject playerResponse) {
        try {
            String visitorData = playerResponse.getJSONObject("responseContext").getString("visitorData");
            return visitorData.isEmpty() ? null : visitorData;
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * "Sign in to confirm you're not a bot": the server did not like the request itself (it says nothing about the video),
     * for example a request without a visitor id. The text is localized, every language has "bot" / "robot" in it.
     */
    private static boolean isBotCheck(@NonNull JSONObject playerResponse) {
        try {
            JSONObject playabilityStatus = playerResponse.getJSONObject("playabilityStatus");
            if (!"LOGIN_REQUIRED".equals(playabilityStatus.getString("status"))) return false;

            return playabilityStatus.getString("reason").toLowerCase(Locale.ROOT).contains("bot");
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * Result of one check request.
     */
    private static final class CheckResult {
        @Nullable
        final LiveStreamRenderer renderer;
        final boolean botCheck;
        @Nullable
        final String visitorData;

        CheckResult(@Nullable LiveStreamRenderer renderer, boolean botCheck, @Nullable String visitorData) {
            this.renderer = renderer;
            this.botCheck = botCheck;
            this.visitorData = visitorData;
        }
    }

    /**
     * Fetches the liveStreamRenderer from the innerTubeBody.
     */
    @NonNull
    private static CheckResult check(@NonNull String videoId, @NonNull ClientType clientType) {
        final JSONObject playerResponse = fetchPlayerResponse(
                String.format(clientType.innerTubeBody, videoId),
                clientType
        );
        if (playerResponse == null) return new CheckResult(null, false, null);

        final boolean botCheck = isBotCheck(playerResponse);
        return new CheckResult(
                getLiveStreamRendererUsingResponse(videoId, playerResponse, clientType, botCheck),
                botCheck,
                readVisitorData(playerResponse)
        );
    }

    @Nullable
    private static LiveStreamRenderer getLiveStreamRendererUsingResponse(@NonNull String videoId,
                                                                         @NonNull JSONObject playerResponse,
                                                                         @NonNull ClientType clientType,
                                                                         boolean botCheck) {
        try {
            // Logger.printDebug(() -> "Parsing liveStreamRenderer from response: " + playerResponse);

            final String clientName = clientType.name();
            // A "not a bot" answer says nothing about the video: it is not declared unplayable because of it.
            final boolean isPlayabilityOk = botCheck || isPlayabilityStatusOk(playerResponse);
            final boolean isLiveStream = isLiveStream(playerResponse);

            LiveStreamRenderer renderer = new LiveStreamRenderer(
                    videoId,
                    clientName,
                    isPlayabilityOk,
                    isLiveStream
            );
            // Logger.printDebug(() -> "Fetched: " + renderer);

            return renderer;
        } catch (Exception e) {
            // Logger.printException(() -> "Failed to get liveStreamRenderer", e);
        }

        return null;
    }

    /**
     * The other logged out VR client, used when the first one could not be reached or did not say "playable".
     */
    private static ClientType otherCheckClient(@NonNull ClientType clientType) {
        return clientType == ClientType.ANDROID_VR_PICO ? ClientType.ANDROID_VR : ClientType.ANDROID_VR_PICO;
    }

    @Nullable
    public static LiveStreamRenderer getLiveStreamRenderer(@NonNull String videoId, @NonNull ClientType clientType) {
        Objects.requireNonNull(videoId);

        CheckResult result = check(videoId, clientType);

        if (result.botCheck && PlayerRoutes.visitorId.isEmpty() && result.visitorData != null) {
            // The app has not sent a request with a visitor id yet (first video after the start of the app), and the
            // server answered the request without one with "not a bot". Ask again with the visitor id the server just
            // created (the app replaces it as soon as it sends its own).
            PlayerRoutes.visitorId = result.visitorData;
            result = check(videoId, clientType);
        }

        LiveStreamRenderer renderer = result.renderer;
        if (renderer == null || !renderer.playabilityOk) {
            // The first client could not be reached (or returned an error), or it did not say "playable": ask another
            // logged out client before the video is declared unplayable (the answer decides which client plays it, and
            // the clients of the "Unplayable video" setting give fewer qualities).
            LiveStreamRenderer other = check(videoId, otherCheckClient(clientType)).renderer;
            if (other != null && (renderer == null || other.playabilityOk)) {
                renderer = other;
            }
        }

        return renderer;
    }
}
