package app.revanced.patches.youtube.extended.layoutswitch.bytecode.patch

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.integrations.patch.integrationsPatch
import app.revanced.shared.fingerprints.layoutSwitchMethodMatch
import app.revanced.shared.util.integrations.Constants.EXTENDED_PATH

// Old name: layout-switch-bytecode-patch (helper patch, never selectable)
val layoutSwitchBytecodePatch = bytecodePatch(
    description = "Tricks the dpi to use some tablet/phone layouts (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(integrationsPatch)

    apply {
        // yrb.P(Context)I in 16.40.36 (smallest width in dp -> 1..4 screen size class):
        //   getApplicationContext(); invoke-static ah(Context)I; move-result p0 (match index 3); if-nez p0 ...
        // The dp value (p0) is replaced right after the `move-result`: 720 = tablet layout, 480 = phone layout.
        layoutSwitchMethodMatch.let {
            it.method.addInstructions(
                it[3] + 1, """
                    invoke-static {p0}, $EXTENDED_PATH/LayoutOverridePatch;->getLayoutOverride(I)I
                    move-result p0
                """
            )
        }
    }
}
