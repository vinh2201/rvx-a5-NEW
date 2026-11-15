package app.revanced.patches.youtube.misc.videoid.mainstream.fingerprint

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef

// Old: MainstreamVideoIdFingerprint
internal val BytecodePatchContext.mainstreamVideoIdMethodMatch by composingFirstMethod {
    definingClass("/SubtitlesOverlayPresenter;")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("L")
    opcodes(
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
    )
}

// Old: PlayerControllerFingerprint (matches a method of the runtime extension, which is merged before the patches run)
internal val BytecodePatchContext.playerControllerMethodMatch by composingFirstMethod {
    definingClass("Lapp/revanced/integrations/sponsorblock/PlayerController;")
    name("setSponsorBarRect")
    // The method contains the placeholder const-string that is replaced by the patch.
    instructions(Opcode.CONST_STRING())
}

// Old: PlayerControllerSetTimeReferenceFingerprint
internal val BytecodePatchContext.playerControllerSetTimeReferenceMethodMatch by composingFirstMethod(
    "Media progress reported outside media playback: "
) {
    opcodes(
        Opcode.INVOKE_DIRECT_RANGE,
        Opcode.IGET_OBJECT,
    )
}

// Old: PlayerInitFingerprint
internal val BytecodePatchContext.playerInitMethodMatch by composingFirstMethod {
    instructions("playVideo called on player response with no videoStreamingData."())
}

// Old: RepeatListenerFingerprint
internal val BytecodePatchContext.repeatListenerMethodMatch by composingFirstMethod("ppoobsa") {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Z")
    opcodes(
        Opcode.INVOKE_VIRTUAL_RANGE,
        Opcode.IF_EQZ,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.CONST_4,
        Opcode.IPUT_BOOLEAN,
        Opcode.GOTO,
    )
}

/**
 * Old: SeekFingerprint.
 * Matches using the class found in [playerInitMethodMatch] (old code resolved it against that class).
 */
internal val ClassDef.seekMethodMatch by ClassDefComposing.composingFirstMethod {
    instructions("Attempting to seek during an ad"())
}

/**
 * Old: VideoTimeFingerprint.
 * Matches using the class found in [videoTimeParentMethodMatch] (old code resolved it against that class).
 */
internal val ClassDef.videoTimeMethodMatch by ClassDefComposing.composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    returnType("V")
    parameterTypes("J", "J", "J", "J", "I", "L")
    opcodes(
        Opcode.INVOKE_DIRECT,
        Opcode.IPUT_WIDE,
        Opcode.IPUT_WIDE,
        Opcode.IPUT_WIDE,
        Opcode.IPUT_WIDE,
    )
}

// Old: VideoTimeParentFingerprint
internal val BytecodePatchContext.videoTimeParentMethodMatch by composingFirstMethod {
    instructions("MedialibPlayerTimeInfo{currentPositionMillis="())
}
