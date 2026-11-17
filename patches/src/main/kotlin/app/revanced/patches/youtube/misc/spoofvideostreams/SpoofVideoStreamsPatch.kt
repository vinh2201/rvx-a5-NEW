package app.revanced.patches.youtube.misc.spoofvideostreams

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: spoof-client (replaces the old misc/spoofclient patch; same mechanism, new client table, see the extension).
val spoofVideoStreamsPatch = resourcePatch(
    name = "Spoof video streams (A5)",
    description = "Spoofs the client of the player request (Android VR / Android Studio / ...) " +
            "so that videos can be played when the default client is blocked or unsupported.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        spoofVideoStreamsBytecodePatch,
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
            "SETTINGS: SPOOF_CLIENT"
        )

        ResourceHelper.patchSuccess(
            this,
            "spoof-video-streams"
        )
    }
}
