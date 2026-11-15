package app.revanced.patches.youtube.layout.player.castbutton.bytecode.patch

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.shared.util.integrations.Constants.PLAYER_LAYOUT

// Old name: hide-cast-button-bytecode-patch (helper patch, never selectable)
val hideCastButtonBytecodePatch = bytecodePatch(
    description = "Hides the cast button in the video player (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    apply {
        // read-only pass first, classes are replaced by getOrReplaceMutable below
        classDefs.toList().filter { it.type.endsWith("MediaRouteButton;") }.forEach { classDef ->
            if (classDef.methods.none { it.name == "setVisibility" }) return@forEach

            val setVisibilityMethod =
                classDefs.getOrReplaceMutable(classDef).methods.first { it.name == "setVisibility" }
            setVisibilityMethod.addInstructions(
                0, """
                    invoke-static {p1}, $PLAYER_LAYOUT->hideCastButton(I)I
                    move-result p1
                """
            )
        }
    }
}
