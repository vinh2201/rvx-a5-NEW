package app.revanced.patches.youtube.misc.pipnotification.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

// Old: PrimaryPiPFingerprint (fac.b(AccountId)V in 16.40.36; the literal is exactly "honeycomb.Shell$HomeActivity")
// The old SecondaryPiPFingerprint was never used by the patch and is not ported.
internal val BytecodePatchContext.primaryPiPMethodMatch by composingFirstMethod(
    $$"honeycomb.Shell$HomeActivity"
) {
    returnType("V")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes("L")
    opcodes(
        Opcode.INVOKE_VIRTUAL,
        Opcode.CONST_4,
        Opcode.IPUT_BOOLEAN,
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
    )
}
