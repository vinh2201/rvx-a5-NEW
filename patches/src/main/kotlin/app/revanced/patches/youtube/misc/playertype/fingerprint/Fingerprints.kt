package app.revanced.patches.youtube.misc.playertype.fingerprint

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

// Old: UpdatePlayerTypeFingerprint
internal val BytecodePatchContext.updatePlayerTypeMethodMatch by composingFirstMethod {
    definingClass("/YouTubePlayerOverlaysLayout;")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("L")
    opcodes(
        Opcode.IF_NE,
        Opcode.RETURN_VOID,
    )
}
