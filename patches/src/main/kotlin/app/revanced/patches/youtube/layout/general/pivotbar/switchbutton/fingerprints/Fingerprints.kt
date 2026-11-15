package app.revanced.patches.youtube.layout.general.pivotbar.switchbutton.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.Opcode

// Old: AutoMotiveFingerprint ("Android Automotive" is a full literal in the constructor of aaua in 16.40.36;
// the pattern is found at the `Android Wear` / `Android Automotive` device type checks, ending on `if-eqz`)
internal val BytecodePatchContext.autoMotiveMethodMatch by composingFirstMethod("Android Automotive") {
    opcodes(
        Opcode.CONST_STRING,
        Opcode.GOTO,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
    )
}
