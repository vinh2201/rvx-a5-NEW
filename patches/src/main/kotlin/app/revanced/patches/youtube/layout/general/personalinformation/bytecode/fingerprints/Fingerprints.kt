package app.revanced.patches.youtube.layout.general.personalinformation.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.youtube.misc.resourceid.patch.accountSwitcherAccessibilityLabelId
import app.revanced.util.containsLiteralInstruction
import com.android.tools.smali.dexlib2.Opcode

// Old: AccountSwitcherAccessibilityLabelFingerprint
// (resource id is read lazily when the match is resolved, i.e. after sharedResourceIdPatch ran)
internal val BytecodePatchContext.accountSwitcherAccessibilityLabelMethodMatch by composingFirstMethod {
    opcodes(
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.NEW_ARRAY,
        Opcode.CONST_4,
        Opcode.APUT_OBJECT,
        Opcode.CONST,
    )
    custom { containsLiteralInstruction(accountSwitcherAccessibilityLabelId) }
}
