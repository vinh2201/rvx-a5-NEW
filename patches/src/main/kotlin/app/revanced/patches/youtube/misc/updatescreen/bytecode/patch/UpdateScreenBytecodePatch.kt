package app.revanced.patches.youtube.misc.updatescreen.bytecode.patch

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.updatescreen.bytecode.fingerprints.updateScreenMethodMatch

// Old name: disable-update-screen-bytecode-patch (helper patch, never selectable)
val updateScreenBytecodePatch = bytecodePatch(
    description = "Disables the app update screen (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    apply {
        // return null instead of the intent of the update screen
        updateScreenMethodMatch.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """
        )
    }
}
