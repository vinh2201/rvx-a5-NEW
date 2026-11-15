package app.revanced.patches.youtube.ads.general.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.ads.general.bytecode.patch.generalAdsBytecodePatch
import app.revanced.patches.youtube.ads.general.bytecode.patch.generalAdsSecondaryBytecodePatch
import app.revanced.patches.youtube.misc.litho.filter.patch.lithoFilterPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.extensions.doRecursively
import app.revanced.shared.extensions.startsWithAny
import app.revanced.shared.util.resources.ResourceHelper
import org.w3c.dom.Element

private val resourceFileNames = arrayOf(
    "promoted_",
    "promotion_",
    "compact_premium_",
    "compact_promoted_",
    "simple_text_section",
)

private val replacements = arrayOf(
    "height",
    "width",
    "marginTop"
)

private val additionalReplacements = arrayOf(
    "Bottom",
    "End",
    "Start",
    "Top"
)

// Old name: hide-general-ads
val generalAdsPatch = resourcePatch(
    name = "Hide general ads",
    description = "Hooks the method which parses the bytes into a ComponentContext to filter components.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        generalAdsBytecodePatch,
        generalAdsSecondaryBytecodePatch,
        lithoFilterPatch,
        settingsPatch,
    )

    apply {
        // PORT-NOTE: old: context.forEach { file -> ... } (all resource files); now every file below res/ is checked.
        val resDirectory = get("res")
        resDirectory.walk()
            .filter { it.isFile && it.name.startsWithAny(*resourceFileNames) }
            .toList()
            .forEach { resourceFile ->
                // for each file in the "layouts" directory replace all necessary attributes content
                document("res/" + resourceFile.relativeTo(resDirectory).invariantSeparatorsPath).use { document ->
                    document.doRecursively {
                        replacements.forEach replacement@{ replacement ->
                            if (it !is Element) return@replacement

                            it.getAttributeNode("android:layout_$replacement")?.let { attribute ->
                                attribute.textContent = "0.0dip"
                            }
                        }
                    }
                }
            }

        document("res/layout/simple_text_section.xml").use { document ->
            document.doRecursively {
                additionalReplacements.forEach replacement@{ replacement ->
                    if (it !is Element) return@replacement

                    it.getAttributeNode("android:padding_$replacement")?.let { attribute ->
                        attribute.textContent = "0.0dip"
                    }
                }
            }
        }

        /*
         add settings
         */
        ResourceHelper.addSettings(
            this,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: ADS_SETTINGS",
            "SETTINGS: HIDE_GENERAL_ADS"
        )

        ResourceHelper.addSettings2(
            this,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: LAYOUT_SETTINGS",
            "PREFERENCE_HEADER: GENERAL",
            "SETTINGS: HIDE_GENERAL_LAYOUT_ADS"
        )

        ResourceHelper.addSettings2(
            this,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: LAYOUT_SETTINGS",
            "PREFERENCE_HEADER: PLAYER",
            "SETTINGS: HIDE_VIEW_PRODUCT"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-general-ads"
        )
    }
}
