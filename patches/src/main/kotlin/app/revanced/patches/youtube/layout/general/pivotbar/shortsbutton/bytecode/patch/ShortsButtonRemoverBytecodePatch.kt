package app.revanced.patches.youtube.layout.general.pivotbar.shortsbutton.bytecode.patch

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.resourceid.patch.sharedResourceIdPatch
import app.revanced.shared.extensions.findOpcodePatternOrThrow
import app.revanced.shared.fingerprints.pivotBarMethodMatch
import app.revanced.shared.util.integrations.Constants.GENERAL_LAYOUT
import app.revanced.shared.util.pivotbar.InjectionUtils.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.shared.util.pivotbar.InjectionUtils.injectHook
import com.android.tools.smali.dexlib2.Opcode

// Former companion object of ShortsButtonRemoverBytecodePatch
private const val enumHook =
    "sput-object v$REGISTER_TEMPLATE_REPLACEMENT, $GENERAL_LAYOUT" +
            "->" +
            "lastPivotTab:Ljava/lang/Enum;"

private const val buttonHook =
    "invoke-static { v$REGISTER_TEMPLATE_REPLACEMENT }, $GENERAL_LAYOUT" +
            "->" +
            "hideShortsButton(Landroid/view/View;)V"

// Old name: hide-shorts-button (helper patch, never selectable)
val shortsButtonRemoverBytecodePatch = bytecodePatch(
    description = "Hides the shorts button in the navigation bar (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(sharedResourceIdPatch)

    apply {
        /*
         * Resolve the patterns inside the pivot bar method
         * (old PivotBarEnumFingerprint / PivotBarShortsButtonViewFingerprint, resolved against the PivotBarFingerprint method)
         */
        val pivotBarMethod = pivotBarMethodMatch.method

        val enumScanResult = pivotBarMethod.findOpcodePatternOrThrow(
            "PivotBarEnumFingerprint",
            Opcode.INVOKE_STATIC,
            Opcode.MOVE_RESULT_OBJECT,
            Opcode.IF_NEZ, // target reference
            Opcode.SGET_OBJECT,
            Opcode.INVOKE_VIRTUAL,
            Opcode.MOVE_RESULT,
        )
        val buttonViewResult = pivotBarMethod.findOpcodePatternOrThrow(
            "PivotBarShortsButtonViewFingerprint",
            Opcode.INVOKE_VIRTUAL_RANGE,
            Opcode.MOVE_RESULT_OBJECT, // target reference
            Opcode.GOTO,
        )

        val enumHookInsertIndex = enumScanResult.first + 2
        val buttonHookInsertIndex = buttonViewResult.last

        // The hook with the highest index goes first, so the other index stays valid.
        mapOf(
            buttonHook to buttonHookInsertIndex,
            enumHook to enumHookInsertIndex
        ).entries.sortedByDescending { it.value }.forEach { (hook, insertIndex) ->
            pivotBarMethod.injectHook(hook, insertIndex)
        }
    }
}
