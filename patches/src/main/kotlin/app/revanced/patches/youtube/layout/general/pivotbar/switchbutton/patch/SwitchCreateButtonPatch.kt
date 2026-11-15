package app.revanced.patches.youtube.layout.general.pivotbar.switchbutton.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: switch-create-notification
val switchCreateButtonPatch = resourcePatch(
    name = "Switch create notification",
    description = "Switches the create button and notification button.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        switchCreateButtonBytecodePatch,
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
            "SETTINGS: SWITCH_CREATE_NOTIFICATION"
        )

        ResourceHelper.patchSuccess(
            this,
            "switch-create-notification"
        )
    }
}
