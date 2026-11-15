package app.revanced.patches.youtube.layout.player.endscreencards.bytecode.patch

import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.layout.player.endscreencards.bytecode.fingerprints.layoutCircleMethodMatch
import app.revanced.patches.youtube.layout.player.endscreencards.bytecode.fingerprints.layoutIconMethodMatch
import app.revanced.patches.youtube.layout.player.endscreencards.bytecode.fingerprints.layoutVideoMethodMatch
import app.revanced.patches.youtube.misc.resourceid.patch.sharedResourceIdPatch
import app.revanced.shared.extensions.injectHideCall
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

// Old name: hide-endscreen-cards-bytecode-patch (helper patch, never selectable)
val hideEndscreenCardsBytecodePatch = bytecodePatch(
    description = "Hides the suggested video cards at the end of a video in fullscreen (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(sharedResourceIdPatch)

    apply {
        // indices are resolved first, then applied from the highest to the lowest index
        listOf(layoutCircleMethodMatch, layoutIconMethodMatch, layoutVideoMethodMatch)
            .map { it to it[-1] }
            .sortedByDescending { (_, checkCastIndex) -> checkCastIndex }
            .forEach { (match, checkCastIndex) ->
                val layoutMethod = match.method
                val viewRegister = layoutMethod.getInstruction<OneRegisterInstruction>(checkCastIndex).registerA
                layoutMethod.injectHideCall(checkCastIndex + 1, viewRegister, "layout/PlayerLayoutPatch", "hideEndscreen")
            }
    }
}
