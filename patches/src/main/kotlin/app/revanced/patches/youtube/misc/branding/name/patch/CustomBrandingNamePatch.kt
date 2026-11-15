package app.revanced.patches.youtube.misc.branding.name.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.patches.options.youTubeAppNameOption
import app.revanced.shared.util.resources.ResourceHelper

// Old name: custom-branding-name
val customBrandingNamePatch = resourcePatch(
    name = "Custom branding name",
    description = "Changes the YouTube launcher name to your choice (defaults to ReVanced Extended).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(settingsPatch)

    // PORT-NOTE: replaces the old dependency on PatchOptions
    youTubeAppNameOption()

    apply {
        // App name
        val appName by youTubeAppNameOption

        val manifest = this["AndroidManifest.xml"]
        manifest.writeText(
            manifest.readText()
                .replace(
                    "android:label=\"@string/application_name",
                    "android:label=\"$appName"
                )
        )

        ResourceHelper.labelPatchSuccess(
            this,
            "$appName"
        )
    }
}
