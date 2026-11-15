package app.revanced.patches.youtube.misc.integrations.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext

// The three hooks below were IntegrationsFingerprint subclasses (see shared/patches/integrations).
// The context register of each hook is resolved in integrationsPatch.

// Old: InitFingerprint ("Application creation" is a full literal in 16.40.36, class amlo)
internal val BytecodePatchContext.initMethodMatch by composingFirstMethod("Application creation") {
}

// Old: ServiceFingerprint (context register: registerCount - parameters.size)
internal val BytecodePatchContext.serviceMethodMatch by composingFirstMethod {
    definingClass("/ApiPlayerService;")
    name("<init>")
}

// Old: StandalonePlayerFingerprint (both strings are full literals in StandalonePlayerActivity)
internal val BytecodePatchContext.standalonePlayerMethodMatch by composingFirstMethod(
    "Invalid PlaybackStartDescriptor. Returning the instance itself.",
    "com.google.android.music",
) {
}
