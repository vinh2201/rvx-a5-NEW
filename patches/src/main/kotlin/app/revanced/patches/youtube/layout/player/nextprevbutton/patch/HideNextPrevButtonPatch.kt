package app.revanced.patches.youtube.layout.player.nextprevbutton.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.patches.playerbutton.playerButtonPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-next-prev-button
val hideNextPrevButtonPatch = resourcePatch(
    name = "Hide next prev button",
    description = "Hides the next prev button in the player controller.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        playerButtonPatch,
        settingsPatch,
    )

    apply {
        /*
         add settings
         */
        ResourceHelper.addSettings3(
            this,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: LAYOUT_SETTINGS",
            "PREFERENCE_HEADER: PLAYER",
            "SETTINGS: HIDE_NEXT_BUTTON",
            "SETTINGS: HIDE_PREV_BUTTON"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-next-prev-button"
        )
    }
}
