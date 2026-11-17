package app.revanced.patches.youtube.layout.player.infocards.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.player.infocards.bytecode.patch.hideInfoCardsBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-info-cards
val hideInfocardsPatch = resourcePatch(
    name = "Hide info cards (A5)",
    description = "Hides info-cards in videos.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        hideInfoCardsBytecodePatch,
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
            "PREFERENCE_HEADER: PLAYER",
            "SETTINGS: HIDE_INFO_CARDS"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-info-cards"
        )
    }
}
