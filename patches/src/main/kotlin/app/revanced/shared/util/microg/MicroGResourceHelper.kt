package app.revanced.shared.util.microg

import app.revanced.patcher.patch.ResourcePatchContext

/**
 * Helper class for applying resource patches needed for the microg-support patches.
 */
internal object MicroGResourceHelper {
    /**
     * Patch the manifest to work with MicroG.
     *
     * @param context Resource context.
     * @param fromPackageName Original package name.
     * @param toPackageName The package name to accept.
     */
    fun patchManifest(
        context: ResourcePatchContext,
        fromPackageName: String,
        toPackageName: String
    ) {
        val manifest = context["AndroidManifest.xml"].readText()
        context["AndroidManifest.xml"].writeText(
            manifest.replace(
                // Set the package attribute of the <manifest> tag to exactly the requested value (same as the
                // original "Change package name"): nothing is appended to it and nothing of the old value is kept.
                Regex("""(<manifest\b[^>]*?\spackage=")[^"]*(")""")
            ) { match ->
                match.groupValues[1] + toPackageName + match.groupValues[2]
            }.replace(
                "android:authorities=\"$fromPackageName",
                "android:authorities=\"$toPackageName"
            ).replace(
                "$fromPackageName.permission.C2D_MESSAGE",
                "$toPackageName.permission.C2D_MESSAGE"
            ).replace(
                "$fromPackageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
                "$toPackageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
            ).replace(
                "com.google.android.c2dm",
                "${Constants.MICROG_VENDOR}.android.c2dm"
            ).replace(
                "</queries>",
                "<package android:name=\"${Constants.MICROG_VENDOR}.android.gms\"/></queries>"
            )
        )
    }
}
