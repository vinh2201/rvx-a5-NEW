package app.revanced.patches.youtube.video.customspeed.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

// Old: SpeedArrayGeneratorFingerprint ("0.0#" is a full literal in 16.40.36, class ahiy)
internal val BytecodePatchContext.speedArrayGeneratorMethodMatch by composingFirstMethod("0.0#") {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returnType("[L")
}

// Old: SpeedLimiterFingerprint
internal val BytecodePatchContext.speedLimiterMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("F")
    opcodes(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.CONST_HIGH16,
        Opcode.GOTO,
        Opcode.CONST_HIGH16,
        Opcode.CONST_HIGH16,
        Opcode.INVOKE_STATIC,
    )
}

// Old: VideoSpeedEntriesFingerprint (static initializer of the VideoSpeedEntries class of the merged extension)
internal val BytecodePatchContext.videoSpeedEntriesMethodMatch by composingFirstMethod {
    definingClass("/VideoSpeedEntries;")
    name("<clinit>")
    opcodes(Opcode.FILL_ARRAY_DATA)
}
