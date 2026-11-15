package app.revanced.patches.youtube.layout.player.endscreencards.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.youtube.misc.resourceid.patch.layoutCircle
import app.revanced.patches.youtube.misc.resourceid.patch.layoutIcon
import app.revanced.patches.youtube.misc.resourceid.patch.layoutVideo
import app.revanced.util.containsLiteralInstruction
import com.android.tools.smali.dexlib2.Opcode

// (resource ids are read lazily when the match is resolved, i.e. after sharedResourceIdPatch ran)

// Old: LayoutCircleFingerprint (ahxy.c()View in 16.40.36)
internal val BytecodePatchContext.layoutCircleMethodMatch by composingFirstMethod {
    opcodes(
        Opcode.CONST,
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
    )
    custom { containsLiteralInstruction(layoutCircle) }
}

// Old: LayoutIconFingerprint (ahyf.c()View in 16.40.36)
internal val BytecodePatchContext.layoutIconMethodMatch by composingFirstMethod {
    opcodes(
        Opcode.CONST,
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
    )
    custom { containsLiteralInstruction(layoutIcon) }
}

// Old: LayoutVideoFingerprint (ahxw.c()View in 16.40.36)
internal val BytecodePatchContext.layoutVideoMethodMatch by composingFirstMethod {
    opcodes(
        Opcode.CONST,
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
    )
    custom { containsLiteralInstruction(layoutVideo) }
}
