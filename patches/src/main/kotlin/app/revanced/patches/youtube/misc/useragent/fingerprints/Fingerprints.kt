package app.revanced.patches.youtube.misc.useragent.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.Opcode

// Old: UserAgentHeaderBuilderFingerprint (wfw.ad(Context, String, String)String in 16.40.36;
// "(Linux; U; Android " is the full literal)
internal val BytecodePatchContext.userAgentHeaderBuilderMethodMatch by composingFirstMethod("(Linux; U; Android ") {
    parameterTypes("L", "L", "L")
    opcodes(
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
    )
}
