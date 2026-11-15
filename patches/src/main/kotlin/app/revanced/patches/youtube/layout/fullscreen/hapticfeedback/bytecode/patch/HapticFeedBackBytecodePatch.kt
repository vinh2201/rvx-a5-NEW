package app.revanced.patches.youtube.layout.fullscreen.hapticfeedback.bytecode.patch

import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.removeInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.layout.fullscreen.hapticfeedback.bytecode.fingerprints.markerHapticsMethodMatch
import app.revanced.patches.youtube.layout.fullscreen.hapticfeedback.bytecode.fingerprints.seekHapticsMethodMatch
import app.revanced.shared.util.integrations.Constants.FULLSCREEN_LAYOUT
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

// Old name: disable-haptic-feedback-bytecode-patch (helper patch, never selectable)
val hapticFeedBackBytecodePatch = bytecodePatch(
    description = "Disable haptic feedback when swiping (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    apply {
        // SeekHapticsFingerprint -> disableSeekVibrate
        seekHapticsMethodMatch.let {
            val targetMethodName = "disableSeekVibrate"

            val startIndex = it[0] + 4
            val endIndex = it[-1]
            val insertIndex = endIndex + 2

            with(it.method) {
                val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA
                val dummyRegister = targetRegister + 1

                removeInstruction(insertIndex)
                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $FULLSCREEN_LAYOUT->$targetMethodName()Z
                        move-result v$dummyRegister
                        if-eqz v$dummyRegister, :vibrate
                        const-wide/16 v$targetRegister, 0x0
                        goto :exit
                        :vibrate
                        const-wide/16 v$targetRegister, 0x19
                    """, ExternalLabel("exit", getInstruction(insertIndex))
                )
                addInstructionsWithLabels(
                    startIndex, """
                        invoke-static {}, $FULLSCREEN_LAYOUT->$targetMethodName()Z
                        move-result v$dummyRegister
                        if-eqz v$dummyRegister, :vibrate
                        return-void
                    """, ExternalLabel("vibrate", getInstruction(startIndex))
                )
            }
        }

        // MarkerHapticsFingerprint -> disableChapterVibrate
        markerHapticsMethodMatch.method.let {
            it.addInstructionsWithLabels(
                0, """
                    invoke-static {}, $FULLSCREEN_LAYOUT->disableChapterVibrate()Z
                    move-result v0
                    if-eqz v0, :vibrate
                    return-void
                """, ExternalLabel("vibrate", it.getInstruction(0))
            )
        }
    }
}
