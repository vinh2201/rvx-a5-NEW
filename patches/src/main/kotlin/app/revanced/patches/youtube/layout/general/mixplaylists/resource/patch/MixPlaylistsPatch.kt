package app.revanced.patches.youtube.layout.general.mixplaylists.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.general.mixplaylists.bytecode.patch.mixPlaylistsBytecodePatch
import app.revanced.patches.youtube.misc.litho.filter.patch.lithoFilterPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-mix-playlists
val mixPlaylistsPatch = resourcePatch(
    name = "Hide mix playlists (A5)",
    description = "Removes mix playlists from home feed and video player.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        lithoFilterPatch,
        mixPlaylistsBytecodePatch,
        settingsPatch,
    )

    apply {
        /*
         add settings
         */
        ResourceHelper.addSettings2(
            this,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: LAYOUT_SETTINGS",
            "PREFERENCE_HEADER: GENERAL",
            "SETTINGS: HIDE_MIX_PLAYLISTS"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-mix-playlists"
        )
    }
}
