package app.revanced.patches.youtube.misc.litho.filter.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.util.containsLiteralInstruction
import com.android.tools.smali.dexlib2.AccessFlags

// Old: LithoFingerprint (matched by the narrow literal 255496645)
internal val BytecodePatchContext.lithoMethodMatch by composingFirstMethod {
    returnType("L")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes("L", "L", "L", "L", "L", "Z")
    custom { containsLiteralInstruction(255496645L) }
}
