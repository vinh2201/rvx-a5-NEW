package app.revanced.patches.youtube.misc.layoutupdates

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.integrations.patch.integrationsPatch
import app.revanced.patches.youtube.misc.layoutupdates.fingerprints.configLoadMethodMatch
import app.revanced.patches.youtube.misc.layoutupdates.fingerprints.requestConfigInfoMethodMatch
import app.revanced.shared.util.integrations.Constants.UTILS_PATH
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

internal const val LAYOUT_UPDATES_CLASS_DESCRIPTOR = "$UTILS_PATH/LayoutUpdatesPatch;"

private const val CONFIG_PREFIX = "com.google.android.libraries.youtube.innertube."

// Preference key -> extension method that receives the value that was read from the preferences.
private val FROZEN_VALUES = listOf(
    CONFIG_PREFIX + "cold_config_group" to "loadColdConfigGroup",
    CONFIG_PREFIX + "cold_hash_data" to "loadColdHashData",
    CONFIG_PREFIX + "hot_config_group" to "loadHotConfigGroup",
    CONFIG_PREFIX + "hot_hash_data" to "loadHotHashData",
)

private data class Hook(val insertIndex: Int, val register: Int, val method: String)

// Old names: freeze-layout-updates + disable-layout-updates (helper patches, never selectable), merged because both
// hook the same layout configuration: the guard decides at runtime (see LayoutUpdatesPatch.java).
val layoutConfigGuardBytecodePatch = bytecodePatch(
    description = "Controls which layout configuration the app uses (guard, freeze and disable layout updates).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(integrationsPatch)

    apply {
        // Request: the stored configuration is not sent when it is ignored or "Disable layout updates" is on.
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

        // Load: the stored configuration is read from the preferences by aauq.run().
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
