package app.revanced.patches.youtube.layout.player.autoplaybutton.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.player.autoplaybutton.bytecode.patch.hideAutoplayButtonBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-autoplay-button
val hideAutoplayButtonPatch = resourcePatch(
    name = "Hide autoplay button",
    description = "Hides the autoplay button in the video player.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        hideAutoplayButtonBytecodePatch,
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
            "SETTINGS: HIDE_AUTOPLAY_BUTTON"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-autoplay-button"
        )
    }
}
