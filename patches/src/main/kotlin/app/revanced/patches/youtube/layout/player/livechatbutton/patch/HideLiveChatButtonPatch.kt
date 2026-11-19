package app.revanced.patches.youtube.layout.player.livechatbutton.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.patches.playerbutton.playerButtonPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-live-chat-button
val hideLiveChatButtonPatch = resourcePatch(
    name = "Hide live chat button (A5)",
    description = "Hides the live chat button in the video player.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        playerButtonPatch,
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
            "PREFERENCE_HEADER: PLAYER",
            "SETTINGS: HIDE_LIVE_CHATS_BUTTON"
        )

        ResourceHelper.patchSuccess(
            this,
            "hide-live-chat-button"
        )
    }
}
