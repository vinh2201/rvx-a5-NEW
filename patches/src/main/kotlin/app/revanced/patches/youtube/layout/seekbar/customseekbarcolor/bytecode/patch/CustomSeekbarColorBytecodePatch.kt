package app.revanced.patches.youtube.layout.seekbar.customseekbarcolor.bytecode.patch

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.shared.extensions.applyToLiteralHits
import app.revanced.shared.extensions.findLiteralHits
import app.revanced.shared.util.integrations.Constants.SEEKBAR_LAYOUT
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

// Old name: custom-seekbar-color-bytecode-patch (helper patch, never selectable)
// TODO: Fix (16.40.36): the color "inline_time_bar_colorized_bar_played_color_dark" does not exist in 16.40.36
// (it is looked up lazily, so nothing fails as long as the resource patch below stays unselectable).
val customSeekbarColorBytecodePatch = bytecodePatch(
    description = "Change seekbar color in dark mode (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(resourceMappingPatch)

    apply {
        // color "inline_time_bar_colorized_bar_played_color_dark"
        val playedColorDarkId = ResourceType.COLOR["inline_time_bar_colorized_bar_played_color_dark"]

        applyToLiteralHits(findLiteralHits(playedColorDarkId), "inline_time_bar_colorized_bar_played_color_dark") { hit, method ->
            // seekbar color
            val registerIndex = hit.index + 2
            val viewRegister = (hit.method.implementation!!.instructions.elementAt(registerIndex) as OneRegisterInstruction).registerA

            method.addInstructions(
                registerIndex + 1, """
                    invoke-static {v$viewRegister}, $SEEKBAR_LAYOUT->enableCustomSeekbarColor(I)I
                    move-result v$viewRegister
                """
            )
            true
        }
    }
}
