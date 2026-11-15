package app.revanced.patches.youtube.layout.general.snackbar.bytecode.patch

import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.layout.general.snackbar.bytecode.fingerprints.hideSnackbarMethodMatch
import app.revanced.shared.util.integrations.Constants.GENERAL_LAYOUT

// Old name: hide-snackbar-bytecode-patch (helper patch, never selectable)
val hideSnackbarBytecodePatch = bytecodePatch(
    description = "Hides the snackbar action popup (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    apply {
        hideSnackbarMethodMatch.method.let {
            it.addInstructionsWithLabels(
                0, """
                    invoke-static {}, $GENERAL_LAYOUT->hideSnackbar()Z
                    move-result v0
                    if-eqz v0, :default
                    return-void
                """, ExternalLabel("default", it.getInstruction(0))
            )
        }
    }
}
