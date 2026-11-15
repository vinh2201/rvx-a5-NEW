package app.revanced.patches.youtube.layout.fullscreen.fullscreenpanels.bytecode.patch

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.shared.extensions.applyToLiteralHits
import app.revanced.shared.extensions.findLiteralHits
import app.revanced.shared.extensions.injectHideCall
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

// Old name: hide-fullscreen-buttoncontainer-bytecode-patch (helper patch, never selectable)
val fullscreenButtonContainerBytecodePatch = bytecodePatch(
    description = "Hides the fullscreen button container (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(resourceMappingPatch)

    apply {
        // id "quick_actions_element_container"
        val quickActionsContainerId = ResourceType.ID["quick_actions_element_container"]

        applyToLiteralHits(findLiteralHits(quickActionsContainerId), "quick_actions_element_container") { hit, method ->
            // fullscreen panel
            val insertIndex = hit.index + 3
            val checkCastInstruction = hit.method.implementation!!.instructions.elementAtOrNull(insertIndex)
            if (checkCastInstruction?.opcode != Opcode.CHECK_CAST) return@applyToLiteralHits false

            val viewRegister = (checkCastInstruction as OneRegisterInstruction).registerA
            method.injectHideCall(insertIndex, viewRegister, "layout/FullscreenLayoutPatch", "hideFullscreenPanels")
            true
        }
    }
}
