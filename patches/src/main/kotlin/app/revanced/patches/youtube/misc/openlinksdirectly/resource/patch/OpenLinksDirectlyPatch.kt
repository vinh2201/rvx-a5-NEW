package app.revanced.patches.youtube.misc.openlinksdirectly.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.openlinksdirectly.bytecode.patch.openLinksDirectlyBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: enable-open-links-directly
val openLinksDirectlyPatch = resourcePatch(
    name = "Enable open links directly (A5)",
    description = "Bypass URL redirects (youtube.com/redirect) when opening links in video descriptions.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        openLinksDirectlyBytecodePatch,
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
            "SETTINGS: ENABLE_OPEN_LINKS_DIRECTLY"
        )

        ResourceHelper.patchSuccess(
            this,
            "enable-open-links-directly"
        )
    }
}
