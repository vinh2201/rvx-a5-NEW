package app.revanced.patches.youtube.misc.litho.filter.patch

import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.stringReference
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.doublebacktoclose.patch.doubleBackToClosePatch
import app.revanced.patches.youtube.misc.litho.filter.fingerprints.lithoMethodMatch
import app.revanced.patches.youtube.misc.swiperefresh.patch.swipeRefreshPatch
import app.revanced.shared.util.integrations.Constants.ADS_PATH
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

// Old name: LithoFilterPatch (helper patch, never selectable)
val lithoFilterPatch = bytecodePatch {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        doubleBackToClosePatch,
        swipeRefreshPatch,
    )

    apply {

        //Litho
        val lithoMethod = lithoMethodMatch.method
        val lithoMethodThirdParam = lithoMethod.parameterTypes[2]
        val lithoRegister1 = lithoMethod.implementation!!.registerCount - lithoMethod.parameters.size + 1
        val lithoRegister2 = lithoRegister1 + 1

        val lithoIndex = lithoMethod.indexOfFirstInstructionOrThrow {
            opcode == Opcode.CONST_STRING &&
                    stringReference?.string == "Element missing type extension"
        } + 2

        val firstReference =
            lithoMethod.getInstruction<ReferenceInstruction>(lithoIndex).reference as MethodReference

        val secondReference =
            lithoMethod.getInstruction<ReferenceInstruction>(lithoIndex + 2).reference as FieldReference

        lithoMethod.addInstructionsWithLabels(
            0, """
                move-object/from16 v1, v$lithoRegister1
                invoke-virtual {v1}, Ljava/lang/Object;->toString()Ljava/lang/String;
                move-result-object v1
                move-object/from16 v2, v$lithoRegister2
                iget-object v2, v2, $lithoMethodThirdParam->b:Ljava/nio/ByteBuffer;
                invoke-static {v1, v2}, $ADS_PATH/ExtendedLithoFilterPatch;->InflatedLithoView(Ljava/lang/String;Ljava/nio/ByteBuffer;)Z
                move-result v3
                if-eqz v3, :do_not_block
                move-object/from16 v0, p1
                invoke-static {v0}, $firstReference
                move-result-object v0
                iget-object v0, v0, ${secondReference.definingClass}->${secondReference.name}:${secondReference.type}
                return-object v0
            """, ExternalLabel("do_not_block", lithoMethod.getInstruction(0))
        )
    }
}
