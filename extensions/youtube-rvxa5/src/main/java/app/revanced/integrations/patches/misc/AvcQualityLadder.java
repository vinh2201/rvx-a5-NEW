package app.revanced.integrations.patches.misc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.revanced.integrations.utils.LogHelper;

/**
 * Part of "Spoof video streams (A5)".
 * <p>
 * The app chooses ONE codec family before it builds the quality list. When a response (for example the one of Android VR)
 * has only one VP9 quality but several H.264 (AVC) qualities, the app can choose VP9 and then the quality row of the player
 * menu says "Unavailable" (the list has a single entry), although the H.264 ladder would be selectable. The official
 * ReVanced patches handle exactly this case ("prefer multiple AVC qualities"): in that case the VP9 formats are left out so
 * that the complete H.264 ladder is used. This does the same at the start of the format selector of the app.
 * <p>
 * Only runs while client spoofing is on and only changes anything in that one case. The calls use the method names of
 * YouTube 16.40.36 through reflection (<code>P</code> = has a video stream, <code>u</code> = quality label,
 * <code>t</code> = mime type); when a name does not exist nothing is changed.
 */
@SuppressWarnings("unused")
public final class AvcQualityLadder {

    private AvcQualityLadder() {
    }

    /**
     * Injection point: start of the format selector of the app.
     *
     * @param formats all formats of the player response (collection of the app's format model).
     * @return the same collection, or a copy without the VP9 video formats.
     */
    public static Collection preferMultipleAvcQualities(Collection formats) {
        try {
            if (formats == null || !SpoofClientPatch.isClientSpoofingEnabled()) return formats;

            Set<String> avcQualities = new HashSet<>();
            Set<String> vp9Qualities = new HashSet<>();

            for (Object format : formats) {
                if (!isVideo(format)) continue;

                String label = call(format, "u");
                String mimeType = call(format, "t");
                if (label == null || label.isEmpty() || label.equals("?") || mimeType == null) continue;

                if (mimeType.contains("avc1")) {
                    avcQualities.add(label);
                } else if (isVp9(mimeType)) {
                    vp9Qualities.add(label);
                }
            }

            if (vp9Qualities.size() != 1 || avcQualities.size() <= vp9Qualities.size()) {
                return formats;
            }

            List<Object> kept = new ArrayList<>(formats.size());
            for (Object format : formats) {
                if (isVideo(format) && isVp9(call(format, "t"))) continue;
                kept.add(format);
            }

            return kept;
        } catch (Throwable ex) {
            LogHelper.printException(AvcQualityLadder.class, "Could not check the quality ladder", ex);
            return formats;
        }
    }

    private static boolean isVideo(Object format) {
        return format != null && "true".equals(call(format, "P"));
    }

    private static boolean isVp9(String mimeType) {
        return mimeType != null && (mimeType.contains("vp9") || mimeType.contains("vp09"));
    }

    private static String call(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            return value == null ? null : String.valueOf(value);
        } catch (Throwable ex) {
            return "?";
        }
    }
}
