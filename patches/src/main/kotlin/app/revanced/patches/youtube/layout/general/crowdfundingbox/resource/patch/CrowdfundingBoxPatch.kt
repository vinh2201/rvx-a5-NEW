package app.revanced.patches.youtube.layout.general.crowdfundingbox.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.general.crowdfundingbox.bytecode.patch.crowdfundingBoxBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-crowdfunding-box
val crowdfundingBoxPatch = resourcePatch(
    name = "Hide crowdfunding box",
    description = "Hides the crowdfunding box between the player and video description.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        crowdfundingBoxBytecodePatch,
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
            "SETTINGS: HIDE_CROWDFUNDING_BOX"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-crowdfunding-box"
        )
    }
}
