package app.revanced.patches.youtube.misc.settings.bytecode.patch

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.wideLiteral
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.patches.youtube.misc.integrations.patch.integrationsPatch
import app.revanced.patches.youtube.misc.resourceid.patch.sharedResourceIdPatch
import app.revanced.patches.youtube.misc.settings.bytecode.fingerprints.themeSetterSystemMethodMatch
import app.revanced.shared.extensions.findMutableMethodOf
import app.revanced.shared.extensions.injectTheme
import app.revanced.shared.util.bytecode.BytecodeHelper
import app.revanced.shared.util.integrations.Constants.INTEGRATIONS_PATH
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

// Former companion object of SettingsBytecodePatch
const val SET_THEME =
    "invoke-static {v0}, $INTEGRATIONS_PATH/utils/ThemeHelper;->setTheme(Ljava/lang/Object;)V"

private class PrimaryThemeHit(val classDef: ClassDef, val method: Method, val viewRegister: Int)

// Old name: settings-bytecode-patch (helper patch, never selectable)
val settingsBytecodePatch = bytecodePatch(
    description = "Hooks the theme of the settings page and the initialization of the ReVanced settings.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        integrationsPatch,
        resourceMappingPatch,
        sharedResourceIdPatch,
    )

    apply {
        // resource id of "Theme.YouTube.Light" (type style); old: resourceMappings.single { it.name == name }.id
        val primaryThemeId = ResourceType.STYLE["Theme.YouTube.Light"]

        // PORT-NOTE: the old code mutated classes while iterating over them. The hits are collected first
        // (read-only pass over the original classes), then applied.
        val hits = mutableListOf<PrimaryThemeHit>()

        classDefs.toList().forEach { classDef ->
            classDef.methods.forEach { method ->
                val implementation = method.implementation ?: return@forEach

                implementation.instructions.forEachIndexed { index, instruction ->
                    if (instruction.opcode != Opcode.CONST) return@forEachIndexed

                    if (instruction.wideLiteral == primaryThemeId) { // primary theme
                        val insertIndex = index - 3
                        if (insertIndex < 0) return@forEachIndexed

                        val invokeInstruction = implementation.instructions.elementAt(insertIndex)
                        if (invokeInstruction.opcode != Opcode.IF_NE) return@forEachIndexed

                        val viewRegister = (instruction as OneRegisterInstruction).registerA
                        hits.add(PrimaryThemeHit(classDef, method, viewRegister))
                    }
                }
            }
        }

        hits.forEach { hit ->
            val mutableMethod = classDefs.getOrReplaceMutable(hit.classDef).findMutableMethodOf(hit.method)
            mutableMethod.implementation!!.injectTheme(
                mutableMethod.implementation!!.instructions.size - 1,
                hit.viewRegister,
                "setPrimaryTheme"
            )
        }

        // apply the current theme of the settings page
        themeSetterSystemMethodMatch.let {
            with(it.method) {
                addInstruction(
                    it[0],
                    SET_THEME
                )
                addInstruction(
                    this.implementation!!.instructions.size - 1,
                    SET_THEME
                )
            }
        }

        BytecodeHelper.injectInit(this, "FirstRun", "initializationRVX")
    }
}
