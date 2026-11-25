package app.revanced.integrations.patches.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import app.revanced.integrations.utils.LogHelper;
import app.revanced.integrations.utils.ResourceType;
import app.revanced.integrations.utils.ResourceUtils;

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
        }

        return transparent();
    }

    private static Drawable transparent() {
        return new ColorDrawable(Color.TRANSPARENT);
    }

    // region Default color of the Litho text (dark theme)

    /**
     * Result of the last theme lookup: the text color of one theme (null = the theme is not dark, keep the original color).
     */
    private static final class TextColorOfTheme {
        final WeakReference<Resources.Theme> theme;
        final ColorStateList darkColor;

        TextColorOfTheme(Resources.Theme theme, ColorStateList darkColor) {
            this.theme = new WeakReference<>(theme);
            this.darkColor = darkColor;
        }
    }

    private static volatile TextColorOfTheme lastTextColor;

    /**
     * Called at the start of the three methods that measure / draw a Litho text (the default color list is a parameter of each).
     * <p>
     * Why: the text component of the feed (video title, channel line, shelf titles ...) paints every part of the text that
     * has no color of its own with a fixed default color, which is black. The text of the feed that this old app gets
     * from a current server carries no color (the server relies on the theme), so on the dark theme it was black on a dark
     * background (and the rest of the app, which reads the colors of the theme, was white).
     * <p>
     * The default color is replaced by the primary text color of the theme the text is shown in (the attribute that every
     * native text of the app uses), only when that color is light, which means a dark theme. On the light theme
     * null is returned and the original color stays, so nothing changes there. Text that has a color of its own (links, badges, the
     * labels on the thumbnails) is not affected, because its color is applied by a span over the default one.
     *
     * @param context Android context of the component (carries the theme).
     * @return the color list to paint with on a dark theme, or null to keep the original color list.
     */
    public static ColorStateList getDarkTextColor(Context context) {
        if (context == null) return null;

        try {
            Resources.Theme theme = context.getTheme();
            if (theme == null) return null;

            TextColorOfTheme last = lastTextColor;
            if (last == null || last.theme.get() != theme) {
                last = new TextColorOfTheme(theme, resolveDarkThemeTextColor(context, theme));
                lastTextColor = last;
            }

            return last.darkColor;
        } catch (Exception ex) {
            LogHelper.printException(MissingResourcesPatch.class, "Failed to read the text color of the theme", ex);
            return null;
        }
    }

    /**
     * @return the primary text color of the theme when the theme is dark (that color is light), otherwise null.
     */
    private static ColorStateList resolveDarkThemeTextColor(Context context, Resources.Theme theme) {
        int attribute = ResourceUtils.identifier("ytTextPrimary", ResourceType.ATTR, context);
        if (attribute == 0) return null;

        TypedArray array = theme.obtainStyledAttributes(new int[]{attribute});
        try {
            ColorStateList color = array.getColorStateList(0);
            if (color == null) return null;

            int rgb = color.getDefaultColor();
            // Perceived brightness (0..255) of the color: light text means a dark theme.
            int brightness = (Color.red(rgb) * 299 + Color.green(rgb) * 587 + Color.blue(rgb) * 114) / 1000;
            return brightness > 160 ? color : null;
        } finally {
            array.recycle();
        }
    }

    // endregion

    // region White icons (dark theme)

    private static final String BLACK_MARK = "_black_";
    private static final String WHITE_MARK = "_white_";

    /** Resource id of the white variant of a drawable name that the server sent, or 0 when there is none. */
    private static final Map<String, Integer> whiteIconIds = new ConcurrentHashMap<>();

    /**
     * Called at the start of the method that turns a resource NAME (the server of the feed / the Shorts player refers to
     * images by name) into a resource id.
     * <p>
     * Why: the icons that the server asks for are the black ones ("yt_outline_x_black_24", "..._cairo_black_24"). The
     * current apps paint them with the color of the theme, this old app does not, so on the dark theme they show black on a
     * dark background (some icons were white only because the app has a white sibling of them). The patch adds a white sibling
     * ("..._white_...") for every black icon that has none, and on the dark theme the white one is returned instead.
     * The light theme is not touched.
     *
     * @return the id of the white variant, or 0 to let the app resolve the name as usual.
     */
    public static int getDarkDrawableId(Context context, String type, String name) {
        try {
            if (context == null || name == null || !"drawable".equals(type)) return 0;
            if (!name.contains(BLACK_MARK)) return 0;
            // The light theme keeps the black icons.
            if (getDarkTextColor(context) == null) return 0;

            Integer cached = whiteIconIds.get(name);
            if (cached == null) {
                int id = ResourceUtils.identifier(name.replace(BLACK_MARK, WHITE_MARK), ResourceType.DRAWABLE, context);
                cached = id;
                whiteIconIds.put(name, cached);
            }
            return cached;
        } catch (Exception ex) {
            LogHelper.printException(MissingResourcesPatch.class, "Failed to look up the white variant of an icon", ex);
            return 0;
        }
    }

    // endregion
}
