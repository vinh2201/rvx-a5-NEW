package app.revanced.patches.youtube.video.customspeed.bytecode.patch

import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.video.customspeed.bytecode.fingerprints.speedArrayGeneratorMethodMatch
import app.revanced.patches.youtube.video.customspeed.bytecode.fingerprints.speedLimiterMethodMatch
import app.revanced.patches.youtube.video.customspeed.bytecode.fingerprints.videoSpeedEntriesMethodMatch
import app.revanced.patches.youtube.misc.integrations.patch.integrationsPatch
import app.revanced.shared.patches.options.customSpeedArraysOption
import app.revanced.shared.util.integrations.Constants.VIDEO_PATH
import com.android.tools.smali.dexlib2.builder.instruction.BuilderArrayPayload
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

// Former companion object of CustomVideoSpeedBytecodePatch
private const val INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/VideoSpeedPatch;"
private const val INTEGRATIONS_VIDEO_SPEED_ENTRIES_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/VideoSpeedEntries;"
private const val speedLimitMin = 0.0f
private const val speedLimitMax = 100f

// Old name: custom-speed-bytecode-patch (helper patch, never selectable)
val customVideoSpeedBytecodePatch = bytecodePatch(
    description = "Adds more video speed options (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    // The fingerprint of VideoSpeedEntries matches a class of the extension, which is merged by integrationsPatch.
    dependsOn(integrationsPatch)

    customSpeedArraysOption()

    apply {
        val speed by customSpeedArraysOption

        val splits = speed!!.replace(" ", "").split(",")
        if (splits.isEmpty()) throw IllegalArgumentException("Invalid speed elements")

        val videoSpeedsArray = splits.map { it.toFloat().toRawBits() }

        with(speedArrayGeneratorMethodMatch.method) {
            val sizeCallIndex = implementation!!.instructions
                .indexOfFirst { ((it as? ReferenceInstruction)?.reference as? MethodReference)?.name == "size" }

            if (sizeCallIndex == -1) throw PatchException("Couldn't find call to size()")

            val sizeCallResultRegister =
                (implementation!!.instructions.elementAt(sizeCallIndex + 1) as OneRegisterInstruction).registerA

            addInstructionsWithLabels(
                sizeCallIndex + 2,
                """
                    invoke-static {}, $INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR->isCustomVideoSpeedEnabled()Z
                    move-result v9
                    if-eqz v9, :defaultspeed
                    const/4 v$sizeCallResultRegister, 0x0
                """, ExternalLabel("defaultspeed", getInstruction(sizeCallIndex + 2))
            )

            val (arrayLengthConstIndex, arrayLengthConst) = implementation!!.instructions.withIndex()
                .first { (it.value as? NarrowLiteralInstruction)?.narrowLiteral == 7 }
            val arrayLengthConstDestination = (arrayLengthConst as OneRegisterInstruction).registerA
            val videoSpeedsArrayType = "$INTEGRATIONS_VIDEO_SPEED_ENTRIES_CLASS_DESCRIPTOR->videoSpeed:[F"

            addInstructionsWithLabels(
                arrayLengthConstIndex + 1,
                """
                    if-eqz v9, :defaultspeed
                    sget-object v$arrayLengthConstDestination, $videoSpeedsArrayType
                    array-length v$arrayLengthConstDestination, v$arrayLengthConstDestination
                """, ExternalLabel("defaultspeed", getInstruction(arrayLengthConstIndex + 1))
            )

            val (originalArrayFetchIndex, originalArrayFetch) = implementation!!.instructions.withIndex()
                .first {
                    val reference = (it.value as? ReferenceInstruction)?.reference as? FieldReference
                    reference != null &&
                            reference.definingClass.contains("PlayerConfigModel") &&
                            reference.type == "[F"
                }
            val originalArrayFetchDestination = (originalArrayFetch as OneRegisterInstruction).registerA

            addInstructionsWithLabels(
                originalArrayFetchIndex + 1,
                """
                    if-eqz v9, :defaultspeed
                    sget-object v$originalArrayFetchDestination, $videoSpeedsArrayType
                """, ExternalLabel("defaultspeed", getInstruction(originalArrayFetchIndex + 1))
            )
        }

        with(speedLimiterMethodMatch.method) {
            val (limiterMinConstIndex, limiterMinConst) = implementation!!.instructions.withIndex()
                .first { (it.value as? NarrowLiteralInstruction)?.narrowLiteral == 0.25f.toRawBits() }
            val (limiterMaxConstIndex, limiterMaxConst) = implementation!!.instructions.withIndex()
                .first { (it.value as? NarrowLiteralInstruction)?.narrowLiteral == 2.0f.toRawBits() }

            val limiterMinConstDestination = (limiterMinConst as OneRegisterInstruction).registerA
            val limiterMaxConstDestination = (limiterMaxConst as OneRegisterInstruction).registerA

            fun hexFloat(float: Float): String = "0x%08x".format(float.toRawBits())

            replaceInstruction(
                limiterMinConstIndex,
                "const/high16 v$limiterMinConstDestination, ${hexFloat(speedLimitMin)}"
            )
            replaceInstruction(
                limiterMaxConstIndex,
                "const/high16 v$limiterMaxConstDestination, ${hexFloat(speedLimitMax)}"
            )
        }

        with(videoSpeedEntriesMethodMatch.method) {
            val arrayPayloadIndex = implementation!!.instructions.size - 1
            replaceInstruction(
                0,
                "const/16 v0, ${videoSpeedsArray.size}"
            )
            implementation!!.replaceInstruction(
                arrayPayloadIndex,
                BuilderArrayPayload(
                    4,
                    videoSpeedsArray
                )
            )
        }
    }
}
