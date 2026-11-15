package app.revanced.patches.youtube.misc.videocpn.patch

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.videocpn.fingerprint.organicPlaybackContextModelMethodMatch

// Former companion object of VideoCpnPatch
private lateinit var insertMethod: MutableMethod

fun injectCall(
    methodDescriptor: String
) {
    insertMethod.addInstructions(
        2,
        "invoke-static {p1,p2}, $methodDescriptor"
    )
}

val videoCpnPatch = bytecodePatch {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    apply {
        insertMethod = organicPlaybackContextModelMethodMatch.method
    }
}
