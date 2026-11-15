package app.revanced.patches.youtube.button.autorepeat.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef

// Old: AutoNavInformerFingerprint
internal val BytecodePatchContext.autoNavInformerMethodMatch by composingFirstMethod {
    definingClass("/WillAutonavInformer;")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Z")
    parameterTypes("L")
    opcodes(
        Opcode.IGET,
        Opcode.AND_INT_LIT8,
        Opcode.IF_EQZ,
        Opcode.IGET_BOOLEAN,
        Opcode.RETURN,
        Opcode.IGET_BOOLEAN,
        Opcode.RETURN,
    )
}

// Old: AutoRepeatParentFingerprint (both strings are full literals in 16.40.36, class ajcz; the apostrophe is escaped in smali)
internal val BytecodePatchContext.autoRepeatParentMethodMatch by composingFirstMethod(
    "play() called when the player wasn't loaded.",
    "play() blocked because Background Playability failed",
) {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
}

// Old: AutoRepeatFingerprint (was resolved inside the class of AutoRepeatParentFingerprint)
internal val ClassDef.autoRepeatMethodMatch by ClassDefComposing.composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes()
    custom { implementation?.instructions?.count() == 3 && annotations.isEmpty() }
}
