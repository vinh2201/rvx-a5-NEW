package app.revanced.patches.youtube.misc.branding.icon.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.IconHelper
import app.revanced.shared.util.resources.ResourceHelper

// Old name: custom-branding-icon-afn-blue (was @Patch(false))
val customBrandingIconBluePatch = resourcePatch(
    name = "Custom branding icon Afn (Blue) (A5)",
    description = "Changes the YouTube launcher icon (Afn / Blue).",
    use = false,
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(settingsPatch)

    apply {
        IconHelper.customIcon(
            this,
            "blue"
        )

        ResourceHelper.iconPatchSuccess(
            this,
            "blue"
        )
    }
}
