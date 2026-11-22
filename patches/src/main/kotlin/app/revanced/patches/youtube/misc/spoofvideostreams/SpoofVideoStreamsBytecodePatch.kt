package app.revanced.patches.youtube.misc.spoofvideostreams

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.fieldReference
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.methodReference
import app.revanced.patcher.extensions.stringReference
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.integrations.patch.integrationsPatch
import app.revanced.patches.youtube.misc.spoofvideostreams.fingerprints.buildInitPlaybackRequestMethodMatch
import app.revanced.patches.youtube.misc.spoofvideostreams.fingerprints.buildPlayerRequestUriMethodMatch
import app.revanced.patches.youtube.misc.spoofvideostreams.fingerprints.createPlaybackSpeedMenuItemMethodMatch
import app.revanced.patches.youtube.misc.spoofvideostreams.fingerprints.createPlayerRequestBodyMethodMatch
import app.revanced.patches.youtube.misc.spoofvideostreams.fingerprints.createPlayerRequestBodyWithModelMethodMatch
import app.revanced.patches.youtube.misc.spoofvideostreams.fingerprints.formatSelectionMethodMatch
import app.revanced.patches.youtube.misc.spoofvideostreams.fingerprints.nerdsStatsVideoFormatBuilderMethodMatch
import app.revanced.patches.youtube.misc.spoofvideostreams.fingerprints.playerGestureConfigSyntheticMethodMatch
import app.revanced.patches.youtube.misc.spoofvideostreams.fingerprints.playerParameterBuilderMethodMatch
import app.revanced.patches.youtube.misc.spoofvideostreams.fingerprints.qualityListBuilderMethodMatch
import app.revanced.patches.youtube.misc.spoofvideostreams.fingerprints.setPlayerRequestClientTypeMethodMatch
import app.revanced.patches.youtube.misc.spoofvideostreams.fingerprints.volleyPlayerRequestMethodMatch
import app.revanced.shared.util.integrations.Constants.MISC_PATH
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.findInstructionIndicesReversedOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS_DESCRIPTOR = "$MISC_PATH/SpoofClientPatch;"
private const val FORMAT_SELECTION_LOG_CLASS_DESCRIPTOR = "$MISC_PATH/FormatSelectionLog;"
private const val AVC_QUALITY_LADDER_CLASS_DESCRIPTOR = "$MISC_PATH/AvcQualityLadder;"

/**
 * @return The first instance field that is written with the value of the register of the instruction at [sourceIndex]
 *         (iput / iput-object) after [sourceIndex] and that belongs to [clientInfoClass].
 */
private fun MutableMethod.clientInfoFieldSetAfter(sourceIndex: Int, clientInfoClass: String): FieldReference {
    val register = getInstruction<OneRegisterInstruction>(sourceIndex).registerA

    val setIndex = indexOfFirstInstructionOrThrow(sourceIndex + 1) {
        (opcode == Opcode.IPUT || opcode == Opcode.IPUT_OBJECT) &&
                (this as TwoRegisterInstruction).registerA == register &&
                fieldReference?.definingClass == clientInfoClass
    }

    return getInstruction(setIndex).fieldReference
        ?: throw PatchException("Could not find the client info field set at $setIndex")
}

/**
 * Index of `sget` / `sget-object` of the static field [definingClass]->[name] (`android.os.Build` fields).
 */
private fun MutableMethod.indexOfBuildFieldGet(definingClass: String, name: String) =
    indexOfFirstInstructionOrThrow {
        (opcode == Opcode.SGET || opcode == Opcode.SGET_OBJECT) &&
                fieldReference?.let { it.definingClass == definingClass && it.name == name } == true
    }

/**
 * Field references of the client info (protobuf message) and its container, resolved from the app code.
 */
private class ClientInfoFields(
    val clientInfoField: FieldReference,
    val clientTypeField: FieldReference,
    val clientVersionField: FieldReference,
    val clientModelField: FieldReference,
    val clientMakeField: FieldReference,
    val clientOsVersionField: FieldReference,
    val clientSdkField: FieldReference,
    val clientOsNameField: FieldReference,
)

// Old name: spoof-client-bytecode-patch (helper patch, never selectable).
//
// Mechanism (kept from the old patch, see PROGRESS notes): the client info of the /player request body that the app itself
// builds is rewritten to the spoofed client (type, version, model, make, os version, sdk) and the headers of the request
// (user agent, X-YouTube-Client-*) are rewritten in the extension, so the server answers with streams that this old
// app can still play. The client table and all request details live in the extension (SpoofClientPatch / PlayerRoutes).
val spoofVideoStreamsBytecodePatch = bytecodePatch(
    description = "Spoofs the client of the player request so that video playback keeps working (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    // The extension class (SpoofClientPatch) is merged by integrationsPatch.
    dependsOn(integrationsPatch)

    apply {
        // region Override the headers of player requests + block /initplayback requests to fall back to /get_watch requests.

        buildInitPlaybackRequestMethodMatch.let { match ->
            val moveUriStringIndex = match[0]

            with(match.method) {
                // 1. (higher index, done first) Override the request headers.
                //    The request headers of this Cronet request builder are collected in a HashMap, the last
                //    Map.putAll(...) adds the headers of the request (Laub;->e). Right after it the request headers can still be
                //    changed before they are added to the UrlRequest.Builder.
                val putAllIndex = findInstructionIndicesReversedOrThrow {
                    opcode == Opcode.INVOKE_INTERFACE &&
                            methodReference?.let { it.definingClass == "Ljava/util/Map;" && it.name == "putAll" } == true
                }.first()

                val putAllInstruction = getInstruction<FiveRegisterInstruction>(putAllIndex)
                val headersRegister = putAllInstruction.registerC
                // The register that held the headers of the request (it is overwritten right after the headers are added).
                val freeRegister = putAllInstruction.registerD

                val uriGetIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IGET_OBJECT && fieldReference?.type == "Landroid/net/Uri;"
                }
                val uriField = getInstruction(uriGetIndex).fieldReference!!
                val requestRegister = getInstruction<TwoRegisterInstruction>(uriGetIndex).registerB

                if (headersRegister > 15 || freeRegister > 15 || requestRegister > 15) {
                    throw PatchException("Registers are not usable by invoke-static: $headersRegister, $freeRegister, $requestRegister")
                }

                addInstructions(
                    putAllIndex + 1,
                    """
                        iget-object v$freeRegister, v$requestRegister, $uriField
                        invoke-static { v$headersRegister, v$freeRegister }, $EXTENSION_CLASS_DESCRIPTOR->overrideRequestHeaders(Ljava/util/Map;Landroid/net/Uri;)V
                    """,
                )

                // 2. (lower index, done last) Block /initplayback requests.
                //    Inserted right after Uri.toString() -> move-result-object (the request URI string).
                val targetRegister = getInstruction<OneRegisterInstruction>(moveUriStringIndex).registerA

                addInstructions(
                    moveUriStringIndex + 1,
                    """
                        invoke-static { v$targetRegister }, $EXTENSION_CLASS_DESCRIPTOR->blockInitPlaybackRequest(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$targetRegister
                    """,
                )
            }
        }

        // endregion

        // region The /player request of this app (Volley request -> Cronet request, yyi.a()).
        //
        // The hooks above are in the Cronet data source of the media player (awa.o): they see the requests of the streams,
        // never the /player request. That one is a Volley request (aauz) that yyi.a() turns into a Cronet request: the
        // URL string, the headers (a HashMap filled with the headers of the request) and the body are all in registers
        // right before the request builder is created. The Android VR family answers the app's own request with HTTP 400,
        // so for these clients the extension replaces URL, headers and body there (SpoofClientPatch.beginPlayerRequest).

        volleyPlayerRequestMethodMatch.let { match ->
            with(match.method) {
                // The headers: the only Map.putAll call of the method (adds the headers of the request to the HashMap).
                val putAllIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_INTERFACE &&
                            methodReference?.let { it.definingClass == "Ljava/util/Map;" && it.name == "putAll" } == true
                }
                val headersRegister = getInstruction<FiveRegisterInstruction>(putAllIndex).registerC

                // The body: the result of the only call of the method that returns byte[] (Request.getBody()).
                val bodyCallIndex = indexOfFirstInstructionOrThrow {
                    (opcode == Opcode.INVOKE_VIRTUAL || opcode == Opcode.INVOKE_INTERFACE) &&
                            methodReference?.returnType == "[B"
                }
                val bodyMoveIndex = bodyCallIndex + 1
                if (getInstruction(bodyMoveIndex).opcode != Opcode.MOVE_RESULT_OBJECT) {
                    throw PatchException("The body of the request is not stored right after it is read")
                }
                val bodyRegister = getInstruction<OneRegisterInstruction>(bodyMoveIndex).registerA

                // The URL: the first argument of CronetEngine.newUrlRequestBuilder(String, Callback, Executor).
                val builderIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            methodReference?.let { it.name == "newUrlRequestBuilder" && it.parameterTypes.size == 3 } == true
                }
                val urlRegister = getInstruction<FiveRegisterInstruction>(builderIndex).registerD

                if (putAllIndex > bodyCallIndex || bodyMoveIndex > builderIndex) {
                    throw PatchException("Unexpected order of the headers, the body and the request builder")
                }

                // The headers and the body must be in consecutive registers (invoke-static/range), the URL is copied
                // to the register after them. In 16.40.36 the method has 19 locals, v17 is first assigned further down.
                val scratchRegister = bodyRegister + 1
                val localRegisters = implementation!!.registerCount - (parameterTypes.size + 1)
                if (bodyRegister != headersRegister + 1 || scratchRegister >= localRegisters ||
                    scratchRegister == urlRegister || urlRegister > 15
                ) {
                    throw PatchException(
                        "Registers of the player request are not as expected: headers=$headersRegister body=$bodyRegister " +
                                "url=$urlRegister locals=$localRegisters"
                    )
                }

                addInstructions(
                    bodyMoveIndex + 1,
                    """
                        move-object/from16 v$scratchRegister, v$urlRegister
                        invoke-static/range { v$headersRegister .. v$scratchRegister }, $EXTENSION_CLASS_DESCRIPTOR->beginPlayerRequest(Ljava/util/Map;[BLjava/lang/String;)V
                        invoke-static/range { v$scratchRegister .. v$scratchRegister }, $EXTENSION_CLASS_DESCRIPTOR->overridePlayerRequestUrl(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$urlRegister
                        invoke-static/range { v$bodyRegister .. v$bodyRegister }, $EXTENSION_CLASS_DESCRIPTOR->overridePlayerRequestBody([B)[B
                        move-result-object v$bodyRegister
                    """,
                )
            }
        }

        // endregion

        // region Block /get_watch requests to fall back to /player requests.

        buildPlayerRequestUriMethodMatch.let { match ->
            val invokeToStringIndex = match[0]

            with(match.method) {
                val uriRegister = getInstruction<FiveRegisterInstruction>(invokeToStringIndex).registerC

                addInstructions(
                    invokeToStringIndex,
                    """
                        invoke-static { v$uriRegister }, $EXTENSION_CLASS_DESCRIPTOR->blockGetWatchRequest(Landroid/net/Uri;)Landroid/net/Uri;
                        move-result-object v$uriRegister
                    """,
                )
            }
        }

        // endregion

        // region Get the field references of the client info to be used below.
        // PORT-NOTE: the old patch hardcoded the obfuscated name of the client info class (Larua; in 16.40.36).
        // It is derived here: the client type field of the "10.29" method is a field of the client info class.

        val clientInfo = setPlayerRequestClientTypeMethodMatch.let { match ->
            with(match.method) {
                // The client type field is written by the last instruction of the matched pattern (iget -> iput).
                val clientTypeField = getInstruction(match[-1]).fieldReference
                    ?: throw PatchException("Could not find clientInfoClientTypeField")
                val clientInfoClass = clientTypeField.definingClass

                val versionStringIndex = indexOfFirstInstructionOrThrow { stringReference?.string == "10.29" }
                val clientVersionField = clientInfoFieldSetAfter(versionStringIndex, clientInfoClass)

                // Field in the player request context (container) that holds the client info object:
                // requestMessage.clientInfo = clientInfoBuilder.build();
                val clientInfoFieldIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IPUT_OBJECT && fieldReference?.type == clientInfoClass
                }
                val clientInfoField = getInstruction(clientInfoFieldIndex).fieldReference
                    ?: throw PatchException("Could not find clientInfoField")

                // Device fields: they are filled from android.os.Build by the method that builds the real client info.
                with(createPlayerRequestBodyWithModelMethodMatch.method) {
                    ClientInfoFields(
                        clientInfoField = clientInfoField,
                        clientTypeField = clientTypeField,
                        clientVersionField = clientVersionField,
                        clientModelField = clientInfoFieldSetAfter(
                            indexOfBuildFieldGet("Landroid/os/Build;", "MODEL"),
                            clientInfoClass,
                        ),
                        clientMakeField = clientInfoFieldSetAfter(
                            indexOfBuildFieldGet("Landroid/os/Build;", "MANUFACTURER"),
                            clientInfoClass,
                        ),
                        clientOsVersionField = clientInfoFieldSetAfter(
                            indexOfBuildFieldGet("Landroid/os/Build\$VERSION;", "RELEASE"),
                            clientInfoClass,
                        ),
                        clientSdkField = clientInfoFieldSetAfter(
                            indexOfBuildFieldGet("Landroid/os/Build\$VERSION;", "SDK_INT"),
                            clientInfoClass,
                        ),
                        // The OS name ("Android") is not read from android.os.Build, it comes from a field of the
                        // request builder. It is the client info field that is set right before Build.MANUFACTURER is
                        // read (SDK_INT -> osName -> MANUFACTURER -> BRAND -> MODEL).
                        clientOsNameField = run {
                            val manufacturerIndex = indexOfBuildFieldGet("Landroid/os/Build;", "MANUFACTURER")
                            val osNameSetIndex = indexOfFirstInstructionReversedOrThrow(manufacturerIndex - 1) {
                                opcode == Opcode.IPUT_OBJECT && fieldReference?.definingClass == clientInfoClass
                            }
                            getInstruction(osNameSetIndex).fieldReference
                                ?: throw PatchException("Could not find clientOsNameField")
                        },
                    )
                }
            }
        }

        val clientInfoField = clientInfo.clientInfoField
        val clientTypeField = clientInfo.clientTypeField
        val clientVersionField = clientInfo.clientVersionField
        val clientModelField = clientInfo.clientModelField
        val clientMakeField = clientInfo.clientMakeField
        val clientOsVersionField = clientInfo.clientOsVersionField
        val clientSdkField = clientInfo.clientSdkField
        val clientOsNameField = clientInfo.clientOsNameField

        // endregion

        // region Spoof the client info of /player requests.

        createPlayerRequestBodyMethodMatch.let { match ->
            val setClientInfoMethodName = "setClientInfo"
            val checkCastIndex = match[0]

            val requestContextRegister = match.method.getInstruction<OneRegisterInstruction>(checkCastIndex).registerA
            val requestContextClass = ((match.method.getInstruction(checkCastIndex) as ReferenceInstruction).reference as TypeReference).type
            val definingClass = match.classDef.type

            match.method.addInstruction(
                checkCastIndex + 1,
                "invoke-static { v$requestContextRegister }, $definingClass->$setClientInfoMethodName($requestContextClass)V",
            )

            // Change the client info to use the spoofed values.
            // Done in a helper method to avoid the need of picking out multiple free registers from the hooked code.
            match.classDef.methods.add(
                ImmutableMethod(
                    definingClass,
                    setClientInfoMethodName,
                    listOf(ImmutableMethodParameter(requestContextClass, null, "requestContext")),
                    "V",
                    AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
                    null,
                    null,
                    MutableMethodImplementation(3),
                ).toMutable().apply {
                    addInstructionsWithLabels(
                        0,
                        """
                            invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->isClientSpoofingEnabled()Z
                            move-result v0
                            if-eqz v0, :disabled

                            iget-object v0, p0, $clientInfoField
                            if-eqz v0, :disabled

                            # Client type.
                            iget v1, v0, $clientTypeField
                            invoke-static { v1 }, $EXTENSION_CLASS_DESCRIPTOR->getClientTypeId(I)I
                            move-result v1
                            iput v1, v0, $clientTypeField

                            # Client version.
                            iget-object v1, v0, $clientVersionField
                            invoke-static { v1 }, $EXTENSION_CLASS_DESCRIPTOR->getClientVersion(Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v1
                            iput-object v1, v0, $clientVersionField

                            # Device model.
                            iget-object v1, v0, $clientModelField
                            invoke-static { v1 }, $EXTENSION_CLASS_DESCRIPTOR->getClientModel(Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v1
                            iput-object v1, v0, $clientModelField

                            # Device make.
                            iget-object v1, v0, $clientMakeField
                            invoke-static { v1 }, $EXTENSION_CLASS_DESCRIPTOR->getClientDeviceMake(Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v1
                            iput-object v1, v0, $clientMakeField

                            # OS version.
                            iget-object v1, v0, $clientOsVersionField
                            invoke-static { v1 }, $EXTENSION_CLASS_DESCRIPTOR->getClientOsVersion(Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v1
                            iput-object v1, v0, $clientOsVersionField

                            # OS name (only the Apple clients change it).
                            iget-object v1, v0, $clientOsNameField
                            invoke-static { v1 }, $EXTENSION_CLASS_DESCRIPTOR->getClientOsName(Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v1
                            iput-object v1, v0, $clientOsNameField

                            # Android SDK version.
                            iget v1, v0, $clientSdkField
                            invoke-static { v1 }, $EXTENSION_CLASS_DESCRIPTOR->getClientAndroidSdkVersion(I)I
                            move-result v1
                            iput v1, v0, $clientSdkField

                            :disabled
                            return-void
                        """,
                    )
                },
            )
        }

        // endregion

        // region Check whether the video is Shorts or Clips (and prefetch the live stream renderer).

        playerParameterBuilderMethodMatch.method.apply {
            val videoIdRegister = 1
            val playerParameterRegister = 3

            addInstructions(
                0,
                """
                    invoke-static { p$videoIdRegister, p$playerParameterRegister }, $EXTENSION_CLASS_DESCRIPTOR->setPlayerResponseVideoId(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                    move-result-object p$playerParameterRegister
                """,
            )
        }

        // endregion

        // region Fix the player gesture (the player config of the spoofed player response has no gesture config).

        playerGestureConfigSyntheticMethodMatch.let { match ->
            val endIndex = match[-1]
            val downAndOutLandscapeAllowedIndex = endIndex - 3
            val downAndOutPortraitAllowedIndex = endIndex - 8

            arrayOf(
                downAndOutLandscapeAllowedIndex,
                downAndOutPortraitAllowedIndex,
            ).forEach { index ->
                // PlayerConfigModel.az()Z / aA()Z
                val gestureAllowedMethod = navigate(match.immutableMethod).to(index).stop()

                gestureAllowedMethod.apply {
                    val isAllowedIndex = implementation!!.instructions.size - 1
                    val isAllowed = getInstruction<OneRegisterInstruction>(isAllowedIndex).registerA

                    addInstructions(
                        isAllowedIndex,
                        """
                            invoke-static { v$isAllowed }, $EXTENSION_CLASS_DESCRIPTOR->enablePlayerGesture(Z)Z
                            move-result v$isAllowed
                        """,
                    )
                }
            }
        }

        // endregion

        // region Fix the playback speed menu item (missing from the player response of some clients).

        createPlaybackSpeedMenuItemMethodMatch.let { match ->
            val shouldCreateMenuIndex = match[-1]

            match.method.apply {
                val shouldCreateMenuRegister = getInstruction<OneRegisterInstruction>(shouldCreateMenuIndex).registerA

                addInstructions(
                    shouldCreateMenuIndex,
                    """
                        invoke-static { v$shouldCreateMenuRegister }, $EXTENSION_CLASS_DESCRIPTOR->forceCreatePlaybackSpeedMenu(Z)Z
                        move-result v$shouldCreateMenuRegister
                    """,
                )
            }
        }

        // endregion

        // region Append the spoofed client to the video format of Stats for nerds.

        nerdsStatsVideoFormatBuilderMethodMatch.method.apply {
            for (index in implementation!!.instructions.size - 1 downTo 0) {
                val instruction = getInstruction(index)
                if (instruction.opcode != Opcode.RETURN_OBJECT) continue

                val register = (instruction as OneRegisterInstruction).registerA

                addInstructions(
                    index,
                    """
                        invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->appendSpoofedClient(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$register
                    """,
                )
            }
        }

        // endregion

        // region Quality list: keep the H.264 ladder selectable, and log what the format selector received and kept.
        // Why: with some clients the quality row says "Unavailable" because the quality list has fewer than two entries.
        // p2..p5 of the selector are (formats, selector config, allowed itags, allowed itags); p1 and p3 of the list builder are
        // (formats, caps). The two log calls only read their arguments.

        // p2 is first replaced by the formats to use: when the response has a single VP9 quality but several H.264
        // qualities the VP9 formats are left out, so that the quality list is not reduced to one entry.
        formatSelectionMethodMatch.method.addInstructions(
            0,
            """
                invoke-static/range { p2 .. p2 }, $AVC_QUALITY_LADDER_CLASS_DESCRIPTOR->preferMultipleAvcQualities(Ljava/util/Collection;)Ljava/util/Collection;
                move-result-object p2
                invoke-static/range { p2 .. p5 }, $FORMAT_SELECTION_LOG_CLASS_DESCRIPTOR->logFormatSelection(Ljava/util/Collection;Ljava/lang/Object;Ljava/util/Set;Ljava/util/Set;)V
            """,
        )

        qualityListBuilderMethodMatch.method.addInstruction(
            0,
            "invoke-static { p1, p3 }, $FORMAT_SELECTION_LOG_CLASS_DESCRIPTOR->" +
                "logQualityListInput(Ljava/util/List;Ljava/lang/Object;)V",
        )

        // endregion
    }
}
