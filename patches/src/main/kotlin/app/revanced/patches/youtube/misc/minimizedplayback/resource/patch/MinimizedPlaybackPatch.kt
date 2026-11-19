package app.revanced.patches.youtube.misc.minimizedplayback.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.minimizedplayback.bytecode.patch.minimizedPlaybackBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: enable-minimized-playback
val minimizedPlaybackPatch = resourcePatch(
    name = "Enable minimized playback (A5)",
    description = "Enables minimized and background playback.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        minimizedPlaybackBytecodePatch,
        settingsPatch,
    )

    apply {
        ResourceHelper.patchSuccess(
            this,
            "enable-minimized-playback"
        )
    }
}
