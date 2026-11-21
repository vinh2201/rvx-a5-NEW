package app.revanced.integrations.patches.misc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import app.revanced.integrations.utils.LogHelper;

/**
 * Diagnostics only (part of "Spoof video streams (A5)"), no effect on playback.
 * <p>
 * Why: with some spoofed clients (for example Android VR) the quality row of the player menu says "Unavailable" for a
 * video whose quality list is selectable with another client. The quality list is built by the format selector of the app
 * from the formats of the player response, and it needs at least two different qualities to be selectable. These two
 * log lines show what the selector received and what it kept, so the reason can be read from the log of one video
 * (tag <code>revanced: FormatSelectionLog</code>) instead of guessed:
 * <ul>
 * <li><code>Format selection input</code>: every format of the response (itag, quality id, quality label, video only,
 * size, fps), the allowed itag sets given to the selector and the numeric fields of the selector configuration;</li>
 * <li><code>Quality list input</code>: the formats that were left for the quality list and the caps that were applied.</li>
 * </ul>
 * Only runs while client spoofing is on. The calls use the method names of YouTube 16.40.36 through reflection; when a
 * name does not exist the value is logged as "?" and nothing else happens.
 */
@SuppressWarnings("unused")
public final class FormatSelectionLog {

    private static final int MAX_LINE = 3000;
    private static final int MAX_SET_ITEMS = 80;

    private static volatile String lastInput = "";
    private static volatile String lastList = "";

    private FormatSelectionLog() {
    }

    /**
     * Injection point: start of the format selector of the app.
     *
     * @param formats        all formats of the player response (collection of the app's format model).
     * @param selectorConfig configuration of the selector (may be null).
     * @param allowedA       allowed itags (may be null).
     * @param allowedB       allowed itags (may be null).
     */
    public static void logFormatSelection(Collection<?> formats, Object selectorConfig, Set<?> allowedA, Set<?> allowedB) {
        try {
            if (!SpoofClientPatch.isClientSpoofingEnabled()) return;

            String text = "formats=" + describeFormats(formats)
                    + " | allowedA=" + describeSet(allowedA)
                    + " | allowedB=" + describeSet(allowedB)
                    + " | config=" + describeObject(selectorConfig, 1);
            if (text.equals(lastInput)) return;
            lastInput = text;
            print("Format selection input: ", text);
        } catch (Throwable ex) {
            LogHelper.printException(FormatSelectionLog.class, "Could not log the format selection", ex);
        }
    }

    /**
     * Injection point: start of the method that builds the quality list.
     *
     * @param formats the formats that are left for the quality list.
     * @param caps    caps that are applied to the list (may be null).
     */
    public static void logQualityListInput(List<?> formats, Object caps) {
        try {
            if (!SpoofClientPatch.isClientSpoofingEnabled()) return;

            String text = "formats=" + describeFormats(formats) + " | caps=" + describeObject(caps, 0);
            if (text.equals(lastList)) return;
            lastList = text;
            print("Quality list input: ", text);
        } catch (Throwable ex) {
            LogHelper.printException(FormatSelectionLog.class, "Could not log the quality list input", ex);
        }
    }

    private static void print(String prefix, String text) {
        for (int start = 0; start < text.length(); start += MAX_LINE) {
            int end = Math.min(text.length(), start + MAX_LINE);
            LogHelper.info(FormatSelectionLog.class, (start == 0 ? prefix : "... ") + text.substring(start, end));
        }
    }

    private static String describeFormats(Collection<?> formats) {
        if (formats == null) return "null";

        StringBuilder builder = new StringBuilder();
        builder.append(formats.size()).append(" [");
        boolean first = true;
        for (Object format : formats) {
            if (!first) builder.append(' ');
            first = false;
            if (format == null) {
                builder.append("null");
                continue;
            }
            // e = itag, f = quality id, u = quality label, P = video only, i = width, d = height, c = frame rate.
            builder.append(call(format, "e")).append('/')
                    .append(call(format, "f")).append('/')
                    .append(call(format, "u")).append('/')
                    .append("true".equals(call(format, "P")) ? "V" : "-").append('/')
                    .append(call(format, "i")).append('x').append(call(format, "d")).append('@')
                    .append(call(format, "c"));
        }
        return builder.append(']').toString();
    }

    private static String describeSet(Set<?> set) {
        if (set == null) return "null";

        StringBuilder builder = new StringBuilder();
        builder.append(set.size()).append(" {");
        int count = 0;
        for (Object item : set) {
            if (count++ >= MAX_SET_ITEMS) {
                builder.append(" ...");
                break;
            }
            if (count > 1) builder.append(',');
            builder.append(item);
        }
        return builder.append('}').toString();
    }

    /**
     * Numeric / boolean / string fields of an object (obfuscated names), and the same for the objects they hold
     * down to {@code depth}.
     */
    private static String describeObject(Object object, int depth) {
        if (object == null) return "null";

        StringBuilder builder = new StringBuilder(object.getClass().getSimpleName()).append('{');
        boolean first = true;
        for (Field field : object.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;

            Object value;
            try {
                field.setAccessible(true);
                value = field.get(object);
            } catch (Throwable ex) {
                continue;
            }

            String text;
            if (value == null) {
                continue;
            } else if (value instanceof Number || value instanceof Boolean || value instanceof String) {
                text = String.valueOf(value);
            } else if (depth > 0 && !value.getClass().isArray() && !(value instanceof Collection)
                    && !value.getClass().getName().startsWith("java.")) {
                text = describeObject(value, depth - 1);
            } else {
                continue;
            }

            if (!first) builder.append(' ');
            first = false;
            builder.append(field.getName()).append('=').append(text);
        }
        return builder.append('}').toString();
    }

    private static String call(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return String.valueOf(method.invoke(target));
        } catch (Throwable ex) {
            return "?";
        }
    }
}
