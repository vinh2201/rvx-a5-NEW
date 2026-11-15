package app.revanced.patches.youtube.swipe.swipecontrols.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext

// Old: SwipeControlsHostActivityFingerprint (class of the merged extension, constructor)
internal val BytecodePatchContext.swipeControlsHostActivityMethodMatch by composingFirstMethod {
    definingClass("Lapp/revanced/integrations/swipecontrols/SwipeControlsHostActivity;")
    name("<init>")
}

// Old: WatchWhileActivityFingerprint (constructor)
internal val BytecodePatchContext.watchWhileActivityMethodMatch by composingFirstMethod {
    definingClass("/WatchWhileActivity;")
    name("<init>")
}
