package app.revanced.patches.youtube.layout.player.autoplaybutton.bytecode.patch

import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.youtube.misc.resourceid.patch.sharedResourceIdPatch
import app.revanced.shared.fingerprints.layoutConstructorMethodMatch
import app.revanced.shared.util.integrations.Constants.PLAYER_LAYOUT
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

// Old name: hide-autoplay-button-bytecode-patch (helper patch, never selectable)
val hideAutoplayButtonBytecodePatch = bytecodePatch(
    description = "Hides the autoplay button in the video player (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        resourceMappingPatch,
        sharedResourceIdPatch,
    )

    apply {
        // resolve the offsets such as ...
        val autoNavPreviewStubId = ResourceType.ID["autonav_preview_stub"]

        layoutConstructorMethodMatch.method.let { method ->
            val instructions = method.implementation!!.instructions.toList()

            // where to insert the branch instructions and ...
            val insertIndex = instructions.indexOfFirst {
                (it as? WideLiteralInstruction)?.wideLiteral == autoNavPreviewStubId
            }

            // where to branch away
            val branchIndex = instructions.subList(insertIndex + 1, instructions.size - 1).indexOfFirst {
                ((it as? ReferenceInstruction)?.reference as? MethodReference)?.name == "addOnLayoutChangeListener"
            } + 2

            val jumpInstruction = instructions[insertIndex + branchIndex]

            method.addInstructionsWithLabels(
                insertIndex, """
                    invoke-static {}, $PLAYER_LAYOUT->hideAutoPlayButton()Z
                    move-result v14
                    if-nez v14, :hidden
                """, ExternalLabel("hidden", jumpInstruction)
            )
        }
    }
}
