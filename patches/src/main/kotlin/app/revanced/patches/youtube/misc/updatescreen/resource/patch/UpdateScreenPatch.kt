package app.revanced.patches.youtube.misc.updatescreen.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.patches.youtube.misc.updatescreen.bytecode.patch.updateScreenBytecodePatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: disable-update-screen
val updateScreenPatch = resourcePatch(
    name = "Disable update screen",
    description = "Disables the app update screen.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        updateScreenBytecodePatch,
        settingsPatch,
    )

    apply {
        ResourceHelper.patchSuccess(
            this,
            "disable-update-screen"
        )
    }
}
