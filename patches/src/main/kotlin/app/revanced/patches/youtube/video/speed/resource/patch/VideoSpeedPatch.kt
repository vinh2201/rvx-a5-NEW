package app.revanced.patches.youtube.video.speed.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.patches.youtube.video.speed.bytecode.patch.videoSpeedBytecodePatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: default-video-speed
val videoSpeedPatch = resourcePatch(
    name = "Default video speed",
    description = "Adds ability to set default video speed settings.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        videoSpeedBytecodePatch,
        settingsPatch,
    )

    apply {
        /*
         add settings
         */
        ResourceHelper.addSettings(
            this,
            "PREFERENCE_CATEGORY: REVANCED_EXTENDED_SETTINGS",
            "PREFERENCE: VIDEO_SETTINGS",
            "SETTINGS: DEFAULT_VIDEO_SPEED"
        )

        ResourceHelper.patchSuccess(
            this,
            "default-video-speed"
        )
    }
}
