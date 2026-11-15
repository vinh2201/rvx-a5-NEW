package app.revanced.patches.youtube.misc.pipnotification.bytecode.patch

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.pipnotification.bytecode.fingerprints.primaryPiPMethodMatch

// Old name: hide-pip-notification-bytecode-patch (helper patch, never selectable)
val pipNotificationBytecodePatch = bytecodePatch(
    description = "Disable the first-time background play notification (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    apply {
        primaryPiPMethodMatch.method.addInstruction(primaryPiPMethodMatch[0], "return-void")
    }
}
