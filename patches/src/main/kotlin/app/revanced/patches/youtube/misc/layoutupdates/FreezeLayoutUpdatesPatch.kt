package app.revanced.patches.youtube.misc.layoutupdates

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: freeze-layout-updates (rv6: "Freeze layout updates")
val freezeLayoutUpdatesPatch = resourcePatch(
    name = "Freeze layout updates",
    description = "Adds an option to keep the layout configuration of the first start and ignore later layout updates.",
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
            "SETTINGS: FREEZE_LAYOUT_UPDATES"
        )

        ResourceHelper.patchSuccess(
            this,
            "freeze-layout-updates"
        )
    }
}
