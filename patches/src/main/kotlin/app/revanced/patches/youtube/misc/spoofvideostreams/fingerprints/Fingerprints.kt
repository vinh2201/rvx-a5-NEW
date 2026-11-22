package app.revanced.patches.youtube.misc.spoofvideostreams.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

// All fingerprints below were checked against the 16.40.36 smali (every one resolves to exactly one method):
//   buildInitPlaybackRequestMethodMatch          -> awa.o(Laub;)UrlRequest$Builder        (Cronet request builder)
//   volleyPlayerRequestMethodMatch               -> yyi.a()V                               (Volley request -> Cronet request: URL, headers, body)
//   buildPlayerRequestUriMethodMatch             -> aauz.t()String                         (player request URI)
//   setPlayerRequestClientTypeMethodMatch        -> aeqm.b(Lasel;Laepm;String)Lasej;       (client info of the "10.29" client)
//   createPlayerRequestBodyMethodMatch           -> aiqc.c()V                              (player request body)
//   createPlayerRequestBodyWithModelMethodMatch  -> aaua.c()Laoum;                         (builds the real client info)
//   nerdsStatsVideoFormatBuilderMethodMatch      -> ahhp.l(FormatStreamModel)String
//   playerGestureConfigSyntheticMethodMatch      -> olt.a(Object)V / ong.a(Object)V
//   createPlaybackSpeedMenuItemMethodMatch       -> aidh.c([Lawcz;)V
//   playerParameterBuilderMethodMatch            -> aiqa.b(...)Laiqc;
//   formatSelectionMethodMatch                   -> aein.c(PlayerConfigModel,Collection,...)Laeii;   (diagnostics)
//   qualityListBuilderMethodMatch                -> aein.n(List,String,Laeil)VideoQuality[]          (diagnostics)

private fun Method.referencesBuildField(definingClass: String, name: String): Boolean =
    implementation?.instructions?.any {
        val reference = (it as? ReferenceInstruction)?.reference
        reference is FieldReference && reference.definingClass == definingClass && reference.name == name
    } == true

// Old: BuildInitPlaybackRequestFingerprint (strings "Content-Type", "Range" are full literals).
// The matched pattern is `Uri.toString()` -> move-result-object (the URL string) -> iget-object.
internal val BytecodePatchContext.buildInitPlaybackRequestMethodMatch by composingFirstMethod(
    "Content-Type",
    "Range",
) {
    returnType("Lorg/chromium/net/UrlRequest\$Builder;")
    opcodes(
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET_OBJECT, // Moves the request URI string to a register to build the request with.
    )
}

// The Volley -> Cronet bridge of the app (yyi.a()V in 16.40.36). This is where the /player request of this app is turned
// into a network request: the URL string, the headers (HashMap, filled with the headers of the request) and the body
// (Request.getBody()) are all in registers here. The awa.o hook above only sees the requests of the media data source
// (streams), never the /player request. "If-None-Match" is only used by this method (checked against the smali).
internal val BytecodePatchContext.volleyPlayerRequestMethodMatch by composingFirstMethod(
    "If-None-Match",
    "If-Modified-Since",
) {
    returnType("V")
    parameterTypes()
}

// Old: BuildPlayerRequestURIFingerprint ("key" and "asig" are full literals).
internal val BytecodePatchContext.buildPlayerRequestUriMethodMatch by composingFirstMethod(
    "key",
    "asig",
) {
    returnType("Ljava/lang/String;")
    opcodes(
        Opcode.INVOKE_VIRTUAL, // Register holds player request URI.
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.RETURN_OBJECT,
    )
}

// Old: SetPlayerRequestClientTypeFingerprint ("10.29" is a full literal).
internal val BytecodePatchContext.setPlayerRequestClientTypeMethodMatch by composingFirstMethod("10.29") {
    opcodes(
        Opcode.IGET,
        Opcode.IPUT, // Sets ClientInfo.clientId.
    )
}

// Old: CreatePlayerRequestBodyFingerprint ("ms" is a full literal).
internal val BytecodePatchContext.createPlayerRequestBodyMethodMatch by composingFirstMethod("ms") {
    returnType("V")
    parameterTypes()
    opcodes(
        Opcode.CHECK_CAST,
        Opcode.IGET,
        Opcode.AND_INT_LIT16,
    )
}

// Old: CreatePlayerRequestBodyWithModelFingerprint.
// PORT-NOTE: the old fingerprint (public final, no parameters, returns an object, contains OR_INT_LIT16 and reads
// Build.MODEL + Build.VERSION.RELEASE) also matches ludh.b()Laoqz; in 16.40.36, a device/attestation proto builder. The old
// resolver silently used whichever was found first. The real target (aaua.c) is the only one that also reads Build.BRAND
// and Build.MANUFACTURER, so those are required here.
internal val BytecodePatchContext.createPlayerRequestBodyWithModelMethodMatch by composingFirstMethod {
    returnType("L")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes()
    opcodes(Opcode.OR_INT_LIT16)
    custom {
        referencesBuildField("Landroid/os/Build;", "MODEL") &&
                referencesBuildField("Landroid/os/Build;", "BRAND") &&
                referencesBuildField("Landroid/os/Build;", "MANUFACTURER") &&
                referencesBuildField("Landroid/os/Build\$VERSION;", "RELEASE") &&
                referencesBuildField("Landroid/os/Build\$VERSION;", "SDK_INT")
    }
}

// Old: NerdsStatsVideoFormatBuilderFingerprint (`codecs="` is a full literal).
internal val BytecodePatchContext.nerdsStatsVideoFormatBuilderMethodMatch by composingFirstMethod("codecs=\"") {
    returnType("Ljava/lang/String;")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    parameterTypes("Lcom/google/android/libraries/youtube/innertube/model/media/FormatStreamModel;")
}

// Old: PlayerGestureConfigSyntheticFingerprint.
// PORT-NOTE: in 16.40.36 two classes match (olt = DownAndOutController, ong = UpForFullController). Both read the same two
// PlayerConfigModel getters (az()Z / aA()Z) and the patch hooks those getters (the callee), so any one of them is enough.
internal val BytecodePatchContext.playerGestureConfigSyntheticMethodMatch by composingFirstMethod {
    name("a")
    returnType("V")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes("Ljava/lang/Object;")
    opcodes(
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL, // playerGestureConfig.downAndOutLandscapeAllowed
        Opcode.MOVE_RESULT,
        Opcode.IPUT_BOOLEAN,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL, // playerGestureConfig.downAndOutPortraitAllowed
        Opcode.MOVE_RESULT,
        Opcode.IPUT_BOOLEAN,
        Opcode.RETURN_VOID,
    )
    custom {
        // This kind of class always has a single method (+ 1 constructor).
        immutableClassDef.methods.count() == 2 &&
                implementation?.instructions?.any {
                    val reference = (it as? ReferenceInstruction)?.reference
                    reference is MethodReference &&
                            reference.definingClass == "Lcom/google/android/libraries/youtube/innertube/model/media/PlayerConfigModel;" &&
                            reference.parameterTypes.isEmpty() &&
                            reference.returnType == "Z"
                } == true
    }
}

// Old: CreatePlaybackSpeedMenuItemFingerprint.
internal val BytecodePatchContext.createPlaybackSpeedMenuItemMethodMatch by composingFirstMethod {
    returnType("V")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes("[L")
    opcodes(
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT, // Return value controls the creation of the playback speed menu item.
        Opcode.IF_EQZ, // If the return value is false, the playback speed menu item is not created.
    )
}

// Old: PlayerParameterBuilderFingerprint (misc/protobufpoof), aiqa.b(...) in 16.40.36.
internal val BytecodePatchContext.playerParameterBuilderMethodMatch by composingFirstMethod {
    name("b")
    returnType("L")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes(
        "Ljava/lang/String;",
        "[B",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "I",
        "I",
        "Ljava/util/Set;",
        "Ljava/lang/String;",
        "L",
        "Z",
    )
    opcodes(
        Opcode.NEW_INSTANCE,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_DIRECT,
        Opcode.INVOKE_VIRTUAL,
    )
}

// region Diagnostics of the quality list (FormatSelectionLog; no effect on playback)

// The format selector of the app: it receives the formats of the player response and the player config and decides which
// formats are usable. Public final, 10 parameters, the first one is the PlayerConfigModel.
internal val BytecodePatchContext.formatSelectionMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("L")
    parameterTypes(
        "Lcom/google/android/libraries/youtube/innertube/model/media/PlayerConfigModel;",
        "Ljava/util/Collection;",
        "L",
        "Ljava/util/Set;",
        "Ljava/util/Set;",
        "I",
        "I",
        "Ljava/lang/String;",
        "L",
        "L",
    )
}

// The method that turns the usable formats into the quality list (VideoQuality[]; one entry per quality label).
internal val BytecodePatchContext.qualityListBuilderMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PRIVATE, AccessFlags.FINAL)
    returnType("[Lcom/google/android/libraries/youtube/innertube/model/media/VideoQuality;")
    parameterTypes("Ljava/util/List;", "Ljava/lang/String;", "L")
}

// endregion
