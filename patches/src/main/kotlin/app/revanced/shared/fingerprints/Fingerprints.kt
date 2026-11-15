package app.revanced.shared.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.youtube.misc.resourceid.patch.imageOnlyTabId
import app.revanced.patches.youtube.misc.resourceid.patch.liveChatButtonId
import app.revanced.patches.youtube.misc.resourceid.patch.playerHeadingViewContainerId
import app.revanced.util.containsLiteralInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction

// Old: EmptyColorFingerprint
// (the method additionally had to contain the narrow literal -16777216 = 0xFF000000)
internal val BytecodePatchContext.emptyColorMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PROTECTED, AccessFlags.FINAL)
    returnType("V")
    parameterTypes()
    opcodes(
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE,
        Opcode.INVOKE_VIRTUAL,
    )
    custom {
        implementation?.instructions?.any {
            (it as? NarrowLiteralInstruction)?.narrowLiteral == -16777216
        } == true
    }
}

// Old: LayoutConstructorFingerprint
// (resource id is read lazily when the match is resolved, i.e. after sharedResourceIdPatch ran)
internal val BytecodePatchContext.layoutConstructorMethodMatch by composingFirstMethod {
    definingClass("/YouTubeControlsOverlay;")
    custom { containsLiteralInstruction(playerHeadingViewContainerId) }
}

// Old: LayoutSwitchFingerprint
internal val BytecodePatchContext.layoutSwitchMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returnType("I")
    parameterTypes("L")
    opcodes(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ,
        Opcode.CONST_4,
        Opcode.RETURN,
        Opcode.CONST_16,
        Opcode.IF_GE,
        Opcode.CONST_4,
        Opcode.RETURN,
        Opcode.CONST_16,
        Opcode.IF_GE,
        Opcode.CONST_4,
        Opcode.RETURN,
        Opcode.CONST_16,
        Opcode.IF_GE,
        Opcode.CONST_4,
        Opcode.RETURN,
        Opcode.CONST_4,
        Opcode.RETURN,
    )
}

// Old: LegacyVideoAdsFingerprint
// (the method additionally had to contain the narrow literal 4)
internal val BytecodePatchContext.legacyVideoAdsMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes()
    opcodes(
        Opcode.CONST_WIDE_16,
        Opcode.IPUT_WIDE,
        Opcode.CONST_WIDE_16,
        Opcode.IPUT_WIDE,
        Opcode.IPUT_WIDE,
        Opcode.IPUT_WIDE,
        Opcode.IPUT_WIDE,
        Opcode.CONST_4,
    )
    custom {
        implementation?.instructions?.any {
            (it as? NarrowLiteralInstruction)?.narrowLiteral == 4
        } == true
    }
}

// Old: LithoThemeFingerprint
internal val BytecodePatchContext.lithoThemeMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PROTECTED, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("L")
    opcodes(
        Opcode.APUT,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.IGET_OBJECT,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IPUT_OBJECT,
        Opcode.IGET,
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ,
        Opcode.IGET_OBJECT,
        Opcode.IGET,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID,
    )
}

// Old: LiveChatFingerprint
// (resource id is read lazily when the match is resolved, i.e. after sharedResourceIdPatch ran)
internal val BytecodePatchContext.liveChatMethodMatch by composingFirstMethod {
    opcodes(Opcode.NEW_INSTANCE)
    custom { containsLiteralInstruction(liveChatButtonId) }
}

/**
 * Old: MainstreamVideoAdsFingerprint.
 * Matches using the class found in [mainstreamVideoAdsParentMethodMatch] (old code resolved it against that class).
 */
internal val ClassDef.mainstreamVideoAdsMethodMatch by ClassDefComposing.composingFirstMethod {
    accessFlags(AccessFlags.PRIVATE, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("L", "Z")
    opcodes(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CONST_4,
        Opcode.IPUT_BOOLEAN,
        Opcode.IF_NEZ,
    )
}

// Old: MainstreamVideoAdsParentFingerprint ("exitSlot" is a full literal in 16.40.36)
internal val BytecodePatchContext.mainstreamVideoAdsParentMethodMatch by composingFirstMethod {
    instructions("exitSlot"())
}

// Old: MiniplayerColorParentFingerprint (unused by any old patch, kept 1:1)
internal val BytecodePatchContext.miniplayerColorParentMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PRIVATE, AccessFlags.FINAL)
    returnType("V")
    opcodes(
        Opcode.IGET,
        Opcode.IGET,
        Opcode.CONST_WIDE_16,
        Opcode.IF_EQ,
        Opcode.IPUT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET,
        Opcode.IGET,
        Opcode.IF_EQ,
        Opcode.IPUT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
    )
}

/**
 * Old: OnDrawFingerprint.
 * Matches using the class found in [emptyColorMethodMatch] (old code resolved it against that class).
 */
internal val ClassDef.onDrawMethodMatch by ClassDefComposing.composingFirstMethod {
    name("onDraw")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("L")
    opcodes(
        Opcode.MOVE_OBJECT_FROM16,
        Opcode.MOVE_OBJECT_FROM16,
    )
}

// Old: PivotBarFingerprint
// (resource id is read lazily when the match is resolved, i.e. after sharedResourceIdPatch ran)
internal val BytecodePatchContext.pivotBarMethodMatch by composingFirstMethod {
    custom { containsLiteralInstruction(imageOnlyTabId) }
}

// Old: StartVideoInformerFingerprint ("pc" is a full literal in 16.40.36; two methods contain it, the first one in dex order is the old result)
internal val BytecodePatchContext.startVideoInformerMethodMatch by composingFirstMethod("pc") {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    opcodes(
        Opcode.INVOKE_INTERFACE,
        Opcode.RETURN_VOID,
    )
}

// Old: SubtitleButtonControllerFingerprint
internal val BytecodePatchContext.subtitleButtonControllerMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("L")
    opcodes(
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.RETURN_VOID,
        Opcode.IGET_BOOLEAN,
        Opcode.CONST_4,
        Opcode.IF_NEZ,
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET_OBJECT,
    )
}

// Old: TimebarFingerprint
internal val BytecodePatchContext.timebarMethodMatch by composingFirstMethod {
    definingClass("/TimeBar;")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("L")
    custom { name.contains("draw") }
}
