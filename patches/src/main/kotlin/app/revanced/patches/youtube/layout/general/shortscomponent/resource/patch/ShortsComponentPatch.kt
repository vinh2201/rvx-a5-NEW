package app.revanced.patches.youtube.layout.general.shortscomponent.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.layout.general.shortscomponent.bytecode.patch.shortsComponentBytecodePatch
import app.revanced.patches.youtube.misc.litho.filter.patch.lithoFilterPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-shorts-component
val shortsComponentPatch = resourcePatch(
    name = "Hide shorts components",
    description = "Hides other Shorts components.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        lithoFilterPatch,
        shortsComponentBytecodePatch,
        settingsPatch,
    )

    apply {
        /*
         add settings
         */
        ResourceHelper.addSettings5(
            this,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: LAYOUT_SETTINGS",
            "PREFERENCE_HEADER: GENERAL",
            "SETTINGS: SHORTS_COMPONENT.PARENT",
            "SETTINGS: SHORTS_COMPONENT_PARENT.A",
            "SETTINGS: SHORTS_COMPONENT_PARENT.B",
            "SETTINGS: HIDE_SHORTS_COMPONENTS"
        )

        ResourceHelper.addSettings4(
            this,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: LAYOUT_SETTINGS",
            "PREFERENCE_HEADER: GENERAL",
            "SETTINGS: SHORTS_COMPONENT.PARENT",
            "SETTINGS: SHORTS_COMPONENT_PARENT.A",
            "SETTINGS: HIDE_SHORTS_SHELF"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-shorts-component"
        )
    }
}
