package app.revanced.patches.youtube.extended.spoofversion.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.extended.spoofversion.bytecode.patch.spoofAppVersionBytecodePatch
import app.revanced.patches.youtube.misc.missingresources.addMissingResourcesPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: spoof-app-version
val spoofAppVersionPatch = resourcePatch(
    name = "Spoof app version (A5)",
    description = "Spoofs the YouTube client version (default 20.05.46, minimum 20.02.34) so the server keeps " +
        "serving the app. Needs \"Add missing resources (A5)\" because the server then answers with the layout of the new version.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        spoofAppVersionBytecodePatch,
        addMissingResourcesPatch, // Icons/drawables the newer layout asks for; without them the app can crash on open.
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
            "SETTINGS: SPOOF_APP_VERSION"
        )

        ResourceHelper.patchSuccess(
            this,
            "spoof-app-version"
        )
    }
}
