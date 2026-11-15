package app.revanced.patches.youtube.layout.seekbar.customseekbarcolor.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.seekbar.customseekbarcolor.bytecode.patch.customSeekbarColorBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: custom-seekbar-color
// TODO: Fix (16.40.36): the old patch was already disabled (`//@Patch`). Without a `name` this patch is not selectable.
val customSeekbarColorPatch = resourcePatch(
    description = "Change seekbar color in dark mode.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        customSeekbarColorBytecodePatch,
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
            "PREFERENCE_HEADER: SEEKBAR",
            "SETTINGS: CUSTOM_SEEKBAR_COLOR"
        )

        ResourceHelper.patchSuccess(
            this,
            "custom-seekbar-color"
        )
    }
}
