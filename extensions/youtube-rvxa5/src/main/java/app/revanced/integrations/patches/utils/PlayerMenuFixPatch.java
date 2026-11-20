package app.revanced.integrations.patches.utils;

import android.widget.ListAdapter;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

import app.revanced.integrations.utils.LogHelper;

/**
 * Runtime part of the fix of the player "three dots" menu (YouTube 16.40.36), part of "Add missing resources (A5)".
 * <p>
 * Why: when the client is spoofed to a 20.x version the server sends the settings entries of that menu (quality, playback speed,
 * captions, audio track, additional settings) as rows of its own. 16.40.36 shows them, but the command behind every row is one
 * of the new version and this old app does not know it: a tap only closes the menu, no chooser opens. The old app has the
 * native rows for the same things (they are the English rows of the same menu, or hidden ones), and their click actions work.
 * <p>
 * What it does: the hook in the click handler of the menu calls {@link #resolveRowClick} for a tapped server row. The row is
 * recognised and the click of the native row of the app that does the same is performed instead. A row that is not recognised
 * is left alone (the original code runs, which is what happened before).
 * <p>
 * How a row is recognised, in this order:
 * <ol>
 * <li>the words that are in the command of the row (for example "quality", "caption", "audio", "speed"),</li>
 * <li>the position of the row: the 20.x server sends exactly five rows in the order quality, speed, captions, audio track,
 * additional settings; the position is only used when the menu has exactly these five rows.</li>
 * </ol>
 * There is no option to turn this off: without it the entries of the menu do nothing.
 */
@SuppressWarnings("unused")
public final class PlayerMenuFixPatch {

    /** The row is not one this fix knows: the original code of the app runs. */
    public static final int NOT_HANDLED = -1;
    /** The fix did everything itself (nothing more to do for the caller). */
    public static final int HANDLED = -2;

    // Index of the native rows in the list of rows of the menu (the order of the array that the menu builds).
    private static final int NATIVE_QUALITY = 0;
    private static final int NATIVE_CAPTIONS = 3;
    private static final int NATIVE_AUDIO_TRACK = 4;
    private static final int NATIVE_SPEED = 5;

    private static final int KIND_NONE = 0;
    private static final int KIND_QUALITY = 1;
    private static final int KIND_SPEED = 2;
    private static final int KIND_CAPTIONS = 3;
    private static final int KIND_AUDIO_TRACK = 4;
    private static final int KIND_ADDITIONAL = 5;

    /** Order of the rows that the 20.x server sends. */
    private static final int[] ORDER_OF_SERVER_ROWS = {
            KIND_QUALITY, KIND_SPEED, KIND_CAPTIONS, KIND_AUDIO_TRACK, KIND_ADDITIONAL
    };

    private PlayerMenuFixPatch() {
    }

    /**
     * Called when a row that came from the server was tapped (before the original code runs).
     *
     * @param row         the tapped row (a row of the server).
     * @param endpoint    the command of the row (protobuf message), may be null.
     * @param adapter     adapter of the list of the menu (to find the position of the row), may be null.
     * @param nativeRows  the list with the native rows of the app (to click one of them), may be null.
     * @return {@link #NOT_HANDLED}, {@link #HANDLED}, or the index (in nativeRows) of the native row to click.
     */
    public static int resolveRowClick(Object row, Object endpoint, ListAdapter adapter, List<?> nativeRows) {
        try {
            String words = commandWords(endpoint);
            int kind = kindFromWords(words);
            String how = "words";

            int position = -1;
            int count = 0;
            if (adapter != null && row != null) {
                Class<?> rowClass = row.getClass();
                for (int i = 0, size = adapter.getCount(); i < size; i++) {
                    Object item = adapter.getItem(i);
                    if (item == null || item.getClass() != rowClass) continue;
                    if (item == row) position = count;
                    count++;
                }
            }

            if (kind == KIND_NONE && count == ORDER_OF_SERVER_ROWS.length && position >= 0) {
                kind = ORDER_OF_SERVER_ROWS[position];
                how = "position";
            }

            LogHelper.info(PlayerMenuFixPatch.class, "Server row of the player menu: position=" + position + "/" + count
                    + " kind=" + kind + " by=" + how + " words=[" + words + "]");

            switch (kind) {
                case KIND_QUALITY:
                    return nativeIndex(nativeRows, NATIVE_QUALITY);
                case KIND_SPEED:
                    return nativeIndex(nativeRows, NATIVE_SPEED);
                case KIND_CAPTIONS:
                    return nativeIndex(nativeRows, NATIVE_CAPTIONS);
                case KIND_AUDIO_TRACK:
                    return nativeIndex(nativeRows, NATIVE_AUDIO_TRACK);
                case KIND_ADDITIONAL:
                    // This old version has no "additional settings" sheet: the settings of the app are the closest thing.
                    TopBarButtonsPatch.openAppSettings();
                    return HANDLED;
                default:
                    return NOT_HANDLED;
            }
        } catch (Throwable ex) {
            LogHelper.printException(PlayerMenuFixPatch.class, "Player menu fix failure", ex);
            return NOT_HANDLED;
        }
    }

    private static int nativeIndex(List<?> nativeRows, int index) {
        if (nativeRows == null || index >= nativeRows.size() || nativeRows.get(index) == null) {
            return NOT_HANDLED;
        }
        return index;
    }

    private static int kindFromWords(String words) {
        if (words.contains("quality")) return KIND_QUALITY;
        if (words.contains("playback_rate") || words.contains("playbackrate") || words.contains("speed")) {
            return KIND_SPEED;
        }
        if (words.contains("caption") || words.contains("subtitle")) return KIND_CAPTIONS;
        if (words.contains("audio")) return KIND_AUDIO_TRACK;
        if (words.contains("additional")) return KIND_ADDITIONAL;
        return KIND_NONE;
    }

    /**
     * The readable text (runs of at least 4 printable ASCII characters) in the serialized command, in lower case.
     */
    private static String commandWords(Object endpoint) {
        if (endpoint == null) return "";

        try {
            Method toByteArray = endpoint.getClass().getMethod("toByteArray");
            byte[] bytes = (byte[]) toByteArray.invoke(endpoint);
            if (bytes == null) return "";

            StringBuilder words = new StringBuilder();
            int start = -1;
            for (int i = 0; i <= bytes.length; i++) {
                boolean printable = i < bytes.length && bytes[i] >= 0x20 && bytes[i] < 0x7F;
                if (printable) {
                    if (start < 0) start = i;
                } else if (start >= 0) {
                    if (i - start >= 4) {
                        if (words.length() > 0) words.append(' ');
                        words.append(new String(bytes, start, i - start, "US-ASCII"));
                    }
                    start = -1;
                }
            }
            return words.toString().toLowerCase(Locale.ROOT);
        } catch (Exception ex) {
            LogHelper.printException(PlayerMenuFixPatch.class, "Could not read the command of a row", ex);
            return "";
        }
    }
}
