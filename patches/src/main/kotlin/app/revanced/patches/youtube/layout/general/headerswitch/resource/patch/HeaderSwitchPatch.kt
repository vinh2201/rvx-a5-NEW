package app.revanced.patches.youtube.layout.general.headerswitch.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.general.headerswitch.bytecode.patch.headerSwitchBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: header-switch
val headerSwitchPatch = resourcePatch(
    name = "Header switch (A5)",
    description = "Add switch to change header.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        headerSwitchBytecodePatch,
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
            "SETTINGS: HEADER_SWITCH"
        )

        ResourceHelper.patchSuccess(
            this,
            "header-switch"
        )
    }
}
