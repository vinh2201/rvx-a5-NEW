package app.revanced.patches.youtube.layout.general.widesearchbar.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.shared.extensions.containsStringLiteral
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef

// Old: WideSearchbarOneParentFingerprint (all three strings are full literals in mls.k(PaneDescriptor)V in 16.40.36)
internal val BytecodePatchContext.wideSearchbarOneParentMethodMatch by composingFirstMethod(
    "FEhistory",
    "FEmy_videos",
    "FEpurchases"
) {
    returnType("V")
    accessFlags(AccessFlags.PRIVATE, AccessFlags.FINAL)
    parameterTypes("L")
}

// Old: WideSearchbarOneFingerprint (was resolved inside the class of the parent; mls.h(atnr, amuc)View in 16.40.36)
internal val ClassDef.wideSearchbarOneMethodMatch by ClassDefComposing.composingFirstMethod {
    returnType("L")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes("L", "L")
    opcodes(
        Opcode.IF_NEZ,
        Opcode.SGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_STATIC,
    )
}

// Old: WideSearchbarTwoParentFingerprint ("inappreviews_proto.pb" is a full literal in ejk.d() in 16.40.36)
internal val BytecodePatchContext.wideSearchbarTwoParentMethodMatch by composingFirstMethod("inappreviews_proto.pb") {
    returnType("L")
    accessFlags(AccessFlags.PRIVATE, AccessFlags.FINAL)
}

// Old: WideSearchbarTwoFingerprint (was resolved inside the class of the parent; ejk.j() in 16.40.36)
internal val ClassDef.wideSearchbarTwoMethodMatch by ClassDefComposing.composingFirstMethod {
    returnType("L")
    accessFlags(AccessFlags.PRIVATE, AccessFlags.FINAL)
    opcodes(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.GOTO,
        Opcode.SGET_OBJECT,
        Opcode.RETURN_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
    )
    // "VIDEO_QUALITIES_MENU_BOTTOM_SHEET_FRAGMENT" (a full literal in this method; the string also exists in other classes)
    custom { containsStringLiteral("VIDEO_QUALITIES_MENU_BOTTOM_SHEET_FRAGMENT") }
}
