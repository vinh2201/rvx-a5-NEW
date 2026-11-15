package app.revanced.patches.youtube.extended.layoutswitch.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.extended.layoutswitch.bytecode.patch.layoutSwitchBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: layout-switch
val layoutSwitchPatch = resourcePatch(
    name = "Layout switch",
    description = "Tricks the dpi to use some tablet/phone layouts.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        layoutSwitchBytecodePatch,
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
            "SETTINGS: LAYOUT_SWITCH"
        )

        ResourceHelper.patchSuccess(
            this,
            "layout-switch"
        )
    }
}
