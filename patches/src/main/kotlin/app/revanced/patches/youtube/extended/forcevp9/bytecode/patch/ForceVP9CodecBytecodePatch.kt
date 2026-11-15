package app.revanced.patches.youtube.extended.forcevp9.bytecode.patch

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.removeInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.integrations.patch.integrationsPatch
import app.revanced.patches.youtube.extended.forcevp9.bytecode.fingerprints.vp9PrimaryMethodMatch
import app.revanced.patches.youtube.extended.forcevp9.bytecode.fingerprints.vp9PropsMethodMatch
import app.revanced.patches.youtube.extended.forcevp9.bytecode.fingerprints.vp9PropsParentMethodMatch
import app.revanced.patches.youtube.extended.forcevp9.bytecode.fingerprints.vp9SecondaryMethodMatch
import app.revanced.shared.fingerprints.layoutSwitchMethodMatch
import app.revanced.shared.util.integrations.Constants.EXTENDED_PATH
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

// Former companion object of ForceVP9CodecBytecodePatch
private const val INTEGRATIONS_CLASS_DESCRIPTOR =
    "$EXTENDED_PATH/CodecOverridePatch;"

private const val INTEGRATIONS_CLASS_METHOD_REFERENCE =
    "$INTEGRATIONS_CLASS_DESCRIPTOR->shouldForceVP9(Z)Z"

private fun MutableMethod.hookOverride(
    index: Int,
    register: Int
) {
    addInstructions(
        index, """
            invoke-static {v$register}, $INTEGRATIONS_CLASS_METHOD_REFERENCE
            move-result v$register
            return v$register
        """
    )
}

/**
 * [startIndex] and [endIndex] are the indices of the two `return` instructions of the matched pattern.
 */
private fun MutableMethod.injectOverride(startIndex: Int, endIndex: Int) {
    val startRegister = getInstruction<OneRegisterInstruction>(startIndex).registerA
    val endRegister = getInstruction<OneRegisterInstruction>(endIndex).registerA

    hookOverride(endIndex + 1, endRegister)
    removeInstruction(endIndex)
    hookOverride(startIndex + 1, startRegister)
    removeInstruction(startIndex)
}

private fun MutableMethod.hookProps(
    fieldName: String,
    descriptor: String
) {
    val instructions = implementation!!.instructions

    for ((index, instruction) in instructions.withIndex()) {
        if (instruction.opcode != Opcode.SGET_OBJECT) continue

        val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference ?: continue

        if (reference.definingClass != "Landroid/os/Build;" ||
            reference.name != fieldName ||
            reference.type != "Ljava/lang/String;"
        ) continue

        val register = (instruction as OneRegisterInstruction).registerA

        addInstructions(
            index + 1, """
                invoke-static {v$register}, $INTEGRATIONS_CLASS_DESCRIPTOR->$descriptor(Ljava/lang/String;)Ljava/lang/String;
                move-result-object v$register
            """
        )
        break
    }
}

// Old name: force-vp9-codec-bytecode-patch (helper patch, never selectable)
val forceVP9CodecBytecodePatch = bytecodePatch(
    description = "Forces the VP9 codec for videos (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(integrationsPatch)

    apply {
        // The VP9 helper methods live in the class of the layout switch method.
        val layoutSwitchClass = layoutSwitchMethodMatch.immutableClassDef

        val primaryMatch = layoutSwitchClass.vp9PrimaryMethodMatch
        val secondaryMatch = layoutSwitchClass.vp9SecondaryMethodMatch

        primaryMatch.method.injectOverride(primaryMatch[0], primaryMatch[-1])
        secondaryMatch.method.injectOverride(secondaryMatch[0], secondaryMatch[-1])

        vp9PropsParentMethodMatch.immutableClassDef.vp9PropsMethodMatch.method.let {
            mapOf(
                "MANUFACTURER" to "getManufacturer",
                "BRAND" to "getBrand",
                "MODEL" to "getModel"
            ).forEach { (fieldName, descriptor) ->
                it.hookProps(fieldName, descriptor)
            }
        }
    }
}
