package app.revanced.patches.youtube.layout.fullscreen.fullscreenpanels.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

// Old: FullscreenViewAdderFingerprint (kso.d(Context, View)V in 16.40.36)
internal val BytecodePatchContext.fullscreenViewAdderMethodMatch by composingFirstMethod {
    returnType("V")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.BRIDGE, AccessFlags.SYNTHETIC)
    parameterTypes("Landroid/content/Context;", "Landroid/view/View;")
    opcodes(
        Opcode.INVOKE_VIRTUAL,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.GOTO,
        Opcode.IGET_BOOLEAN,
        Opcode.IF_EQ,
        Opcode.GOTO,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
    )
}
