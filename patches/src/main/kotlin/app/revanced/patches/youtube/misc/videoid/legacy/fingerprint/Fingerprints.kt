package app.revanced.patches.youtube.misc.videoid.legacy.fingerprint

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

// Old: LegacyVideoIdFingerprint
internal val BytecodePatchContext.legacyVideoIdMethodMatch by composingFirstMethod {
    definingClass("/PlaybackLifecycleMonitor;")
    accessFlags(AccessFlags.DECLARED_SYNCHRONIZED, AccessFlags.FINAL, AccessFlags.PUBLIC)
    returnType("V")
    parameterTypes("L")
    opcodes(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
    )
}
