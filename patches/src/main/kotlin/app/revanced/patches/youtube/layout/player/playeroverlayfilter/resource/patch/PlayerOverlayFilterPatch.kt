package app.revanced.patches.youtube.layout.player.playeroverlayfilter.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.player.playeroverlayfilter.bytecode.patch.playerOverlayFilterBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-player-overlay-filter
val playerOverlayFilterPatch = resourcePatch(
    name = "Hide player overlay filter (A5)",
    description = "Remove the dark filter layer from the player's background.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        settingsPatch,
        playerOverlayFilterBytecodePatch,
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
            "SETTINGS: HIDE_PALYER_OVERLAY_FILTER"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-player-overlay-filter"
        )
    }
}
