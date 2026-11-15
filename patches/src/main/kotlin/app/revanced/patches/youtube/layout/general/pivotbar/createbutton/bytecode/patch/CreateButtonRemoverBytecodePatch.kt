package app.revanced.patches.youtube.layout.general.pivotbar.createbutton.bytecode.patch

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.resourceid.patch.sharedResourceIdPatch
import app.revanced.shared.extensions.findOpcodePatternOrThrow
import app.revanced.shared.fingerprints.pivotBarMethodMatch
import app.revanced.shared.util.integrations.Constants.GENERAL_LAYOUT
import app.revanced.shared.util.pivotbar.InjectionUtils.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.shared.util.pivotbar.InjectionUtils.injectHook
import com.android.tools.smali.dexlib2.Opcode

// Old name: hide-create-button-bytecode-patch (helper patch, never selectable)
val createButtonRemoverBytecodePatch = bytecodePatch(
    description = "Hides the create button in the navigation bar (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(sharedResourceIdPatch)

    apply {
        /*
         * Resolve the pattern inside the pivot bar method
         * (old PivotBarCreateButtonViewFingerprint, resolved against the PivotBarFingerprint method)
         */
        val pivotBarMethod = pivotBarMethodMatch.method

        val insertIndex = pivotBarMethod.findOpcodePatternOrThrow(
            "PivotBarCreateButtonViewFingerprint",
            Opcode.INVOKE_DIRECT_RANGE, // unique instruction anchor
            Opcode.CONST_4,
            Opcode.INVOKE_VIRTUAL,
            Opcode.MOVE_RESULT_OBJECT,
            Opcode.MOVE_OBJECT
        ).last

        /*
         * Inject hooks
         */
        val hook =
            "invoke-static { v$REGISTER_TEMPLATE_REPLACEMENT }, $GENERAL_LAYOUT->hideCreateButton(Landroid/view/View;)V"

        pivotBarMethod.injectHook(hook, insertIndex)
    }
}
