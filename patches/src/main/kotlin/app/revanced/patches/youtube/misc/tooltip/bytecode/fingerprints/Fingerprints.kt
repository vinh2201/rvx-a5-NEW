package app.revanced.patches.youtube.misc.tooltip.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.youtube.misc.resourceid.patch.tooltipLabelId
import app.revanced.util.containsLiteralInstruction
import com.android.tools.smali.dexlib2.AccessFlags

// Old: TooltipContentViewFingerprint (akka.c(akjl)V in 16.40.36, the only PUBLIC|FINAL V(L) method using the
// tooltip_content_view layout id; the id is read lazily after sharedResourceIdPatch ran)
internal val BytecodePatchContext.tooltipContentViewMethodMatch by composingFirstMethod {
    returnType("V")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes("L")
    custom { containsLiteralInstruction(tooltipLabelId) }
}
