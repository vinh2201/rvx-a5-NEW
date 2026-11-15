package app.revanced.patches.youtube.extended.forcevp9.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef

// Old: Vp9PrimaryFingerprint (was resolved inside the class of LayoutSwitchFingerprint; yrb.ac(I)Z in 16.40.36)
internal val ClassDef.vp9PrimaryMethodMatch by ClassDefComposing.composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returnType("Z")
    parameterTypes("I")
    opcodes(
        Opcode.RETURN,
        Opcode.RETURN,
    )
}

// Old: Vp9SecondaryFingerprint (same class; yrb.W(Context, I)Z in 16.40.36)
internal val ClassDef.vp9SecondaryMethodMatch by ClassDefComposing.composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returnType("Z")
    parameterTypes("L", "I")
    opcodes(
        Opcode.RETURN,
        Opcode.CONST_4,
        Opcode.RETURN,
    )
}

// Old: Vp9PropsParentFingerprint ("Android Wear" is a full literal in the constructor of aaua in 16.40.36)
internal val BytecodePatchContext.vp9PropsParentMethodMatch by composingFirstMethod("Android Wear") {
    returnType("V")
}

// Old: Vp9PropsFingerprint (was resolved inside the class of Vp9PropsParentFingerprint; aaua.c() in 16.40.36)
internal val ClassDef.vp9PropsMethodMatch by ClassDefComposing.composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("L")
    parameterTypes()
    opcodes(
        Opcode.OR_INT_LIT16,
    )
}
