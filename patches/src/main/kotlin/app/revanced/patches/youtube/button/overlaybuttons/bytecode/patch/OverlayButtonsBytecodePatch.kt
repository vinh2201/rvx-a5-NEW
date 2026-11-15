package app.revanced.patches.youtube.button.overlaybuttons.bytecode.patch

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.playercontrols.bytecode.patch.initializeControl
import app.revanced.patches.youtube.misc.playercontrols.bytecode.patch.injectVisibility
import app.revanced.patches.youtube.misc.playercontrols.bytecode.patch.playerControlsBytecodePatch
import app.revanced.patches.youtube.misc.videoid.mainstream.patch.mainstreamVideoIdPatch
import app.revanced.shared.util.integrations.Constants.BUTTON_PATH

// Old name: overlay-buttons-bytecode-patch (helper patch, never selectable)
val overlayButtonsBytecodePatch = bytecodePatch(
    description = "Adds the overlay buttons to the bottom controls of the player.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        playerControlsBytecodePatch,
        mainstreamVideoIdPatch,
    )

    apply {
        val AutoRepeat = "$BUTTON_PATH/AutoRepeat;"
        val Copy = "$BUTTON_PATH/Copy;"
        val CopyWithTimeStamp = "$BUTTON_PATH/CopyWithTimeStamp;"
        val Download = "$BUTTON_PATH/Download;"

        arrayOf(
            Download,
            AutoRepeat,
            CopyWithTimeStamp,
            Copy,
        ).forEach { descriptor ->
            initializeControl(descriptor)
            injectVisibility(descriptor)
        }
    }
}
