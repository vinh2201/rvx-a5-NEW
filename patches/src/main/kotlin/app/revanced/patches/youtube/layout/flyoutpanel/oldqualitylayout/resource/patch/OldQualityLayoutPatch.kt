package app.revanced.patches.youtube.layout.flyoutpanel.oldqualitylayout.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.flyoutpanel.oldqualitylayout.bytecode.patch.oldQualityLayoutBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: enable-old-quality-layout
val oldQualityLayoutPatch = resourcePatch(
    name = "Enable old quality layout (A5)",
    description = "Enables the original quality flyout menu.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        oldQualityLayoutBytecodePatch,
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
            "PREFERENCE_HEADER: FLYOUT_PANEL",
            "SETTINGS: ENABLE_OLD_QUALITY_LAYOUT"
        )

        ResourceHelper.patchSuccess(
            this,
            "enable-old-quality-layout"
        )
    }
}
