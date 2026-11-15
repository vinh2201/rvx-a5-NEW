package app.revanced.patches.youtube.misc.optimize.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.shared.util.resources.ResourceUtils.copyXmlNode

private val LANGUAGE_LIST = arrayOf(
    "af",
    "am",
    "ar",
    "az",
    "b+sr+Latn",
    "be",
    "bg",
    "bn",
    "bs",
    "ca",
    "cs",
    "da",
    "de",
    "el",
    "en-rGB",
    "en-rIN",
    "es",
    "es-rUS",
    "et",
    "eu",
    "fa",
    "fi",
    "fr",
    "fr-rCA",
    "gl",
    "gu",
    "hi",
    "hr",
    "hu",
    "hy",
    "in",
    "is",
    "it",
    "iw",
    "ja",
    "ka",
    "kk",
    "km",
    "kn",
    "ko",
    "ky",
    "lo",
    "lt",
    "lv",
    "mk",
    "ml",
    "mn",
    "mr",
    "ms",
    "my",
    "nb",
    "ne",
    "nl",
    "pa",
    "pl",
    "pt-rBR",
    "pt-rPT",
    "ro",
    "ru",
    "si",
    "sk",
    "sl",
    "sq",
    "sr",
    "sv",
    "sw",
    "ta",
    "te",
    "th",
    "tl",
    "tr",
    "uk",
    "ur",
    "uz",
    "vi",
    "zh-rCN",
    "zh-rHK",
    "zh-rTW",
    "zu",
)

// Old name: add-missing-translation-patch (helper patch, never selectable)
// PORT-NOTE: like in the old project it is NOT a dependency of optimize-resource
// ("TODO: Fix crash when adding missing translations").
val missingTranslationPatch = resourcePatch(
    description = "Adds missing translation files from YouTube.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    apply {
        LANGUAGE_LIST.forEach { name ->
            copyXmlNode("youtube/resource/host", "values-$name/strings.xml", "resources")
        }
    }
}
