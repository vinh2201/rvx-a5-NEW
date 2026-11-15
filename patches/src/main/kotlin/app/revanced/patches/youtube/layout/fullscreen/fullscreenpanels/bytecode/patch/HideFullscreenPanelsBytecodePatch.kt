package app.revanced.patches.youtube.layout.fullscreen.fullscreenpanels.bytecode.patch

import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.removeInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.layout.fullscreen.fullscreenpanels.bytecode.fingerprints.fullscreenViewAdderMethodMatch
import app.revanced.patches.youtube.misc.resourceid.patch.sharedResourceIdPatch
import app.revanced.shared.fingerprints.layoutConstructorMethodMatch
import app.revanced.shared.util.integrations.Constants.FULLSCREEN_LAYOUT
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

// Old name: hide-fullscreen-panels-bytecode-patch (helper patch, never selectable)
val hideFullscreenPanelsBytecodePatch = bytecodePatch(
    description = "Hides video description and comments panel in fullscreen view (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(sharedResourceIdPatch)

    apply {
        fullscreenViewAdderMethodMatch.let {
            val endIndex = it[-1]

            with(it.method) {
                val register = getInstruction<FiveRegisterInstruction>(endIndex).registerD

                for (i in 1..3) removeInstruction(endIndex - i)

                addInstructions(
                    endIndex - 3, """
                        invoke-static {}, $FULLSCREEN_LAYOUT->hideFullscreenPanels()I
                        move-result v$register
                    """
                )
            }
        }

        layoutConstructorMethodMatch.method.let { method ->
            val invokeIndex = method.implementation!!.instructions.indexOfFirst {
                if (it.opcode != Opcode.INVOKE_VIRTUAL) return@indexOfFirst false

                val reference = (it as? ReferenceInstruction)?.reference as? MethodReference
                reference?.definingClass == "Landroid/widget/FrameLayout;" &&
                        reference.name == "addView" &&
                        reference.parameterTypes.map { type -> type.toString() } == listOf("Landroid/view/View;") &&
                        reference.returnType == "V"
            }
            if (invokeIndex == -1) throw PatchException("Could not find FrameLayout.addView in the layout constructor")

            method.addInstructionsWithLabels(
                invokeIndex, """
                    invoke-static {}, $FULLSCREEN_LAYOUT->hideFullscreenPanel()Z
                    move-result v14
                    if-nez v14, :hidden
                """, ExternalLabel("hidden", method.getInstruction(invokeIndex + 1))
            )
        }
    }
}
