package app.revanced.patches.youtube.layout.general.widesearchbar.bytecode.patch

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.layout.general.widesearchbar.bytecode.fingerprints.wideSearchbarOneMethodMatch
import app.revanced.patches.youtube.layout.general.widesearchbar.bytecode.fingerprints.wideSearchbarOneParentMethodMatch
import app.revanced.patches.youtube.layout.general.widesearchbar.bytecode.fingerprints.wideSearchbarTwoMethodMatch
import app.revanced.patches.youtube.layout.general.widesearchbar.bytecode.fingerprints.wideSearchbarTwoParentMethodMatch
import app.revanced.shared.util.integrations.Constants.GENERAL_LAYOUT

private fun injectSearchBarHook(method: MutableMethod) {
    val index = method.implementation!!.instructions.size - 1
    method.addInstructions(
        index, """
            invoke-static {}, $GENERAL_LAYOUT->enableWideSearchbar()Z
            move-result p0
        """
    )
}

// Old name: enable-wide-searchbar-bytecode-patch (helper patch, never selectable)
val wideSearchbarBytecodePatch = bytecodePatch(
    description = "Replaces the search icon with a wide search bar (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    apply {
        // One: the method called at the last instruction (invoke-static) of the matched pattern
        wideSearchbarOneParentMethodMatch.immutableClassDef.wideSearchbarOneMethodMatch.let {
            // old: toMethodWalker(it.method).nextMethod(endIndex, true)
            injectSearchBarHook(navigate(it.immutableMethod).to(it[-1]).stop())
        }

        // Two: the method called at the first instruction (invoke-static) of the matched pattern
        wideSearchbarTwoParentMethodMatch.immutableClassDef.wideSearchbarTwoMethodMatch.let {
            // old: toMethodWalker(it.method).nextMethod(startIndex, true)
            injectSearchBarHook(navigate(it.immutableMethod).to(it[0]).stop())
        }
    }
}
