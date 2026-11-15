package app.revanced.patches.youtube.misc.playertype.patch

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.integrations.patch.integrationsPatch
import app.revanced.patches.youtube.misc.playertype.fingerprint.updatePlayerTypeMethodMatch
import app.revanced.shared.util.integrations.Constants.UTILS_PATH

// Old name: player-type-hook (helper patch, never selectable)
val playerTypeHookPatch = bytecodePatch(
    description = "Hook to get the current player type of WatchWhileActivity",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(integrationsPatch)

    apply {
        // hook YouTubePlayerOverlaysLayout.updatePlayerLayout()
        updatePlayerTypeMethodMatch.method.addInstruction(
            0,
            "invoke-static { p1 }, $UTILS_PATH/PlayerTypeHookPatch;->YouTubePlayerOverlaysLayout_updatePlayerTypeHookEX(Ljava/lang/Object;)V"
        )
    }
}
