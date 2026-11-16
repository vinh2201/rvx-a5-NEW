package app.revanced.integrations.patches.utils;

import app.revanced.integrations.settings.SettingsEnum;
import app.revanced.integrations.utils.ReVancedUtils;

/**
 * Runtime part of "Disable layout updates" and "Freeze layout updates" (YouTube 16.40.36).
 * <p>
 * The client sends the hash of its stored layout configuration ("hot" and "cold" config) in every request and
 * stores the configuration that the server answers with. A newer layout configuration can break the old layout.
 * <ul>
 *     <li>Disable: the configuration values are not sent, see {@link #disableRequestConfig(String)}.</li>
 *     <li>Freeze: the configuration that the app starts with is saved the first time and used from then on,
 *     see {@link #freezeHotConfigGroup(String)} and the other freeze methods.</li>
 * </ul>
 * Differences to the rv6 version: nothing is saved when the original value is null or empty (the frozen values
 * could be incomplete or inconsistent), and the settings are only read once the application context exists,
 * so a too early call can never lock {@link SettingsEnum} to its default values.
 */
@SuppressWarnings("unused")
public final class LayoutUpdatesPatch {

    private LayoutUpdatesPatch() {
    }

    // region Disable layout updates

    /**
     * Called for the hot hash data, the cold hash data and the cold config data of the request context.
     * Empty values are not added to the request.
     */
    public static String disableRequestConfig(String original) {
        if (isReady() && SettingsEnum.DISABLE_LAYOUT_UPDATES.getBoolean()) {
            return "";
        }

        return original;
    }

    // endregion

    // region Freeze layout updates

    public static String freezeHotConfigGroup(String original) {
        return freeze(original, SettingsEnum.FROZEN_HOT_CONFIG_GROUP, null);
    }

    public static String freezeHotHashData(String original) {
        return freeze(original, SettingsEnum.FROZEN_HOT_HASH_DATA, "");
    }

    public static String freezeColdConfigGroup(String original) {
        return freeze(original, SettingsEnum.FROZEN_COLD_CONFIG_GROUP, null);
    }

    public static String freezeColdHashData(String original) {
        return freeze(original, SettingsEnum.FROZEN_COLD_HASH_DATA, "");
    }

    /**
     * @param original      the value that was stored on disk.
     * @param frozen        setting that holds the frozen value.
     * @param disabledValue value to return when layout updates are also disabled (no configuration at all).
     */
    private static String freeze(String original, SettingsEnum frozen, String disabledValue) {
        if (!isReady() || !SettingsEnum.FREEZE_LAYOUT_UPDATES.getBoolean()) {
            return original;
        }

        String saved = frozen.getString();
        if (saved == null || saved.isEmpty()) {
            // First run with this option: remember the current configuration.
            if (original != null && !original.isEmpty()) {
                frozen.saveValue(original);
            }

            return original;
        }

        if (SettingsEnum.DISABLE_LAYOUT_UPDATES.getBoolean()) {
            return disabledValue;
        }

        return saved;
    }

    // endregion

    private static boolean isReady() {
        // Do not touch SettingsEnum before the context is set: its static initializer would then keep the defaults.
        return ReVancedUtils.getContext() != null;
    }
}
