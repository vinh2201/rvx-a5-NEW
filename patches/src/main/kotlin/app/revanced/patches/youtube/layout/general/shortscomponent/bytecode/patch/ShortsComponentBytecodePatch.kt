package app.revanced.patches.youtube.layout.general.shortscomponent.bytecode.patch

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.shared.extensions.applyToLiteralHits
import app.revanced.shared.extensions.findLiteralHits
import app.revanced.shared.extensions.injectHideCall
import app.revanced.shared.util.bytecode.BytecodeHelper
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

// Old name: hide-shorts-component-bytecode-patch (helper patch, never selectable)
val shortsComponentBytecodePatch = bytecodePatch(
    description = "Hides other Shorts components (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(resourceMappingPatch)

    apply {
        // drawable "ic_right_comment_32c"
        val shortsCommentId = ResourceType.DRAWABLE["ic_right_comment_32c"]

        applyToLiteralHits(findLiteralHits(shortsCommentId), "ic_right_comment_32c") { hit, method ->
            val instructions = hit.method.implementation!!.instructions

            // shorts player comment
            val invokeInstruction = instructions.elementAtOrNull(hit.index - 3)
            if (invokeInstruction?.opcode != Opcode.CONST_HIGH16) return@applyToLiteralHits false

            val viewRegister = (instructions.elementAt(hit.index + 3) as OneRegisterInstruction).registerA
            method.injectHideCall(hit.index + 4, viewRegister, "layout/GeneralLayoutPatch", "hideShortsPlayerCommentsButton")
            true
        }

        BytecodeHelper.patchStatus(this, "ShortsComponent")
    }
}
