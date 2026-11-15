package app.revanced.patches.youtube.misc.externalbrowser.bytecode.patch

import app.revanced.patcher.CompositeMatch
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.externalbrowser.bytecode.fingerprints.externalBrowserPrimaryMethodMatch
import app.revanced.patches.youtube.misc.externalbrowser.bytecode.fingerprints.externalBrowserSecondaryMethodMatch
import app.revanced.patches.youtube.misc.externalbrowser.bytecode.fingerprints.externalBrowserTertiaryMethodMatch
import app.revanced.shared.util.integrations.Constants.MISC_PATH
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private fun CompositeMatch.hookExternalBrowser() {
    val endIndex = this[-1]
    val register = method.getInstruction<OneRegisterInstruction>(endIndex).registerA

    method.addInstructions(
        endIndex + 1, """
            invoke-static {v$register}, $MISC_PATH/ExternalBrowserPatch;->enableExternalBrowser(Ljava/lang/String;)Ljava/lang/String;
            move-result-object v$register
        """
    )
}

// Old name: enable-external-browser-bytecode-patch (helper patch, never selectable)
val externalBrowserBytecodePatch = bytecodePatch(
    description = "Use an external browser to open the url (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    apply {
        externalBrowserPrimaryMethodMatch.hookExternalBrowser()
        externalBrowserSecondaryMethodMatch.hookExternalBrowser()
        externalBrowserTertiaryMethodMatch.hookExternalBrowser()
    }
}
