package app.revanced.patches.youtube.layout.fullscreen.hapticfeedback.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.Opcode

// Old: MarkerHapticsFingerprint (ahzl.d(VibrationEffect, J)V in 16.40.36)
internal val BytecodePatchContext.markerHapticsMethodMatch by composingFirstMethod(
    "Failed to execute markers haptics vibrate."
)

// Old: SeekHapticsFingerprint (krm.run()V in 16.40.36)
internal val BytecodePatchContext.seekHapticsMethodMatch by composingFirstMethod(
    "Failed to easy seek haptics vibrate."
) {
    opcodes(
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID,
    )
}

// NOTE: the old ScrubbingHapticsFingerprint ("Failed to haptics vibrate for fine scrubbing.") and
// ZoomHapticsFingerprint ("Failed to haptics vibrate for video zoom") do not exist in 16.40.36 (already disabled
// in the old patch), so they are not ported.
