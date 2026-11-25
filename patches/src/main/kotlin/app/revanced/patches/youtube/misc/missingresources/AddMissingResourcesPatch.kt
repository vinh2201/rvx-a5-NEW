package app.revanced.patches.youtube.misc.missingresources

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper
import app.revanced.shared.util.resources.ResourceUtils.ResourceGroup
import app.revanced.shared.util.resources.ResourceUtils.copyResources
import java.io.File

private const val SETTINGS_SHELL_ACTIVITY = "com.google.android.apps.youtube.app.application.Shell_SettingsActivity"

// Drawable alias -> candidate targets (the first target that exists in 16.40.36 is used; aliases without any existing
// target are skipped, aliases whose name already exists are never replaced).
//
// Merged from rv6 (17.34.36 names) and the community. Newer clients/servers look these names up by name
// (Shorts action buttons, Cairo navigation icons, comment icons); the aliases point to the legacy artwork that 16.40.36 has.
// All 33 "*cairo*" drawables of 20.05.46 / 21.07.240 were compared with this version: the ones with a counterpart are aliased
// here. Without a counterpart (skipped): yt_fill_*/yt_outline_maximize|screen_full_cairo_* (16.40.36 only has white ones),
// live_chat_trash_icon_background_highlighted_cairo.
// PORT-NOTE: in 16.40.36 the following rv6 / community aliases have no target and are therefore skipped:
// remix (ic_remix_filled_white_*), sparkle (yt_fill_spark*), Shorts heart, shorts_creation_secondary_rounded_button_background.
private val DRAWABLE_ALIASES: Map<String, List<String>> = linkedMapOf(
    // Cairo navigation / top bar icons.
    "yt_fill_home_cairo_black_24" to listOf("yt_fill_home_black_24"),
    "yt_outline_home_cairo_black_24" to listOf("yt_outline_home_black_24"),
    "yt_fill_subscriptions_cairo_black_24" to listOf("yt_fill_subscriptions_black_24"),
    "yt_outline_subscriptions_cairo_black_24" to listOf("yt_outline_subscriptions_black_24"),
    "yt_fill_youtube_shorts_cairo_black_24" to listOf("yt_fill_youtube_shorts_black_24"),
    "yt_outline_youtube_shorts_cairo_black_24" to listOf("yt_outline_youtube_shorts_black_24"),
    "yt_fill_library_cairo_black_24" to listOf("yt_fill_library_black_24"),
    "yt_outline_library_cairo_black_24" to listOf("yt_outline_library_black_24"),
    "yt_fill_bell_cairo_black_24" to listOf("yt_fill_bell_black_24"),
    "yt_outline_bell_cairo_black_24" to listOf("yt_outline_bell_black_24"),
    "yt_outline_add_circle_cairo_black_36" to listOf("yt_outline_add_circle_black_36"),
    "yt_fill_search_cairo_black_24" to listOf("yt_fill_search_black_24", "yt_outline_search_black_24"),
    "yt_outline_search_cairo_black_24" to listOf("yt_outline_search_black_24"),
    "yt_outline_overflow_vertical_cairo_black_24" to listOf("yt_outline_overflow_vertical_black_24"),
    "yt_fill_chromecast_cairo_black_24" to listOf("yt_fill_chromecast_black_24"),
    "yt_outline_chromecast_cairo_black_24" to listOf("yt_outline_chromecast_black_24"),
    "yt_fill_compass_cairo_black_24" to listOf("yt_fill_compass_black_24"),
    "yt_outline_compass_cairo_black_24" to listOf("yt_outline_compass_black_24"),
    "yt_outline_gear_cairo_black_24" to listOf("yt_outline_gear_black_24"),
    "yt_outline_share_cairo_black_24" to listOf("yt_outline_share_black_24"),
    "yt_outline_arrow_left_cairo_black_24" to listOf("yt_outline_arrow_left_black_24", "yt_outline_arrow_left_white_24"),
    "yt_outline_add_cairo_black_24" to listOf("yt_outline_add_black_24"),
    "yt_fill_add_cairo_black_24" to listOf("yt_outline_add_black_24"),
    "yt_fill_add_cairo_black_16" to listOf("yt_outline_add_black_24"),
    "yt_outline_chevron_right_cairo_black_24" to listOf("yt_outline_chevron_right_black_24"),
    "yt_outline_x_cairo_black_24" to listOf("yt_outline_x_black_24"),
    "ic_creation_tab_large_with_cairo_circle" to listOf("yt_outline_add_circle_black_36", "ic_add_circle_white"),
    "ic_search_tab_large_with_cairo_circle" to listOf("ic_search_large"),
    "youtube_premium_standalone_cairo" to listOf("youtube_premium_standalone"),
    "ic_outlined_media_route_cairo" to listOf("ic_outlined_media_route"),
    "ic_outlined_media_route_outline_checked_cairo" to listOf("ic_outlined_media_route_outline_checked"),
    "channel_list_sub_item_live_cairo" to listOf("channel_list_sub_item_live"),
    "new_content_count_background_cairo" to listOf("new_content_count_background"),
    "new_content_dot_background_cairo" to listOf("new_content_dot_background"),

    // Shorts action buttons (names requested by newer server-side layouts).
    "youtube_shorts_like_outline_32dp" to listOf("ic_right_like_off_shadowed", "ic_right_like_off_32c"),
    "youtube_shorts_like_fill_32dp" to listOf("ic_right_like_on_shadowed", "ic_right_like_on_32c"),
    "youtube_shorts_thumbs_up_outline_28dp" to listOf("ic_right_like_off_shadowed", "ic_right_like_off_32c"),
    "youtube_shorts_thumbs_up_fill_28dp" to listOf("ic_right_like_on_shadowed", "ic_right_like_on_32c"),
    "youtube_shorts_dislike_outline_32dp" to listOf("ic_right_dislike_off_shadowed", "ic_right_dislike_off_32c"),
    "youtube_shorts_dislike_fill_32dp" to listOf("ic_right_dislike_on_shadowed", "ic_right_dislike_on_32c"),
    "youtube_shorts_thumbs_down_outline_28dp" to listOf("ic_right_dislike_off_shadowed", "ic_right_dislike_off_32c"),
    "youtube_shorts_thumbs_down_fill_28dp" to listOf("ic_right_dislike_on_shadowed", "ic_right_dislike_on_32c"),
    "youtube_shorts_comment_outline_28dp" to listOf("ic_right_comment_shadowed", "ic_right_comment_32c"),
    "youtube_shorts_comment_outline_32dp" to listOf("ic_right_comment_shadowed", "ic_right_comment_32c"),
    "youtube_shorts_share_outline_28dp" to listOf("ic_right_share_shadowed", "ic_right_share_32c"),
    "youtube_shorts_share_outline_32dp" to listOf("ic_right_share_shadowed", "ic_right_share_32c"),
    "youtube_shorts_remix_outline_28dp" to listOf("ic_remix_filled_white_shadowed", "ic_remix_filled_white_24"),
    "youtube_shorts_remix_outline_32dp" to listOf("ic_remix_filled_white_shadowed", "ic_remix_filled_white_24"),
    "youtube_shorts_save_outline_28dp" to listOf("yt_outline_bookmark_white_24", "yt_outline_bookmark_black_24"),
    "youtube_shorts_save_outline_32dp" to listOf("yt_outline_bookmark_white_24", "yt_outline_bookmark_black_24"),
    "youtube_shorts_save_fill_28dp" to listOf("yt_fill_bookmark_white_24", "yt_fill_bookmark_black_24"),
    "youtube_shorts_save_fill_32dp" to listOf("yt_fill_bookmark_white_24", "yt_fill_bookmark_black_24"),
    "youtube_shorts_save_fill_selected_32dp" to listOf("yt_fill_bookmark_white_24", "yt_fill_bookmark_black_24"),
    "youtube_shorts_save_fill_unselected_32dp" to listOf("yt_outline_bookmark_white_24", "yt_outline_bookmark_black_24"),
    "youtube_shorts_original_sound_16dp" to listOf("quantum_ic_music_note_white_24"),
    "youtube_shorts_pivot_fab" to listOf("ic_youtube_shorts_24"),
    "ic_youtube_shorts_24_cairo" to listOf("ic_youtube_shorts_24"),
    "shorts_creation_secondary_rounded_button_background" to listOf(
        "shorts_rounded_button_background",
        "shorts_creation_rounded_button_background",
    ),

    // Shorts action buttons of 17.34.36 (white, shadowed) -> artwork of this version.
    "ic_right_like_off_shadowed" to listOf("ic_right_like_off_32c"),
    "ic_right_like_on_shadowed" to listOf("ic_right_like_on_32c"),
    "ic_right_dislike_off_shadowed" to listOf("ic_right_dislike_off_32c"),
    "ic_right_dislike_on_shadowed" to listOf("ic_right_dislike_on_32c"),
    "ic_right_comment_shadowed" to listOf("ic_right_comment_32c"),
    "ic_right_share_shadowed" to listOf("ic_right_share_32c"),
    "ic_remix_filled_white_shadowed" to listOf("ic_remix_filled_white_24"),

    // Comments.
    "yt_outline_thumb_up_cairo_black_18" to listOf("yt_outline_thumb_up_black_24"),
    "yt_outline_thumb_down_cairo_black_18" to listOf("yt_outline_thumb_down_black_24"),
    "yt_fill_thumb_up_cairo_black_18" to listOf("yt_fill_thumb_up_black_24"),
    "yt_fill_thumb_down_cairo_black_18" to listOf("yt_fill_thumb_down_black_24"),
    "yt_outline_thumb_up_black_18" to listOf("yt_outline_thumb_up_black_24"),
    "yt_outline_thumb_down_black_18" to listOf("yt_outline_thumb_down_black_24"),
    "yt_fill_thumb_up_black_18" to listOf("yt_fill_thumb_up_black_24"),
    "yt_fill_thumb_down_black_18" to listOf("yt_fill_thumb_down_black_24"),
    "yt_fill_spark_cairo_black_24" to listOf("yt_fill_sparkle_white_24"),
    "yt_fill_spark_black_24" to listOf("yt_fill_sparkle_white_24"),

    // Icons of the buttons that are added to the top bar (see TopBarButtonsPatch.java): subscriptions (list of the
    // subscribed channels, Subscriptions tab) and the settings gear (You tab). The icons are the ones the app already has.
    "revanced_top_bar_subscriptions" to listOf(
        "yt_outline_subscriptions_black_24",
        "yt_fill_subscriptions_black_24",
    ),
    "revanced_top_bar_settings" to listOf("yt_outline_gear_black_24"),
)

// Old name: add-missing-resources (rv6: "Add missing resources")
val addMissingResourcesPatch = resourcePatch(
    name = "Add missing resources (A5)",
    description = "Adds drawables and icon handling that newer server responses expect, " +
        "which prevents crashes and wrong icons (bell, search, create) when the client version is spoofed. " +
        "Also adds the buttons that the old UI is missing: the list of subscribed channels (Subscriptions tab) " +
        "and the settings gear (You tab) in the top bar.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        addMissingResourcesBytecodePatch,
        settingsPatch, // Needed to mark the patch as included in the About screen of the settings.
    )

    apply {
        // region drawable aliases

        // Names of all drawables that exist (values/drawables.xml entries + drawable resource files).
        val existingDrawables = mutableSetOf<String>()

        document("res/values/drawables.xml").use { document ->
            val nodes = document.getElementsByTagName("drawable")
            for (i in 0 until nodes.length) {
                nodes.item(i).attributes?.getNamedItem("name")?.nodeValue?.let { existingDrawables.add(it) }
            }
        }

        // Names of the drawables that are bitmaps (png / webp): only those can get a white copy with a tint.
        val bitmapDrawables = mutableSetOf<String>()

        this["res"].listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("drawable") }
            ?.forEach { directory ->
                directory.listFiles()
                    ?.filter { it.isFile }
                    ?.forEach { file ->
                        val name = file.name.substringBeforeLast(".").removeSuffix(".9")
                        existingDrawables.add(name)
                        if ((file.extension == "png" || file.extension == "webp") && !file.name.endsWith(".9.png")) bitmapDrawables.add(name)
                    }
            }

        // region white copies of the black icons

        // The server asks for the black icons ("*_black_24"); the old app paints them as they are, so on the dark theme many were
        // black on a dark background. Only some of them have a white sibling ("*_white_24"): the others get one here, as a bitmap
        // that shows the black icon tinted white. The extension returns the white sibling instead of the black one on the dark
        // theme (MissingResourcesPatch.getDarkDrawableId); the Shorts player (always dark) points to the white ones directly.
        val blackIcon = Regex("_black_(\\d+)$")
        val drawableDirectory = File(this["res"], "drawable").also { it.mkdirs() }

        bitmapDrawables.filter { blackIcon.containsMatchIn(it) }.sorted().forEach { black ->
            val white = black.replace(blackIcon) { "_white_" + it.groupValues[1] }
            if (white in existingDrawables) return@forEach

            File(drawableDirectory, "$white.xml").writeText(
                """<?xml version="1.0" encoding="utf-8"?>
<bitmap xmlns:android="http://schemas.android.com/apk/res/android"
    android:src="@drawable/$black"
    android:tint="#ffffffff" />
"""
            )
            existingDrawables.add(white)
        }

        // endregion

        document("res/values/drawables.xml").use { document ->
            val rootNode = document.documentElement

            DRAWABLE_ALIASES.forEach { (name, targets) ->
                if (name in existingDrawables) return@forEach

                val target = targets.firstOrNull { it in existingDrawables } ?: return@forEach

                val element = document.createElement("drawable")
                element.setAttribute("name", name)
                element.textContent = "@drawable/$target"
                rootNode.appendChild(element)

                // An alias can be the target of a later alias (e.g. *_shadowed).
                existingDrawables.add(name)
            }

            // The white sibling of every black alias (the name that the server asks for on the dark theme) points to the white
            // sibling of its target.
            DRAWABLE_ALIASES.forEach { (name, targets) ->
                if (!name.contains("_black_")) return@forEach

                val whiteName = name.replace("_black_", "_white_")
                if (whiteName in existingDrawables) return@forEach

                val whiteTarget = targets
                    .firstOrNull { it in existingDrawables && it.contains("_black_") }
                    ?.replace("_black_", "_white_")
                    ?.takeIf { it in existingDrawables }
                    ?: return@forEach

                val element = document.createElement("drawable")
                element.setAttribute("name", whiteName)
                element.textContent = "@drawable/$whiteTarget"
                rootNode.appendChild(element)
                existingDrawables.add(whiteName)
            }
        }

        // endregion

        // region add resources

        if ("ic_waveform_elements" !in existingDrawables) {
            copyResources(
                "youtube/addmissingresources",
                ResourceGroup("drawable", "ic_waveform_elements.xml"),
            )
        }

        // endregion

        // region transparent shell activity of the settings

        // The settings button of the top bar (TopBarButtonsPatch.java) starts Shell_SettingsActivity, which only looks up
        // the account and then starts the real settings screen. With its own theme (dark, with a title bar) a black window
        // with the name of the app shows up before the settings, so it gets a transparent theme (same as the app does for
        // its other shell activities, which use Theme.NoDisplay).
        copyResources(
            "youtube/addmissingresources",
            ResourceGroup("values", "revanced_shell_transparent_theme.xml"),
        )

        document("AndroidManifest.xml").use { document ->
            val activities = document.getElementsByTagName("activity")

            for (i in 0 until activities.length) {
                val attributes = activities.item(i).attributes ?: continue

                if (attributes.getNamedItem("android:name")?.nodeValue != SETTINGS_SHELL_ACTIVITY) continue

                val theme = attributes.getNamedItem("android:theme")
                    ?: document.createAttribute("android:theme").also(attributes::setNamedItem)
                theme.nodeValue = "@style/Theme.RvxA5.TransparentShell"
                break
            }
        }

        // endregion

        // region settings

        // Switches of the top bar buttons (TopBarButtonsPatch.java).
        ResourceHelper.addSettings2(
            this,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: LAYOUT_SETTINGS",
            "PREFERENCE_HEADER: GENERAL",
            "SETTINGS: TOP_BAR_BUTTONS"
        )

        // endregion

        ResourceHelper.patchSuccess(
            this,
            "add-missing-resources"
        )
    }
}
