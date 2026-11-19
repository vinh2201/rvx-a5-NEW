package app.revanced.patches.youtube.layout.player.endscreencards.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.player.endscreencards.bytecode.patch.hideEndscreenCardsBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-endscreen-cards
val hideEndscreenCardsPatch = resourcePatch(
    name = "Hide endscreen cards (A5)",
    description = "Hides the suggested video cards at the end of a video in fullscreen.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        hideEndscreenCardsBytecodePatch,
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
            "SETTINGS: HIDE_ENDSCREEN_CARDS"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-endscreen-cards"
        )
    }
}
