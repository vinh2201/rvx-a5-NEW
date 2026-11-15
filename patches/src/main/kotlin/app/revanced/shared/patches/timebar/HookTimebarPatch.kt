package app.revanced.shared.patches.timebar

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.resourceid.patch.sharedResourceIdPatch
import app.revanced.shared.fingerprints.emptyColorMethodMatch
import app.revanced.shared.fingerprints.onDrawMethodMatch
import app.revanced.shared.fingerprints.timebarMethodMatch

// Former companion object of HookTimebarPatch.
// The old MethodFingerprintResults are replaced by the mutable methods of the matches:
//  EmptyColorFingerprintResult.mutableMethod -> emptyColorMethod
//  SetTimbarFingerprintResult.mutableMethod  -> setTimebarMethod (the onDraw method of the class of emptyColorMethod)
//  TimbarFingerprintResult.mutableMethod     -> timebarMethod
lateinit var emptyColorMethod: MutableMethod
lateinit var setTimebarMethod: MutableMethod
lateinit var timebarMethod: MutableMethod

// Old name: hook-timebar-patch (helper patch, never selectable)
val hookTimebarPatch = bytecodePatch(
    description = "Hooks the timebar of the video player.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(sharedResourceIdPatch)

    apply {
        emptyColorMethod = emptyColorMethodMatch.method
        setTimebarMethod = emptyColorMethodMatch.immutableClassDef.onDrawMethodMatch.method
        timebarMethod = timebarMethodMatch.method
    }
}
