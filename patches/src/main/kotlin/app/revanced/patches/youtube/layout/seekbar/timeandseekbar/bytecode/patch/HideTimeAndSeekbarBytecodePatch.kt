package app.revanced.patches.youtube.layout.seekbar.timeandseekbar.bytecode.patch

import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.layout.seekbar.timeandseekbar.bytecode.fingerprints.timeCounterMethodMatch
import app.revanced.patches.youtube.layout.seekbar.timeandseekbar.bytecode.fingerprints.timeCounterParentMethodMatch
import app.revanced.patches.youtube.misc.resourceid.patch.sharedResourceIdPatch
import app.revanced.shared.patches.timebar.hookTimebarPatch
import app.revanced.shared.patches.timebar.setTimebarMethod
import app.revanced.shared.util.integrations.Constants.SEEKBAR_LAYOUT

// Old name: hide-time-and-seekbar-bytecode-patch (helper patch, never selectable)
val hideTimeAndSeekbarBytecodePatch = bytecodePatch(
    description = "Hides progress bar and time counter on videos (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        hookTimebarPatch,
        sharedResourceIdPatch,
    )

    apply {
        val counterMethod = timeCounterParentMethodMatch.immutableClassDef.timeCounterMethodMatch.method

        listOf(
            setTimebarMethod,
            counterMethod
        ).forEach { method ->
            method.addInstructionsWithLabels(
                0, """
                    invoke-static {}, $SEEKBAR_LAYOUT->hideTimeAndSeekbar()Z
                    move-result v0
                    if-eqz v0, :hide_time_and_seekbar
                    return-void
                """, ExternalLabel("hide_time_and_seekbar", method.getInstruction(0))
            )
        }
    }
}
