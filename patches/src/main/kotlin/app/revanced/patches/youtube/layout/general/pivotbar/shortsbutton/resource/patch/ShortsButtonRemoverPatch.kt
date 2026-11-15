package app.revanced.patches.youtube.layout.general.pivotbar.shortsbutton.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.general.pivotbar.shortsbutton.bytecode.patch.shortsButtonRemoverBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-shorts-button
val shortsButtonRemoverPatch = resourcePatch(
    name = "Hide shorts button",
    description = "Hides the shorts button in the navigation bar.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        shortsButtonRemoverBytecodePatch,
        settingsPatch,
    )

    apply {
        /*
         add settings
         */
        ResourceHelper.addSettings4(
            this,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: LAYOUT_SETTINGS",
            "PREFERENCE_HEADER: GENERAL",
            "SETTINGS: SHORTS_COMPONENT.PARENT",
            "SETTINGS: SHORTS_COMPONENT_PARENT.A",
            "SETTINGS: HIDE_SHORTS_BUTTON"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-shorts-button"
        )
    }
}
