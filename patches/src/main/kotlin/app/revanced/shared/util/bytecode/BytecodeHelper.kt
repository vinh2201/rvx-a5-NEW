package app.revanced.shared.util.bytecode

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.shared.util.integrations.Constants.UTILS_PATH

internal object BytecodeHelper {

    fun injectInit(
        context: BytecodePatchContext,
        descriptor: String,
        methods: String
    ) {
        // PORT-NOTE: classes are collected first (copy), because getOrReplaceMutable() replaces classes inside classDefs.
        context.classDefs.toList().filter { it.type.endsWith("WatchWhileActivity;") }.forEach { classDef ->
            classDef.methods.forEach { method ->
                if (method.name == "onCreate") {
                    val hookMethod =
                        context.classDefs.getOrReplaceMutable(classDef).methods.first { it.name == "onCreate" }

                    hookMethod.addInstruction(
                        2,
                        "invoke-static/range {p0 .. p0}, $UTILS_PATH/$descriptor;->$methods(Landroid/content/Context;)V"
                    )
                }
            }
        }
    }

    fun patchStatus(
        context: BytecodePatchContext,
        name: String
    ) {
        // PORT-NOTE: classes are collected first (copy), because getOrReplaceMutable() replaces classes inside classDefs.
        context.classDefs.toList().filter { it.type.endsWith("PatchStatus;") }.forEach { classDef ->
            classDef.methods.forEach { method ->
                if (method.name == name) {
                    val patchStatusMethod =
                        context.classDefs.getOrReplaceMutable(classDef).methods.first { it.name == name }

                    patchStatusMethod.replaceInstruction(
                        0,
                        "const/4 v0, 0x1"
                    )
                }
            }
        }
    }
}
