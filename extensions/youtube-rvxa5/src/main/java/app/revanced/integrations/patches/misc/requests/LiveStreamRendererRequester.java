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
import java.util.Objects;

import app.revanced.integrations.patches.misc.LiveStreamRenderer;
import app.revanced.integrations.patches.misc.requests.PlayerRoutes.ClientType;
import app.revanced.integrations.requests.Requester;
import app.revanced.integrations.utils.LogHelper;
import app.revanced.integrations.utils.ReVancedUtils;

public class LiveStreamRendererRequester {

    private LiveStreamRendererRequester() {
    }

    private static void handleConnectionError(@NonNull String toastMessage, @Nullable Exception ex) {
        // Only written to the log (tag "revanced"): the failure of this check is not shown to the user.
        LogHelper.info(LiveStreamRendererRequester.class, toastMessage);
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

            // Always show a toast for this, as a non 200 response means something is broken.
            handleConnectionError("Fetch livestreams not available: HTTP " + responseCode + " (" + clientType.name() + ")", null);
            connection.disconnect();
        } catch (SocketTimeoutException ex) {
            handleConnectionError("Fetch livestreams temporarily not available (API timed out)", ex);
        } catch (IOException ex) {
            handleConnectionError("Fetch livestreams temporarily not available: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            // Logger.printException(() -> "Fetch livestreams failed", ex); // Should never happen.
        } finally {
            // Logger.printDebug(() -> "Request took: " + (System.currentTimeMillis() - startTime) + "ms");
        }

        return null;
    }

    /**
     * "status / reason" of the playability status, for the log only.
     */
    private static String describePlayabilityStatus(@NonNull JSONObject playerResponse) {
        try {
            JSONObject playabilityStatus = playerResponse.getJSONObject("playabilityStatus");

            String status = "?";
            String reason = "-";
            try {
                status = playabilityStatus.getString("status");
            } catch (JSONException ignored) {
            }
            try {
                reason = playabilityStatus.getString("reason");
            } catch (JSONException ignored) {
            }
            return status + " / " + reason;
        } catch (JSONException e) {
            return "no playabilityStatus";
        }
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
     * Fetches the liveStreamRenderer from the innerTubeBody.
     *
     * @return LiveStreamRenderer or null if playabilityStatus is not OK.
     */
    @Nullable
    private static LiveStreamRenderer getLiveStreamRendererUsingBody(@NonNull String videoId,
                                                                     @NonNull ClientType clientType) {
        final JSONObject playerResponse = fetchPlayerResponse(
                String.format(clientType.innerTubeBody, videoId),
                clientType
        );
        if (playerResponse != null)
            return getLiveStreamRendererUsingResponse(videoId, playerResponse, clientType);

        return null;
    }

    @Nullable
    private static LiveStreamRenderer getLiveStreamRendererUsingResponse(@NonNull String videoId,
                                                                         @NonNull JSONObject playerResponse,
                                                                         @NonNull ClientType clientType) {
        try {
            // Logger.printDebug(() -> "Parsing liveStreamRenderer from response: " + playerResponse);

            final String clientName = clientType.name();
            final boolean isPlayabilityOk = isPlayabilityStatusOk(playerResponse);
            final boolean isLiveStream = isLiveStream(playerResponse);

            // Kept in the log: a check that wrongly says "not playable" sends the video to the client of the
            // "Unplayable video" setting (with fewer qualities).
            LogHelper.info(LiveStreamRendererRequester.class, "Playability check: video=" + videoId
                    + " client=" + clientName + " status=" + describePlayabilityStatus(playerResponse)
                    + " live=" + isLiveStream);

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

        LiveStreamRenderer renderer = getLiveStreamRendererUsingBody(videoId, clientType);
        if (renderer == null || !renderer.playabilityOk) {
            // The first client could not be reached (or returned an error), or it did not say "playable": ask another
            // logged out client before the video is declared unplayable (the answer decides which client plays it, and
            // the clients of the "Unplayable video" setting give fewer qualities).
            LiveStreamRenderer other = getLiveStreamRendererUsingBody(videoId, otherCheckClient(clientType));
            if (other != null && (renderer == null || other.playabilityOk)) {
                renderer = other;
            }
        }

        return renderer;
    }
}
