package app.revanced.patches.youtube.misc.microg.bytecode.patch

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.layout.player.castbutton.resource.patch.hideCastButtonPatch
import app.revanced.patches.youtube.misc.microg.bytecode.fingerprints.castContextFetchMethodMatch
import app.revanced.patches.youtube.misc.microg.bytecode.fingerprints.castDynamiteModuleMethodMatch
import app.revanced.patches.youtube.misc.microg.bytecode.fingerprints.castDynamiteModuleV2MethodMatch
import app.revanced.patches.youtube.misc.microg.bytecode.fingerprints.googlePlayUtilityMethodMatch
import app.revanced.patches.youtube.misc.microg.bytecode.fingerprints.integrityCheckMethodMatch
import app.revanced.patches.youtube.misc.microg.bytecode.fingerprints.primeMethodMatch
import app.revanced.patches.youtube.misc.microg.bytecode.fingerprints.serviceCheckMethodMatch
import app.revanced.patches.youtube.misc.microg.shared.Constants.PACKAGE_NAME
import app.revanced.patches.youtube.misc.useragent.spoofUserAgentPatch
import app.revanced.shared.patches.options.youTubePackageNameOption
import app.revanced.shared.util.bytecode.BytecodeHelper
import app.revanced.shared.util.microg.MicroGBytecodeHelper

// Old name: microg-support-bytecode-patch (helper patch, never selectable)
val microGBytecodePatch = bytecodePatch(
    description = "Allows YouTube to run under a different package name with GmsCore (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        spoofUserAgentPatch,
        hideCastButtonPatch,
    )

    // PORT-NOTE: replaces the old dependency on PatchOptions
    youTubePackageNameOption()

    apply {
        val packageName by youTubePackageNameOption

        // apply common microG patch
        MicroGBytecodeHelper.patchBytecode(
            this,
            arrayOf(
                MicroGBytecodeHelper.packageNameTransform(
                    PACKAGE_NAME,
                    "$packageName"
                )
            ),
            MicroGBytecodeHelper.PrimeMethodTransformationData(
                primeMethodMatch,
                PACKAGE_NAME,
                "$packageName"
            ),
            listOf(
                integrityCheckMethodMatch,
                serviceCheckMethodMatch,
                googlePlayUtilityMethodMatch,
                castDynamiteModuleMethodMatch,
                castDynamiteModuleV2MethodMatch,
                castContextFetchMethodMatch,
            )
        )

        BytecodeHelper.injectInit(this, "MicroGPatch", "checkAvailability")
    }
}
