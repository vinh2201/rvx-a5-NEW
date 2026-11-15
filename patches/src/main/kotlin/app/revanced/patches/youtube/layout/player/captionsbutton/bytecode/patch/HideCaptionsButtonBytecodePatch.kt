package app.revanced.patches.youtube.layout.player.captionsbutton.bytecode.patch

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.shared.fingerprints.subtitleButtonControllerMethodMatch
import app.revanced.shared.util.integrations.Constants.PLAYER_LAYOUT
import com.android.tools.smali.dexlib2.Opcode

// Old name: hide-captions-button-bytecode-patch (helper patch, never selectable)
val hideCaptionsButtonBytecodePatch = bytecodePatch(
    description = "Hides the captions button in the video player (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    apply {
        subtitleButtonControllerMethodMatch.method.let {
            val index = it.implementation!!.instructions.indexOfFirst { instruction ->
                instruction.opcode == Opcode.IGET_BOOLEAN
            }
            if (index == -1) throw PatchException("Could not find the iget-boolean of the subtitle button controller")

            it.addInstruction(
                index + 1,
                "invoke-static {v0}, $PLAYER_LAYOUT->hideCaptionsButton(Landroid/widget/ImageView;)V"
            )
        }
    }
}
