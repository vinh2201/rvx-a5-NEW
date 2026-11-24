package app.revanced.integrations.utils;

import android.os.SystemClock;
import android.util.Log;

/**
 * Logging of errors only (so a crash or a failure can be traced later), as light as possible: everything goes to logcat
 * (the ring buffer of the system), nothing is written to a file or kept by the app. The same error of the same class is
 * written at most once every few seconds, so a failure in a hot path (a menu refreshed again and again) cannot flood the log.
 */
public class LogHelper {

    private static final long REPEAT_MILLISECONDS = 5000;

    private static String lastError = "";
    private static long lastErrorTime;

    private static boolean isRepeat(Class<?> clazz, String message) {
        String error = (clazz != null ? clazz.getSimpleName() : "") + ": " + message;
        long now = SystemClock.elapsedRealtime();

        synchronized (LogHelper.class) {
            if (error.equals(lastError) && now - lastErrorTime < REPEAT_MILLISECONDS) {
                return true;
            }
            lastError = error;
            lastErrorTime = now;
            return false;
        }
    }

    public static void printException(Class<?> clazz, String message, Throwable ex) {
        if (isRepeat(clazz, message)) return;
        Log.e("revanced: " + (clazz != null ? clazz.getSimpleName() : ""), message, ex);
    }

    public static void printException(Class<?> clazz, String message) {
        if (isRepeat(clazz, message)) return;
        Log.e("revanced: " + (clazz != null ? clazz.getSimpleName() : ""), message);
    }

    public static void info(Class<?> clazz, String message) {
        Log.i("revanced: " + (clazz != null ? clazz.getSimpleName() : ""), message);
    }
}
