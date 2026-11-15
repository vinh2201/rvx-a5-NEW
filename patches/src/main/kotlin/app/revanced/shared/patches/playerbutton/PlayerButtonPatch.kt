package app.revanced.shared.patches.playerbutton

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.firstClassDef
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.resourceid.patch.sharedResourceIdPatch
import app.revanced.shared.fingerprints.liveChatMethodMatch
import app.revanced.shared.util.integrations.Constants.PLAYER_LAYOUT
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c

// Old name: hook-player-button-patch (helper patch, never selectable)
val playerButtonPatch = bytecodePatch(
    description = "Hook for the buttons of the video player.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(sharedResourceIdPatch)

    apply {
        val imageButtonType = liveChatMethodMatch.let {
            it.method.getInstruction<ReferenceInstruction>(it[-1]).reference.toString()
        }
        val imageButtonClass = firstClassDef(imageButtonType)

        for (method in imageButtonClass.methods) {
            val implementation = method.implementation ?: continue

            // PORT-NOTE: the old code inserted while iterating; the targets are collected first
            // and inserted from the last to the first so that the collected indices stay valid.
            val targets = mutableListOf<Triple<Int, Int, Int>>()

            var jumpInstruction = true
            implementation.instructions.forEachIndexed { index, instruction ->
                if (instruction.opcode == Opcode.INVOKE_VIRTUAL) {
                    val definedInstruction = instruction as? Instruction35c
                    if (definedInstruction?.reference.toString() ==
                        "Landroid/view/View;->setVisibility(I)V"
                    ) {
                        jumpInstruction = !jumpInstruction
                        if (jumpInstruction) return@forEachIndexed

                        targets.add(
                            Triple(index, definedInstruction!!.registerC, definedInstruction.registerD)
                        )
                    }
                }
            }

            targets.asReversed().forEach { (index, firstRegister, secondRegister) ->
                method.addInstructions(
                    index, """
                        invoke-static {v$firstRegister, v$secondRegister}, $PLAYER_LAYOUT->hidePlayerButton(Landroid/view/View;I)I
                        move-result v$secondRegister
                        """
                )
            }
        }
    }
}
