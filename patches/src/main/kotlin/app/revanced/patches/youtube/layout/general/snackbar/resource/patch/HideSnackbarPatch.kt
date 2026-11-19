package app.revanced.patches.youtube.layout.general.snackbar.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.general.snackbar.bytecode.patch.hideSnackbarBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-snackbar
val hideSnackbarPatch = resourcePatch(
    name = "Hide snackbar (A5)",
    description = "Hides the snackbar action popup.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        hideSnackbarBytecodePatch,
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
            "PREFERENCE_HEADER: GENERAL",
            "SETTINGS: HIDE_SNACKBAR"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-snackbar"
        )
    }
}
