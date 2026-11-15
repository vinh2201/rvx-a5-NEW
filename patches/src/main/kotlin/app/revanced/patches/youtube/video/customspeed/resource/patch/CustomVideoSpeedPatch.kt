package app.revanced.patches.youtube.video.customspeed.resource.patch

import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.patches.youtube.video.customspeed.bytecode.patch.customVideoSpeedBytecodePatch
import app.revanced.shared.patches.options.customSpeedArraysOption
import app.revanced.shared.util.resources.ResourceHelper
import app.revanced.shared.util.resources.ResourceUtils.copyXmlNode

// Old name: custom-video-speed
val customVideoSpeedPatch = resourcePatch(
    name = "Custom video speed",
    description = "Adds more video speed options.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        customVideoSpeedBytecodePatch,
        settingsPatch,
    )

    customSpeedArraysOption()

    apply {
        /*
         * Copy arrays
         */
        copyXmlNode("youtube/speed/host", "values/arrays.xml", "resources")

        val speed by customSpeedArraysOption
        val speedArrays = speed
            ?: throw PatchException("Invalid video speed array.")

        val splits = speedArrays.replace(" ", "").split(",")
        if (splits.isEmpty()) throw IllegalArgumentException("Invalid speed elements")

        val speedElements = splits.map { it }
        for (index in 0 until splits.count()) {
            ResourceHelper.addEntryValues(this, speedElements[index])
            ResourceHelper.addEntries(this, speedElements[index] + "x")
        }

        ResourceHelper.addSpeed(this)

        /*
         add settings
         */
        ResourceHelper.addSettings(
            this,
            "PREFERENCE_CATEGORY: REVANCED_EXTENDED_SETTINGS",
            "PREFERENCE: VIDEO_SETTINGS",
            "SETTINGS: CUSTOM_VIDEO_SPEED"
        )

        ResourceHelper.patchSuccess(
            this,
            "custom-video-speed"
        )
    }
}
