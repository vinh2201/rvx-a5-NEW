package app.revanced.patches.youtube.misc.sponsorblock.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext

// Old: NextGenWatchLayoutFingerprint
// (NextGenWatchLayout.<init>(Context, AttributeSet, int) in 16.40.36, the only 3 parameter constructor)
//
// PORT-NOTE: the old AppendTimeFingerprint and PlayerOverlaysLayoutInitFingerprint were registered in the old
// BytecodePatch but their results were never used, so they are not ported (the two hooks they were meant for
// are done by SponsorBlockSecondaryBytecodePatch by scanning for the resource ids).
internal val BytecodePatchContext.nextGenWatchLayoutMethodMatch by composingFirstMethod {
    returnType("V")
    definingClass("NextGenWatchLayout;")
    name("<init>")
    custom { parameterTypes.size == 3 }
}
