package app.revanced.patches.youtube.layout.player.castbutton.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.player.castbutton.bytecode.patch.hideCastButtonBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-cast-button
val hideCastButtonPatch = resourcePatch(
    name = "Hide cast button (A5)",
    description = "Hides the cast button in the video player.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        hideCastButtonBytecodePatch,
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
            "SETTINGS: HIDE_CAST_BUTTON"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-cast-button"
        )
    }
}
