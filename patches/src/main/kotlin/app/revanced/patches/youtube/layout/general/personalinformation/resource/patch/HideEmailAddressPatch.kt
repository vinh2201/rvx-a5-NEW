package app.revanced.patches.youtube.layout.general.personalinformation.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.general.personalinformation.bytecode.patch.hideEmailAddressBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-email-address
val hideEmailAddressPatch = resourcePatch(
    name = "Hide email address",
    description = "Hides the email address in the account switcher.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        hideEmailAddressBytecodePatch,
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
            "SETTINGS: HIDE_EMAIL_ADDRESS"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-email-address"
        )
    }
}
