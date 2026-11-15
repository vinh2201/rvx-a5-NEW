package app.revanced.patches.youtube.misc.returnyoutubedislike.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.util.containsLiteralInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

// Old: DislikeFingerprint (abbr.<init> in 16.40.36; "like/dislike" is the full literal)
internal val BytecodePatchContext.dislikeMethodMatch by composingFirstMethod("like/dislike") {
    returnType("V")
    accessFlags(AccessFlags.PROTECTED, AccessFlags.CONSTRUCTOR)
}

// Old: LikeFingerprint (abbs.<init> in 16.40.36)
internal val BytecodePatchContext.likeMethodMatch by composingFirstMethod("like/like") {
    returnType("V")
    accessFlags(AccessFlags.PROTECTED, AccessFlags.CONSTRUCTOR)
}

// Old: RemoveLikeFingerprint (abbt.<init> in 16.40.36)
internal val BytecodePatchContext.removeLikeMethodMatch by composingFirstMethod("like/removelike") {
    returnType("V")
    accessFlags(AccessFlags.PROTECTED, AccessFlags.CONSTRUCTOR)
}

// Old: ShortsTextComponentParentFingerprint (gnk.e(asus, aouo)V in 16.40.36).
// The pattern occurs twice in this method, both times followed by adax.aq(asum)Spanned, so it does not matter
// which occurrence is used.
internal val BytecodePatchContext.shortsTextComponentParentMethodMatch by composingFirstMethod {
    returnType("V")
    accessFlags(AccessFlags.PROTECTED, AccessFlags.FINAL)
    parameterTypes("L", "L")
    opcodes(
        Opcode.IGET_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.IGET_BOOLEAN,
        Opcode.IF_EQZ,
        Opcode.INVOKE_STATIC,
    )
}

// Old: TextComponentSpecFingerprint (stw.e(...)dmj in 16.40.36, the only static method with this shape).
internal val BytecodePatchContext.textComponentSpecMethodMatch by composingFirstMethod {
    returnType("L")
    accessFlags(AccessFlags.STATIC)
    opcodes(Opcode.CMPL_FLOAT)
    custom { containsLiteralInstruction(16842907L) }
}
