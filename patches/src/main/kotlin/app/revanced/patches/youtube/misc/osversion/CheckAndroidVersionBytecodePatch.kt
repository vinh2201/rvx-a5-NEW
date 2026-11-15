package app.revanced.patches.youtube.misc.osversion

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.shared.util.bytecode.BytecodeHelper

// Old name: check-android-version-bytecode-patch (helper patch, never selectable)
val checkAndroidVersionBytecodePatch = bytecodePatch(
    description = "Shows a warning dialog when the device is Android 6.0 or higher (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    apply {
        BytecodeHelper.injectInit(this, "CheckAndroidVersionPatch", "checkAndroidVersion")
    }
}
