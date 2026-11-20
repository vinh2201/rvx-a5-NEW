package app.revanced.integrations.patches.extended;

import android.content.Context;
import android.content.SharedPreferences;

import app.revanced.integrations.utils.LogHelper;
import app.revanced.integrations.utils.ReVancedUtils;
import app.revanced.integrations.utils.SharedPrefHelper;

/**
 * "Spoof app version": replaces the version string the app reports to the server.
 *
 * <p>
 * The server no longer serves clients that are too old: a 16.40.36 app that reports its real version gets
 * errors or an empty home feed. Spoofing a recent version (default {@link #DEFAULT_TARGET}) fixes this, but then the
 * server answers with the layout of that version. The patches "Add missing resources", "Disable layout updates" and
 * "Freeze layout updates" keep the old app from crashing on that layout. The spoof is never switched off by the
 * app itself: it stays on until the user turns it off in the settings.
 *
 * <p>
 * Presets worth knowing (from community): 20.05.46 (recommended), 20.13.41 (restores the non collapsed action bar),
 * 20.20.42 (restores the old components filter), 21.11.48 (restores the old captions behavior).
 */
public class VersionOverridePatch {
    /**
     * Version used when the setting is empty or invalid.
     */
    public static final String DEFAULT_TARGET = "20.05.46";

    /**
     * Versions older than this do not work reliably any more (community), they are replaced with {@link #DEFAULT_TARGET}.
     */
    public static final String MINIMUM_TARGET = "20.02.34";

    private static final String PREF_ENABLED = "revanced_spoof_app_version";
    private static final String PREF_TARGET = "revanced_spoof_app_version_target";

    /**
     * Whether this process reports the spoofed version (null = not decided yet).
     */
    private static Boolean spoofActive;

    /**
     * Injected into {@code zkf.c(Context)} (the app version getter, the result is cached by the app), right after the
     * version name has been read from the package info.
     *
     * @param version The real version.
     * @param appContext The context the app passed to the getter. Used when the integrations context is not set yet.
     */
    public static String getVersionOverride(String version, Context appContext) {
        try {
            Context ctx = appContext != null ? appContext : ReVancedUtils.context;
            SharedPreferences prefs = SharedPrefHelper.getPreferences(ctx, SharedPrefHelper.SharedPrefNames.REVANCED);
            if (prefs == null) {
                return version;
            }

            if (!decideSpoof(ctx, prefs)) {
                return version;
            }

            return resolveTarget(prefs.getString(PREF_TARGET, ""));
        } catch (Throwable ex) {
            LogHelper.printException(VersionOverridePatch.class, "Failed to get the version to spoof", ex);
            return version;
        }
    }

    /**
     * Whether the setting is on. The result is remembered for this process.
     */
    private static synchronized boolean decideSpoof(Context ctx, SharedPreferences prefs) {
        if (spoofActive != null) {
            return spoofActive;
        }

        boolean active = prefs.getBoolean(PREF_ENABLED, true);
        spoofActive = active;
        return active;
    }

    /**
     * @return true if this process reports the spoofed version to the server, so the server answers with the layout
     * of the newer app. Used by the layout configuration guard. Returns false while the integrations context is
     * not available yet (nothing is remembered in that case).
     */
    public static boolean isSpoofActive() {
        try {
            if (spoofActive != null) {
                return spoofActive;
            }

            Context ctx = ReVancedUtils.context;
            SharedPreferences prefs = SharedPrefHelper.getPreferences(ctx, SharedPrefHelper.SharedPrefNames.REVANCED);
            return prefs != null && decideSpoof(ctx, prefs);
        } catch (Throwable ex) {
            LogHelper.printException(VersionOverridePatch.class, "Failed to check if the app version is spoofed", ex);
            return false;
        }
    }

    /**
     * @return The version to spoof: the setting if it is a valid version that is not older than {@link #MINIMUM_TARGET},
     * otherwise {@link #DEFAULT_TARGET}.
     */
    static String resolveTarget(String setting) {
        int[] target = parseVersion(setting);
        int[] minimum = parseVersion(MINIMUM_TARGET);
        if (target == null || minimum == null || compare(target, minimum) < 0) {
            return DEFAULT_TARGET;
        }
        return setting.trim();
    }

    /**
     * @return {major, minor, patch} or null if the text is not in the form "20.05.46".
     */
    static int[] parseVersion(String text) {
        if (text == null) {
            return null;
        }

        String[] parts = text.trim().split("\\.");
        if (parts.length != 3) {
            return null;
        }

        int[] result = new int[3];
        try {
            for (int i = 0; i < 3; i++) {
                result[i] = Integer.parseInt(parts[i].trim());
                if (result[i] < 0) {
                    return null;
                }
            }
        } catch (NumberFormatException ex) {
            return null;
        }
        return result;
    }

    private static int compare(int[] a, int[] b) {
        for (int i = 0; i < 3; i++) {
            if (a[i] != b[i]) {
                return a[i] < b[i] ? -1 : 1;
            }
        }
        return 0;
    }
}
