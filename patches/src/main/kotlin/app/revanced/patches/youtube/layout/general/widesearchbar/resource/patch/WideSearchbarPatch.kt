package app.revanced.patches.youtube.layout.general.widesearchbar.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.general.widesearchbar.bytecode.patch.wideSearchbarBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: enable-wide-searchbar
val wideSearchbarPatch = resourcePatch(
    name = "Enable wide search bar (A5)",
    description = "Replaces the search icon with a wide search bar. This will hide the YouTube logo when active.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        wideSearchbarBytecodePatch,
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
            "SETTINGS: ENABLE_WIDE_SEARCHBAR"
        )

        ResourceHelper.patchSuccess(
            this,
            "enable-wide-searchbar"
        )
    }
}
