package app.revanced.patches.youtube.layout.general.autocaptions.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

// Old: SubtitleTrackFingerprint ("DISABLE_CAPTIONS_OPTION" is a full literal in SubtitleTrack in 16.40.36)
internal val BytecodePatchContext.subtitleTrackMethodMatch by composingFirstMethod("DISABLE_CAPTIONS_OPTION") {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Z")
    parameterTypes()
    opcodes(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CONST_STRING,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.RETURN,
    )
}
