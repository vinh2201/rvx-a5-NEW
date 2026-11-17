package app.revanced.patches.youtube.swipe.swipecontrols.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.patches.youtube.swipe.swipebrightnessinhdr.bytecode.patch.swipeGestureBrightnessInHDRPatch
import app.revanced.patches.youtube.swipe.swipecontrols.bytecode.patch.swipeControlsBytecodePatch
import app.revanced.shared.util.resources.ResourceHelper
import app.revanced.shared.util.resources.ResourceUtils
import app.revanced.shared.util.resources.ResourceUtils.copyResources

// Old name: swipe-controls
val swipeControlsPatch = resourcePatch(
    name = "Swipe controls (A5)",
    description = "Adds volume and brightness swipe controls.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        settingsPatch,
        swipeControlsBytecodePatch,
        swipeGestureBrightnessInHDRPatch,
    )

    apply {
        /*
         add settings
         */
        ResourceHelper.addSettings(
            this,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: SWIPE_SETTINGS",
            "SETTINGS: SWIPE_CONTROLS"
        )

        ResourceHelper.patchSuccess(
            this,
            "swipe-controls"
        )

        copyResources(
            "youtube/swipecontrols",
            ResourceUtils.ResourceGroup(
                "drawable",
                "ic_sc_brightness_auto.xml",
                "ic_sc_brightness_manual.xml",
                "ic_sc_volume_mute.xml",
                "ic_sc_volume_normal.xml"
            )
        )
    }
}
