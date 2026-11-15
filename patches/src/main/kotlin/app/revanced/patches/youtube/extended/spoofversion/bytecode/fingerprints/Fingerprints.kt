package app.revanced.patches.youtube.extended.spoofversion.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

// Old: AppVersionFingerprint ("Unset" is a full literal in zkf.c(Context) in 16.40.36)
internal val BytecodePatchContext.appVersionMethodMatch by composingFirstMethod("Unset") {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returnType("L")
    parameterTypes("L")
    opcodes(
        Opcode.IGET_OBJECT,
        Opcode.GOTO,
        Opcode.CONST_STRING,
    )
}
