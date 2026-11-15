package app.revanced.patches.youtube.misc.translations.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

private val LANGUAGE_LIST = arrayOf(
    "ar",
    "az-rAZ",
    "bn",
    "be-rBY",
    "de-rDE",
    "el-rGR",
    "es-rES",
    "fr-rFR",
    "hi-rIN",
    "hu-rHU",
    "id-rID",
    "in",
    "it-rIT",
    "ja-rJP",
    "ko-rKR",
    "pl-rPL",
    "pt-rBR",
    "ru-rRU",
    "tr-rTR",
    "uk-rUA",
    "vi-rVN",
    "zh-rCN",
    "zh-rTW"
)

// Old name: translations
val translationsPatch = resourcePatch(
    name = "Translations",
    description = "Add Crowdin Translations.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(settingsPatch)

    apply {
        ResourceHelper.addTranslations(this, "youtube", LANGUAGE_LIST)

        ResourceHelper.patchSuccess(
            this,
            "translations"
        )
    }
}
