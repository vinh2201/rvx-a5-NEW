package app.revanced.patches.youtube.misc.minimizedplayback.bytecode.patch

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.minimizedplayback.bytecode.fingerprints.kidsMinimizedPlaybackPolicyControllerMethodMatch
import app.revanced.patches.youtube.misc.minimizedplayback.bytecode.fingerprints.minimizedPlaybackManagerMethodMatch
import app.revanced.patches.youtube.misc.minimizedplayback.bytecode.fingerprints.minimizedPlaybackSettingsMethodMatch
import app.revanced.patches.youtube.misc.resourceid.patch.sharedResourceIdPatch
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private fun MutableMethod.hookPlayback() {
    addInstructions(
        0, """
            const/4 v0, 0x1
            return v0
        """
    )
}

private fun MutableMethod.hookPlaybackController() {
    addInstruction(0, "return-void")
}

// Old name: enable-minimized-playback-bytecode-patch (helper patch, never selectable)
val minimizedPlaybackBytecodePatch = bytecodePatch(
    description = "Enables minimized and background playback (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(sharedResourceIdPatch)

    apply {
        kidsMinimizedPlaybackPolicyControllerMethodMatch.method.hookPlaybackController()

        minimizedPlaybackManagerMethodMatch.method.hookPlayback()

        // Old: walkMutable(): the second call returning boolean inside the settings method
        // (ltt.b() calls ltt.e()Z first, then ltt.c()Z, which is the one that has to return true)
        minimizedPlaybackSettingsMethodMatch.immutableMethod.let { settingsMethod ->
            val booleanIndex = settingsMethod.implementation!!.instructions.withIndex()
                .filter { ((it.value as? ReferenceInstruction)?.reference as? MethodReference)?.returnType == "Z" }
                .elementAt(1).index

            navigate(settingsMethod).to(booleanIndex).stop().hookPlayback()
        }
    }
}
