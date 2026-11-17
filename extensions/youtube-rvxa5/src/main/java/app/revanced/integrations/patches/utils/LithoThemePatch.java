package app.revanced.integrations.patches.utils;

import static app.revanced.integrations.utils.ReVancedUtils.getContext;

import android.annotation.SuppressLint;
import android.content.Context;

import app.revanced.integrations.utils.ResourceUtils;

public class LithoThemePatch {
    private static final int[] WHITE_VALUES = {
        -1, // comments chip background
        -394759, // music related results panel background
        -83886081 // video chapters list background
    };

    private static final int[] DARK_VALUES = {
        -14145496, // drawer content view background
        -14606047, // comments chip background
        -15198184, // music related results panel background
        -15790321, // comments chip background (new layout)
        -98492127 // video chapters list background
    };

    // background colors
    private static int whiteColor = 0;
    private static int blackColor = 0;

    public static int applyLithoTheme(int originalValue) {
        if (anyEquals(originalValue, DARK_VALUES)) return getBlackColor();
        else if (anyEquals(originalValue, WHITE_VALUES)) return getWhiteColor();
        return originalValue;
    }

    private static int getBlackColor() {
        if (blackColor == 0) blackColor = getColor("yt_black1");
        return blackColor;
    }

    private static int getWhiteColor() {
        if (whiteColor == 0) whiteColor = getColor("yt_white1");
        return whiteColor;
    }

    /**
     * Determines the color for a color resource.
     *
     * @param name The color resource name.
     * @return The value of the color.
     */
    @SuppressLint("DiscouragedApi")
    private static int getColor(String name) {
        Context context = getContext();

        if (context == null) return 0;

        // androidx.core.content.ContextCompat does not exist in YouTube 16.40.36 -> use the framework directly.
        final int id = ResourceUtils.identifier(name, "color", context);
        if (id == 0) return 0;

        return android.os.Build.VERSION.SDK_INT >= 23
                ? context.getColor(id)
                : context.getResources().getColor(id);
    }

    private static boolean anyEquals(int value, int... of) {
        for (int v : of) if (value == v) return true;
        return false;
    }
}


