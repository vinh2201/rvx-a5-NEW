package app.revanced.patches.youtube.misc.theme.patch

import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatchContext
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.patches.options.darkThemeBackgroundColorOption
import app.revanced.shared.patches.theme.bytecode.generalThemePatch
import app.revanced.shared.util.resources.ResourceHelper
import org.w3c.dom.Element

private val DARK_COLOR_NAMES = setOf(
    "yt_black0", "yt_black1", "yt_black1_opacity95", "yt_black1_opacity98", "yt_black2", "yt_black3",
    "yt_black4", "yt_status_bar_background_dark", "material_grey_850"
)

private fun ResourcePatchContext.setTheme(valuesPath: String, darkThemeBackgroundColor: String) {
    document("res/$valuesPath/colors.xml").use { document ->
        val resourcesNode = document.getElementsByTagName("resources").item(0) as Element

        for (i in 0 until resourcesNode.childNodes.length) {
            val node = resourcesNode.childNodes.item(i) as? Element ?: continue

            if (node.getAttribute("name") in DARK_COLOR_NAMES) {
                node.textContent = darkThemeBackgroundColor
            }
        }
    }
}

// Old name: theme
val themePatch = resourcePatch(
    name = "Theme",
    description = "Applies a custom theme (default: amoled).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        generalThemePatch,
        settingsPatch,
    )

    // PORT-NOTE: replaces the old dependency on PatchOptions
    darkThemeBackgroundColorOption()

    apply {
        val darkThemeBackgroundColor by darkThemeBackgroundColorOption
        val color = darkThemeBackgroundColor
            ?: throw PatchException("Invalid dark theme background color.")

        // PORT-NOTE: yt_black0 does not exist in 16.40.36 (and values-v31 has none of these names).
        // Like in the old patch, names that are absent are simply skipped.
        arrayOf("values", "values-v31").forEach { valuesPath ->
            setTheme(valuesPath, color)
        }

        ResourceHelper.themePatchSuccess(
            this,
            "default",
            "amoled"
        )
        ResourceHelper.themePatchSuccess(
            this,
            "materialyou",
            "mix"
        )
    }
}
