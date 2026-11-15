package app.revanced.patches.youtube.misc.microg.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags

// All of these were verified in 16.40.36: every literal is the *full* string (patcher v22 matches exact literals,
// the old fingerprints matched substrings) and every fingerprint resolves to exactly one method.

// Old: CastContextFetchFingerprint (adba.a(rsh)V in 16.40.36)
internal val BytecodePatchContext.castContextFetchMethodMatch by composingFirstMethod("Error fetching CastContext.")

// Old: CastDynamiteModuleFingerprint (qju.c(Context)qjw in 16.40.36)
internal val BytecodePatchContext.castDynamiteModuleMethodMatch by composingFirstMethod(
    "com.google.android.gms.cast.framework.internal.CastDynamiteModuleImpl"
)

// Old: CastDynamiteModuleV2Fingerprint (qyi.c(Context, String, Z)I in 16.40.36; the literal ends with a space)
internal val BytecodePatchContext.castDynamiteModuleV2MethodMatch by composingFirstMethod("Failed to load module via V2: ")

// Old: GooglePlayUtilityFingerprint (qqa.b(Context, I)I in 16.40.36)
internal val BytecodePatchContext.googlePlayUtilityMethodMatch by composingFirstMethod(
    "This should never happen.",
    "MetadataValueReader",
    "com.google.android.gms"
) {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
}

// Old: IntegrityCheckFingerprint (ebo.d(Context, ebn)String in 16.40.36)
internal val BytecodePatchContext.integrityCheckMethodMatch by composingFirstMethod(
    "This should never happen.",
    "GooglePlayServicesUtil",
    "Google Play Store signature invalid."
) {
    returnType("L")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    parameterTypes("L", "L")
}

// Old: PrimeFingerprint (amty.p()Z in 16.40.36)
internal val BytecodePatchContext.primeMethodMatch by composingFirstMethod(
    "com.google.android.GoogleCamera",
    "com.android.vending"
)

// Old: ServiceCheckFingerprint (qqa.d(Context, I)V in 16.40.36; the second literal ends with a space)
internal val BytecodePatchContext.serviceCheckMethodMatch by composingFirstMethod(
    "Google Play Services not available",
    "GooglePlayServices not available due to error "
) {
    returnType("V")
}
