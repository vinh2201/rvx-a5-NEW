package app.revanced.patches.youtube.layout.general.pivotbar.createbutton.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.general.pivotbar.createbutton.bytecode.patch.createButtonRemoverBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-create-button
val createButtonRemoverPatch = resourcePatch(
    name = "Hide create button",
    description = "Hides the create button in the navigation bar.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        createButtonRemoverBytecodePatch,
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
            "SETTINGS: HIDE_CREATE_BUTTON"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-create-button"
        )
    }
}
