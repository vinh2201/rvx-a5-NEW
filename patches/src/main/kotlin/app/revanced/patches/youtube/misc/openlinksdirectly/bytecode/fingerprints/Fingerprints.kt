package app.revanced.patches.youtube.misc.openlinksdirectly.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

// Old: OpenLinksDirectlyFingerprintPrimary (eue.a(Object)Object in 16.40.36).
//
// PORT-NOTE: the old fingerprint (opcode pattern only) also matches three unrelated lambda/switch methods
// in 16.40.36 (ima.apply, jcw.apply, jpu.a). The old resolver silently used the first one it found.
// The real target is the one handling the WebviewEndpoint (Uri.parse(url) -> return, then
// WebviewEndpointOuterClass$WebviewEndpoint.webviewEndpoint), so that field reference is required here.
internal val BytecodePatchContext.openLinksDirectlyPrimaryMethodMatch by composingFirstMethod {
    returnType("Ljava/lang/Object;")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes("Ljava/lang/Object;")
    opcodes(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.RETURN_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.SGET,
        Opcode.SGET_OBJECT,
    )
    custom {
        implementation?.instructions?.any {
            val reference = (it as? ReferenceInstruction)?.reference
            reference is FieldReference &&
                    reference.definingClass.contains("WebviewEndpointOuterClass\$WebviewEndpoint") &&
                    reference.name == "webviewEndpoint"
        } == true
    }
}

// Old: OpenLinksDirectlyFingerprintSecondary (wfw.al(String)Uri in 16.40.36; "://" is a full literal)
internal val BytecodePatchContext.openLinksDirectlySecondaryMethodMatch by composingFirstMethod("://") {
    returnType("Landroid/net/Uri;")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    parameterTypes("Ljava/lang/String;")
    opcodes(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
    )
}
