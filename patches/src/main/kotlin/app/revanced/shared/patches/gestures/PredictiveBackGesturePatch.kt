package app.revanced.shared.patches.gestures

import app.revanced.patcher.patch.resourcePatch

private const val FLAG = "android:enableOnBackInvokedCallback"

// Old name: predictive-back-gesture (helper patch, never selectable)
val predictiveBackGesturePatch = resourcePatch(
    description = "Enables the predictive back gesture introduced on Android 13.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    apply {
        document("AndroidManifest.xml").use { document ->
            with(document.getElementsByTagName("application").item(0)) {
                if (attributes.getNamedItem(FLAG) != null) return@with

                document.createAttribute(FLAG)
                    .apply { value = "false" }
                    .let(attributes::setNamedItem)
            }
        }
    }
}
