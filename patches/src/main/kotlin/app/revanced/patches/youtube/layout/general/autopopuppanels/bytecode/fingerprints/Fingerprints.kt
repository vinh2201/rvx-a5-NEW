package app.revanced.patches.youtube.layout.general.autopopuppanels.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags

// Old: EngagementPanelControllerFingerprint (full literal in nzk in 16.40.36)
internal val BytecodePatchContext.engagementPanelControllerMethodMatch by composingFirstMethod(
    "EngagementPanelController: cannot show EngagementPanel before EngagementPanelController.init() has been called."
) {
    accessFlags(AccessFlags.PRIVATE, AccessFlags.FINAL)
    returnType("L")
}
