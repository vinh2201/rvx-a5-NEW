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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.revanced.integrations.utils.LogHelper;
import app.revanced.integrations.utils.ResourceType;
import app.revanced.integrations.utils.ReVancedUtils;
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

    /** Largest channel value of a color that is still "almost black" (0x59 = 89 of 255): black, #0F0F0F, the dark browns ... */
    private static final int ALMOST_BLACK_MAX_CHANNEL = 0x59;

    /**
     * Called for every part of a text (a "run") that the server paints with its own color, before the color span is created.
     * <p>
     * Why: the server of the posts / the description of the video sends the colors of the light theme (text #0F0F0F, dark browns
     * on the tinted description panel) and this old app has no other color to use, so on the dark theme that text was almost
     * black on a dark background. On a dark theme an almost black run color is replaced by the primary text color of the theme
     * (the alpha of the original color is kept). Every other color (links, red badges, grays ...) and the light theme are
     * not touched.
     *
     * @param color   ARGB color of the run.
     * @param context Android context of the text component (carries the theme).
     * @return the color to paint the run with.
     */
    public static int adjustTextRunColor(int color, Context context) {
        try {
            int max = Math.max(Color.red(color), Math.max(Color.green(color), Color.blue(color)));
            if (max > ALMOST_BLACK_MAX_CHANNEL) return color;

            ColorStateList primary = getDarkTextColor(context);
            if (primary == null) return color;

            return (color & 0xFF000000) | (primary.getDefaultColor() & 0x00FFFFFF);
        } catch (Exception ex) {
            LogHelper.printException(MissingResourcesPatch.class, "Failed to adjust the color of a text run", ex);
            return color;
        }
    }

    // endregion

    // region Icons by name (dark theme + names that only newer apps know)

    private static final String BLACK_MARK = "_black_";
    private static final String WHITE_MARK = "_white_";

    /** An icon that was found for a name: its resource id and name, and whether it stands in for another name. */
    private static final class IconByName {
        final int id;
        final String name;
        final boolean substitute;

        IconByName(int id, String name, boolean substitute) {
            this.id = id;
            this.name = name;
            this.substitute = substitute;
        }
    }

    private static final IconByName NO_ICON = new IconByName(0, "", false);

    /** Result of the lookup of a drawable name (resource ids never change while the app runs). */
    private static final Map<String, IconByName> iconsByName = new ConcurrentHashMap<>();

    /** Resource id of the white variant of a drawable name, or 0 when there is none. */
    private static final Map<String, Integer> whiteIconIds = new ConcurrentHashMap<>();

    /**
     * Name of an icon of the app: yt_{outline|fill}_{picture}[_cairo|_experimental]_{black|white}_{size}.
     * Groups: 1 = style, 2 = picture, 3 = color, 4 = size.
     */
    private static final Pattern ICON_NAME = Pattern.compile(
            "^yt_(outline|fill|filled)_(?:experimental_)?(.+?)(?:_(?:cairo|experimental|v\\d{1,2}))*_(black|white)_(\\d{1,3})$");

    /** Sizes that are tried when the size that was asked for does not exist (all drawables are scaled by the view anyway). */
    private static final int[] FALLBACK_SIZES = {24, 18, 16, 20};

    /**
     * The same picture under another name: names that newer apps use -> names that 16.40.36 has.
     * (picture name of the server, picture name of this app)
     */
    private static final String[][] PICTURE_ALIASES = {
            {"thumbs_up", "thumb_up"},
            {"thumbs_down", "thumb_down"},
            {"like", "thumb_up"},
            {"dislike", "thumb_down"},
            {"comment", "message_bubble_right"},
            {"comments", "message_bubble_right"},
            {"reply", "message_bubble_right"},
            {"chat_bubble", "message_bubble_right"},
            {"chat", "message_bubble_right"},
            {"message", "message_bubble_right"},
            {"more_vert", "overflow_vertical"},
            {"more_vertical", "overflow_vertical"},
            {"more_horiz", "overflow_horizontal"},
            {"more_horizontal", "overflow_horizontal"},
            {"delete", "trash_can"},
            {"trash", "trash_can"},
            {"edit", "pencil"},
            {"close", "x_mark"},
            {"cancel", "x_mark"},
            {"clear", "x_mark"},
            {"favorite", "heart"},
            {"notifications", "bell"},
            {"notification", "bell"},
            {"playlist_add", "list_add"},
            {"watch_later", "clock"},
            {"history", "clock"},
            {"info", "info_circle"},
            {"help", "question_circle"},
            {"settings", "gear"},
    };

    /**
     * Finds an icon for a drawable name: the icon itself, or when the app has no icon with that name the icon of the same
     * picture under the name that 16.40.36 uses (without "_cairo", another size, a different spelling of the picture).
     */
    private static IconByName findIcon(Context context, String name) {
        IconByName found = iconsByName.get(name);
        if (found != null) return found;

        int id = ResourceUtils.identifier(name, ResourceType.DRAWABLE, context);
        if (id != 0) {
            found = new IconByName(id, name, false);
        } else {
            found = findSubstitute(context, name);
        }

        iconsByName.put(name, found);
        return found;
    }

    private static IconByName findSubstitute(Context context, String name) {
        Matcher matcher = ICON_NAME.matcher(name);
        if (!matcher.matches()) return NO_ICON;

        String style = matcher.group(1);
        if (style.equals("filled")) style = "fill";
        String picture = matcher.group(2);
        String color = matcher.group(3);
        int size = Integer.parseInt(matcher.group(4));

        String[] pictures = {picture, aliasOf(picture)};
        // "fill" icons that this app does not have fall back to the outlined picture.
        String[] styles = style.equals("fill") ? new String[]{"fill", "outline"} : new String[]{style};

        for (String candidatePicture : pictures) {
            if (candidatePicture == null) continue;

            for (String candidateStyle : styles) {
                for (int i = -1; i < FALLBACK_SIZES.length; i++) {
                    int candidateSize = i < 0 ? size : FALLBACK_SIZES[i];
                    if (i >= 0 && candidateSize == size) continue;

                    String candidate = "yt_" + candidateStyle + "_" + candidatePicture + "_" + color + "_" + candidateSize;
                    if (candidate.equals(name)) continue;

                    int id = ResourceUtils.identifier(candidate, ResourceType.DRAWABLE, context);
                    if (id != 0) return new IconByName(id, candidate, true);
                }
            }
        }

        return NO_ICON;
    }

    private static String aliasOf(String picture) {
        for (String[] alias : PICTURE_ALIASES) {
            if (alias[0].equals(picture) && !alias[1].equals(picture)) return alias[1];
        }
        return null;
    }

    /** Resource id of the white sibling of a black drawable name, or 0. */
    private static int whiteIconId(Context context, String blackName) {
        Integer cached = whiteIconIds.get(blackName);
        if (cached == null) {
            cached = ResourceUtils.identifier(blackName.replace(BLACK_MARK, WHITE_MARK), ResourceType.DRAWABLE, context);
            whiteIconIds.put(blackName, cached);
        }
        return cached;
    }

    /**
     * Called at the start of the method that turns a resource NAME (the server of the feed / the comments / the Shorts player
     * refers to images by name) into a resource id.
     * <p>
     * Why (dark theme): the icons that the server asks for are the black ones ("yt_outline_x_black_24"). The current apps
     * paint them with the color of the theme, this old app does not, so on the dark theme they show black on a dark
     * background (some icons were white only because the app has a white sibling of them). The patch adds a white sibling
     * ("..._white_...") for every black icon that has none, and on the dark theme the white one is returned instead.
     * The light theme is not touched.
     * <p>
     * Why (new names): the server also asks for icons that only newer apps have (for example "yt_outline_thumb_up_cairo_black_24"
     * for the like / dislike / reply icons under a comment). 16.40.36 resolves such a name to id 0, which is drawn as nothing at
     * all, on the light theme as well. For those names the icon of the same picture that this app has is returned
     * (see {@link #findSubstitute}).
     *
     * @return the id of the icon to use, or 0 to let the app resolve the name as usual.
     */
    public static int getDarkDrawableId(Context context, String type, String name) {
        try {
            if (context == null || name == null || !"drawable".equals(type)) return 0;

            IconByName icon = findIcon(context, name);
            if (icon.id == 0) return 0;

            // The light theme keeps the black icons.
            if (icon.name.contains(BLACK_MARK) && getDarkTextColor(context) != null) {
                int white = whiteIconId(context, icon.name);
                if (white != 0) return white;
            }

            return icon.substitute ? icon.id : 0;
        } catch (Exception ex) {
            LogHelper.printException(MissingResourcesPatch.class, "Failed to look up an icon by its name", ex);
            return 0;
        }
    }

    // endregion

    // region White thumbs (dark theme)

    /** Black thumb icons that the icon type table (id based) returns: yt_{outline|fill}_thumb_{up|down}_black_NN. */
    private static final Pattern THUMB_ICON_NAME = Pattern.compile("^yt_(?:outline|fill)_thumb_(?:up|down)_black_\\d{1,3}$");

    /** Resource id of a black thumb icon -> id of its white sibling (0 = leave the icon as it is). */
    private static final Map<Integer, Integer> whiteThumbIds = new ConcurrentHashMap<>();

    /**
     * Called with the result of the icon type -> drawable id table (the table of the black icons of the app).
     * <p>
     * Why: the like button of the video (and the like / dislike pictures that are looked up by icon type) get the black
     * thumb drawable from that table and are not painted with the color of the theme, so on the dark theme the like button was
     * black on a dark background. On the dark theme the white sibling of the thumb drawable is returned instead. Only the
     * thumbs are changed: every other picture of that table is painted by the app itself. The light theme is not touched.
     *
     * @param id resource id from the table.
     * @return the id to use.
     */
    public static int getDarkIconId(int id) {
        try {
            if (id == 0) return 0;

            TextColorOfTheme last = lastTextColor;
            // Unknown theme (nothing was measured yet) or the light theme.
            if (last == null || last.darkColor == null) return id;

            Integer cached = whiteThumbIds.get(id);
            if (cached == null) {
                Context context = ReVancedUtils.getContext();
                // The app context is not known yet: leave the icon as it is and ask again next time.
                if (context == null) return id;

                int white = 0;
                String name = context.getResources().getResourceEntryName(id);
                if (name != null && THUMB_ICON_NAME.matcher(name).matches()) {
                    white = ResourceUtils.identifier(name.replace(BLACK_MARK, WHITE_MARK), ResourceType.DRAWABLE, context);
                }
                cached = white;
                whiteThumbIds.put(id, cached);
            }

            return cached != 0 ? cached : id;
        } catch (Exception ex) {
            LogHelper.printException(MissingResourcesPatch.class, "Failed to look up the white thumb icon", ex);
            return id;
        }
    }

    // endregion
}
