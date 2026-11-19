package app.revanced.patches.youtube.extended.forcevp9.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.extended.forcevp9.bytecode.patch.forceVP9CodecBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: force-vp9-codec
val forceVP9CodecPatch = resourcePatch(
    name = "Force VP9 codec (A5)",
    description = "Forces the VP9 codec for videos.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        forceVP9CodecBytecodePatch,
        settingsPatch,
    )

    apply {
        /*
         add settings
         */
        ResourceHelper.addSettings(
            this,
            "PREFERENCE_CATEGORY: REVANCED_EXTENDED_SETTINGS",
            "PREFERENCE: MISC_SETTINGS",
            "SETTINGS: ENABLE_VP9_CODEC"
        )

        ResourceHelper.patchSuccess(
            this,
            "force-vp9-codec"
        )
    }
}
