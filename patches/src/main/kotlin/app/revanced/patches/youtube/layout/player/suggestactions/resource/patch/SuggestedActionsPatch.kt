package app.revanced.patches.youtube.layout.player.suggestactions.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.player.suggestactions.bytecode.patch.suggestedActionsBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-suggested-actions
val suggestedActionsPatch = resourcePatch(
    name = "Hide suggested actions (A5)",
    description = "Hide the suggested actions bar inside the player.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        settingsPatch,
        suggestedActionsBytecodePatch,
    )

    apply {
        /*
         add settings
         */
        ResourceHelper.addSettings2(
            this,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: LAYOUT_SETTINGS",
            "PREFERENCE_HEADER: PLAYER",
            "SETTINGS: HIDE_SUGGESTED_ACTION"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-suggested-actions"
        )
    }
}
