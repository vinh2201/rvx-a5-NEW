package app.revanced.patches.youtube.layout.general.tabletminiplayer.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.general.tabletminiplayer.bytecode.patch.tabletMiniPlayerBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: enable-tablet-miniplayer
val tabletMiniPlayerPatch = resourcePatch(
    name = "Enable tablet mini player",
    description = "Enables the tablet mini player layout.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        tabletMiniPlayerBytecodePatch,
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
            "SETTINGS: ENABLE_TABLET_MINIPLAYER"
        )

        ResourceHelper.patchSuccess(
            this,
            "enable-tablet-miniplayer"
        )
    }
}
