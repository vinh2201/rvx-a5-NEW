package app.revanced.patches.youtube.layout.player.suggestactions.bytecode.patch

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.shared.extensions.applyToLiteralHits
import app.revanced.shared.extensions.findLiteralHits
import app.revanced.shared.extensions.injectHideCall
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

// Old name: hide-suggested-actions-bytecode-patch (helper patch, never selectable)
val suggestedActionsBytecodePatch = bytecodePatch(
    description = "Hide the suggested actions bar inside the player (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(resourceMappingPatch)

    apply {
        // layout "suggested_action"
        val suggestedActionId = ResourceType.LAYOUT["suggested_action"]

        applyToLiteralHits(findLiteralHits(suggestedActionId), "suggested_action") { hit, method ->
            val insertIndex = hit.index + 4
            val iPutInstruction = hit.method.implementation!!.instructions.elementAtOrNull(insertIndex)
            if (iPutInstruction?.opcode != Opcode.IPUT_OBJECT) return@applyToLiteralHits false

            val viewRegister = (iPutInstruction as TwoRegisterInstruction).registerA
            method.injectHideCall(insertIndex, viewRegister, "layout/PlayerLayoutPatch", "hideSuggestedActions")
            true
        }
    }
}
