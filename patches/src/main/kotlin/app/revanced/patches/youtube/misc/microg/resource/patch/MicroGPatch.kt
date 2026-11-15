package app.revanced.patches.youtube.misc.microg.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.microg.bytecode.patch.microGBytecodePatch
import app.revanced.patches.youtube.misc.microg.shared.Constants.PACKAGE_NAME
import app.revanced.patches.youtube.misc.microg.shared.Constants.SPOOFED_PACKAGE_NAME
import app.revanced.patches.youtube.misc.microg.shared.Constants.SPOOFED_PACKAGE_SIGNATURE
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.patches.youtube.misc.spoofvideostreams.spoofVideoStreamsPatch
import app.revanced.patches.youtube.misc.useragent.spoofUserAgentPatch
import app.revanced.shared.patches.options.youTubePackageNameOption
import app.revanced.shared.util.microg.MicroGManifestHelper
import app.revanced.shared.util.microg.MicroGResourceHelper
import app.revanced.shared.util.resources.ResourceHelper

// Old name: microg-support
val microGPatch = resourcePatch(
    name = "GmsCore support",
    description = "Allows YouTube ReVanced to run without root and under a different package name with GmsCore. (\"GmsCore\" is new Vanced MicroG).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    // PORT-NOTE: the old SpoofClientPatch dependency is now the Spoof video streams patch.
    dependsOn(
        settingsPatch,
        microGBytecodePatch,
        spoofUserAgentPatch,
        spoofVideoStreamsPatch,
    )

    // PORT-NOTE: replaces the old dependency on PatchOptions
    youTubePackageNameOption()

    apply {
        val packageName by youTubePackageNameOption

        /*
         add settings
         */
        ResourceHelper.addSettings(
            this,
            "PREFERENCE_CATEGORY: MICROG_SETTINGS",
            "PREFERENCE: MICROG_SETTINGS",
            "SETTINGS: MICROG_SETTINGS"
        )

        ResourceHelper.patchSuccess(
            this,
            "microg-support"
        )

        val settingsFragment = this["res/xml/settings_fragment.xml"]
        settingsFragment.writeText(
            settingsFragment.readText().replace(
                "android:targetPackage=\"com.google.android.youtube",
                "android:targetPackage=\"$packageName"
            )
        )

        val settingsFragmentv22 = this["res/xml-v22/settings_fragment.xml"]
        settingsFragmentv22.writeText(
            settingsFragmentv22.readText().replace(
                "android:targetPackage=\"com.google.android.youtube",
                "android:targetPackage=\"$packageName"
            )
        )

        // PORT-NOTE (new): the app shortcuts also target the original package name (res/xml/main_shortcuts.xml in
        // YouTube 16.40.36), which would start the original app (or nothing) when the package name is changed.
        arrayOf("res/xml/main_shortcuts.xml", "res/xml-v22/main_shortcuts.xml").forEach { path ->
            val shortcuts = this[path]
            if (!shortcuts.exists()) return@forEach

            shortcuts.writeText(
                shortcuts.readText().replace(
                    "android:targetPackage=\"com.google.android.youtube",
                    "android:targetPackage=\"$packageName"
                )
            )
        }

        // update manifest
        MicroGResourceHelper.patchManifest(
            this,
            PACKAGE_NAME,
            "$packageName"
        )

        // add metadata to manifest
        MicroGManifestHelper.addSpoofingMetadata(
            this,
            SPOOFED_PACKAGE_NAME,
            SPOOFED_PACKAGE_SIGNATURE
        )
    }
}
