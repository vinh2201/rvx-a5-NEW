package app.revanced.patches.youtube.misc.playeroverlay.fingerprint

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext

// Old: PlayerOverlaysOnFinishInflateFingerprint
internal val BytecodePatchContext.playerOverlaysOnFinishInflateMethodMatch by composingFirstMethod {
    definingClass("/YouTubePlayerOverlaysLayout;")
    name("onFinishInflate")
}
