package app.revanced.patches.youtube.layout.general.tabletminiplayer.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.youtube.misc.resourceid.patch.floatybarQueueLabelId
import app.revanced.util.containsLiteralInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef

// Old: MiniPlayerDimensionsCalculatorFingerprint (oiq.ph(I, wx)V in 16.40.36)
// (resource id is read lazily when the match is resolved, i.e. after sharedResourceIdPatch ran)
internal val BytecodePatchContext.miniPlayerDimensionsCalculatorMethodMatch by composingFirstMethod {
    returnType("V")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    custom { containsLiteralInstruction(floatybarQueueLabelId) }
}

// Old: MiniPlayerOverrideNoContextFingerprint (was resolved inside the class of the dimensions calculator; oiq.h()Z)
internal val ClassDef.miniPlayerOverrideNoContextMethodMatch by ClassDefComposing.composingFirstMethod {
    returnType("Z")
    accessFlags(AccessFlags.PRIVATE, AccessFlags.FINAL)
    opcodes(Opcode.RETURN) // anchor to insert the instruction
}

// Old: MiniPlayerOverrideFingerprint (same class; oiq.g(Context)Z)
internal val ClassDef.miniPlayerOverrideMethodMatch by ClassDefComposing.composingFirstMethod {
    returnType("Z")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    parameterTypes("L")
    opcodes(Opcode.RETURN) // anchor to insert the instruction
}

// Old: MiniPlayerResponseModelSizeCheckFingerprint (same class)
// NOTE: this method does NOT exist in 16.40.36 (it was added in later YouTube versions); the patch treats it as optional.
internal val ClassDef.miniPlayerResponseModelSizeCheckMethodMatch by ClassDefComposing.composingFirstMethod {
    returnType("L")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes("L", "L")
    opcodes(
        Opcode.RETURN_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.CHECK_CAST,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ,
    )
}
