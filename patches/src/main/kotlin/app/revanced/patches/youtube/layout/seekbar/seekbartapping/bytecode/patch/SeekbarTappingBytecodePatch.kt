package app.revanced.patches.youtube.layout.seekbar.seekbartapping.bytecode.patch

import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.layout.seekbar.seekbartapping.bytecode.fingerprints.seekbarTappingMethodMatch
import app.revanced.patches.youtube.layout.seekbar.seekbartapping.bytecode.fingerprints.seekbarTappingParentMethodMatch
import app.revanced.patches.youtube.misc.resourceid.patch.sharedResourceIdPatch
import app.revanced.shared.util.integrations.Constants.SEEKBAR_LAYOUT
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction

// Old name: enable-seekbar-tapping-bytecode-patch (helper patch, never selectable)
val seekbarTappingBytecodePatch = bytecodePatch(
    description = "Enables tap-to-seek on the seekbar of the video player (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(sharedResourceIdPatch)

    apply {
        val tapSeekMethods = mutableMapOf<String, Method>()

        for (method in seekbarTappingParentMethodMatch.immutableClassDef.methods) {
            val implementation = method.implementation ?: continue
            val instructions = implementation.instructions

            // here we make sure we actually find the method because it has more than 7 instructions
            if (instructions.count() < 7) continue

            // we know that the 7th instruction has the opcode CONST_4
            val instruction = instructions.elementAt(6)
            if (instruction.opcode != Opcode.CONST_4) continue

            // the literal for this instruction has to be either 1 or 2
            val literal = (instruction as NarrowLiteralInstruction).narrowLiteral

            // method founds
            if (literal == 1) tapSeekMethods["P"] = method
            if (literal == 2) tapSeekMethods["O"] = method
        }

        val pMethod = tapSeekMethods["P"] ?: throw PatchException("Could not find the tap seek method P")
        val oMethod = tapSeekMethods["O"] ?: throw PatchException("Could not find the tap seek method O")

        seekbarTappingMethodMatch.let {
            val insertIndex = it[-1]

            with(it.method) {
                val register = getInstruction<FiveRegisterInstruction>(insertIndex - 1).registerC

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $SEEKBAR_LAYOUT->enableSeekbarTapping()Z
                        move-result v0
                        if-eqz v0, :off
                        invoke-virtual { v$register, v2 }, ${oMethod.definingClass}->${oMethod.name}(I)V
                        invoke-virtual { v$register, v2 }, ${pMethod.definingClass}->${pMethod.name}(I)V
                    """, ExternalLabel("off", getInstruction(insertIndex))
                )
            }
        }
    }
}
