package app.revanced.patches.youtube.misc.swiperefresh.fingerprint

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

// Old: SwipeRefreshLayoutFingerprint (SwipeRefreshLayout.o()Z in 16.40.36)
internal val BytecodePatchContext.swipeRefreshLayoutMethodMatch by composingFirstMethod {
    definingClass("Landroidx/swiperefreshlayout/widget/SwipeRefreshLayout;")
    returnType("Z")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes()
    opcodes(
        Opcode.RETURN,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.RETURN,
    )
}
