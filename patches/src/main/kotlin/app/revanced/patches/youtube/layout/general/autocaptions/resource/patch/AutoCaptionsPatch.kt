package app.revanced.patches.youtube.layout.general.autocaptions.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.general.autocaptions.bytecode.patch.autoCaptionsBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-auto-captions
val autoCaptionsPatch = resourcePatch(
    name = "Hide auto captions",
    description = "Hide captions from being automatically enabled.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        autoCaptionsBytecodePatch,
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
            "SETTINGS: HIDE_AUTO_CAPTIONS"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-auto-captions"
        )
    }
}
