package app.revanced.patches.youtube.misc.doublebacktoclose.patch

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.integrations.patch.integrationsPatch
import app.revanced.patches.youtube.misc.doublebacktoclose.fingerprint.onBackPressedMethodMatch
import app.revanced.patches.youtube.misc.doublebacktoclose.fingerprint.scrollPositionMethodMatch
import app.revanced.patches.youtube.misc.doublebacktoclose.fingerprint.scrollTopMethodMatch
import app.revanced.patches.youtube.misc.doublebacktoclose.fingerprint.scrollTopParentMethodMatch
import app.revanced.shared.util.integrations.Constants.UTILS_PATH

private const val INTEGRATIONS_CLASS_DESCRIPTOR = "$UTILS_PATH/DoubleBackToClosePatch;"

private fun injectScrollView(
    method: MutableMethod,
    index: Int,
    descriptor: String
) {
    method.addInstruction(
        index,
        "invoke-static {}, $INTEGRATIONS_CLASS_DESCRIPTOR->$descriptor()V"
    )
}

// Old name: double-back-to-close (helper patch, never selectable; the setting lives in the settings patch)
val doubleBackToClosePatch = bytecodePatch(
    description = "Double back to close (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    // The extension classes that are called by the hooks are merged by integrationsPatch.
    dependsOn(integrationsPatch)

    apply {
        /*
        Hook onBackPressed method inside WatchWhileActivity
         */
        onBackPressedMethodMatch.let {
            it.method.addInstruction(
                it[-1],
                "invoke-static {p0}, $INTEGRATIONS_CLASS_DESCRIPTOR" +
                        "->" +
                        "closeActivityOnBackPressed(Lcom/google/android/apps/youtube/app/watchwhile/WatchWhileActivity;)V"
            )
        }

        /*
        Inject the methods which start of ScrollView
         */
        scrollPositionMethodMatch.let {
            // old: toMethodWalker(it.method).nextMethod(startIndex + 1, true)
            val insertMethod = navigate(it.immutableMethod).to(it[0] + 1).stop()
            val insertIndex = insertMethod.implementation!!.instructions.size - 1 - 1

            injectScrollView(insertMethod, insertIndex, "onStartScrollView")
        }

        /*
        Inject the methods which stop of ScrollView
         */
        scrollTopParentMethodMatch.immutableClassDef.scrollTopMethodMatch.let {
            injectScrollView(it.method, it[-1], "onStopScrollView")
        }
    }
}
