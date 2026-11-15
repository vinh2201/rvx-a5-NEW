package app.revanced.patches.youtube.misc.minimizedplayback.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.youtube.misc.resourceid.patch.backgroundCategoryLabelId
import app.revanced.util.containsLiteralInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

// Old: KidsMinimizedPlaybackPolicyControllerFingerprint
// (MinimizedPlaybackPolicyController.h(int, fmk, atlp)V in 16.40.36)
internal val BytecodePatchContext.kidsMinimizedPlaybackPolicyControllerMethodMatch by composingFirstMethod {
    definingClass("MinimizedPlaybackPolicyController;")
    returnType("V")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes("I", "L", "L")
    opcodes(
        Opcode.IF_EQZ,
        Opcode.SGET_OBJECT,
        Opcode.IF_NE,
        Opcode.CONST_4,
        Opcode.IPUT_BOOLEAN,
        Opcode.IF_EQZ,
        Opcode.IGET,
        Opcode.INVOKE_STATIC,
    )
}

// Old: MinimizedPlaybackManagerFingerprint (ahib.h(asfk)Z in 16.40.36, the only method with this shape)
internal val BytecodePatchContext.minimizedPlaybackManagerMethodMatch by composingFirstMethod {
    returnType("Z")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    parameterTypes("L")
    opcodes(Opcode.AND_INT_LIT16)
    custom { containsLiteralInstruction(64657230L) }
}

// Old: MinimizedPlaybackSettingsFingerprint (ltt.b()String in 16.40.36)
// (resource id is read lazily when the match is resolved, i.e. after sharedResourceIdPatch ran)
internal val BytecodePatchContext.minimizedPlaybackSettingsMethodMatch by composingFirstMethod {
    returnType("L")
    opcodes(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.IF_NEZ,
        Opcode.GOTO,
    )
    custom { containsLiteralInstruction(backgroundCategoryLabelId) }
}
