package app.revanced.patches.youtube.misc.playercontrols.bytecode.patch

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.playercontrols.fingerprints.bottomControlsInflateMethodMatch
import app.revanced.patches.youtube.misc.playercontrols.fingerprints.controlsLayoutInflateMethodMatch
import app.revanced.patches.youtube.misc.playercontrols.fingerprints.playerControlsVisibilityMethodMatch
import app.revanced.patches.youtube.misc.playercontrols.fingerprints.visibilityNegatedMethodMatch
import app.revanced.patches.youtube.misc.playercontrols.fingerprints.visibilityNegatedParentMethodMatch
import app.revanced.patches.youtube.misc.resourceid.patch.sharedResourceIdPatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

// Former companion object of PlayerControlsBytecodePatch.
// The old code kept MethodFingerprintResults; the new code keeps the mutable method (+ index of the last matched instruction).
private lateinit var showPlayerControlsMethod: MutableMethod

private lateinit var controlsLayoutInflateMethod: MutableMethod
private var controlsLayoutInflateEndIndex = -1

private lateinit var inflateMethod: MutableMethod
private var inflateEndIndex = -1

private lateinit var visibilityNegatedMethod: MutableMethod

private fun MutableMethod.injectVisibilityCall(
    descriptor: String,
    fieldname: String
) {
    addInstruction(
        0,
        "invoke-static {p1}, $descriptor->$fieldname(Z)V"
    )
}

private fun MutableMethod.injectCalls(
    endIndex: Int,
    descriptor: String
) {
    val viewRegister = getInstruction<OneRegisterInstruction>(endIndex).registerA
    addInstruction(
        endIndex + 1,
        "invoke-static {v$viewRegister}, $descriptor->initialize(Ljava/lang/Object;)V"
    )
}

fun injectVisibility(descriptor: String) {
    showPlayerControlsMethod.injectVisibilityCall(descriptor, "changeVisibility")
}

fun injectVisibilityNegated(descriptor: String) {
    visibilityNegatedMethod.injectVisibilityCall(descriptor, "changeVisibilityNegatedImmediate")
}

fun initializeSB(descriptor: String) {
    controlsLayoutInflateMethod.injectCalls(controlsLayoutInflateEndIndex, descriptor)
}

fun initializeControl(descriptor: String) {
    inflateMethod.injectCalls(inflateEndIndex, descriptor)
}

// Old name: player-controls-bytecode-patch (helper patch, never selectable)
val playerControlsBytecodePatch = bytecodePatch(
    description = "Manages the code for the player controls of the YouTube player.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(sharedResourceIdPatch)

    apply {
        showPlayerControlsMethod = playerControlsVisibilityMethodMatch.method

        controlsLayoutInflateMethodMatch.let {
            controlsLayoutInflateMethod = it.method
            controlsLayoutInflateEndIndex = it[-1]
        }

        bottomControlsInflateMethodMatch.let {
            inflateMethod = it.method
            inflateEndIndex = it[-1]
        }

        visibilityNegatedMethod =
            visibilityNegatedParentMethodMatch.immutableClassDef.visibilityNegatedMethodMatch.method
    }
}
