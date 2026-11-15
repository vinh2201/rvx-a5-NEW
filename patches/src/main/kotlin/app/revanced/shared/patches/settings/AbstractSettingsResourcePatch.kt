package app.revanced.shared.patches.settings

import app.revanced.patcher.patch.ResourcePatchContext
import app.revanced.shared.util.resources.ResourceHelper
import app.revanced.shared.util.resources.ResourceUtils
import app.revanced.shared.util.resources.ResourceUtils.copyResources
import app.revanced.shared.util.resources.ResourceUtils.copyXmlNode

// PORT-NOTE: the old abstract class AbstractSettingsResourcePatch became this helper function.
// The YouTube settingsPatch calls it in its apply block (the old execute()).

/**
 * Settings resource setup
 *
 * @param sourceDirectory Source directory to copy the preference template from
 * @param sourcehostDirectory Source directory to copy the preference template from
 */
internal fun ResourcePatchContext.applySettingsResources(
    sourceDirectory: String,
    sourcehostDirectory: String,
    isYouTube: Boolean,
) {
    /*
     * used for self-restart
     */
    document("AndroidManifest.xml").use { document ->
        document.getElementsByTagName("manifest").item(0).also {
            it.appendChild(it.ownerDocument.createElement("uses-permission").also { element ->
                element.setAttribute("android:name", "android.permission.SCHEDULE_EXACT_ALARM")
            })
        }
    }

    /*
     * Copy strings
     */
    copyXmlNode(sourcehostDirectory, "values/strings.xml", "resources")
    copyXmlNode(sourcehostDirectory, "values/arrays.xml", "resources")

    /* initialize ReVanced Settings */
    if (isYouTube) {
        copyResources(sourceDirectory, ResourceUtils.ResourceGroup("xml", "revanced_prefs.xml"))
        ResourceHelper.initReVancedSettings(this)
    }
}
