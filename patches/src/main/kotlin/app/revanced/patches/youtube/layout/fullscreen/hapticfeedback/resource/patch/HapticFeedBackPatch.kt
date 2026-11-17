package app.revanced.patches.youtube.layout.fullscreen.hapticfeedback.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.fullscreen.hapticfeedback.bytecode.patch.hapticFeedBackBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: disable-haptic-feedback
val hapticFeedBackPatch = resourcePatch(
    name = "Disable haptic feedback (A5)",
    description = "Disable haptic feedback when swiping.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        hapticFeedBackBytecodePatch,
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
            "PREFERENCE_HEADER: FULLSCREEN",
            "SETTINGS: DISABLE_HAPTIC_FEEDBACK"
        )

        ResourceHelper.patchSuccess(
            this,
            "disable-haptic-feedback"
        )
    }
}
