package app.revanced.patches.youtube.misc.settings.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.youtube.misc.resourceid.patch.appearanceStringId
import app.revanced.util.containsLiteralInstruction
import com.android.tools.smali.dexlib2.Opcode

// Old: ThemeSetterSystemFingerprint
// (resource id is read lazily when the match is resolved, i.e. after sharedResourceIdPatch ran)
internal val BytecodePatchContext.themeSetterSystemMethodMatch by composingFirstMethod {
    returnType("L")
    opcodes(Opcode.RETURN_OBJECT)
    custom { containsLiteralInstruction(appearanceStringId) }
}
