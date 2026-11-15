package app.revanced.patches.youtube.button.autorepeat.patch

import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.button.autorepeat.fingerprints.autoNavInformerMethodMatch
import app.revanced.patches.youtube.button.autorepeat.fingerprints.autoRepeatMethodMatch
import app.revanced.patches.youtube.button.autorepeat.fingerprints.autoRepeatParentMethodMatch
import app.revanced.shared.util.integrations.Constants.UTILS_PATH
import app.revanced.shared.util.integrations.Constants.VIDEO_PATH

// Old name: always-autorepeat (helper patch, never selectable)
val autoRepeatPatch = bytecodePatch(
    description = "Hooks the auto repeat of the video player.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    apply {
        autoRepeatParentMethodMatch.immutableClassDef.autoRepeatMethodMatch.method.let {
            it.addInstructionsWithLabels(
                0, """
                invoke-static {}, $VIDEO_PATH/VideoInformation;->videoEnded()Z
                move-result v0
                if-eqz v0, :noautorepeat
                return-void
            """, ExternalLabel("noautorepeat", it.getInstruction(0))
            )
        }

        autoNavInformerMethodMatch.method.let {
            val index = it.implementation!!.instructions.size - 1
            it.addInstructions(
                index, """
                invoke-static {p1}, $UTILS_PATH/EnableAutoRepeatPatch;->enableAutoRepeat(Z)Z
                move-result p1
            """
            )
        }
    }
}
