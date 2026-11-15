package app.revanced.patches.youtube.layout.general.mixplaylists.bytecode.patch

import app.revanced.patcher.CompositeMatch
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.layout.general.mixplaylists.bytecode.fingerprints.createMixPlaylistMethodMatch
import app.revanced.patches.youtube.layout.general.mixplaylists.bytecode.fingerprints.fourthCreateMixPlaylistMethodMatch
import app.revanced.patches.youtube.layout.general.mixplaylists.bytecode.fingerprints.secondCreateMixPlaylistMethodMatch
import app.revanced.patches.youtube.layout.general.mixplaylists.bytecode.fingerprints.thirdCreateMixPlaylistMethodMatch
import app.revanced.patches.youtube.misc.resourceid.patch.sharedResourceIdPatch
import app.revanced.shared.extensions.injectHideCall
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private fun CompositeMatch.addHook() {
    val insertIndex = this[-1] - 3
    val register = method.getInstruction<OneRegisterInstruction>(insertIndex - 2).registerA
    method.injectHideCall(insertIndex, register, "layout/GeneralLayoutPatch", "hideMixPlaylists")
}

private fun CompositeMatch.hookMixPlaylists() {
    // third: iput-object, fourth: check-cast; both have the register in registerA
    val endIndex = this[-1]
    val register = method.getInstruction<OneRegisterInstruction>(endIndex).registerA
    method.injectHideCall(endIndex, register, "layout/GeneralLayoutPatch", "hideMixPlaylists")
}

// Old name: hide-mix-playlists-bytecode-patch (helper patch, never selectable)
val mixPlaylistsBytecodePatch = bytecodePatch(
    description = "Removes mix playlists from home feed and video player (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(sharedResourceIdPatch)

    apply {
        arrayOf(
            createMixPlaylistMethodMatch,
            secondCreateMixPlaylistMethodMatch
        ).forEach {
            it.addHook()
        }

        arrayOf(
            thirdCreateMixPlaylistMethodMatch,
            fourthCreateMixPlaylistMethodMatch
        ).forEach {
            it.hookMixPlaylists()
        }
    }
}
