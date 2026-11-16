package app.revanced.patches.youtube.misc.layoutupdates

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: disable-layout-updates (rv6: "Disable layout updates")
val disableLayoutUpdatesPatch = resourcePatch(
    name = "Disable layout updates",
    description = "Adds an option to disable layout updates by the server.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        layoutConfigGuardBytecodePatch,
        settingsPatch,
    )

    apply {
        /*
         add settings
         */
        ResourceHelper.addSettings(
            this,
            "PREFERENCE_CATEGORY: REVANCED_EXTENDED_SETTINGS",
            "PREFERENCE: MISC_SETTINGS",
            "SETTINGS: DISABLE_LAYOUT_UPDATES"
        )

        ResourceHelper.patchSuccess(
            this,
            "disable-layout-updates"
        )
    }
}
