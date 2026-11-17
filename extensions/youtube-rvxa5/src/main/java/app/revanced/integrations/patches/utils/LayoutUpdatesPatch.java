package app.revanced.integrations.patches.utils;

import static app.revanced.integrations.utils.StringRef.str;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;

import app.revanced.integrations.patches.extended.VersionOverridePatch;
import app.revanced.integrations.settings.SettingsEnum;
import app.revanced.integrations.utils.LogHelper;
import app.revanced.integrations.utils.ReVancedUtils;
import app.revanced.integrations.utils.SharedPrefHelper;

/**
 * Runtime part of the layout configuration guard, "Disable layout updates" and "Freeze layout updates"
 * (YouTube 16.40.36).
 * <p>
 * The client sends the hash of its stored layout configuration ("hot" and "cold" config) in every request and
 * stores the configuration that the server answers with. At the next start the stored configuration is read again
 * by {@code aauq.run()}. A configuration made for a newer app can break the old layout (or crash the app on open).
 * <p>
 * All values that are read from the preferences go through {@link #load}, which decides in this order:
 * <ol>
 *     <li><b>Ignore the stored configuration</b> (the app starts with its built-in layout, exactly like a fresh
 *     install) when
 *     <ul>
 *         <li>"Disable layout updates" is on, or</li>
 *         <li>the app version is spoofed (the server then answers with the layout of the newer app). The stored
 *         configuration is marked as <i>tainted</i>, or</li>
 *         <li>the stored configuration is tainted and the spoof was switched off since (once: the real version is
 *         reported again for this start, so the server sends a configuration that fits this app, which replaces
 *         the tainted one; the next start reads it).</li>
 *     </ul></li>
 *     <li><b>Freeze</b> ("Freeze layout updates" on): the configuration that was stored the first time (while it
 *     could not be tainted) is saved and used from then on.</li>
 *     <li>Otherwise the stored value is used unchanged.</li>
 * </ol>
 * <p>
 * <b>Crash loop breaker:</b> a start that did not live {@link #STARTUP_GRACE_MILLISECONDS} (the app died while
 * opening) is remembered. If the previous start failed, "Disable layout updates" is switched on (with a notice) so
 * the app starts with its built-in layout instead of crashing on a stored configuration it cannot read. The spoof
 * setting is never touched; the user can switch "Disable layout updates" off again in the settings.
 * <p>
 * Nothing is frozen while the configuration is ignored or tainted, and nothing is saved when the value is null or
 * empty. The settings are only read once the application context exists, so a too early call can never lock
 * {@link SettingsEnum} to its default values.
 */
@SuppressWarnings("unused")
public final class LayoutUpdatesPatch {

    private LayoutUpdatesPatch() {
    }

    // region Disable layout updates (request)

    /**
     * Called for the hot hash data, the cold hash data and the cold config data of the request context.
     * Empty values are not added to the request.
     */
    public static String disableRequestConfig(String original) {
        if (isReady() && (SettingsEnum.DISABLE_LAYOUT_UPDATES.getBoolean() || ignoreStoredConfig())) {
            return "";
        }

        return original;
    }

    // endregion

    // region Load of the stored configuration

    public static String loadHotConfigGroup(String original) {
        return load(original, SettingsEnum.FROZEN_HOT_CONFIG_GROUP, null);
    }

    public static String loadHotHashData(String original) {
        return load(original, SettingsEnum.FROZEN_HOT_HASH_DATA, "");
    }

    public static String loadColdConfigGroup(String original) {
        return load(original, SettingsEnum.FROZEN_COLD_CONFIG_GROUP, null);
    }

    public static String loadColdHashData(String original) {
        return load(original, SettingsEnum.FROZEN_COLD_HASH_DATA, "");
    }

    /**
     * @param original     the value that was stored on disk.
     * @param frozen       setting that holds the frozen value.
     * @param ignoredValue the value the app gets when the stored configuration is ignored (same as "nothing stored").
     */
    private static String load(String original, SettingsEnum frozen, String ignoredValue) {
        if (!isReady()) {
            return original;
        }

        if (ignoreStoredConfig()) {
            return ignoredValue;
        }

        if (!SettingsEnum.FREEZE_LAYOUT_UPDATES.getBoolean()) {
            return original;
        }

        String saved = frozen.getString();
        if (saved == null || saved.isEmpty()) {
            // First run with this option: remember the current (not tainted) configuration.
            if (original != null && !original.isEmpty()) {
                frozen.saveValue(original);
            }

            return original;
        }

        return saved;
    }

    // endregion

    // region Guard

    private static boolean decided;
    private static boolean ignoreStored;

    /**
     * @return true if the app must start with its built-in layout instead of the stored configuration.
     * The answer is the same for the four values of one start.
     */
    private static synchronized boolean ignoreStoredConfig() {
        if (decided) {
            return ignoreStored;
        }

        boolean ignore;
        try {
            ignore = decideIgnoreStoredConfig();
        } catch (Throwable ex) {
            LogHelper.printException(LayoutUpdatesPatch.class, "Layout configuration guard failed", ex);
            ignore = false;
        }

        decided = true;
        ignoreStored = ignore;
        return ignore;
    }

    private static boolean decideIgnoreStoredConfig() {
        try {
            breakCrashLoop();
        } catch (Throwable ex) {
            LogHelper.printException(LayoutUpdatesPatch.class, "Crash loop breaker failed", ex);
        }

        if (SettingsEnum.DISABLE_LAYOUT_UPDATES.getBoolean()) {
            return true;
        }

        if (PatchStatus.SpoofAppVersion() && VersionOverridePatch.isSpoofActive()) {
            // The server answers with the layout of the newer app: never use (or freeze) what it stores.
            if (!SettingsEnum.LAYOUT_CONFIG_TAINTED.getBoolean()) {
                SettingsEnum.LAYOUT_CONFIG_TAINTED.saveValue(true);
            }
            if (SettingsEnum.LAYOUT_CONFIG_RECOVERING.getBoolean()) {
                SettingsEnum.LAYOUT_CONFIG_RECOVERING.saveValue(false);
            }

            LogHelper.info(LayoutUpdatesPatch.class, "App version is spoofed: the stored layout configuration is ignored");
            return true;
        }

        if (SettingsEnum.LAYOUT_CONFIG_TAINTED.getBoolean()) {
            if (!SettingsEnum.LAYOUT_CONFIG_RECOVERING.getBoolean()) {
                // First start without spoof: ignore the stored configuration once. The server now answers
                // with a configuration for this app version, which replaces the tainted one.
                SettingsEnum.LAYOUT_CONFIG_RECOVERING.saveValue(true);
                LogHelper.info(LayoutUpdatesPatch.class, "Spoof switched off: ignoring the tainted layout configuration once");
                return true;
            }

            // Second start: the configuration was replaced during the previous one.
            SettingsEnum.LAYOUT_CONFIG_RECOVERING.saveValue(false);
            SettingsEnum.LAYOUT_CONFIG_TAINTED.saveValue(false);
        }

        return false;
    }

    // endregion

    // region Crash loop breaker

    private static final String PREF_STARTING = "revanced_layout_guard_starting";
    /**
     * Mark of the old "spoof safety net" (removed). Read once so that an app that was already crashing before the
     * update is recovered at its first start.
     */
    private static final String PREF_STARTING_LEGACY = "revanced_spoof_app_version_starting";
    private static final long STARTUP_GRACE_MILLISECONDS = 6 * 1000;
    private static final long NOTICE_DELAY_MILLISECONDS = 3 * 1000;

    /**
     * The process is marked as "starting" and the mark is removed once it has lived
     * {@link #STARTUP_GRACE_MILLISECONDS}. A mark that is still there at the next start means the previous start
     * did not finish: then "Disable layout updates" is switched on.
     */
    private static void breakCrashLoop() {
        Context ctx = ReVancedUtils.getContext();
        final SharedPreferences prefs = SharedPrefHelper.getPreferences(ctx, SharedPrefHelper.SharedPrefNames.REVANCED);
        if (prefs == null || !isMainProcess(ctx)) {
            return; // Other processes of the app must not touch the mark.
        }

        boolean previousStartFailed = prefs.getBoolean(PREF_STARTING, false)
                || prefs.getBoolean(PREF_STARTING_LEGACY, false);

        prefs.edit()
                .remove(PREF_STARTING_LEGACY)
                .putBoolean(PREF_STARTING, true)
                .commit();

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    prefs.edit().putBoolean(PREF_STARTING, false).apply();
                } catch (Throwable ex) {
                    LogHelper.printException(LayoutUpdatesPatch.class, "Failed to clear the startup mark", ex);
                }
            }
        }, STARTUP_GRACE_MILLISECONDS);

        if (previousStartFailed && !SettingsEnum.DISABLE_LAYOUT_UPDATES.getBoolean()) {
            SettingsEnum.DISABLE_LAYOUT_UPDATES.saveValue(true);
            LogHelper.info(LayoutUpdatesPatch.class, "The previous start failed: Disable layout updates was switched on");
            showNotice(ctx);
        }
    }

    private static void showNotice(final Context ctx) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    Toast.makeText(ctx, str("revanced_layout_guard_auto_disabled"), Toast.LENGTH_LONG).show();
                } catch (Throwable ex) {
                    LogHelper.printException(LayoutUpdatesPatch.class, "Failed to show the notice", ex);
                }
            }
        }, NOTICE_DELAY_MILLISECONDS);
    }

    /**
     * @return true if this is the main process of the app (the package name is the process name).
     */
    private static boolean isMainProcess(Context ctx) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/proc/self/cmdline"));
            String processName = reader.readLine();
            if (processName == null) {
                return true;
            }
            int end = processName.indexOf('\0');
            if (end >= 0) {
                processName = processName.substring(0, end);
            }
            return processName.trim().equals(ctx.getPackageName());
        } catch (Throwable ex) {
            return true;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    // endregion

    private static boolean isReady() {
        // Do not touch SettingsEnum before the context is set: its static initializer would then keep the defaults.
        return ReVancedUtils.getContext() != null;
    }
}
