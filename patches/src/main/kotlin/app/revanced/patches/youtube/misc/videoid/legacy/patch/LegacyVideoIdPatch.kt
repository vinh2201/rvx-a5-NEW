package app.revanced.patches.youtube.misc.videoid.legacy.patch

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.videoid.legacy.fingerprint.legacyVideoIdMethodMatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

// Former companion object of LegacyVideoIdPatch (state shared with the patches that call injectCall).
private var offset = 2

private var insertIndex: Int = 0
private var videoIdRegister: Int = 0
private lateinit var insertMethod: MutableMethod

/**
 * Adds an invoke-static instruction, called with the new id when the video changes
 * @param methodDescriptor which method to call. Params have to be `Ljava/lang/String;`
 */
fun injectCall(
    methodDescriptor: String
) {
    insertMethod.addInstructions(
        insertIndex + offset, // move-result-object offset
        "invoke-static {v$videoIdRegister}, $methodDescriptor"
    )
}

val legacyVideoIdPatch = bytecodePatch(
    description = "Hook to detect when the video id changes (legacy)"
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    apply {
        legacyVideoIdMethodMatch.let {
            insertIndex = it[-1]

            insertMethod = it.method
            videoIdRegister = it.method.getInstruction<OneRegisterInstruction>(insertIndex + 1).registerA
        }

        // PORT-NOTE: the old code did `offset++` on a companion var that starts at 2.
        // It is assigned here so that repeated patcher runs in the same JVM stay correct.
        offset = 3 // offset so setCurrentVideoId is called before any injected call
    }
}
