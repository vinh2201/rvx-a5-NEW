package app.revanced.integrations.patches.misc.requests;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import app.revanced.integrations.utils.LogHelper;
import app.revanced.integrations.utils.ReVancedUtils;

/**
 * Diagnostics only (no effect on playback): when a player request that needs the account (Android TV, Creator) is sent,
 * the same request is sent once more in a few variants from a background thread and the HTTP status of each one is
 * written to the log (tag "revanced"), so that the reason of an HTTP 400 "Request contains an invalid argument" can be
 * read from one log: the account, the client version, one of the headers or the {@code alt=proto} parameter.
 * <p>
 * Nothing secret is logged: only the name of the variant, the status and the error message / status / details of the
 * server (or the playability status of an answer).
 */
public final class PlayerRequestProbe {
    private static final int TIMEOUT_MILLISECONDS = 10 * 1000;
    private static final int MAX_BODY_BYTES = 4 * 1024 * 1024;

    private static volatile String lastKey = "";

    private PlayerRequestProbe() {
    }

    /**
     * Sends the variants once per key (client + video).
     *
     * @param headers    headers of the request that was sent (a copy is used).
     * @param url        URL of the request that was sent.
     * @param body       JSON body of the request that was sent.
     * @param version    client version of the request.
     * @param oldVersion another client version to try, or null.
     */
    public static void runOnce(@NonNull String key, @NonNull Map<String, String> headers, @NonNull String url,
                               @NonNull byte[] body, @NonNull String version, @Nullable String oldVersion) {
        if (key.equals(lastKey)) return;
        lastKey = key;

        final Map<String, String> headersCopy = new LinkedHashMap<>(headers);
        ReVancedUtils.submitOnBackgroundThread(() -> {
            try {
                runAll(key, headersCopy, url, body, version, oldVersion);
            } catch (Throwable ex) {
                LogHelper.info(PlayerRequestProbe.class, "Probe failed: " + ex);
            }
            return null;
        });
    }

    private static void runAll(String key, Map<String, String> headers, String url, byte[] body, String version,
                               @Nullable String oldVersion) {
        probe(key, "as-sent", headers, url, body);
        probe(key, "no-Authorization", without(headers, "Authorization"), url, body);
        probe(key, "no-API-FORMAT-VERSION", without(headers, "X-GOOG-API-FORMAT-VERSION"), url, body);
        probe(key, "no-Visitor-Id", without(headers, "X-Goog-Visitor-Id"), url, body);
        probe(key, "no-alt-proto", headers, stripQuery(url), body);
        probe(key, "bare(no Authorization/Visitor-Id/API-FORMAT-VERSION)",
                without(without(without(headers, "Authorization"), "X-GOOG-API-FORMAT-VERSION"), "X-Goog-Visitor-Id"),
                url, body);

        if (oldVersion != null && !oldVersion.equals(version)) {
            Map<String, String> replaced = new LinkedHashMap<>();
            for (Map.Entry<String, String> header : headers.entrySet()) {
                String value = header.getValue();
                replaced.put(header.getKey(), value == null ? null : value.replace(version, oldVersion));
            }
            String text = new String(body, StandardCharsets.UTF_8).replace(version, oldVersion);
            probe(key, "version-" + oldVersion, replaced, url, text.getBytes(StandardCharsets.UTF_8));
        }

        LogHelper.info(PlayerRequestProbe.class, "Probe " + key + ": done");
    }

    private static Map<String, String> without(Map<String, String> headers, String name) {
        Map<String, String> copy = new LinkedHashMap<>();
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if (header.getKey() != null && !header.getKey().equalsIgnoreCase(name)) {
                copy.put(header.getKey(), header.getValue());
            }
        }
        return copy;
    }

    private static String stripQuery(String url) {
        int index = url.indexOf('?');
        return index < 0 ? url : url.substring(0, index);
    }

    private static void probe(String key, String variant, Map<String, String> headers, String url, byte[] body) {
        String summary;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setConnectTimeout(TIMEOUT_MILLISECONDS);
            connection.setReadTimeout(TIMEOUT_MILLISECONDS);
            for (Map.Entry<String, String> header : headers.entrySet()) {
                if (header.getKey() != null && header.getValue() != null) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
            }
            connection.setFixedLengthStreamingMode(body.length);
            connection.getOutputStream().write(body);

            int code = connection.getResponseCode();
            InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
            byte[] data = readAll(stream);
            String type = connection.getContentType();
            connection.disconnect();

            summary = "HTTP " + code + " " + (code == 200 ? describeAnswer(data, type) : describeError(data, type));
        } catch (Exception ex) {
            summary = "failed: " + ex;
        }

        LogHelper.info(PlayerRequestProbe.class, "Probe " + key + " [" + variant + "]: " + summary);
    }

    private static byte[] readAll(@Nullable InputStream stream) throws Exception {
        if (stream == null) return new byte[0];

        try (InputStream in = stream) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0 && out.size() < MAX_BODY_BYTES) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    private static boolean looksLikeJson(byte[] data) {
        for (byte b : data) {
            if (b == ' ' || b == '\n' || b == '\r' || b == '\t') continue;
            return b == '{';
        }
        return false;
    }

    private static String describeAnswer(byte[] data, @Nullable String type) {
        if (!looksLikeJson(data)) {
            return "answer " + data.length + " bytes (" + type + ")";
        }

        try {
            JSONObject answer = new JSONObject(new String(data, StandardCharsets.UTF_8));
            String text = "answer json";
            if (answer.has("playabilityStatus")) {
                JSONObject status = answer.getJSONObject("playabilityStatus");
                text += " playability=" + status.optString("status", "?") + " / " + status.optString("reason", "-");
            }
            if (answer.has("streamingData")) {
                JSONArray formats = answer.getJSONObject("streamingData").optJSONArray("adaptiveFormats");
                text += " adaptiveFormats=" + (formats == null ? 0 : formats.length());
            }
            return text;
        } catch (Exception ex) {
            return "answer " + data.length + " bytes (" + type + ", not readable: " + ex.getClass().getSimpleName() + ")";
        }
    }

    private static String describeError(byte[] data, @Nullable String type) {
        if (!looksLikeJson(data)) {
            return "error " + data.length + " bytes (" + type + ")";
        }

        try {
            JSONObject error = new JSONObject(new String(data, StandardCharsets.UTF_8)).getJSONObject("error");
            StringBuilder text = new StringBuilder("error status=").append(error.optString("status", "?"))
                    .append(" message=").append(error.optString("message", "-"));
            if (error.has("details")) {
                String details = String.valueOf(error.get("details"));
                text.append(" details=").append(details.length() > 400 ? details.substring(0, 400) + "..." : details);
            }
            return text.toString();
        } catch (Exception ex) {
            return "error " + data.length + " bytes (" + type + ", not readable: " + ex.getClass().getSimpleName() + ")";
        }
    }
}
