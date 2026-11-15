package app.revanced.patches.youtube.misc.updatescreen.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags

// Old: UpdateScreenFingerprint (esx.b(Intent)Intent in 16.40.36).
// Builds the intent of NewVersionAvailableActivity ("show_force_upgrade" / "forward_intent") when the installed
// version is older than "min_app_version" or is in "denylisted_app_versions"; it returns null when no update screen
// has to be shown (every caller, e.g. erc and ott, checks the result for null).
//
// PORT-NOTE: both literals are full literals (exact match in patcher v22). The old opcode pattern was dropped:
// the two literals + the return/parameter types are already unique in 16.40.36 (the only other method containing
// both literals is NewVersionAvailableActivity.onCreate, which returns V), and a pattern only makes it more fragile.
//
// PORT-NOTE (rvx a6 comparison): the a6 version hooks the constructor of "AppBlockingCheckResult(Intent, boolean)"
// and sets the intent to null. That class does not exist in 16.40.36 (it was introduced in later versions),
// so the a6 approach can not be applied here. Both versions have the same effect: no update intent -> no screen.
internal val BytecodePatchContext.updateScreenMethodMatch by composingFirstMethod("show_force_upgrade", "forward_intent") {
    returnType("Landroid/content/Intent;")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes("Landroid/content/Intent;")
}
