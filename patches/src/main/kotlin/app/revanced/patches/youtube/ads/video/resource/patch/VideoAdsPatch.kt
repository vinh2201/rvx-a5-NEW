package app.revanced.patches.youtube.ads.video.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.ads.video.bytecode.patch.videoAdsBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-video-ads
val videoAdsPatch = resourcePatch(
    name = "Hide video ads (A5)",
    description = "Removes ads in the video player.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        videoAdsBytecodePatch,
        settingsPatch,
    )

    apply {
        /*
         add settings
         */
        ResourceHelper.addSettings(
            this,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: ADS_SETTINGS",
            "SETTINGS: HIDE_VIDEO_ADS"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-video-ads"
        )
    }
}
