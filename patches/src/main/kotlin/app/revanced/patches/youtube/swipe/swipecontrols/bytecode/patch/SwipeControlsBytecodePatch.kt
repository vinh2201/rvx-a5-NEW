package app.revanced.patches.youtube.swipe.swipecontrols.bytecode.patch

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.playertype.patch.playerTypeHookPatch
import app.revanced.patches.youtube.swipe.swipecontrols.bytecode.fingerprints.swipeControlsHostActivityMethodMatch
import app.revanced.patches.youtube.swipe.swipecontrols.bytecode.fingerprints.watchWhileActivityMethodMatch
import app.revanced.shared.extensions.transformMethods
import app.revanced.shared.extensions.traverseClassHierarchy
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

// Old name: swipe-controls-bytecode-patch (helper patch, never selectable)
val swipeControlsBytecodePatch = bytecodePatch(
    description = "Injects the swipe controls host activity into the class hierarchy of WatchWhileActivity.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(playerTypeHookPatch)

    apply {
        val wrapperClass = swipeControlsHostActivityMethodMatch.classDef
        val targetClass = watchWhileActivityMethodMatch.classDef

        // inject the wrapper class from integrations into the class hierarchy of WatchWhileActivity
        wrapperClass.setSuperClass(targetClass.superclass)
        targetClass.setSuperClass(wrapperClass.type)

        // ensure all classes and methods in the hierarchy are non-final, so we can override them in integrations
        traverseClassHierarchy(targetClass) {
            accessFlags = accessFlags and AccessFlags.FINAL.value.inv()

            transformMethods {
                ImmutableMethod(
                    definingClass,
                    name,
                    parameters,
                    returnType,
                    accessFlags and AccessFlags.FINAL.value.inv(),
                    annotations,
                    hiddenApiRestrictions,
                    implementation
                ).toMutable()
            }
        }
    }
}
