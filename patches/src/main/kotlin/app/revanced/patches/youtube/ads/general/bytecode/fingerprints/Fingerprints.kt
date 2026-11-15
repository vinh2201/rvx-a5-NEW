package app.revanced.patches.youtube.ads.general.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.Opcode

// Old: ComponentContextParserFingerprint ("LoggingProperties are not in proto format" is a full literal in 16.40.36, class sny)
internal val BytecodePatchContext.componentContextParserMethodMatch by composingFirstMethod("LoggingProperties are not in proto format") {
    opcodes(
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.GOTO,
        Opcode.INVOKE_VIRTUAL,
        Opcode.CONST_16,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IPUT_OBJECT,
        Opcode.INVOKE_VIRTUAL_RANGE,
    )
}

// PORT-NOTE: the old EmptyComponentBuilderFingerprint (opcodes = INVOKE_STATIC_RANGE) was only resolved inside the
// method found by ComponentContextParserFingerprint and used for its startIndex. In v22 this is simply
// `method.indexOfFirstInstructionOrThrow(Opcode.INVOKE_STATIC_RANGE)` (see GeneralAdsBytecodePatch.kt).
