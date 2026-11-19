package app.revanced.patches.youtube.misc.externalbrowser.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.externalbrowser.bytecode.patch.externalBrowserBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: enable-external-browser
val externalBrowserPatch = resourcePatch(
    name = "Enable external browser (A5)",
    description = "Use an external browser to open the url.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        externalBrowserBytecodePatch,
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
            "SETTINGS: ENABLE_EXTERNAL_BROWSER"
        )

        ResourceHelper.patchSuccess(
            this,
            "enable-external-browser"
        )
    }
}
