package app.revanced.patches.youtube.layout.seekbar.timeandseekbar.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.youtube.misc.resourceid.patch.timebarColorLabelId
import app.revanced.util.containsLiteralInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef

// Old: TimeCounterParentFingerprint (fsg.<init>(Context, fsl, fss)V in 16.40.36)
// (resource id is read lazily when the match is resolved, i.e. after sharedResourceIdPatch ran)
internal val BytecodePatchContext.timeCounterParentMethodMatch by composingFirstMethod {
    returnType("V")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    custom { containsLiteralInstruction(timebarColorLabelId) }
}

// Old: TimeCounterFingerprint (was resolved inside the class of the parent; fsg.l()V in 16.40.36)
internal val ClassDef.timeCounterMethodMatch by ClassDefComposing.composingFirstMethod {
    returnType("V")
    accessFlags(AccessFlags.PROTECTED, AccessFlags.FINAL)
    opcodes(
        Opcode.CONST_WIDE_16,
        Opcode.CMP_LONG,
    )
}
