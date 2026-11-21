package app.revanced.integrations.patches.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import app.revanced.integrations.utils.LogHelper;

/**
 * Runtime part of the "Add missing resources" patch (YouTube 16.40.36).
 * <p>
 * Newer servers (the app spoofs a 20.x client) send icon types and resources that 16.40.36 does not know.
 * {@link #getLegacyIconType(int)} maps the new "Cairo" icon type numbers to the legacy icon types with the same meaning,
 * the drawable helpers only make sure that an unknown icon (resource id 0) never crashes the app.
 */
@SuppressWarnings("unused")
public final class MissingResourcesPatch {

    private MissingResourcesPatch() {
    }

    /**
     * Called at the start of the icon type enum lookup (proto number -> enum).
     * Every target value below exists in the icon type enum of 16.40.36.
     * <p>
     * Why: for an unknown number the lookup returns null -> UNKNOWN -> drawable id 0, which showed the wrong icon.
     * rv6 mapped only home/shorts/subscriptions; the top-bar bell/search/create/settings icons were missing.
     * The mapping of those icons is taken from the legacy branch of community.
     *
     * @param iconType icon type number sent by the server.
     * @return the icon type number to use.
     */
    public static int getLegacyIconType(int iconType) {
        switch (iconType) {
            case 1154:
                return 406; // TAB_HOME_CAIRO -> PIVOT_HOME
            case 1155:
                return 408; // TAB_SUBSCRIPTIONS_CAIRO -> PIVOT_SUBSCRIPTIONS
            case 1156:
                return 355; // TAB_ACTIVITY_CAIRO -> TAB_ACTIVITY (toolbar bell)
            case 1157:
                return 776; // TAB_SHORTS_CAIRO -> TAB_SHORTS
            case 1160:
                return 60; // SEARCH_CAIRO -> SEARCH
            case 1161:
                return 734; // CREATION_TAB_LARGE_CAIRO -> CREATION_TAB_LARGE (add circle)
            case 1162:
                return 44; // SETTINGS_CAIRO -> SETTINGS
            case 1185:
                return 264; // NOTIFICATIONS_CAIRO -> NOTIFICATIONS
            default:
                return iconType;
        }
    }

    /**
     * Context.getDrawable(id) for the case that id is 0 (icon unknown to this app version).
     */
    public static Drawable getDrawable(Context context, int id) {
        if (id != 0 && context != null) {
            try {
                return context.getDrawable(id);
            } catch (Resources.NotFoundException ex) {
                LogHelper.printException(MissingResourcesPatch.class, "Missing drawable id: " + id, ex);
            }
        } else {
            LogHelper.info(MissingResourcesPatch.class, "Unknown icon requested (resource id 0), using a transparent drawable");
        }

        return transparent();
    }

    /**
     * Resources.getDrawable(id, theme) for the case that id is 0 (icon unknown to this app version).
     */
    public static Drawable getDrawable(Resources resources, int id, Resources.Theme theme) {
        if (id != 0 && resources != null) {
            try {
                return resources.getDrawable(id, theme);
            } catch (Resources.NotFoundException ex) {
                LogHelper.printException(MissingResourcesPatch.class, "Missing drawable id: " + id, ex);
            }
        } else {
            LogHelper.info(MissingResourcesPatch.class, "Unknown icon requested (resource id 0), using a transparent drawable");
        }

        return transparent();
    }

    private static Drawable transparent() {
        return new ColorDrawable(Color.TRANSPARENT);
    }
}
