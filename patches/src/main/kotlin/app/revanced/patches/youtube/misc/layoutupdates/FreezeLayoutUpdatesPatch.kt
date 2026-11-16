package app.revanced.patches.youtube.misc.layoutupdates

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.misc.integrations.patch.integrationsPatch
import app.revanced.patches.youtube.misc.layoutupdates.fingerprints.configLoadMethodMatch
import app.revanced.patches.youtube.misc.settings.resource.patch.settingsPatch
import app.revanced.shared.util.resources.ResourceHelper
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val CONFIG_PREFIX = "com.google.android.libraries.youtube.innertube."

// Preference key -> extension method that receives the value that was read from the preferences.
private val FROZEN_VALUES = listOf(
    CONFIG_PREFIX + "cold_config_group" to "freezeColdConfigGroup",
    CONFIG_PREFIX + "cold_hash_data" to "freezeColdHashData",
    CONFIG_PREFIX + "hot_config_group" to "freezeHotConfigGroup",
    CONFIG_PREFIX + "hot_hash_data" to "freezeHotHashData",
)

private data class Hook(val insertIndex: Int, val register: Int, val method: String)

// Old name: freeze-layout-updates (helper patch, never selectable)
val freezeLayoutUpdatesBytecodePatch = bytecodePatch(
    description = "Uses the layout configuration that was saved the first time instead of the one that the server sent.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(integrationsPatch)

    apply {
        configLoadMethodMatch.method.apply {
            val instructions = implementation!!.instructions.toList()

            // Resolves the string constant that was last loaded into the register before the given index.
            fun keyBefore(index: Int, register: Int): String? {
                for (i in index - 1 downTo 0) {
                    val instruction = instructions[i]
                    if (instruction.opcode == Opcode.CONST_STRING &&
                        (instruction as OneRegisterInstruction).registerA == register
                    ) {
                        return ((instruction as ReferenceInstruction).reference as StringReference).string
                    }
                }
                return null
            }

            // SharedPreferences.getString(key, default) calls followed by move-result-object.
            val hooks = mutableListOf<Hook>()

            instructions.forEachIndexed { index, instruction ->
                if (instruction.opcode != Opcode.INVOKE_INTERFACE) return@forEachIndexed

                val reference = (instruction as ReferenceInstruction).reference as? MethodReference ?: return@forEachIndexed
                if (reference.definingClass != "Landroid/content/SharedPreferences;" || reference.name != "getString") {
                    return@forEachIndexed
                }

                val result = instructions.getOrNull(index + 1) ?: return@forEachIndexed
                if (result.opcode != Opcode.MOVE_RESULT_OBJECT) return@forEachIndexed

                val key = keyBefore(index, (instruction as FiveRegisterInstruction).registerD) ?: return@forEachIndexed
                val method = FROZEN_VALUES.firstOrNull { it.first == key }?.second ?: return@forEachIndexed

                hooks += Hook(index + 2, (result as OneRegisterInstruction).registerA, method)
            }

            if (hooks.map { it.method }.distinct().size != FROZEN_VALUES.size) {
                throw IllegalStateException("Could not find all layout configuration preference reads")
            }

            // Highest index first, so the indexes of the remaining hooks stay valid.
            hooks.sortedByDescending { it.insertIndex }.forEach { hook ->
                addInstructions(
                    hook.insertIndex,
                    """
                        invoke-static {v${hook.register}}, $LAYOUT_UPDATES_CLASS_DESCRIPTOR->${hook.method}(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v${hook.register}
                    """
                )
            }
        }
    }
}

// Old name: freeze-layout-updates (rv6: "Freeze layout updates")
val freezeLayoutUpdatesPatch = resourcePatch(
    name = "Freeze layout updates",
    description = "Adds an option to keep the layout configuration of the first start and ignore later layout updates.",
    use = true,
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        freezeLayoutUpdatesBytecodePatch,
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
            "SETTINGS: FREEZE_LAYOUT_UPDATES"
        )

        ResourceHelper.patchSuccess(
            this,
            "freeze-layout-updates"
        )
    }
}
