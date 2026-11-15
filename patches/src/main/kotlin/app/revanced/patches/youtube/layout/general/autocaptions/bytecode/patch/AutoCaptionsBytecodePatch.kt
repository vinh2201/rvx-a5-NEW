package app.revanced.patches.youtube.layout.general.autocaptions.bytecode.patch

import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.layout.general.autocaptions.bytecode.fingerprints.subtitleTrackMethodMatch
import app.revanced.shared.fingerprints.startVideoInformerMethodMatch
import app.revanced.shared.fingerprints.subtitleButtonControllerMethodMatch
import app.revanced.shared.util.integrations.Constants.GENERAL_LAYOUT

// Value of the `captionsButtonStatus` flag written by the hooks below
private enum class Status(val value: Int) {
    ENABLED(1),
    DISABLED(0)
}

// Old name: hide-auto-captions-bytecode-patch (helper patch, never selectable)
val autoCaptionsBytecodePatch = bytecodePatch(
    description = "Hide captions from being automatically enabled (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    apply {
        listOf(
            startVideoInformerMethodMatch.method to Status.DISABLED,
            subtitleButtonControllerMethodMatch.method to Status.ENABLED
        ).forEach { (method, status) ->
            with(method) {
                addInstructions(
                    0,
                    """
                    const/4 v0, ${status.value}
                    sput-boolean v0, $GENERAL_LAYOUT->captionsButtonStatus:Z
                    """
                )
            }
        }

        subtitleTrackMethodMatch.method.let {
            it.addInstructionsWithLabels(
                0, """
                    invoke-static {}, $GENERAL_LAYOUT->hideAutoCaptions()Z
                    move-result v0
                    if-eqz v0, :auto_captions_shown
                    sget-boolean v0, $GENERAL_LAYOUT->captionsButtonStatus:Z
                    if-nez v0, :auto_captions_shown
                    const/4 v0, 0x1
                    return v0
                """, ExternalLabel("auto_captions_shown", it.getInstruction(0))
            )
        }
    }
}
