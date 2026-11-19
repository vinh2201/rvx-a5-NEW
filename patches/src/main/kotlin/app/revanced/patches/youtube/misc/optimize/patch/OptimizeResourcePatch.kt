package app.revanced.patches.youtube.misc.optimize.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: optimize-resource
val optimizeResourcePatch = resourcePatch(
    name = "Optimize resource (A5)",
    description = "Removes duplicate resources and adds missing translation files from YouTube.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        redundantResourcePatch,
        //missingTranslationPatch, // TODO: Fix crash when adding missing translations
        settingsPatch,
    )

    apply {
        ResourceHelper.patchSuccess(
            this,
            "optimize-resource"
        )
    }
}
