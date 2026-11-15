package app.revanced.shared.patches.theme.bytecode

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.shared.fingerprints.lithoThemeMethodMatch
import app.revanced.shared.patches.theme.resource.generalThemeResourcePatch
import app.revanced.shared.util.integrations.Constants.UTILS_PATH

// Old name: general-theme (helper patch, never selectable)
val generalThemePatch = bytecodePatch(
    description = "Applies the general theme to the litho components.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(generalThemeResourcePatch)

    apply {
        val patchIndex = lithoThemeMethodMatch[-1] - 1

        lithoThemeMethodMatch.method.addInstructions(
            patchIndex, """
                invoke-static {v0}, $UTILS_PATH/LithoThemePatch;->applyLithoTheme(I)I
                move-result v0
            """
        )
    }
}
