package app.revanced.patches.youtube.misc.forceheader.patch

import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.FileCopyCompat
import app.revanced.shared.util.resources.ResourceHelper
import java.nio.file.Files
import java.nio.file.StandardCopyOption

// Old name: force-premium-heading (was @Patch(false))
val premiumHeadingPatch = resourcePatch(
    name = "Force premium heading (A5)",
    description = "Forces premium heading on the home screen.",
    use = false,
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(settingsPatch)

    apply {
        val resDirectory = this["res"]
        if (!resDirectory.isDirectory) throw PatchException("The res folder can not be found.")

        val (original, replacement) = "yt_premium_wordmark_header" to "yt_wordmark_header"
        val modes = arrayOf("light", "dark")

        // PORT-NOTE: in YouTube 16.40.36 these drawables only exist in drawable-xhdpi and drawable-xxhdpi.
        // The old patch required every density (xxxhdpi .. mdpi) and failed with a PatchException on this version.
        // Now every density that has the premium drawable is replaced, and the patch only fails if none does.
        var copied = 0

        arrayOf("xxxhdpi", "xxhdpi", "xhdpi", "hdpi", "mdpi").forEach { size ->
            val headingDirectory = resDirectory.resolve("drawable-$size")

            modes.forEach { mode ->
                val fromFile = headingDirectory.resolve("${original}_$mode.png")
                val toFile = headingDirectory.resolve("${replacement}_$mode.png")

                if (!fromFile.exists()) return@forEach

                try {
                    Files.copy(
                        fromFile.toPath(),
                        toFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                } catch (e: NoSuchMethodError) {
                    FileCopyCompat.copy(
                        fromFile,
                        toFile
                    )
                }

                copied++
            }
        }

        if (copied == 0)
            throw PatchException("No ${original}_(light|dark).png file exists in the resources. Therefore, this patch can not succeed.")

        val revancedprefs = this["res/xml/revanced_prefs.xml"]
        revancedprefs.writeText(
            revancedprefs.readText()
                .replace(
                    "<SwitchPreference android:title=\"@string/revanced_override_premium_header_title\" android:key=\"revanced_override_premium_header\" android:defaultValue=\"false\" android:summaryOn=\"@string/revanced_override_premium_header_summary_on\" android:summaryOff=\"@string/revanced_override_premium_header_summary_off\" />",
                    ""
                ).replace(
                    "header-switch",
                    "force-premium-heading"
                )
        )

        ResourceHelper.patchSuccess(
            this,
            "force-premium-heading"
        )
    }
}
