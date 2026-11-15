package app.revanced.patches.youtube.misc.sponsorblock.bytecode.patch

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.playercontrols.bytecode.patch.initializeSB
import app.revanced.patches.youtube.misc.playercontrols.bytecode.patch.injectVisibility
import app.revanced.patches.youtube.misc.playercontrols.bytecode.patch.injectVisibilityNegated
import app.revanced.patches.youtube.misc.playercontrols.bytecode.patch.playerControlsBytecodePatch
import app.revanced.patches.youtube.misc.resourceid.patch.sharedResourceIdPatch
import app.revanced.patches.youtube.misc.sponsorblock.bytecode.fingerprints.nextGenWatchLayoutMethodMatch
import app.revanced.patches.youtube.misc.videoid.mainstream.patch.injectCall
import app.revanced.patches.youtube.misc.videoid.mainstream.patch.mainstreamVideoIdPatch
import app.revanced.shared.patches.timebar.setTimebarMethod
import app.revanced.shared.util.bytecode.BytecodeHelper
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR = "Lapp/revanced/integrations/sponsorblock"
private const val INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR = "$INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR/PlayerController;"

private fun MutableMethod.injectCallRectangle(
    insertIndex: Int,
    targetRegister: Int,
    methodDescriptor: String
) {
    addInstruction(
        insertIndex,
        "invoke-static {v$targetRegister}, $INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->$methodDescriptor(Landroid/graphics/Rect;)V"
    )
}

// Old name: sponsorblock-bytecode-patch (helper patch, never selectable)
//
// PORT-NOTE: all the seekbar hooks below were verified against YouTube 16.40.36 (fse.onDraw):
//  - first move-object/from16 is "move-object/from16 v0, p0" (the seekbar)
//  - Math.round(F)I followed by "move-result v2" (thickness)
//  - 4 drawRect(Rect, Paint) calls using invoke-virtual (the /range ones are skipped by the FiveRegisterInstruction filter)
//  - the instruction at size - 3 is Canvas.drawCircle (invoke-virtual {v11, v2, v3, v1, v4}): canvas = v11, centerY = v3
// All indexes are read from the live instruction list right before each insertion, like the old patch did.
val sponsorBlockBytecodePatch = bytecodePatch(
    description = "Integrates SponsorBlock (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(
        mainstreamVideoIdPatch,
        playerControlsBytecodePatch,
        sharedResourceIdPatch,
    )

    apply {
        /*
         inject MainstreamVideoIdPatch
         */
        injectCall("$INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->setCurrentVideoId(Ljava/lang/String;)V")

        /*
         Seekbar drawing
         */
        val insertMethod = setTimebarMethod
        val insertInstructions = insertMethod.implementation!!.instructions

        /*
         Get the instance of the seekbar rectangle
         */
        for ((index, instruction) in insertInstructions.withIndex()) {
            if (instruction.opcode != Opcode.MOVE_OBJECT_FROM16) continue
            val seekbarRegister = (instruction as TwoRegisterInstruction).registerA

            insertMethod.addInstruction(
                index + 1,
                "invoke-static {v$seekbarRegister}, $INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->setSponsorBarRect(Ljava/lang/Object;)V"
            )

            break
        }

        for ((index, instruction) in insertInstructions.withIndex()) {
            if (instruction.opcode != Opcode.INVOKE_STATIC) continue

            val invokeInstruction = instruction as FiveRegisterInstruction
            if (((instruction as ReferenceInstruction).reference as MethodReference).name != "round") continue

            val insertIndex = index + 2

            // set the thickness of the segment
            insertMethod.addInstruction(
                insertIndex,
                "invoke-static {v${invokeInstruction.registerC}}, $INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->setSponsorBarThickness(I)V"
            )

            break
        }

        /*
        Set rectangle absolute left and right positions
        */
        val drawRectangleInstructions = insertInstructions.withIndex().filter { (_, instruction) ->
            instruction is ReferenceInstruction &&
                    (instruction.reference as? MethodReference)?.name == "drawRect" &&
                    instruction is FiveRegisterInstruction
        }.map { (index, instruction) ->
            index to (instruction as FiveRegisterInstruction).registerD
        }

        mapOf(
            "setSponsorBarAbsoluteLeft" to 2,
            "setSponsorBarAbsoluteRight" to 0
        ).forEach { (string, int) ->
            val (index, register) = drawRectangleInstructions[int]
            insertMethod.injectCallRectangle(index, register, string)
        }

        /*
        Draw segment
        */
        val drawSegmentInstructionInsertIndex = (insertInstructions.size - 1 - 2)

        val (canvasInstance, centerY) = (insertInstructions.elementAt(drawSegmentInstructionInsertIndex) as FiveRegisterInstruction).let {
            it.registerC to it.registerE
        }

        insertMethod.addInstruction(
            drawSegmentInstructionInsertIndex,
            "invoke-static {v$canvasInstance, v$centerY}, $INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->drawSponsorTimeBars(Landroid/graphics/Canvas;F)V"
        )

        /*
        Voting & Shield button
         */
        arrayOf("ShieldButton", "VotingButton").forEach { button ->
            initializeSB("$INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR/$button;")
            injectVisibility("$INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR/$button;")
            injectVisibilityNegated("$INTEGRATIONS_BUTTON_CLASS_DESCRIPTOR/$button;")
        }

        // set SegmentHelperLayout.context to the player layout instance
        val instanceRegister = 0

        nextGenWatchLayoutMethodMatch.method.addInstruction(
            11, // after super call
            "invoke-static/range {p$instanceRegister}, $INTEGRATIONS_PLAYER_CONTROLLER_CLASS_DESCRIPTOR->addSkipSponsorView15(Landroid/view/View;)V"
        )

        BytecodeHelper.injectInit(this, "FirstRun", "initializationSB")
    }
}
