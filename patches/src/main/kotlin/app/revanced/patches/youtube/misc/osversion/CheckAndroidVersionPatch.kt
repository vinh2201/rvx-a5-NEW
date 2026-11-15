package app.revanced.patches.youtube.misc.osversion

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: check-android-version
val checkAndroidVersionPatch = resourcePatch(
    name = "Check android version",
    description = "If the device is Android 6.0 or higher, show a warning dialog.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        checkAndroidVersionBytecodePatch,
        settingsPatch,
    )

    apply {
        ResourceHelper.patchSuccess(
            this,
            "check-android-version"
        )
    }
}
