package app.revanced.patches.youtube.layout.buttomplayer.comment.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.ads.general.bytecode.patch.generalAdsBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-comment-component
val commentComponentPatch = resourcePatch(
    name = "Hide comment component (A5)",
    description = "Adds options to hide comment component under a video.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        generalAdsBytecodePatch,
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
            "PREFERENCE_HEADER: BOTTOM_PLAYER",
            "SETTINGS: COMMENT_COMPONENT"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-comment-component"
        )
    }
}
