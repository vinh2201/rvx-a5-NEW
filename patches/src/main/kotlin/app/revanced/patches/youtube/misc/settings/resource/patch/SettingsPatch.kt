package app.revanced.patches.youtube.misc.settings.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.integrations.patch.integrationsPatch
import app.revanced.patches.youtube.misc.resourceid.patch.sharedResourceIdPatch
import app.revanced.patches.youtube.misc.settings.bytecode.patch.settingsBytecodePatch
import app.revanced.shared.patches.settings.applySettingsResources
import app.revanced.shared.util.resources.ResourceHelper
import app.revanced.shared.util.resources.ResourceUtils
import app.revanced.shared.util.resources.ResourceUtils.copyResources
import org.w3c.dom.Element

// Old name: settings
val settingsPatch = resourcePatch(
    name = "Settings (A5)",
    description = "Applies mandatory patches to implement ReVanced settings into the application.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    // PORT-NOTE: the old dependency on PatchOptions is gone; every patch registers the options it reads.
    dependsOn(
        integrationsPatch,
        sharedResourceIdPatch,
        settingsBytecodePatch,
    )

    apply {
        // old: AbstractSettingsResourcePatch("youtube/settings", "youtube/settings/host", true).execute(context)
        applySettingsResources(
            "youtube/settings",
            "youtube/settings/host",
            true
        )

        /*
         * create directory for the untranslated language resources
         */
        this["res/values-v21"].mkdirs()

        arrayOf(
            ResourceUtils.ResourceGroup(
                "layout",
                "revanced_settings_toolbar.xml",
                "revanced_settings_with_toolbar.xml",
                "revanced_settings_with_toolbar_layout.xml"
            ),
            ResourceUtils.ResourceGroup(
                "values-v21",
                "strings.xml"
            )
        ).forEach { resourceGroup ->
            copyResources("youtube/settings", resourceGroup)
        }

        /*
         * remove revanced settings divider
         */
        arrayOf("Theme.YouTube.Settings", "Theme.YouTube.Settings.Dark").forEach { themeName ->
            document("res/values/styles.xml").use { file ->
                with(file) {
                    val resourcesNode = getElementsByTagName("resources").item(0) as Element

                    val newElement: Element = createElement("item")
                    newElement.setAttribute("name", "android:listDivider")

                    for (i in 0 until resourcesNode.childNodes.length) {
                        val node = resourcesNode.childNodes.item(i) as? Element ?: continue

                        if (node.getAttribute("name") == themeName) {
                            newElement.appendChild(createTextNode("@null"))

                            node.appendChild(newElement)
                        }
                    }
                }
            }
        }

        /*
         add settings
         */
        ResourceHelper.addSettings(
            this,
            "PREFERENCE_CATEGORY: REVANCED_EXTENDED_SETTINGS",
            "PREFERENCE: MISC_SETTINGS",
            "SETTINGS: ABOUT"
        )
    }
}
