package app.revanced.patches.youtube.misc.sponsorblock.bytecode.patch

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.resourceid.patch.playerOverlaysId
import app.revanced.patches.youtube.misc.resourceid.patch.sharedResourceIdPatch
import app.revanced.patches.youtube.misc.resourceid.patch.totalTimeId
import app.revanced.shared.extensions.applyToLiteralHits
import app.revanced.shared.extensions.findLiteralHits
import com.android.tools.smali.dexlib2.Opcode

// Old name: sponsorblock-secondary-bytecode-patch (helper patch, never selectable)
//
// PORT-NOTE: the old patch resolved the ids with ResourceMappingPatch ("string total_time", "layout player_overlays")
// and mutated classes while iterating over them. The ids now come from sharedResourceIdPatch and the hits are
// collected first and applied afterwards. Verified in 16.40.36:
//  - total_time: krf.e(CharSequence x3)V, "const p3, total_time" -> getString -> move-result-object p1 -> iget-object (index + 3)
//  - player_overlays: ejk.b()Object, "const v8, player_overlays" -> inflate -> move-result-object v2 -> check-cast (index + 3)
val sponsorBlockSecondaryBytecodePatch = bytecodePatch(
    description = "Integrates SponsorBlock (secondary bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(sharedResourceIdPatch)

    apply {
        // total time
        applyToLiteralHits(findLiteralHits(totalTimeId), "SponsorBlock total time") { hit, method ->
            val insertIndex = hit.index + 3

            val instructions = method.implementation!!.instructions
            if (insertIndex >= instructions.size || instructions[insertIndex].opcode != Opcode.IGET_OBJECT) return@applyToLiteralHits false

            method.addInstructions(
                insertIndex, """
                    invoke-static {p1}, Lapp/revanced/integrations/sponsorblock/SponsorBlockUtils;->appendTimeWithoutSegments(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object p1
                """
            )
            true
        }

        // player overlay
        applyToLiteralHits(findLiteralHits(playerOverlaysId), "SponsorBlock player overlays") { hit, method ->
            val insertIndex = hit.index + 3

            val instructions = method.implementation!!.instructions
            if (insertIndex >= instructions.size || instructions[insertIndex].opcode != Opcode.CHECK_CAST) return@applyToLiteralHits false

            method.addInstruction(
                insertIndex,
                "invoke-static {v2}, Lapp/revanced/integrations/sponsorblock/player/ui/SponsorBlockView;->initialize(Ljava/lang/Object;)V"
            )
            true
        }
    }
}
