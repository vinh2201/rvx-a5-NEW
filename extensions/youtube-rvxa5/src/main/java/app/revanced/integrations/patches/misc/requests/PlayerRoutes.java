package app.revanced.integrations.patches.misc.requests;

import static app.revanced.integrations.utils.StringRef.str;

import android.os.Build;

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
 * Client table: taken from ReVanced patches (v22) and community, the values of rvx a6 were compared as well.
 * Only clients that return direct (non SABR, no PoToken, no JS player) DASH/progressive stream URLs are listed, since this old
 * app can play nothing else. Clients that stopped working were removed on purpose:
 * The values of "Android VR", "Android Studio", "visionOS" and "Android No SDK" were compared with rvx (inotia00).
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

    private static final String USER_AGENT_TV_PS4 =
            "Mozilla/5.0 (PS4; Leanback Shell) Gecko/20100101 Firefox/65.0 LeanbackShell/01.00.01.75 Sony PS4/ (PS4, , no, CH)";
    private static final String USER_AGENT_TV_TIZEN =
            "Mozilla/5.0 (SMART-TV; Linux; Tizen 2.4.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/2.4.0 TV Safari/538.1";

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
     *
     * <p>
     * Fields that are {@code null} (or {@code sdk < 0}) mean "use the value of the real device": the request keeps what the
     * app itself would send. {@code sdk == 0} means "not applicable" (non Android client).
     */
    public enum ClientType {
        /**
         * ReVanced v22 ANDROID_VR_1_61_48 ("Android VR 1.65"), the exact values of the official ReVanced (Android 12, SDK 32).
         * Video not playable: Kids, Paid, Movie, Private, Age-restricted. Can only be used when logged out.
         * AV1 codec available.
         * <a href="https://dumps.tadiphone.dev/dumps/oculus/eureka">Meta Quest 3</a>
         */
        ANDROID_VR(28, "ANDROID_VR",
                "com.google.android.apps.youtube.vr.oculus", "1.65.10",
                "Oculus", "Quest 3", "Android", "12", 32, "UP1A.231005.007.A1",
                false),
        /**
         * rvx ANDROID_VR: same client and version as {@link #ANDROID_VR}, but the device reports Android 14 (SDK 34).
         */
        ANDROID_VR_A14(28, "ANDROID_VR",
                "com.google.android.apps.youtube.vr.oculus", "1.65.10",
                "Oculus", "Quest 3", "Android", "14", 34, "UP1A.231005.007.A1",
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
         * ANDROID_VR_DASH of community ("Android VR Downgraded"): PICO 4 (ByteDance).
         */
        ANDROID_VR_PICO(28, "ANDROID_VR",
                "com.google.android.apps.youtube.vr.pico", "1.64.34",
                "Pico", "A8110", "Android", "10", 29, "5.13.7",
                false),
        /**
         * ANDROID_XR_DASH of community ("Android XR Downgraded"): Samsung Galaxy XR.
         * Video not playable: Kids, Paid, Movie, Private, Age-restricted. Can only be used when logged out.
         */
        ANDROID_XR(28, "ANDROID_VR",
                "com.google.android.apps.youtube.xr", "1.69.27",
                "Samsung", "SM-I610", "Android", "14", 34, "UML1.250710.002.A1",
                false),
        /**
         * ReVanced v22 / community ANDROID_CREATOR ("Android Studio").
         * Cannot play livestreams (HLS only) and lacks HDR, but can play videos with music and labeled "for children".
         * Requires login.
         */
        ANDROID_CREATOR(14, "ANDROID_CREATOR",
                "com.google.android.apps.youtube.creator", "26.10.000",
                "Google", "Pixel 10 Pro XL", "Android", "16", 36, "BD3A.251005.003.W3",
                true),
        /**
         * rvx a6 ANDROID_UNPLUGGED ("Android TV", Google TV Streamer).
         * Version of the YouTube TV app (com.google.android.apps.youtube.unplugged): 10.46.1, the newest one found on
         * APKMirror on 2026-11-19 (it was 10.30.1 before). The device values (Google TV Streamer, Android 14) are unchanged.
         * Requires login.
         */
        ANDROID_UNPLUGGED(29, "ANDROID_UNPLUGGED",
                "com.google.android.apps.youtube.unplugged", "10.46.1",
                "Google", "Google TV Streamer", "Android", "14", 34, "UTTK.241210.003",
                true),
        /**
         * rvx ANDROID_NO_SDK ("Android No SDK"): the default client of rvx. The Android client (id 3) with a recent version,
         * without device make/model and without Android SDK version (the field is sent as 0).
         * Video not playable: none. Logged out. The best candidate for livestreams.
         */
        ANDROID_NO_SDK(3, "ANDROID",
                "com.google.android.youtube", "20.26.46",
                "", "", "Android", null, 0, null,
                false),
        /**
         * Same as {@link #ANDROID_NO_SDK}, but the account of the app is sent (age-restricted videos).
         */
        ANDROID_NO_SDK_AUTH(3, "ANDROID",
                "com.google.android.youtube", "20.26.46",
                "", "", "Android", null, 0, null,
                true),
        /**
         * ReVanced v22 ANDROID_REEL: the plain Android client (the one this app is) reporting a recent version.
         * The device values of the real device are kept. Logged in. Meant for Shorts.
         * Video not playable: Paid, Movie, Private, Age-restricted.
         */
        ANDROID_REEL(3, "ANDROID",
                "com.google.android.youtube", "20.26.46",
                null, null, "Android", null, -1, Build.ID,
                true),
        /**
         * ReVanced v22 ANDROID_REEL_NO_AUTH: same as {@link #ANDROID_REEL}, but without the account.
         */
        ANDROID_REEL_NO_AUTH(3, "ANDROID",
                "com.google.android.youtube", "20.26.46",
                null, null, "Android", null, -1, Build.ID,
                false),
        /**
         * VISIONOS_1_03 of community (internal YouTube client of an unreleased visionOS app, ReVanced v22 "visionOS").
         * Apple client: the OS name is sent as "visionOS" and no Android SDK version is sent.
         * Logged out. Video not playable: Kids, Paid, Movie, Private, Age-restricted. May stop working at any time.
         */
        VISIONOS(101, "VISIONOS", "1.03",
                "Apple", "RealityDevice17,1", "visionOS", "26.6.1", 0,
                "com.google.visionosyoutube/1.03 (RealityDevice17,1; U; CPU visionOS 26_6_1 like Mac OS X; en_US) gzip",
                false),
        /**
         * VISIONOS_1_02 of community: older visionOS app version (no AV1).
         */
        VISIONOS_1_02(101, "VISIONOS", "1.02",
                "Apple", "RealityDevice14,1", "visionOS", "26.6.1", 0,
                "com.google.visionosyoutube/1.02 (RealityDevice14,1; U; CPU visionOS 26_6_1 like Mac OS X; en_US) gzip",
                false),
        /**
         * rvx / ReVanced v22 VISIONOS: OS version 26.5.23O471 and the user agent of Safari.
         */
        VISIONOS_RVX(101, "VISIONOS", "1.03",
                "Apple", "RealityDevice17,1", "visionOS", "26.5.23O471", 0,
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15",
                false),
        /**
         * TV_SABR of community / rvx TV (TVHTML5, game console). EXPERIMENTAL: this client needs the player JavaScript
         * (signature / "n" parameter) and may need a PoToken, so most videos will not play. Livestreams: not playable.
         */
        TV(7, "TVHTML5", "7.20260707.07.00",
                "Sony", "PS4", "PlayStation 4", "", 0,
                USER_AGENT_TV_PS4,
                false),
        /**
         * TV_DASH of community / rvx TV_LEGACY (TVHTML5 5.20150304, Samsung Tizen). EXPERIMENTAL: needs the player JavaScript.
         * Community uses it for livestreams.
         */
        TV_LEGACY(7, "TVHTML5", "5.20150304",
                "Samsung", "SmartTV", "Tizen", "2.4.0", 0,
                USER_AGENT_TV_TIZEN,
                false),
        /**
         * Community / rvx TV_SIMPLY (TVHTML5_SIMPLY). EXPERIMENTAL: needs the player JavaScript and a PoToken when logged out.
         */
        TV_SIMPLY(75, "TVHTML5_SIMPLY", "1.1",
                "Sony", "PS4", "PlayStation 4", "", 0,
                USER_AGENT_TV_PS4,
                false);

        public final String friendlyName;
        public final int id;
        public final String clientName;
        public final String version;
        /**
         * Device make, model and OS version. null = use the value of the real device.
         */
        public final String make;
        public final String model;
        public final String osVersion;
        /**
         * OS name. null = keep the OS name of the real device.
         */
        public final String osName;
        /**
         * Android SDK version. -1 = use the value of the real device, 0 = not applicable (the field is sent as 0 / omitted
         * in the live stream request).
         */
        public final int sdk;
        public final String innerTubeBody;
        public final String userAgent;
        /**
         * If the authorization of the app (when logged in) is sent with the request.
         */
        public final boolean useAuth;

        /**
         * Android client.
         */
        ClientType(int id, String clientName, String packageName, String version,
                   String make, String model, String osName, String osVersion, int sdk, String buildId,
                   boolean useAuth) {
            this(id, clientName, version, make, model, osName, osVersion, sdk,
                    buildId == null
                            ? packageName + "/" + version + " (Linux; U; Android "
                            + (osVersion != null ? osVersion : Build.VERSION.RELEASE) + ") gzip"
                            : createAndroidUserAgent(packageName, version,
                            osVersion != null ? osVersion : Build.VERSION.RELEASE,
                            model != null ? model : Build.MODEL, buildId),
                    useAuth);
        }

        /**
         * Any client with a ready user agent.
         */
        ClientType(int id, String clientName, String version,
                   String make, String model, String osName, String osVersion, int sdk,
                   String userAgent, boolean useAuth) {
            this.friendlyName = str("revanced_spoof_client_options_entry_" + name().toLowerCase(Locale.ENGLISH));
            this.id = id;
            this.clientName = clientName;
            this.version = version;
            this.make = make;
            this.model = model;
            this.osVersion = osVersion;
            this.osName = osName;
            this.sdk = sdk;
            this.innerTubeBody = createInnerTubeBody(clientName, version,
                    make != null ? make : Build.MANUFACTURER,
                    model != null ? model : Build.MODEL,
                    osName != null ? osName : "Android",
                    osVersion != null ? osVersion : Build.VERSION.RELEASE,
                    sdk < 0 ? Build.VERSION.SDK_INT : sdk);
            this.userAgent = userAgent;
            this.useAuth = useAuth;
        }
    }
}
