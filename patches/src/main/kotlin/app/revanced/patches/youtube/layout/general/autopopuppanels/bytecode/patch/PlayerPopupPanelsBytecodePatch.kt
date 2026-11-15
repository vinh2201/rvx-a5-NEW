package app.revanced.patches.youtube.layout.general.autopopuppanels.bytecode.patch

import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.layout.general.autopopuppanels.bytecode.fingerprints.engagementPanelControllerMethodMatch
import app.revanced.shared.util.integrations.Constants.GENERAL_LAYOUT

// Old name: hide-auto-player-popup-panels-bytecode-patch (helper patch, never selectable)
val playerPopupPanelsBytecodePatch = bytecodePatch(
    description = "Hide automatic popup panels on the video player (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    apply {
        engagementPanelControllerMethodMatch.method.let {
            it.addInstructionsWithLabels(
                0, """
                    invoke-static {}, $GENERAL_LAYOUT->hideAutoPlayerPopupPanels()Z
                    move-result v0
                    if-eqz v0, :player_popup_panels_shown
                    if-eqz p4, :player_popup_panels_shown
                    const/4 v0, 0x0
                    return-object v0
                """, ExternalLabel("player_popup_panels_shown", it.getInstruction(0))
            )
        }
    }
}
