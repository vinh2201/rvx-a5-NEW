package app.revanced.patches.youtube.layout.general.personalinformation.bytecode.patch

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.layout.general.personalinformation.bytecode.fingerprints.accountSwitcherAccessibilityLabelMethodMatch
import app.revanced.patches.youtube.misc.resourceid.patch.sharedResourceIdPatch
import app.revanced.shared.util.integrations.Constants.GENERAL_LAYOUT
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

// Old name: hide-email-address-bytecode-patch (helper patch, never selectable)
val hideEmailAddressBytecodePatch = bytecodePatch(
    description = "Hides the email address in the account switcher (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(sharedResourceIdPatch)

    apply {
        accountSwitcherAccessibilityLabelMethodMatch.let {
            val insertIndex = it[-1]

            with(it.method) {
                val register = getInstruction<OneRegisterInstruction>(insertIndex - 2).registerA
                addInstructions(
                    insertIndex, """
                        invoke-static {v$register}, $GENERAL_LAYOUT->hideEmailAddress(I)I
                        move-result v$register
                    """
                )
            }
        }
    }
}
