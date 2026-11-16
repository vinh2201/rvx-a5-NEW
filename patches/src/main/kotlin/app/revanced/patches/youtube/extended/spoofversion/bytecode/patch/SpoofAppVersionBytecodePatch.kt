package app.revanced.patches.youtube.extended.spoofversion.bytecode.patch

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.extended.spoofversion.bytecode.fingerprints.appVersionMethodMatch
import app.revanced.patches.youtube.misc.integrations.patch.integrationsPatch
import app.revanced.patches.youtube.misc.layoutupdates.layoutConfigGuardBytecodePatch
import app.revanced.shared.util.bytecode.BytecodeHelper
import app.revanced.shared.util.integrations.Constants.EXTENDED_PATH
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

// Old name: spoof-app-version-bytecode-patch (helper patch, never selectable)
val spoofAppVersionBytecodePatch = bytecodePatch(
    description = "Spoofs the YouTube client version (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        integrationsPatch,
        // While the version is spoofed the server answers with the layout of the newer app. The guard makes the app
        // ignore the stored layout configuration then (see LayoutUpdatesPatch.java).
        layoutConfigGuardBytecodePatch,
    )

    apply {
        // The guard only acts when this patch is applied.
        BytecodeHelper.patchStatus(this, "SpoofAppVersion")

        // zkf.c(Context) in 16.40.36: returns the (cached) app version. The match is
        // `iget-object vX, vPackageInfo, PackageInfo->versionName` followed by `goto` / `const-string "Unset"`.
        appVersionMethodMatch.let {
            val insertIndex = it[0]

            with(it.method) {
                val register = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                // p0 is still the Context argument here: it is only reassigned after the `goto` that follows.
                // Passing it lets the extension read the settings even when the integrations context is not set yet.
                addInstructions(
                    insertIndex + 1, """
                        invoke-static {v$register, p0}, $EXTENDED_PATH/VersionOverridePatch;->getVersionOverride(Ljava/lang/String;Landroid/content/Context;)Ljava/lang/String;
                        move-result-object v$register
                    """
                )
            }
        }
    }
}
