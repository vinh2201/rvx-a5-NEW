package app.revanced.integrations.patches.misc.requests;

import static app.revanced.integrations.utils.StringRef.str;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Locale;

import app.revanced.integrations.requests.Requester;
import app.revanced.integrations.requests.Route;

/**
 * Routes and the client table used by "Spoof video streams".
 *
 * <p>
 * Client table: taken from ReVanced patches (v22) and Morphe (legacy), the values of rvx a6 were compared as well.
 * Only clients that return direct (non SABR, no PoToken, no JS player) DASH/progressive stream URLs are listed, since this old
 * app can play nothing else. Clients that stopped working were removed on purpose:
 * <ul>
 *     <li>IOS (19.20.2): needs a PoToken now / returns SABR streams only. Removed.</li>
 *     <li>ANDROID_TESTSUITE (1.9), ANDROID, ANDROID_EMBEDDED_PLAYER, WEB, TVHTML5_SIMPLY_EMBEDDED_PLAYER: removed
 *     (no streams without a signature / n-parameter solver).</li>
 * </ul>
 * Settings that still contain a removed value are reset to the default by {@code SpoofClientPatch}.
 */
public final class PlayerRoutes {
    public static final Route.CompiledRoute GET_LIVE_STREAM_RENDERER = new Route(
            Route.Method.POST,
            "player" +
                    "?fields=playabilityStatus.status," +
                    "videoDetails.isLive" +
                    "&prettyPrint=false"
    ).compile();

    private static final String YT_API_URL = "https://youtubei.googleapis.com/youtubei/v1/";

    /**
     * TCP connection and HTTP read timeout
     */
    private static final int CONNECTION_TIMEOUT_MILLISECONDS = 10 * 1000; // 10 Seconds.

    private PlayerRoutes() {
    }

    /**
     * @param sdk 0 if not applicable (the field is not sent).
     */
    private static String createInnerTubeBody(String clientName, String version, String make, String model,
                                              String osName, String osVersion, int sdk) {
        JSONObject innerTubeBody = new JSONObject();

        try {
            JSONObject context = new JSONObject();

            Locale locale = Locale.getDefault();
            String language = locale.getLanguage();
            String country = locale.getCountry();

            JSONObject client = new JSONObject();
            client.put("deviceMake", make);
            client.put("deviceModel", model);
            client.put("clientName", clientName);
            client.put("clientVersion", version);
            client.put("osName", osName);
            client.put("osVersion", osVersion);
            if (sdk > 0) {
                client.put("androidSdkVersion", sdk);
            }
            if (language != null && !language.isEmpty()) {
                client.put("hl", language);
            }
            if (country != null && !country.isEmpty()) {
                client.put("gl", country);
            }

            context.put("client", client);

            innerTubeBody.put("context", context);
            innerTubeBody.put("contentCheckOk", true);
            innerTubeBody.put("racyCheckOk", true);
            innerTubeBody.put("videoId", "%s");
        } catch (JSONException e) {
            // Logger.printException(() -> "Failed to create innerTubeBody", e);
        }

        return innerTubeBody.toString();
    }

    /**
     * Same user agent format as ReVanced (v22):
     * {@code package/version (Linux; U; Android osVersion; locale; model; Build/buildId)}.
     */
    private static String createAndroidUserAgent(String packageName, String version, String osVersion,
                                                 String model, String buildId) {
        return packageName + "/" + version
                + " (Linux; U; Android " + osVersion
                + "; " + Locale.getDefault()
                + "; " + model
                + "; Build/" + buildId + ") gzip";
    }

    public static HttpURLConnection getPlayerResponseConnectionFromRoute(Route.CompiledRoute route,
                                                                         ClientType clientType) throws IOException {
        HttpURLConnection connection = Requester.getConnectionFromCompiledRoute(YT_API_URL, route);

        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", clientType.userAgent);
        // "Client-Name" is the client type id.
        connection.setRequestProperty("X-YouTube-Client-Name", String.valueOf(clientType.id));
        connection.setRequestProperty("X-YouTube-Client-Version", clientType.version);

        connection.setUseCaches(false);
        connection.setDoOutput(true);

        connection.setConnectTimeout(CONNECTION_TIMEOUT_MILLISECONDS);
        connection.setReadTimeout(CONNECTION_TIMEOUT_MILLISECONDS);
        return connection;
    }

    /**
     * Do not rename the constants: the names are stored in the settings and are used to build the name of the string
     * resource of the entry ({@code revanced_spoof_client_options_entry_<name in lower case>}).
     */
    public enum ClientType {
        /**
         * ReVanced v22 ANDROID_VR_1_61_48 ("Android VR 1.65").
         * Video not playable: Kids, Paid, Movie, Private, Age-restricted. Can only be used when logged out.
         * AV1 codec available.
         * <a href="https://dumps.tadiphone.dev/dumps/oculus/eureka">Meta Quest 3</a>
         */
        ANDROID_VR(28, "ANDROID_VR",
                "com.google.android.apps.youtube.vr.oculus", "1.65.10",
                "Oculus", "Quest 3", "Android", "12", 32, "UP1A.231005.007.A1",
                false),
        /**
         * ReVanced v22 ANDROID_VR_1_43_32.
         * Same as {@link #ANDROID_VR}, but an older version that does not use AV1.
         */
        ANDROID_VR_1_43(28, "ANDROID_VR",
                "com.google.android.apps.youtube.vr.oculus", "1.43.32",
                "Oculus", "Quest 3", "Android", "12", 32, "UP1A.231005.007.A1",
                false),
        /**
         * Morphe ANDROID_VR_DASH ("Android VR Downgraded"): PICO 4 (ByteDance).
         */
        ANDROID_VR_PICO(28, "ANDROID_VR",
                "com.google.android.apps.youtube.vr.pico", "1.64.34",
                "Pico", "A8110", "Android", "10", 29, "5.13.7",
                false),
        /**
         * Morphe ANDROID_XR_DASH ("Android XR Downgraded"): Samsung Galaxy XR.
         * Video not playable: Kids, Paid, Movie, Private, Age-restricted. Can only be used when logged out.
         */
        ANDROID_XR(28, "ANDROID_VR",
                "com.google.android.apps.youtube.xr", "1.69.27",
                "Samsung", "SM-I610", "Android", "14", 34, "UML1.250710.002.A1",
                false),
        /**
         * ReVanced v22 / Morphe ANDROID_CREATOR ("Android Studio").
         * Cannot play livestreams and lacks HDR, but can play videos with music and labeled "for children".
         * Requires login.
         */
        ANDROID_CREATOR(14, "ANDROID_CREATOR",
                "com.google.android.apps.youtube.creator", "26.10.000",
                "Google", "Pixel 10 Pro XL", "Android", "16", 36, "BD3A.251005.003.W3",
                true),
        /**
         * rvx a6 ANDROID_UNPLUGGED ("Android TV", Google TV Streamer).
         * Requires login.
         */
        ANDROID_UNPLUGGED(29, "ANDROID_UNPLUGGED",
                "com.google.android.apps.youtube.unplugged", "10.30.1",
                "Google", "Google TV Streamer", "Android", "14", 34, "UTTK.241210.003",
                true);

        public final String friendlyName;
        public final int id;
        public final String clientName;
        public final String version;
        public final String make;
        public final String model;
        public final String osVersion;
        /**
         * Android SDK version.
         */
        public final int sdk;
        public final String innerTubeBody;
        public final String userAgent;
        /**
         * If the authorization of the app (when logged in) is sent with the request.
         */
        public final boolean useAuth;

        ClientType(int id, String clientName, String packageName, String version,
                   String make, String model, String osName, String osVersion, int sdk, String buildId,
                   boolean useAuth) {
            this.friendlyName = str("revanced_spoof_client_options_entry_" + name().toLowerCase(Locale.ENGLISH));
            this.id = id;
            this.clientName = clientName;
            this.version = version;
            this.make = make;
            this.model = model;
            this.osVersion = osVersion;
            this.sdk = sdk;
            this.innerTubeBody = createInnerTubeBody(clientName, version, make, model, osName, osVersion, sdk);
            this.userAgent = createAndroidUserAgent(packageName, version, osVersion, model, buildId);
            this.useAuth = useAuth;
        }
    }
}
