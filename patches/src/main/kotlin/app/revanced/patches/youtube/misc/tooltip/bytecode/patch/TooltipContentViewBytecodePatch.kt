package app.revanced.patches.youtube.misc.tooltip.bytecode.patch

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.resourceid.patch.sharedResourceIdPatch
import app.revanced.patches.youtube.misc.tooltip.bytecode.fingerprints.tooltipContentViewMethodMatch

// Old name: hide-tooltip-content-bytecode-patch (helper patch, never selectable)
val tooltipContentViewBytecodePatch = bytecodePatch(
    description = "Hides the tooltip box that appears on first install (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(sharedResourceIdPatch)

    apply {
        tooltipContentViewMethodMatch.method.addInstruction(
            0,
            "return-void"
        )
    }
}
