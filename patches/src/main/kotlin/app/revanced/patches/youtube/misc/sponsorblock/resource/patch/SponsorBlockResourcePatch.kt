package app.revanced.patches.youtube.misc.sponsorblock.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.patches.youtube.misc.sponsorblock.bytecode.patch.sponsorBlockBytecodePatch
import app.revanced.patches.youtube.misc.sponsorblock.bytecode.patch.sponsorBlockSecondaryBytecodePatch
import app.revanced.shared.util.resources.ResourceHelper
import app.revanced.shared.util.resources.ResourceUtils
import app.revanced.shared.util.resources.ResourceUtils.copyResources
import app.revanced.shared.util.resources.ResourceUtils.copyXmlNode

private const val CONTROLS_LAYOUT_TAG = "com.google.android.apps.youtube.app.player.overlay.ControlsRelativeLayout"

// Old name: sponsorblock
val sponsorBlockPatch = resourcePatch(
    name = "SponsorBlock",
    description = "Integrates SponsorBlock which allows skipping video segments such as sponsored content.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        settingsPatch,
        sponsorBlockBytecodePatch,
        sponsorBlockSecondaryBytecodePatch,
    )

    apply {
        /*
         merge SponsorBlock drawables to main drawables
         */
        arrayOf(
            ResourceUtils.ResourceGroup(
                "layout",
                "inline_sponsor_overlay.xml",
                "new_segment.xml",
                "skip_sponsor_button.xml"
            ),
            ResourceUtils.ResourceGroup(
                // required resource for back button, because when the base APK is used, this resource will not exist
                "drawable",
                "ic_sb_adjust.xml",
                "ic_sb_compare.xml",
                "ic_sb_edit.xml",
                "ic_sb_logo.xml",
                "ic_sb_publish.xml",
                "ic_sb_voting.xml"
            )
        ).forEach { resourceGroup ->
            copyResources("youtube/sponsorblock", resourceGroup)
        }

        /*
        merge xml nodes from the host to their real xml files
         */

        // copy nodes from host resources to their real xml files
        val hostingResourceStream =
            ResourceUtils.javaClass.classLoader.getResourceAsStream("youtube/sponsorblock/host/layout/youtube_controls_layout.xml")!!

        val targetXmlEditor = document("res/layout/youtube_controls_layout.xml")

        CONTROLS_LAYOUT_TAG.copyXmlNode(
            document(hostingResourceStream),
            targetXmlEditor
        ).also {
            val children = targetXmlEditor.getElementsByTagName(CONTROLS_LAYOUT_TAG).item(0).childNodes

            // Replace the startOf with the voting button view so that the button does not overlap
            for (i in 1 until children.length) {
                val view = children.item(i)

                // Replace the attribute for a specific node only
                if (!view.hasAttributes())
                    continue

                val value = view.attributes.getNamedItem("android:id")?.nodeValue ?: continue

                if (!(value.endsWith("live_chat_overlay_button") || value.endsWith("player_learn_more_button"))) continue

                // voting button id from the voting button view from the youtube_controls_layout.xml host file
                val sbButtonId = "@+id/voting_button"

                // (both views have android:layout_toStartOf in 16.40.36)
                view.attributes.getNamedItem("android:layout_toStartOf")?.nodeValue = sbButtonId

                break
            }
        }.close() // close afterwards

        /*
         add ReVanced Settings
         */
        ResourceHelper.addReVancedSettings(
            this,
            "PREFERENCE: SPONSOR_BLOCK"
        )

        ResourceHelper.patchSuccess(
            this,
            "sponsorblock"
        )
    }
}
