package app.revanced.patches.youtube.misc.tooltip.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.patches.youtube.misc.tooltip.bytecode.patch.tooltipContentViewBytecodePatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-tooltip-content (the old class was misnamed MinimizedPlaybackPatch)
val tooltipContentViewPatch = resourcePatch(
    name = "Hide tooltip content",
    description = "Hides the tooltip box that appears on first install.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        tooltipContentViewBytecodePatch,
        settingsPatch,
    )

    apply {
        ResourceHelper.patchSuccess(
            this,
            "hide-tooltip-content"
        )
    }
}
