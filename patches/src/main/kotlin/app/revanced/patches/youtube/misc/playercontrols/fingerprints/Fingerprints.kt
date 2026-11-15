package app.revanced.patches.youtube.misc.playercontrols.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.youtube.misc.resourceid.patch.bottomUiContainerResourceId
import app.revanced.patches.youtube.misc.resourceid.patch.controlsLayoutStubResourceId
import app.revanced.patches.youtube.misc.resourceid.patch.educationTextViewResourceId
import app.revanced.util.containsLiteralInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef

// Old: BottomControlsInflateFingerprint
// (resource ids are read lazily when the match is resolved, i.e. after sharedResourceIdPatch ran)
internal val BytecodePatchContext.bottomControlsInflateMethodMatch by composingFirstMethod {
    opcodes(
        Opcode.CHECK_CAST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
    )
    custom { containsLiteralInstruction(bottomUiContainerResourceId) }
}

// Old: ControlsLayoutInflateFingerprint
internal val BytecodePatchContext.controlsLayoutInflateMethodMatch by composingFirstMethod {
    opcodes(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
    )
    custom { containsLiteralInstruction(controlsLayoutStubResourceId) }
}

// Old: PlayerControlsVisibilityFingerprint
internal val BytecodePatchContext.playerControlsVisibilityMethodMatch by composingFirstMethod {
    definingClass("/YouTubeControlsOverlay;")
    returnType("V")
    parameterTypes("Z", "Z")
}

// Old: VisibilityNegatedParentFingerprint
internal val BytecodePatchContext.visibilityNegatedParentMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    custom { containsLiteralInstruction(educationTextViewResourceId) }
}

// Old: VisibilityNegatedFingerprint (was resolved inside the class of VisibilityNegatedParentFingerprint)
internal val ClassDef.visibilityNegatedMethodMatch by ClassDefComposing.composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Z")
    opcodes(Opcode.IF_EQZ)
}
