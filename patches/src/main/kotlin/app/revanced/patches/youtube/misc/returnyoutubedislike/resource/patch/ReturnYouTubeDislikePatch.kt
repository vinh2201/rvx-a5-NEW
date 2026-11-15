package app.revanced.patches.youtube.misc.returnyoutubedislike.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.returnyoutubedislike.bytecode.patch.returnYouTubeDislikeBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: return-youtube-dislike
val returnYouTubeDislikePatch = resourcePatch(
    name = "Return YouTube Dislike",
    description = "Shows the dislike count of videos using the Return YouTube Dislike API.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        settingsPatch,
        returnYouTubeDislikeBytecodePatch,
    )

    apply {
        /*
         add ReVanced Settings
         */
        ResourceHelper.addReVancedSettings(
            this,
            "PREFERENCE: RETURN_YOUTUBE_DISLIKE"
        )

        ResourceHelper.patchSuccess(
            this,
            "return-youtube-dislike"
        )
    }
}
