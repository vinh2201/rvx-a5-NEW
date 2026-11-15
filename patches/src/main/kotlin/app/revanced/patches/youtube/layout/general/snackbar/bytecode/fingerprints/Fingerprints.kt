package app.revanced.patches.youtube.layout.general.snackbar.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags

// Old: HideSnackbarFingerprint (BottomUiContainer.n(View, ghl)V in 16.40.36)
internal val BytecodePatchContext.hideSnackbarMethodMatch by composingFirstMethod {
    definingClass("Lcom/google/android/apps/youtube/app/common/ui/bottomui/BottomUiContainer;")
    returnType("V")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes("L", "L")
}
