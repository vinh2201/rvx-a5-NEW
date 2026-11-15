package app.revanced.patches.youtube.layout.fullscreen.landscapemode.patch

import app.revanced.patcher.CompositeMatch
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.layout.fullscreen.landscapemode.fingerprints.orientationParentMethodMatch
import app.revanced.patches.youtube.layout.fullscreen.landscapemode.fingerprints.orientationPrimaryMethodMatch
import app.revanced.patches.youtube.layout.fullscreen.landscapemode.fingerprints.orientationSecondaryMethodMatch
import app.revanced.shared.util.integrations.Constants.FULLSCREEN_LAYOUT
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

// Former companion object of LandScapeModeBytecodePatch
private const val INTEGRATIONS_CLASS_DESCRIPTOR =
    "$FULLSCREEN_LAYOUT->disableLandScapeMode(Z)Z"

context(_: BytecodePatchContext)
private fun CompositeMatch.injectOverride() {
    val index = this[-1]

    with(method) {
        val register = getInstruction<OneRegisterInstruction>(index).registerA
        addInstructions(
            index + 1, """
                invoke-static {v$register}, $INTEGRATIONS_CLASS_DESCRIPTOR
                move-result v$register
            """
        )
    }
}

// Old name: (none, helper patch, never selectable)
val landScapeModeBytecodePatch = bytecodePatch(
    description = "Disable landscape mode when entering fullscreen (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    apply {
        val orientationClass = orientationParentMethodMatch.immutableClassDef

        arrayOf(
            orientationClass.orientationPrimaryMethodMatch,
            orientationClass.orientationSecondaryMethodMatch
        ).forEach {
            it.injectOverride()
        }
    }
}
