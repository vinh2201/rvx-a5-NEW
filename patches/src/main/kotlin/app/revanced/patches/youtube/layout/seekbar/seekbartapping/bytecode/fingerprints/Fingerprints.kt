package app.revanced.patches.youtube.layout.seekbar.seekbartapping.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.youtube.misc.resourceid.patch.accessibilityProgressTimeLabelId
import app.revanced.util.containsLiteralInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

// Old: SeekbarTappingParentFingerprint (aiem.I()String in 16.40.36)
// (resource id is read lazily when the match is resolved, i.e. after sharedResourceIdPatch ran)
internal val BytecodePatchContext.seekbarTappingParentMethodMatch by composingFirstMethod {
    returnType("L")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    custom { containsLiteralInstruction(accessibilityProgressTimeLabelId) }
}

// Old: SeekbarTappingFingerprint (fse.onTouchEvent(MotionEvent)Z in 16.40.36, the only match)
internal val BytecodePatchContext.seekbarTappingMethodMatch by composingFirstMethod {
    name("onTouchEvent")
    returnType("Z")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes("L")
    opcodes(
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.IPUT_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.IGET_BOOLEAN,
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN,
    )
}
