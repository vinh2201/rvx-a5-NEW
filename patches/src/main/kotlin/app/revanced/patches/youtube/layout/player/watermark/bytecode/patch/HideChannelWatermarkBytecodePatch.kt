package app.revanced.patches.youtube.layout.player.watermark.bytecode.patch

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.removeInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.layout.player.watermark.bytecode.fingerprints.hideWatermarkMethodMatch
import app.revanced.patches.youtube.layout.player.watermark.bytecode.fingerprints.hideWatermarkParentMethodMatch
import app.revanced.patches.youtube.misc.resourceid.patch.sharedResourceIdPatch
import app.revanced.shared.util.integrations.Constants.PLAYER_LAYOUT
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

// Old name: hide-channel-watermark-bytecode-patch (helper patch, never selectable)
val hideChannelWatermarkBytecodePatch = bytecodePatch(
    description = "Hides creator's watermarks on videos (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(sharedResourceIdPatch)

    apply {
        hideWatermarkParentMethodMatch.immutableClassDef.hideWatermarkMethodMatch.let {
            val insertIndex = it[-1]

            with(it.method) {
                val register = getInstruction<TwoRegisterInstruction>(insertIndex).registerA
                removeInstruction(insertIndex)
                addInstructions(
                    insertIndex, """
                        invoke-static {}, $PLAYER_LAYOUT->hideChannelWatermark()Z
                        move-result v$register
                    """
                )
            }
        }
    }
}
