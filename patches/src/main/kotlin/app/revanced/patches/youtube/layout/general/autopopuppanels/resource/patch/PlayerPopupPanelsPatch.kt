package app.revanced.patches.youtube.layout.general.autopopuppanels.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.general.autopopuppanels.bytecode.patch.playerPopupPanelsBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-auto-player-popup-panels
val playerPopupPanelsPatch = resourcePatch(
    name = "Hide auto player popup panels (A5)",
    description = "Hide automatic popup panels (playlist or live chat) on video player.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        playerPopupPanelsBytecodePatch,
        settingsPatch,
    )

    apply {
        /*
         add settings
         */
        ResourceHelper.addSettings2(
            this,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: LAYOUT_SETTINGS",
            "PREFERENCE_HEADER: GENERAL",
            "SETTINGS: HIDE_AUTO_PLAYER_POPUP_PANELS"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-auto-player-popup-panels"
        )
    }
}
