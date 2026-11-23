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
     * Every target value below exists in the icon type enum of 16.40.36 and has a drawable there.
     * <p>
     * Why: for an unknown number the lookup returns null -> UNKNOWN -> drawable id 0, which showed the wrong icon
     * (or none). Servers of newer versions (20.x / 21.x) send the "Cairo" icon types (numbers 1144..1233) and a few new
     * types that stand for the same picture as an icon that 16.40.36 already has.
     * <p>
     * The table was made by comparing the icon type enums and the icon -> drawable tables of 16.40.36, 20.05.46 and
     * 21.07.240 (the numbers are the same in all of them): a number is mapped when the old app has an icon type with the
     * same meaning AND a drawable for it. The first group (the bar at the bottom, search, bell, create, settings) is
     * confirmed by the drawables of the newer versions (for example TAB_HOME_CAIRO uses yt_outline_home_cairo_black_24).
     * Entries marked "closest icon" have no exact equivalent in 16.40.36, the nearest old picture is used instead.
     * <p>
     * Not mapped (16.40.36 has no picture for them, they stay as before): YOUTUBE_LOGO_CAIRO_PLACEHOLDER_DARK,
     * YOUTUBE_PREMIUM_LOGO_CAIRO_PLACEHOLDER_DARK (the old app has only the light wordmark in its icon table),
     * FASHION_LOGO_CAIRO, GAMING_LOGO_CAIRO, PLAYABLES_LOGO_CAIRO, TROPHY_CAIRO.
     *
     * @param iconType icon type number sent by the server.
     * @return the icon type number to use.
     */
    public static int getLegacyIconType(int iconType) {
        switch (iconType) {
            case 964:
                return 835; // BROADCAST -> OUTLINE_RADAR_LIVE (closest icon)
            case 1021:
                return 345; // CLAPPERBOARD -> MOVIES (closest icon)
            case 1144:
                return 158; // YOUTUBE_LOGO_CAIRO_PLACEHOLDER -> YOUTUBE_LOGO
            case 1146:
                return 531; // YOUTUBE_PREMIUM_LOGO_CAIRO_PLACEHOLDER -> YOUTUBE_PREMIUM_LOGO_SHORT
            case 1149:
                return 600; // SEARCH_TAB_LARGE -> SEARCH_LARGE
            case 1150:
                return 741; // PREMIUM_STANDALONE_CAIRO -> PREMIUM_STANDALONE
            case 1154:
                return 406; // TAB_HOME_CAIRO -> PIVOT_HOME
            case 1155:
                return 408; // TAB_SUBSCRIPTIONS_CAIRO -> PIVOT_SUBSCRIPTIONS
            case 1156:
                return 355; // TAB_ACTIVITY_CAIRO -> TAB_ACTIVITY
            case 1157:
                return 776; // TAB_SHORTS_CAIRO -> TAB_SHORTS
            case 1158:
                return 239; // ACCOUNT_CIRCLE_CAIRO -> ACCOUNT_CIRCLE
            case 1159:
                return 504; // INCOGNITO_CAIRO -> INCOGNITO_CIRCLE
            case 1160:
                return 60; // SEARCH_CAIRO -> SEARCH
            case 1161:
                return 734; // CREATION_TAB_LARGE_CAIRO -> CREATION_TAB_LARGE
            case 1162:
                return 44; // SETTINGS_CAIRO -> SETTINGS
            case 1174:
                return 48; // SHARE_CAIRO -> SHARE
            case 1183:
                return 2; // WATCH_HISTORY_CAIRO -> WATCH_HISTORY
            case 1184:
                return 789; // SHOPPING_BAG_CAIRO -> SHOPPING_BAG
            case 1185:
                return 264; // NOTIFICATIONS_CAIRO -> NOTIFICATIONS
            case 1186:
                return 46; // BOOKMARK_CAIRO -> ADD_TO_PLAYLIST (closest icon)
            case 1187:
                return 19; // EXPAND_CAIRO -> EXPAND
            case 1188:
                return 221; // CHEVRON_RIGHT_CAIRO -> CHEVRON_RIGHT
            case 1189:
                return 20; // COLLAPSE_CAIRO -> COLLAPSE
            case 1190:
                return 345; // CLAPPERBOARD_CAIRO -> MOVIES (closest icon)
            case 1191:
                return 3; // WATCH_LATER_CAIRO -> WATCH_LATER
            case 1192:
                return 147; // OFFLINE_DOWNLOAD_CAIRO -> OFFLINE_DOWNLOAD
            case 1194:
                return 201; // TRENDING_CAIRO -> TRENDING
            case 1195:
                return 52; // FLAG_CAIRO -> FLAG
            case 1197:
                return 7; // PLAYLISTS_CAIRO -> PLAYLISTS
            case 1198:
                return 185; // VIEW_LIST_CAIRO -> VIEW_LIST
            case 1199:
                return 98; // FEEDBACK_CAIRO -> FEEDBACK
            case 1200:
                return 658; // MY_VIDEOS_CAIRO -> MY_VIDEOS
            case 1201:
                return 549; // NEWS_CAIRO -> BREAKING_NEWS (closest icon)
            case 1202:
                return 835; // BROADCAST_CAIRO -> OUTLINE_RADAR_LIVE (closest icon)
            case 1203:
                return 80; // HELP_CAIRO -> HELP
            case 1204:
                return 102; // LIVE_CAIRO -> LIVE
            case 1205:
                return 576; // CONTENT_CUT_CAIRO -> CONTENT_CUT
            case 1206:
                return 16; // LIKES_PLAYLIST_CAIRO -> LIKES_PLAYLIST
            case 1209:
                return 784; // YOUTUBE_MUSIC_MONOCHROME_CAIRO -> YOUTUBE_MUSIC_MONOCHROME
            case 1210:
                return 587; // COURSE_CAIRO -> COURSE
            case 1211:
                return 21; // MUSIC_CAIRO -> MUSIC
            case 1214:
                return 258; // ACCOUNT_BOX_CAIRO -> ACCOUNT_BOX
            case 1232:
                return 587; // LEARNING -> COURSE (closest icon)
            case 1233:
                return 587; // LEARNING_CAIRO -> COURSE (closest icon)
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
