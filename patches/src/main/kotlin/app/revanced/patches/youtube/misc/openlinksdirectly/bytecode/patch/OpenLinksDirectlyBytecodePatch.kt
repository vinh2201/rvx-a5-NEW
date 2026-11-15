package app.revanced.patches.youtube.misc.openlinksdirectly.bytecode.patch

import app.revanced.patcher.CompositeMatch
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.openlinksdirectly.bytecode.fingerprints.openLinksDirectlyPrimaryMethodMatch
import app.revanced.patches.youtube.misc.openlinksdirectly.bytecode.fingerprints.openLinksDirectlySecondaryMethodMatch
import app.revanced.shared.util.integrations.Constants.MISC_PATH
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

context(_: BytecodePatchContext)
private fun CompositeMatch.hookRedirect() {
    val insertIndex = this[0]
    val register = method.getInstruction<FiveRegisterInstruction>(insertIndex).registerC

    method.replaceInstruction(
        insertIndex,
        "invoke-static {v$register}, $MISC_PATH/OpenLinksDirectlyPatch;->enableBypassRedirect(Ljava/lang/String;)Landroid/net/Uri;"
    )
}

// Old name: enable-open-links-directly-bytecode-patch (helper patch, never selectable)
val openLinksDirectlyBytecodePatch = bytecodePatch(
    description = "Bypass URL redirects when opening links (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    apply {
        openLinksDirectlyPrimaryMethodMatch.hookRedirect()
        openLinksDirectlySecondaryMethodMatch.hookRedirect()
    }
}
