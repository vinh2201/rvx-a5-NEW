package app.revanced.patches.youtube.swipe.swipebrightnessinhdr.bytecode.patch

import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.integrations.patch.integrationsPatch
import app.revanced.patches.youtube.swipe.swipebrightnessinhdr.bytecode.fingerprints.hdrVideoMethodMatch
import app.revanced.shared.util.integrations.Constants.SWIPE_PATH

// Old name: enable-swipe-gesture-brightness-in-hdr-patch (helper patch, never selectable)
val swipeGestureBrightnessInHDRPatch = bytecodePatch(
    description = "Enables the brightness swipe gesture in HDR videos.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    // The extension classes that are called by the hooks are merged by integrationsPatch.
    dependsOn(integrationsPatch)

    apply {
        hdrVideoMethodMatch.method.let {
            it.addInstructionsWithLabels(
                0, """
                    invoke-static {}, $SWIPE_PATH/EnableSwipeGestureBrightnessInHDRPatch;->enableSwipeGestureBrightnessInHDR()Z
                    move-result v0
                    if-eqz v0, :default
                    return-void
                """, ExternalLabel("default", it.getInstruction(0))
            )
        }
    }
}
