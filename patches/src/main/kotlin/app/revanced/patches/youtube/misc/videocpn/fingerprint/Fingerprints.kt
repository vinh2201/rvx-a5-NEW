package app.revanced.patches.youtube.misc.videocpn.fingerprint

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext

// Old: OrganicPlaybackContextModelFingerprint ("Null contentCpn" is a full literal in 16.40.36, class xip)
internal val BytecodePatchContext.organicPlaybackContextModelMethodMatch by composingFirstMethod("Null contentCpn") {
    returnType("V")
}
