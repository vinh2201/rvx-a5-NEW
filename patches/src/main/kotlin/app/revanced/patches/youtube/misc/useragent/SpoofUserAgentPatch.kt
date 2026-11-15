package app.revanced.patches.youtube.misc.useragent

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.microg.shared.Constants
import app.revanced.patches.youtube.misc.useragent.fingerprints.userAgentHeaderBuilderMethodMatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

// Old name: spoof-user-agent (helper patch, never selectable)
val spoofUserAgentPatch = bytecodePatch(
    description = "Spoofs the YouTube client to prevent playback issues.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    apply {
        userAgentHeaderBuilderMethodMatch.let {
            // wfw.ad(): StringBuilder.append(getPackageName()) -> the package name register is replaced
            // by the original package name right before it is appended
            val insertIndex = it[-1]

            with(it.method) {
                val packageNameRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerD

                addInstruction(insertIndex, "const-string v$packageNameRegister, \"${Constants.PACKAGE_NAME}\"")
            }
        }
    }
}
