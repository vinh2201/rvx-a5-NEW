package app.revanced.patches.youtube.ads.video.bytecode.patch

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.shared.patches.videoads.generalVideoAdsPatch
import app.revanced.shared.patches.videoads.injectLegacyAds
import app.revanced.shared.patches.videoads.injectMainstreamAds
import app.revanced.shared.util.integrations.Constants.ADS_PATH

// Old name: hide-video-ads-bytecode-patch (helper patch, never selectable)
val videoAdsBytecodePatch = bytecodePatch(
    description = "Hooks the video ads methods.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(generalVideoAdsPatch)

    apply {
        val INTEGRATIONS_CLASS_DESCRIPTOR = "$ADS_PATH/HideVideoAdsPatch;->hideVideoAds()Z"

        injectLegacyAds(INTEGRATIONS_CLASS_DESCRIPTOR)
        injectMainstreamAds(INTEGRATIONS_CLASS_DESCRIPTOR)
    }
}
