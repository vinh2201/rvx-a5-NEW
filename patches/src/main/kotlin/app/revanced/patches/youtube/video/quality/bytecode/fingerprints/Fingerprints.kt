package app.revanced.patches.youtube.video.quality.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef

// Old: VideoQualitySetterFingerprint
internal val BytecodePatchContext.videoQualitySetterMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("[L", "I", "I", "Z", "I")
    opcodes(
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IPUT_BOOLEAN,
    )
}

// Old: VideoQualityReferenceFingerprint (was resolved inside the class of VideoQualitySetterFingerprint)
internal val ClassDef.videoQualityReferenceMethodMatch by ClassDefComposing.composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("L")
    opcodes(
        Opcode.IPUT_OBJECT,
        Opcode.RETURN_VOID,
    )
}

// Old: VideoUserQualityChangeFingerprint (was resolved inside the class of VideoQualitySetterFingerprint)
internal val ClassDef.videoUserQualityChangeMethodMatch by ClassDefComposing.composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("L", "L", "I", "J")
    opcodes(
        Opcode.MOVE,
        Opcode.MOVE_WIDE,
        Opcode.INVOKE_INTERFACE_RANGE,
        Opcode.RETURN_VOID,
    )
}
