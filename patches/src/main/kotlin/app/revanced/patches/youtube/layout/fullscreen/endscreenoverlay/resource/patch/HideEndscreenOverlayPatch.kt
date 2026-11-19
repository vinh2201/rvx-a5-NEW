package app.revanced.patches.youtube.layout.fullscreen.endscreenoverlay.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.fullscreen.endscreenoverlay.bytecode.patch.hideEndscreenOverlayBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-endscreen-overlay (old class name: HideFilmStripPatch)
val hideEndscreenOverlayPatch = resourcePatch(
    name = "Hide endscreen overlay (A5)",
    description = "Hide endscreen overlay on swipe controls.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        hideEndscreenOverlayBytecodePatch,
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
            "PREFERENCE_HEADER: FULLSCREEN",
            "SETTINGS: HIDE_ENDSCREEN_OVERLAY"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-endscreen-overlay"
        )
    }
}
