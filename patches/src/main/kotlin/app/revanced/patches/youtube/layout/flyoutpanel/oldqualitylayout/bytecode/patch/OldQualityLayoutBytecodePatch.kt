package app.revanced.patches.youtube.layout.flyoutpanel.oldqualitylayout.bytecode.patch

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.layout.flyoutpanel.oldqualitylayout.bytecode.fingerprints.qualityMenuViewInflateMethodMatch
import app.revanced.patches.youtube.misc.resourceid.patch.sharedResourceIdPatch
import app.revanced.shared.util.integrations.Constants.FLYOUTPANEL_LAYOUT
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

// Old name: enable-old-quality-layout-bytecode-patch (helper patch, never selectable)
val oldQualityLayoutBytecodePatch = bytecodePatch(
    description = "Enables the original quality flyout menu (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(sharedResourceIdPatch)

    apply {
        qualityMenuViewInflateMethodMatch.method.let {
            val insertIndex = it.implementation!!.instructions.count() - 1 - 1
            val register = it.getInstruction<FiveRegisterInstruction>(insertIndex).registerC

            it.addInstructions(
                insertIndex, // insert the integrations instructions right before the listener
                "invoke-static { v$register }, $FLYOUTPANEL_LAYOUT->enableOldQualityMenu(Landroid/widget/ListView;)V"
            )
        }
    }
}
