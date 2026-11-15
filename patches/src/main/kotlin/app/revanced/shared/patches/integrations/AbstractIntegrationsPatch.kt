package app.revanced.shared.patches.integrations

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.addInstruction

// PORT-NOTE: the old abstract class AbstractIntegrationsPatch / IntegrationsFingerprint is gone.
// The hooks are now plain match vals (see misc/integrations/fingerprints/Fingerprints.kt) and the YouTube
// integrationsPatch calls this helper for each of them (same behaviour as the old IntegrationsFingerprint.invoke()).

/**
 * Stores the application context in the integrations class at the start of this method.
 *
 * @param integrationsDescriptor The class descriptor of the integrations utils class that has a static `context` field.
 * @param contextRegister The register that holds the context. Defaults to the last register (`this` of the hooked method).
 */
fun MutableMethod.injectIntegrationsContext(
    integrationsDescriptor: String,
    contextRegister: Int = implementation!!.registerCount - 1,
) {
    addInstruction(
        0,
        "sput-object v$contextRegister, " +
                "$integrationsDescriptor->context:Landroid/content/Context;"
    )
}
