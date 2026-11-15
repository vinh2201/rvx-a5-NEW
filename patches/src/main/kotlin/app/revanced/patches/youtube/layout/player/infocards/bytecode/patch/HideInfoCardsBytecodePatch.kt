package app.revanced.patches.youtube.layout.player.infocards.bytecode.patch

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.layout.player.infocards.bytecode.fingerprints.infocardsIncognitoMethodMatch
import app.revanced.patches.youtube.layout.player.infocards.bytecode.fingerprints.infocardsIncognitoParentMethodMatch
import app.revanced.patches.youtube.misc.resourceid.patch.sharedResourceIdPatch
import app.revanced.shared.util.integrations.Constants.PLAYER_LAYOUT

// Old name: hide-info-cards-bytecode-patch (helper patch, never selectable)
val hideInfoCardsBytecodePatch = bytecodePatch(
    description = "Hides info-cards in videos (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(sharedResourceIdPatch)

    apply {
        infocardsIncognitoParentMethodMatch.immutableClassDef.infocardsIncognitoMethodMatch.method.addInstructions(
            1, """
                invoke-static {p4}, $PLAYER_LAYOUT->hideInfoCard(Z)Z
                move-result p4
            """
        )
    }
}
