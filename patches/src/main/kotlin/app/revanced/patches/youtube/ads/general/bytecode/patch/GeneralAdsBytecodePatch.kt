package app.revanced.patches.youtube.ads.general.bytecode.patch

import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.youtube.ads.general.bytecode.fingerprints.componentContextParserMethodMatch
import app.revanced.patches.youtube.misc.integrations.patch.integrationsPatch
import app.revanced.shared.util.bytecode.BytecodeHelper
import app.revanced.shared.util.integrations.Constants.ADS_PATH
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private fun ReferenceInstruction.toDescriptor() = when (val reference = this.reference) {
    is MethodReference -> "${reference.definingClass}->${reference.name}(${
        reference.parameterTypes.joinToString(
            ""
        ) { it }
    })${reference.returnType}"

    is FieldReference -> "${reference.definingClass}->${reference.name}:${reference.type}"
    else -> throw PatchException("Unsupported reference type")
}

// Old name: hide-general-ads-bytecode-patch (helper patch, never selectable)
val generalAdsBytecodePatch = bytecodePatch(
    description = "Hooks the method which parses the bytes into a ComponentContext to filter components.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        integrationsPatch, // PatchStatus (extension class) is edited below
        resourceMappingPatch,
    )

    apply {
        with(componentContextParserMethodMatch.method) {
            // old: EmptyComponentBuilderFingerprint resolved inside this method -> first INVOKE_STATIC_RANGE
            val builderMethodIndex = indexOfFirstInstructionOrThrow(Opcode.INVOKE_STATIC_RANGE)
            val emptyComponentFieldIndex = builderMethodIndex + 2

            val insertHookIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.CONST_16 &&
                        (this as? NarrowLiteralInstruction)?.narrowLiteral == 124
            } + 3

            val stringBuilderRegister = getInstruction<TwoRegisterInstruction>(insertHookIndex - 1).registerA
            val clobberedRegister = getInstruction<OneRegisterInstruction>(insertHookIndex - 3).registerA

            val builderMethodDescriptor = getInstruction<ReferenceInstruction>(builderMethodIndex).toDescriptor()
            val emptyComponentFieldDescriptor = getInstruction<ReferenceInstruction>(emptyComponentFieldIndex).toDescriptor()

            addInstructionsWithLabels(
                insertHookIndex, // right after setting the component.pathBuilder field,
                """
                    invoke-static {v$stringBuilderRegister}, $ADS_PATH/LithoFilterPatch;->filter(Ljava/lang/StringBuilder;)Z
                    move-result v$clobberedRegister
                    if-eqz v$clobberedRegister, :not_an_ad
                    move-object/from16 v$stringBuilderRegister, p1
                    invoke-static {v$stringBuilderRegister}, $builderMethodDescriptor
                    move-result-object v$clobberedRegister
                    iget-object v$clobberedRegister, v$clobberedRegister, $emptyComponentFieldDescriptor
                    return-object v$clobberedRegister
                """,
                ExternalLabel("not_an_ad", getInstruction(insertHookIndex))
            )
        }

        BytecodeHelper.patchStatus(this, "GeneralAds")
    }
}
