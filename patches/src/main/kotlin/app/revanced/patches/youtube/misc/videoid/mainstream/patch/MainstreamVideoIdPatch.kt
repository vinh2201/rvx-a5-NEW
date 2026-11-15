package app.revanced.patches.youtube.misc.videoid.mainstream.patch

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.removeInstruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.integrations.patch.integrationsPatch
import app.revanced.patches.youtube.misc.playertype.patch.playerTypeHookPatch
import app.revanced.patches.youtube.misc.videoid.mainstream.fingerprint.mainstreamVideoIdMethodMatch
import app.revanced.patches.youtube.misc.videoid.mainstream.fingerprint.playerControllerMethodMatch
import app.revanced.patches.youtube.misc.videoid.mainstream.fingerprint.playerControllerSetTimeReferenceMethodMatch
import app.revanced.patches.youtube.misc.videoid.mainstream.fingerprint.playerInitMethodMatch
import app.revanced.patches.youtube.misc.videoid.mainstream.fingerprint.repeatListenerMethodMatch
import app.revanced.patches.youtube.misc.videoid.mainstream.fingerprint.seekMethodMatch
import app.revanced.patches.youtube.misc.videoid.mainstream.fingerprint.videoTimeMethodMatch
import app.revanced.patches.youtube.misc.videoid.mainstream.fingerprint.videoTimeParentMethodMatch
import app.revanced.shared.fingerprints.emptyColorMethodMatch
import app.revanced.shared.patches.timebar.hookTimebarPatch
import app.revanced.shared.util.integrations.Constants.VIDEO_PATH
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.util.MethodUtil

// Former companion object of MainstreamVideoIdPatch (state shared with the patches that call injectCall).
internal const val VideoInformation = "$VIDEO_PATH/VideoInformation;"
private var offset = 0

private var insertIndex: Int = 0
private var reactReference: String? = null
private var videoIdRegister: Int = 0
private lateinit var insertMethod: MutableMethod
private lateinit var playerInitMethod: MutableMethod

/**
 * Adds an invoke-static instruction, called with the new id when the video changes
 * @param methodDescriptor which method to call. Params have to be `Ljava/lang/String;`
 */
fun injectCall(
    methodDescriptor: String
) {
    insertMethod.addInstructions(
        insertIndex + offset, // move-result-object offset
        "invoke-static {v$videoIdRegister}, $methodDescriptor"
    )
}

fun injectCallonCreate(MethodClass: String, MethodName: String) =
    playerInitMethod.addInstruction(
        4,
        "invoke-static {v0}, $MethodClass->$MethodName(Ljava/lang/Object;)V"
    )

val mainstreamVideoIdPatch = bytecodePatch(
    description = "Hook to detect when the video id changes (mainstream)"
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))
    // PORT-NOTE: integrationsPatch added because this patch modifies a class of the runtime extension
    // (PlayerController) and the extension must be merged first. The old patcher merged integrations globally.
    dependsOn(
        integrationsPatch,
        hookTimebarPatch,
        playerTypeHookPatch
    )

    apply {
        repeatListenerMethodMatch.let {
            val removeIndex = it[0]
            // removeInstruction(removeIndex)
            it.method.removeInstruction(removeIndex - 1)
        }


        playerInitMethodMatch.let { parentMatch ->
            playerInitMethod = parentMatch.classDef.methods.first { MethodUtil.isConstructor(it) }

            // Old SeekFingerprint was resolved against the class of PlayerInitFingerprint.
            parentMatch.immutableClassDef.seekMethodMatch.let {
                val resultMethod = it.method

                val seekHelperMethod = ImmutableMethod(
                    resultMethod.definingClass,
                    "seekTo",
                    listOf(ImmutableMethodParameter("J", null, "time")),
                    "Z",
                    AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                    null, null,
                    MutableMethodImplementation(4)
                ).toMutable()

                val seekSourceEnumType = resultMethod.parameterTypes[1].toString()

                seekHelperMethod.addInstructions(
                    0,
                    """
                        sget-object v0, $seekSourceEnumType->a:$seekSourceEnumType
                        invoke-virtual {p0, p1, p2, v0}, ${resultMethod.definingClass}->${resultMethod.name}(J$seekSourceEnumType)Z
                        move-result p1
                        return p1
                        """
                )

                parentMatch.classDef.methods.add(seekHelperMethod)
            }
        }


        // Old VideoTimeFingerprint was resolved against the class of VideoTimeParentFingerprint.
        videoTimeParentMethodMatch.immutableClassDef.videoTimeMethodMatch.method.addInstruction(
            0,
            "invoke-static {p1, p2}, $VideoInformation->setCurrentVideoTimeHighPrecision(J)V"
        )


        /*
        Set current video time
        */
        playerControllerSetTimeReferenceMethodMatch.let {
            navigate(it.immutableMethod).to(it[0]).stop().addInstruction(
                2,
                "invoke-static {p1, p2}, $VideoInformation->setCurrentVideoTime(J)V"
            )
        }


        // Old: HookTimebarPatch.EmptyColorFingerprintResult
        with(emptyColorMethodMatch.method) {
            val instructions = implementation!!.instructions

            reactReference =
                ((instructions.elementAt(instructions.count() - 3) as ReferenceInstruction).reference as FieldReference).name


            for ((index, instruction) in instructions.withIndex()) {
                if (instruction.opcode != Opcode.MOVE_RESULT_WIDE) continue
                val primaryRegister = (instruction as OneRegisterInstruction).registerA
                val secondaryRegister = primaryRegister + 1
                addInstructions(
                    index + 1, """
                        invoke-static {v$primaryRegister, v$secondaryRegister}, $VideoInformation->setCurrentVideoLength(J)V
                    """
                )
                break
            }
        }


        playerControllerMethodMatch.method.let {
            val instructions = it.implementation!!.instructions

            for ((index, instruction) in instructions.withIndex()) {
                if (instruction.opcode != Opcode.CONST_STRING) continue
                val register = (instruction as OneRegisterInstruction).registerA
                it.replaceInstruction(
                    index,
                    "const-string v$register, \"$reactReference\""
                )
                break
            }
        }


        mainstreamVideoIdMethodMatch.let {
            insertIndex = it[-1]

            insertMethod = it.method
            videoIdRegister = it.method.getInstruction<OneRegisterInstruction>(insertIndex).registerA
        }
        // PORT-NOTE: the old code did `offset++` on a companion var that starts at 0.
        // It is assigned here so that repeated patcher runs in the same JVM stay correct.
        offset = 1 // offset so setCurrentVideoId is called before any injected call


        injectCall("$VideoInformation->setCurrentVideoId(Ljava/lang/String;)V")
        injectCallonCreate(VideoInformation, "onCreate")
    }
}
