package app.revanced.patches.youtube.layout.seekbar.seekbartapping.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.seekbar.seekbartapping.bytecode.patch.seekbarTappingBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: enable-seekbar-tapping
val seekbarTappingPatch = resourcePatch(
    name = "Enable seekbar tapping",
    description = "Enables tap-to-seek on the seekbar of the video player.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        seekbarTappingBytecodePatch,
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
            "PREFERENCE_HEADER: SEEKBAR",
            "SETTINGS: ENABLE_SEEKBAR_TAPPING"
        )

        ResourceHelper.patchSuccess(
            this,
            "enable-seekbar-tapping"
        )
    }
}
