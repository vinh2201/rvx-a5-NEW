package app.revanced.patches.youtube.video.speed.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef

// Old: VideoSpeedParentFingerprint ("PLAYBACK_RATE_MENU_BOTTOM_SHEET_FRAGMENT" is in two classes of 16.40.36,
// but only the one in class nwm returns void; the other (ejk) returns Object)
internal val BytecodePatchContext.videoSpeedParentMethodMatch by composingFirstMethod("PLAYBACK_RATE_MENU_BOTTOM_SHEET_FRAGMENT") {
    returnType("V")
}

// Old: VideoSpeedChangedFingerprint (was resolved inside the class of VideoSpeedParentFingerprint)
internal val ClassDef.videoSpeedChangedMethodMatch by ClassDefComposing.composingFirstMethod {
    name("onItemClick")
    opcodes(
        Opcode.IGET_OBJECT,
        Opcode.IF_EQZ,
        Opcode.IF_EQZ,
        Opcode.IGET,
        Opcode.CHECK_CAST,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
    )
}

// Old: VideoSpeedSetterFingerprint
internal val BytecodePatchContext.videoSpeedSetterMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("[L", "I")
    opcodes(
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.INVOKE_STATIC,
        Opcode.INVOKE_VIRTUAL,
    )
}
