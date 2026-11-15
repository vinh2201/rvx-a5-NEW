package app.revanced.patches.youtube.layout.flyoutpanel.oldqualitylayout.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.youtube.misc.resourceid.patch.videoqualityfragmentLabelId
import app.revanced.util.containsLiteralInstruction
import com.android.tools.smali.dexlib2.Opcode

// Old: QualityMenuViewInflateFingerprint (nxa.mj(LayoutInflater, ViewGroup, Bundle)View in 16.40.36;
// nwv.a(..) also contains the literal but has no invoke-super)
// (resource id is read lazily when the match is resolved, i.e. after sharedResourceIdPatch ran)
internal val BytecodePatchContext.qualityMenuViewInflateMethodMatch by composingFirstMethod {
    opcodes(Opcode.INVOKE_SUPER)
    custom { containsLiteralInstruction(videoqualityfragmentLabelId) }
}
