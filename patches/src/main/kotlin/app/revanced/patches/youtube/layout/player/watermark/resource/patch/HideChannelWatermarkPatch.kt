package app.revanced.patches.youtube.layout.player.watermark.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.player.watermark.bytecode.patch.hideChannelWatermarkBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-channel-watermark
val hideChannelWatermarkPatch = resourcePatch(
    name = "Hide channel watermark",
    description = "Hides creator's watermarks on videos.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        hideChannelWatermarkBytecodePatch,
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
            "SETTINGS: HIDE_CHANNEL_WATERMARK"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-channel-watermark"
        )
    }
}
