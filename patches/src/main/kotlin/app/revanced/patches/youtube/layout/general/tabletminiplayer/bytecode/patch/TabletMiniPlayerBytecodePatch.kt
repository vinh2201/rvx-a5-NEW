package app.revanced.patches.youtube.layout.general.tabletminiplayer.bytecode.patch

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.layout.general.tabletminiplayer.bytecode.fingerprints.miniPlayerDimensionsCalculatorMethodMatch
import app.revanced.patches.youtube.layout.general.tabletminiplayer.bytecode.fingerprints.miniPlayerOverrideMethodMatch
import app.revanced.patches.youtube.layout.general.tabletminiplayer.bytecode.fingerprints.miniPlayerOverrideNoContextMethodMatch
import app.revanced.patches.youtube.layout.general.tabletminiplayer.bytecode.fingerprints.miniPlayerResponseModelSizeCheckMethodMatch
import app.revanced.patches.youtube.misc.resourceid.patch.sharedResourceIdPatch
import app.revanced.shared.util.integrations.Constants.GENERAL_LAYOUT
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private fun MutableMethod.insertOverride(index: Int, overrideRegister: Int) {
    this.addInstructions(
        index,
        """
            invoke-static {v$overrideRegister}, $GENERAL_LAYOUT->enableTabletMiniPlayer(Z)Z
            move-result v$overrideRegister
        """
    )
}

// Old name: enable-tablet-miniplayer-bytecode-patch (helper patch, never selectable)
val tabletMiniPlayerBytecodePatch = bytecodePatch(
    description = "Enables the tablet mini player layout (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(sharedResourceIdPatch)

    apply {
        // first resolve the fingerprints via the parent fingerprint
        val miniPlayerClass = miniPlayerDimensionsCalculatorMethodMatch.immutableClassDef

        // resolve all indices before anything is mutated
        val noContextMatch = miniPlayerClass.miniPlayerOverrideNoContextMethodMatch
        val noContextIndex = noContextMatch[-1]

        val overrideMatch = miniPlayerClass.miniPlayerOverrideMethodMatch
        val overrideIndex = overrideMatch[-1]

        // not present in 16.40.36 -> optional
        val sizeCheckMatch = miniPlayerClass.miniPlayerResponseModelSizeCheckMethodMatch
        val sizeCheckIndex = sizeCheckMatch.methodOrNull?.let { sizeCheckMatch[-1] }

        // MiniPlayerOverrideNoContextFingerprint
        with(noContextMatch.method) {
            val parameterRegister = getInstruction<OneRegisterInstruction>(noContextIndex).registerA
            insertOverride(noContextIndex, parameterRegister)
            // second hook on the last instruction of the method
            insertOverride(implementation!!.instructions.size - 1, parameterRegister)
        }

        // MiniPlayerOverrideFingerprint
        with(overrideMatch.method) {
            val parameterRegister = getInstruction<OneRegisterInstruction>(overrideIndex).registerA
            insertOverride(overrideIndex, parameterRegister)
        }

        // MiniPlayerResponseModelSizeCheckFingerprint
        if (sizeCheckIndex != null) {
            with(sizeCheckMatch.method) {
                val parameterRegister = getInstruction<OneRegisterInstruction>(sizeCheckIndex).registerA
                insertOverride(sizeCheckIndex, parameterRegister)
            }
        }
    }
}
