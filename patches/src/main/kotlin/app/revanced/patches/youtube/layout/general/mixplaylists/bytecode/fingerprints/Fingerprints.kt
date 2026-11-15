package app.revanced.patches.youtube.layout.general.mixplaylists.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.youtube.misc.resourceid.patch.abclistmenuitemLabelId
import app.revanced.patches.youtube.misc.resourceid.patch.bottompaneloverlaytextLabelId
import app.revanced.util.containsLiteralInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

// Old: CreateMixPlaylistFingerprint
internal val BytecodePatchContext.createMixPlaylistMethodMatch by composingFirstMethod {
    returnType("V")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    parameterTypes("L", "L", "L", "L", "L", "L", "L")
    opcodes(
        Opcode.INVOKE_DIRECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
    )
}

// Old: SecondCreateMixPlaylistFingerprint
internal val BytecodePatchContext.secondCreateMixPlaylistMethodMatch by composingFirstMethod {
    returnType("V")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    parameterTypes("L", "L", "L", "L", "L", "L")
    opcodes(
        Opcode.INVOKE_DIRECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID,
    )
}

// Old: ThirdCreateMixPlaylistFingerprint
// (resource id is read lazily when the match is resolved, i.e. after sharedResourceIdPatch ran)
internal val BytecodePatchContext.thirdCreateMixPlaylistMethodMatch by composingFirstMethod {
    opcodes(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IPUT_OBJECT,
    )
    custom { containsLiteralInstruction(bottompaneloverlaytextLabelId) }
}

// Old: FourthCreateMixPlaylistFingerprint
internal val BytecodePatchContext.fourthCreateMixPlaylistMethodMatch by composingFirstMethod {
    opcodes(
        Opcode.CONST,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.MOVE_OBJECT,
        Opcode.CHECK_CAST,
    )
    custom { containsLiteralInstruction(abclistmenuitemLabelId) }
}
