package app.revanced.patches.youtube.layout.general.headerswitch.bytecode.patch

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.shared.extensions.applyToLiteralHits
import app.revanced.shared.extensions.findLiteralHits

// Old name: header-switch-bytecode-patch (helper patch, never selectable)
val headerSwitchBytecodePatch = bytecodePatch(
    description = "Add switch to change header (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(resourceMappingPatch)

    apply {
        // attr "ytWordmarkHeader"
        val wordmarkHeaderId = ResourceType.ATTR["ytWordmarkHeader"]

        applyToLiteralHits(findLiteralHits(wordmarkHeaderId), "ytWordmarkHeader") { hit, method ->
            val viewRegister = hit.register
            method.addInstructions(
                hit.index + 1, """
                    invoke-static {v$viewRegister}, Lapp/revanced/integrations/patches/layout/GeneralLayoutPatch;->enablePremiumHeader(I)I
                    move-result v$viewRegister
                """
            )
            true
        }
    }
}
