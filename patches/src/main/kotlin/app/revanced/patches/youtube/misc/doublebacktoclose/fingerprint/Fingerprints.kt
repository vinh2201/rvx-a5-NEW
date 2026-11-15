package app.revanced.patches.youtube.misc.doublebacktoclose.fingerprint

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef

// Old: OnBackPressedFingerprint (WatchWhileActivity.onBackPressed()V in 16.40.36)
internal val BytecodePatchContext.onBackPressedMethodMatch by composingFirstMethod {
    definingClass("/WatchWhileActivity;")
    name("onBackPressed")
    opcodes(Opcode.RETURN_VOID)
}

// Old: ScrollPositionFingerprint (akda.H(Bundle)V in 16.40.36; "scroll_position" is a full literal)
internal val BytecodePatchContext.scrollPositionMethodMatch by composingFirstMethod("scroll_position") {
    returnType("V")
    accessFlags(AccessFlags.PROTECTED, AccessFlags.FINAL)
    parameterTypes("L")
    opcodes(
        Opcode.IF_NEZ,
        Opcode.INVOKE_DIRECT,
        Opcode.RETURN_VOID,
    )
}

// Old: ScrollTopParentFingerprint (mgk.<init>(axxo, AppTabsBar, ConstraintLayout, ViewGroup) in 16.40.36)
internal val BytecodePatchContext.scrollTopParentMethodMatch by composingFirstMethod {
    returnType("V")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    parameterTypes("L", "L", "L", "L")
    name("<init>")
    opcodes(
        Opcode.INVOKE_DIRECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.CONST_16,
        Opcode.INVOKE_VIRTUAL,
        Opcode.NEW_INSTANCE,
    )
}

// Old: ScrollTopFingerprint (was resolved inside the class of the parent; mgk.h()V in 16.40.36:
// RecyclerView.scrollToPosition(0))
internal val ClassDef.scrollTopMethodMatch by ClassDefComposing.composingFirstMethod {
    returnType("V")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes()
    opcodes(
        Opcode.CHECK_CAST,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.GOTO,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
    )
}
