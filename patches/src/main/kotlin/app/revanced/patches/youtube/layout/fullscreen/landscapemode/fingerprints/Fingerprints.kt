package app.revanced.patches.youtube.layout.fullscreen.landscapemode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef

// Old: OrientationParentFingerprint (constructor of the obfuscated class `ovi` in 16.40.36;
// `ovi` keeps an Activity and a "isTablet"-like boolean that is also refreshed in a(Configuration)V)
internal val BytecodePatchContext.orientationParentMethodMatch by composingFirstMethod {
    definingClass("Lovi;")
    name("<init>")
    returnType("V")
}

// Old: OrientationPrimaryFingerprint (was resolved inside the class of the parent: ovi.<init>(Activity))
internal val ClassDef.orientationPrimaryMethodMatch by ClassDefComposing.composingFirstMethod {
    name("<init>")
    returnType("V")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    parameterTypes("L")
    opcodes(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
    )
}

// Old: OrientationSecondaryFingerprint (same class: ovi.a(Configuration)V)
internal val ClassDef.orientationSecondaryMethodMatch by ClassDefComposing.composingFirstMethod {
    returnType("V")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes("L")
    opcodes(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
    )
}
