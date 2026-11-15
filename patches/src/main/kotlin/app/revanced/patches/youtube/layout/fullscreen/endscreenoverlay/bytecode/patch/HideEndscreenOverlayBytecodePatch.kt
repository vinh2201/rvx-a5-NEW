package app.revanced.patches.youtube.layout.fullscreen.endscreenoverlay.bytecode.patch

import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.shared.extensions.applyToLiteralHits
import app.revanced.shared.extensions.findLiteralHits
import app.revanced.shared.util.integrations.Constants.FULLSCREEN_LAYOUT
import com.android.tools.smali.dexlib2.Opcode

// Old name: hide-endscreen-overlay-bytecode-patch (helper patch, never selectable)
val hideEndscreenOverlayBytecodePatch = bytecodePatch(
    description = "Hide endscreen overlay on swipe controls (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(resourceMappingPatch)

    apply {
        // layout "app_related_endscreen_results"
        val endscreenResultsId = ResourceType.LAYOUT["app_related_endscreen_results"]

        applyToLiteralHits(findLiteralHits(endscreenResultsId), "app_related_endscreen_results") { hit, method ->
            // end screen result
            val insertIndex = hit.index - 13
            val ifNezInstruction = hit.method.implementation!!.instructions.elementAtOrNull(insertIndex)
            if (ifNezInstruction?.opcode != Opcode.IF_NEZ) return@applyToLiteralHits false

            val dummyRegister = hit.register

            method.addInstructionsWithLabels(
                insertIndex, """
                    invoke-static {}, $FULLSCREEN_LAYOUT->hideEndscreenOverlay()Z
                    move-result v$dummyRegister
                    if-eqz v$dummyRegister, :on
                    return-void
                """, ExternalLabel("on", method.getInstruction(insertIndex))
            )
            true
        }
    }
}
