package app.revanced.patches.youtube.layout.general.pivotbar.switchbutton.patch

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.layout.general.pivotbar.switchbutton.fingerprints.autoMotiveMethodMatch
import app.revanced.shared.util.integrations.Constants.GENERAL_LAYOUT
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

// Old name: switch-create-notification-bytecode-patch (helper patch, never selectable)
val switchCreateButtonBytecodePatch = bytecodePatch(
    description = "Switches the create button and notification button (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    apply {
        autoMotiveMethodMatch.let {
            val insertIndex = it[-1]

            with(it.method) {
                val register = getInstruction<OneRegisterInstruction>(insertIndex).registerA
                addInstructions(
                    insertIndex, """
                        invoke-static {v$register}, $GENERAL_LAYOUT->switchCreateNotification(Z)Z
                        move-result v$register
                    """
                )
            }
        }
    }
}
