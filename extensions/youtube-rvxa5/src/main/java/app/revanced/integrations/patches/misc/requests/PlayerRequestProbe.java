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
 * read from one log. First log (Android TV): with the authorization of the app -> 400, without it -> 200, whatever the other
 * headers were. So the variants now all keep the authorization and change one thing of the body or of the headers
 * (client fields of the body, client headers, user agent), and an answer (protobuf or JSON) is described by its
 * playability status and the size of its fields (a big field = stream data), so that a 200 can be told from a
 * "sign in" answer.
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

    /**
     * A change of the JSON body of the request.
     */
    private interface BodyEdit {
        void apply(JSONObject root, JSONObject client) throws Exception;
    }

    private static void runAll(String key, Map<String, String> headers, String url, byte[] body, String version,
                               @Nullable String oldVersion) {
        probe(key, "as-sent", headers, url, body);

        // Without the account: what does the server answer (protobuf, then JSON)?
        Map<String, String> noAuth = without(headers, "Authorization");
        probe(key, "no-Authorization", noAuth, url, body);
        probe(key, "no-Authorization, JSON answer", noAuth, stripQuery(url), body);

        // With the account, one change at a time.
        probe(key, "auth, JSON answer", headers, stripQuery(url), body);
        probe(key, "auth, no gmscoreVersionCode/timeZone/utcOffsetMinutes", headers, url, edit(body, new BodyEdit() {
            @Override
            public void apply(JSONObject root, JSONObject client) {
                client.remove("gmscoreVersionCode");
                client.remove("timeZone");
                client.remove("utcOffsetMinutes");
            }
        }));
        probe(key, "auth, no device fields", headers, url, edit(body, new BodyEdit() {
            @Override
            public void apply(JSONObject root, JSONObject client) {
                client.remove("deviceMake");
                client.remove("deviceModel");
                client.remove("osName");
                client.remove("osVersion");
                client.remove("androidSdkVersion");
                client.remove("gmscoreVersionCode");
            }
        }));
        probe(key, "auth, androidSdkVersion as text", headers, url, edit(body, new BodyEdit() {
            @Override
            public void apply(JSONObject root, JSONObject client) throws Exception {
                if (client.has("androidSdkVersion")) {
                    client.put("androidSdkVersion", String.valueOf(client.get("androidSdkVersion")));
                }
            }
        }));
        probe(key, "auth, no hl/gl", headers, url, edit(body, new BodyEdit() {
            @Override
            public void apply(JSONObject root, JSONObject client) {
                client.remove("hl");
                client.remove("gl");
            }
        }));
        probe(key, "auth, no contentCheckOk/racyCheckOk", headers, url, edit(body, new BodyEdit() {
            @Override
            public void apply(JSONObject root, JSONObject client) {
                root.remove("contentCheckOk");
                root.remove("racyCheckOk");
            }
        }));
        probe(key, "auth, user.lockedSafetyMode=false", headers, url, edit(body, new BodyEdit() {
            @Override
            public void apply(JSONObject root, JSONObject client) throws Exception {
                JSONObject user = new JSONObject();
                user.put("lockedSafetyMode", false);
                root.getJSONObject("context").put("user", user);
            }
        }));

        probe(key, "auth, no X-YouTube-Client-Name/Version",
                without(without(headers, "X-YouTube-Client-Name"), "X-YouTube-Client-Version"), url, body);
        probe(key, "auth, only Content-Type/User-Agent/Authorization", only(headers,
                "Content-Type", "User-Agent", "Authorization"), url, body);
        Map<String, String> authUser = new LinkedHashMap<>(headers);
        authUser.put("X-Goog-AuthUser", "0");
        probe(key, "auth, + X-Goog-AuthUser: 0", authUser, url, body);

        String userAgent = headers.get("User-Agent");
        if (userAgent != null) {
            // The user agent in the format of rvx a6: package/version(Linux; U; Android 14; en_US; Model Build/ID) gzip
            Map<String, String> a6 = new LinkedHashMap<>(headers);
            a6.put("User-Agent", userAgent.replace(" (Linux", "(Linux").replace("; Build/", " Build/") + " gzip");
            probe(key, "auth, user agent of rvx a6", a6, url, body);
        }

        if (oldVersion != null && !oldVersion.equals(version)) {
            Map<String, String> replaced = new LinkedHashMap<>();
            for (Map.Entry<String, String> header : headers.entrySet()) {
                String value = header.getValue();
                replaced.put(header.getKey(), value == null ? null : value.replace(version, oldVersion));
            }
            String text = new String(body, StandardCharsets.UTF_8).replace(version, oldVersion);
            probe(key, "auth, version " + oldVersion, replaced, url, text.getBytes(StandardCharsets.UTF_8));
        }

        LogHelper.info(PlayerRequestProbe.class, "Probe " + key + ": done");
    }

    private static byte[] edit(byte[] body, BodyEdit edit) {
        try {
            JSONObject root = new JSONObject(new String(body, StandardCharsets.UTF_8));
            JSONObject client = root.getJSONObject("context").getJSONObject("client");
            edit.apply(root, client);
            return root.toString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            LogHelper.info(PlayerRequestProbe.class, "Probe: body not changed: " + ex);
            return body;
        }
    }

    private static Map<String, String> only(Map<String, String> headers, String... names) {
        Map<String, String> copy = new LinkedHashMap<>();
        for (Map.Entry<String, String> header : headers.entrySet()) {
            for (String name : names) {
                if (header.getKey() != null && header.getKey().equalsIgnoreCase(name)) {
                    copy.put(header.getKey(), header.getValue());
                }
            }
        }
        return copy;
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
            return "answer " + data.length + " bytes (" + type + ") " + describeProto(data);
        }

        try {
            JSONObject answer = new JSONObject(new String(data, StandardCharsets.UTF_8));
            String text = "answer json " + data.length + " bytes";
            if (answer.has("playabilityStatus")) {
                JSONObject status = answer.getJSONObject("playabilityStatus");
                text += " playability=" + status.optString("status", "?") + " / " + status.optString("reason", "-");
            }
            if (answer.has("streamingData")) {
                JSONObject streaming = answer.getJSONObject("streamingData");
                JSONArray adaptive = streaming.optJSONArray("adaptiveFormats");
                JSONArray formats = streaming.optJSONArray("formats");
                text += " adaptiveFormats=" + (adaptive == null ? 0 : adaptive.length())
                        + " formats=" + (formats == null ? 0 : formats.length());
            } else {
                text += " (no streamingData)";
            }
            return text;
        } catch (Exception ex) {
            return "answer " + data.length + " bytes (" + type + ", not readable: " + ex.getClass().getSimpleName() + ")";
        }
    }

    /**
     * The top level fields of a protobuf answer (field number: size in bytes, or value) and what is inside field 2, the
     * playability status (numbers = values, s = texts). A field of several KB is the stream data.
     */
    private static String describeProto(byte[] data) {
        StringBuilder text = new StringBuilder("fields[");
        int[] pos = {0};
        int listed = 0;
        String playability = "";

        while (pos[0] < data.length && listed < 16) {
            long tag = readVarint(data, pos);
            if (tag < 0) break;

            int wireType = (int) (tag & 7);
            long field = tag >>> 3;
            if (wireType == 0) {
                long value = readVarint(data, pos);
                if (value < 0) break;
                text.append(listed == 0 ? "" : " ").append(field).append('=').append(value);
            } else if (wireType == 2) {
                long length = readVarint(data, pos);
                if (length < 0 || length > data.length - pos[0]) break;
                text.append(listed == 0 ? "" : " ").append(field).append(':').append(length);
                if (field == 2 && playability.isEmpty()) {
                    playability = describePlayability(data, pos[0], (int) length);
                }
                pos[0] += (int) length;
            } else if (wireType == 1) {
                pos[0] += 8;
                text.append(listed == 0 ? "" : " ").append(field).append(":fixed64");
            } else if (wireType == 5) {
                pos[0] += 4;
                text.append(listed == 0 ? "" : " ").append(field).append(":fixed32");
            } else {
                break;
            }
            listed++;
        }

        text.append(']');
        return text + (playability.isEmpty() ? "" : " playability{" + playability + "}");
    }

    private static String describePlayability(byte[] data, int start, int length) {
        byte[] inner = new byte[length];
        System.arraycopy(data, start, inner, 0, length);

        StringBuilder text = new StringBuilder();
        int[] pos = {0};
        int listed = 0;
        while (pos[0] < inner.length && listed < 10) {
            long tag = readVarint(inner, pos);
            if (tag < 0) break;

            int wireType = (int) (tag & 7);
            long field = tag >>> 3;
            if (wireType == 0) {
                long value = readVarint(inner, pos);
                if (value < 0) break;
                text.append(listed == 0 ? "" : " ").append('v').append(field).append('=').append(value);
            } else if (wireType == 2) {
                long size = readVarint(inner, pos);
                if (size < 0 || size > inner.length - pos[0]) break;
                String value = new String(inner, pos[0], (int) size, StandardCharsets.UTF_8);
                boolean printable = !value.isEmpty();
                for (int i = 0; i < value.length(); i++) {
                    if (value.charAt(i) < 32) {
                        printable = false;
                        break;
                    }
                }
                text.append(listed == 0 ? "" : " ").append('s').append(field);
                text.append(printable ? "=" + (value.length() > 120 ? value.substring(0, 120) + "..." : value)
                        : ":" + size + "b");
                pos[0] += (int) size;
            } else {
                break;
            }
            listed++;
        }
        return text.toString();
    }

    /**
     * The readable parts of a (small) binary answer: runs of at least 6 printable characters.
     */
    private static String printableRuns(byte[] data) {
        StringBuilder out = new StringBuilder();
        StringBuilder run = new StringBuilder();
        for (int i = 0; i <= data.length && out.length() < 300; i++) {
            int c = i < data.length ? data[i] & 0xff : 0;
            if (c >= 32 && c < 127) {
                run.append((char) c);
            } else {
                if (run.length() >= 6) out.append(out.length() == 0 ? "" : " | ").append(run);
                run.setLength(0);
            }
        }
        return out.toString();
    }

    private static long readVarint(byte[] data, int[] pos) {
        long result = 0;
        for (int shift = 0; shift < 64; shift += 7) {
            if (pos[0] >= data.length) return -1;
            int b = data[pos[0]++] & 0xff;
            result |= (long) (b & 0x7f) << shift;
            if ((b & 0x80) == 0) return result;
        }
        return -1;
    }

    private static String describeError(byte[] data, @Nullable String type) {
        if (!looksLikeJson(data)) {
            return "error " + data.length + " bytes (" + type + ") " + describeProto(data) + " text=\"" + printableRuns(data) + "\"";
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
