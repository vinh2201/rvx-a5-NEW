package app.revanced.patches.youtube.layout.fullscreen.landscapemode.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: disable-landscape-mode (old annotation: @Patch(false) -> not selected by default)
val landScapeModePatch = resourcePatch(
    name = "Disable landscape mode (A5)",
    description = "Disable landscape mode when entering fullscreen.",
    use = false,
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        landScapeModeBytecodePatch,
        settingsPatch,
    )

    apply {
        ResourceHelper.addSettings2(
            this,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: LAYOUT_SETTINGS",
            "PREFERENCE_HEADER: FULLSCREEN",
            "SETTINGS: DISABLE_LANDSCAPE_MODE"
        )

        ResourceHelper.patchSuccess(
            this,
            "disable-landscape-mode"
        )
    }
}
