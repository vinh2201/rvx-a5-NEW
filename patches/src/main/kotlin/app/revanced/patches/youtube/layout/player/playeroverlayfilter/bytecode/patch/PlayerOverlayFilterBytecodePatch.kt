package app.revanced.patches.youtube.layout.player.playeroverlayfilter.bytecode.patch

import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.shared.extensions.applyToLiteralHits
import app.revanced.shared.extensions.findLiteralHits
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

// Old name: hide-player-overlay-filter-bytecode-patch (helper patch, never selectable)
val playerOverlayFilterBytecodePatch = bytecodePatch(
    description = "Remove the dark filter layer from the player's background (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(resourceMappingPatch)

    apply {
        // id "scrim_overlay" and color "google_transparent"
        val scrimOverlayId = ResourceType.ID["scrim_overlay"]
        val transparent = ResourceType.COLOR["google_transparent"]

        applyToLiteralHits(findLiteralHits(scrimOverlayId), "scrim_overlay") { hit, method ->
            // player overlay filter
            val insertIndex = hit.index + 3
            val checkCastInstruction = hit.method.implementation!!.instructions.elementAtOrNull(insertIndex)
            if (checkCastInstruction?.opcode != Opcode.CHECK_CAST) return@applyToLiteralHits false

            val dummyRegister = hit.register
            val viewRegister = (checkCastInstruction as OneRegisterInstruction).registerA

            method.addInstructionsWithLabels(
                insertIndex + 1, """
                    invoke-static {}, Lapp/revanced/integrations/patches/layout/PlayerLayoutPatch;->hidePlayerOverlayFilter()Z
                    move-result v$dummyRegister
                    if-eqz v$dummyRegister, :currentcolor
                    const v$dummyRegister, $transparent
                    invoke-virtual {v$viewRegister, v$dummyRegister}, Landroid/widget/ImageView;->setImageResource(I)V
                """, ExternalLabel("currentcolor", method.getInstruction(insertIndex + 1))
            )
            true
        }
    }
}
