package app.revanced.patches.youtube.misc.layoutupdates

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.integrations.patch.integrationsPatch
import app.revanced.patches.youtube.misc.layoutupdates.fingerprints.requestConfigInfoMethodMatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.integrations.Constants.UTILS_PATH
import app.revanced.shared.util.resources.ResourceHelper
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val LAYOUT_UPDATES_CLASS_DESCRIPTOR = "$UTILS_PATH/LayoutUpdatesPatch;"

// Old name: disable-layout-updates (helper patch, never selectable)
val disableLayoutUpdatesBytecodePatch = bytecodePatch(
    description = "Does not send the stored layout configuration to the server.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(integrationsPatch)

    apply {
        requestConfigInfoMethodMatch.method.apply {
            // The first three TextUtils.isEmpty() calls check the three values of the config info:
            // hot hash data, cold hash data and cold config data (always the same registers as the checked values).
            val isEmptyIndexes = implementation!!.instructions
                .withIndex()
                .filter { (_, instruction) ->
                    val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                    reference != null &&
                        reference.definingClass == "Landroid/text/TextUtils;" &&
                        reference.name == "isEmpty"
                }
                .map { it.index }

            if (isEmptyIndexes.size < 3) throw IllegalStateException("Config info checks not found")

            val registers = isEmptyIndexes.take(3).map { getInstruction<FiveRegisterInstruction>(it).registerC }

            if (registers.distinct().size != 3) throw IllegalStateException("Unexpected config info registers")

            // Insert before the first check, so every later use of the values is covered.
            val smali = registers.joinToString("\n") { register ->
                """
                    invoke-static {v$register}, $LAYOUT_UPDATES_CLASS_DESCRIPTOR->disableRequestConfig(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$register
                """
            }

            addInstructions(isEmptyIndexes.first(), smali)
        }
    }
}

// Old name: disable-layout-updates (rv6: "Disable layout updates")
val disableLayoutUpdatesPatch = resourcePatch(
    name = "Disable layout updates",
    description = "Adds an option to disable layout updates by the server.",
    use = true,
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        disableLayoutUpdatesBytecodePatch,
        settingsPatch,
    )

    apply {
        /*
         add settings
         */
        ResourceHelper.addSettings(
            this,
            "PREFERENCE_CATEGORY: REVANCED_EXTENDED_SETTINGS",
            "PREFERENCE: MISC_SETTINGS",
            "SETTINGS: DISABLE_LAYOUT_UPDATES"
        )

        ResourceHelper.patchSuccess(
            this,
            "disable-layout-updates"
        )
    }
}
