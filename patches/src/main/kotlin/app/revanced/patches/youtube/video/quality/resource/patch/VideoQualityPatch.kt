package app.revanced.patches.youtube.video.quality.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.patches.youtube.video.quality.bytecode.patch.videoQualityBytecodePatch
import app.revanced.shared.util.resources.ResourceHelper
import app.revanced.shared.util.resources.ResourceUtils.copyXmlNode

// Old name: default-video-quality
val videoQualityPatch = resourcePatch(
    name = "Default video quality",
    description = "Adds ability to set default video quality settings.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        videoQualityBytecodePatch,
        settingsPatch,
    )

    apply {
        /*
         * Copy arrays
         */
        copyXmlNode("youtube/quality/host", "values/arrays.xml", "resources")

        /*
         add settings
         */
        ResourceHelper.addSettings(
            this,
            "PREFERENCE_CATEGORY: REVANCED_EXTENDED_SETTINGS",
            "PREFERENCE: VIDEO_SETTINGS",
            "SETTINGS: DEFAULT_VIDEO_QUALITY"
        )

        ResourceHelper.patchSuccess(
            this,
            "default-video-quality"
        )
    }
}
