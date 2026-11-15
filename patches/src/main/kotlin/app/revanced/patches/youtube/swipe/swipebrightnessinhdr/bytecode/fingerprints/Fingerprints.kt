package app.revanced.patches.youtube.swipe.swipebrightnessinhdr.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext

// Old: HDRVideoFingerprint ("mediaViewambientBrightnessSensor" is a full literal in 16.40.36, class afpz)
internal val BytecodePatchContext.hdrVideoMethodMatch by composingFirstMethod("mediaViewambientBrightnessSensor") {
    returnType("V")
}
