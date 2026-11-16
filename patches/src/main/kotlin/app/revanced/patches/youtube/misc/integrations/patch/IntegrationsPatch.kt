package app.revanced.patches.youtube.misc.integrations.patch

import app.revanced.patcher.firstClassDef
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.integrations.fingerprints.initMethodMatch
import app.revanced.patches.youtube.misc.integrations.fingerprints.serviceMethodMatch
import app.revanced.patches.youtube.misc.integrations.fingerprints.standalonePlayerMethodMatch
import app.revanced.shared.patches.integrations.injectIntegrationsContext
import app.revanced.shared.util.integrations.Constants.INTEGRATIONS_PATH

private const val INTEGRATIONS_DESCRIPTOR = "$INTEGRATIONS_PATH/utils/ReVancedUtils;"

// Old name: integrations (helper patch, never selectable). This is the ONLY patch that merges the extension.
val integrationsPatch = bytecodePatch(
    description = "Applies mandatory patches to implement the ReVanced integrations into the application.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    extendWith("extensions/youtube-rvxa5.rve")

    apply {
        // Verify that the integrations have been merged (throws a PatchException otherwise).
        firstClassDef(INTEGRATIONS_DESCRIPTOR)

        // Same order and same register resolvers as the old hooks list: Init, StandalonePlayer, Service.
        initMethodMatch.method.injectIntegrationsContext(INTEGRATIONS_DESCRIPTOR)

        standalonePlayerMethodMatch.method.injectIntegrationsContext(INTEGRATIONS_DESCRIPTOR)

        serviceMethodMatch.method.let {
            it.injectIntegrationsContext(
                INTEGRATIONS_DESCRIPTOR,
                it.implementation!!.registerCount - it.parameters.size
            )
        }
    }
}
