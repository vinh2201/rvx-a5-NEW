package app.revanced.patches.youtube.layout.seekbar.timeandseekbar.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.seekbar.timeandseekbar.bytecode.patch.hideTimeAndSeekbarBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-time-and-seekbar
val hideTimeAndSeekbarPatch = resourcePatch(
    name = "Hide time and seekbar",
    description = "Hides progress bar and time counter on videos.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        hideTimeAndSeekbarBytecodePatch,
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
            "SETTINGS: HIDE_TIME_AND_SEEKBAR"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-time-and-seekbar"
        )
    }
}
