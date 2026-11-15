package app.revanced.patches.youtube.misc.playeroverlay.patch

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.integrations.patch.integrationsPatch
import app.revanced.patches.youtube.misc.playeroverlay.fingerprint.playerOverlaysOnFinishInflateMethodMatch
import app.revanced.shared.util.integrations.Constants.UTILS_PATH

// Old name: player-overlays-hook (helper patch, never selectable)
val playerOverlaysHookPatch = bytecodePatch(
    description = "Hook for adding custom overlays to the video player.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(integrationsPatch)

    apply {
        // hook YouTubePlayerOverlaysLayout.onFinishInflate()
        playerOverlaysOnFinishInflateMethodMatch.method.let {
            it.addInstruction(
                it.implementation!!.instructions.size - 2,
                "invoke-static { p0 }, $UTILS_PATH/PlayerOverlaysHookPatch;->YouTubePlayerOverlaysLayout_onFinishInflateHook(Ljava/lang/Object;)V"
            )
        }
    }
}
