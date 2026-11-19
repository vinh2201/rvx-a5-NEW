package app.revanced.patches.youtube.misc.pipnotification.resource.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.pipnotification.bytecode.patch.pipNotificationBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper

// Old name: hide-firsttime-background-notification (renamed from hide-pip-notification)
val pipNotificationPatch = resourcePatch(
    name = "Hide first-time background notification (A5)",
    description = "Disable notification when you launch background play for the first time.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        pipNotificationBytecodePatch,
        settingsPatch,
    )

    apply {
        ResourceHelper.patchSuccess(
            this,
            "hide-firsttime-background-notification"
        )
    }
}
