package app.revanced.patches.youtube.misc.swiperefresh.patch

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.swiperefresh.fingerprint.swipeRefreshLayoutMethodMatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

// Old name: enable-swipe-refresh (helper patch, never selectable)
val swipeRefreshPatch = bytecodePatch(
    description = "Enable swipe refresh.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    apply {
        swipeRefreshLayoutMethodMatch.let {
            with(it.method) {
                val insertIndex = it[-1]
                val register = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "const/4 v$register, 0x0"
                )
            }
        }
    }
}
