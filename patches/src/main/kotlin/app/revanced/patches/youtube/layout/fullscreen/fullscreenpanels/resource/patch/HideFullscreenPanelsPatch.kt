package app.revanced.patches.youtube.layout.fullscreen.fullscreenpanels.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.fullscreen.fullscreenpanels.bytecode.patch.fullscreenButtonContainerBytecodePatch
import app.revanced.patches.youtube.layout.fullscreen.fullscreenpanels.bytecode.patch.hideFullscreenPanelsBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-fullscreen-panels
val hideFullscreenPanelsPatch = resourcePatch(
    name = "Hide fullscreen panels (A5)",
    description = "Hides video description and comments panel in fullscreen view.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        fullscreenButtonContainerBytecodePatch,
        hideFullscreenPanelsBytecodePatch,
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
            "SETTINGS: HIDE_FULLSCREEN_PANELS"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-fullscreen-panels"
        )
    }
}
