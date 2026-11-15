package app.revanced.patches.youtube.layout.general.crowdfundingbox.bytecode.patch

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.shared.extensions.applyToLiteralHits
import app.revanced.shared.extensions.findLiteralHits
import app.revanced.shared.extensions.injectHideCall
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

// Old name: hide-crowdfunding-box-bytecode-patch (helper patch, never selectable)
val crowdfundingBoxBytecodePatch = bytecodePatch(
    description = "Hides the crowdfunding box (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(resourceMappingPatch)

    apply {
        // layout "donation_companion"
        val donationCompanionId = ResourceType.LAYOUT["donation_companion"]

        applyToLiteralHits(findLiteralHits(donationCompanionId), "donation_companion") { hit, method ->
            val insertIndex = hit.index + 5
            val iPutInstruction = hit.method.implementation!!.instructions.elementAtOrNull(insertIndex)
            if (iPutInstruction?.opcode != Opcode.IPUT_OBJECT) return@applyToLiteralHits false

            val viewRegister = (iPutInstruction as TwoRegisterInstruction).registerA
            method.injectHideCall(insertIndex, viewRegister, "layout/GeneralLayoutPatch", "hideCrowdfundingBox")
            true
        }
    }
}
