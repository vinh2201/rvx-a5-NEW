package app.revanced.patches.youtube.layout.player.watermark.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.youtube.misc.resourceid.patch.inVideoProgrammingOverlayId
import app.revanced.util.containsLiteralInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef

// Old: HideWatermarkParentFingerprint (kpf.a(Context)View in 16.40.36)
// (resource id is read lazily when the match is resolved, i.e. after sharedResourceIdPatch ran)
internal val BytecodePatchContext.hideWatermarkParentMethodMatch by composingFirstMethod {
    returnType("L")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    custom { containsLiteralInstruction(inVideoProgrammingOverlayId) }
}

// Old: HideWatermarkFingerprint (was resolved inside the class of the parent; kpf.d(Context, View)V in 16.40.36)
internal val ClassDef.hideWatermarkMethodMatch by ClassDefComposing.composingFirstMethod {
    returnType("V")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes("L", "L")
    opcodes(
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.IGET_BOOLEAN,
    )
}
