package app.revanced.patches.youtube.layout.player.captionsbutton.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.player.captionsbutton.bytecode.patch.hideCaptionsButtonBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-player-captions-button
val hideCaptionsButtonPatch = resourcePatch(
    name = "Hide player captions button",
    description = "Hides the captions button in the video player.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        hideCaptionsButtonBytecodePatch,
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
            "SETTINGS: HIDE_CAPTIONS_BUTTON"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-captions-button"
        )
    }
}
