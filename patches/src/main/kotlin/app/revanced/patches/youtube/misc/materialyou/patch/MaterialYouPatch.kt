package app.revanced.patches.youtube.misc.materialyou.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.patches.theme.bytecode.generalThemePatch
import app.revanced.shared.util.resources.ResourceHelper
import app.revanced.shared.util.resources.ResourceUtils
import app.revanced.shared.util.resources.ResourceUtils.copyResources
import app.revanced.shared.util.resources.ResourceUtils.copyXmlNode

// Old name: materialyou (was @Patch(false))
val materialYouPatch = resourcePatch(
    name = "Material You (A5)",
    description = "Enables MaterialYou theme for Android 12+",
    use = false,
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        generalThemePatch,
        settingsPatch,
    )

    apply {
        // Creates the -v31 directories and copies the Material You drawables/layout into the apk resources
        copyResources(
            "youtube/materialyou",
            ResourceUtils.ResourceGroup(
                "drawable-night-v31",
                "new_content_dot_background.xml"
            ),
            ResourceUtils.ResourceGroup(
                "drawable-v31",
                "new_content_count_background.xml",
                "new_content_dot_background.xml"
            ),
            ResourceUtils.ResourceGroup(
                "layout-v31",
                "new_content_count.xml"
            ),
        )

        // Add the Material You colors to the existing res/values-v31/colors.xml
        val sourcePath = ResourceUtils.javaClass.classLoader.getResourceAsStream("youtube/materialyou/host/values-v31/colors.xml")!!

        "resources".copyXmlNode(
            document(sourcePath),
            document("res/values-v31/colors.xml")
        ).close()

        /*
         add settings
         */
        ResourceHelper.themePatchSuccess(
            this,
            "default",
            "materialyou"
        )
    }
}
