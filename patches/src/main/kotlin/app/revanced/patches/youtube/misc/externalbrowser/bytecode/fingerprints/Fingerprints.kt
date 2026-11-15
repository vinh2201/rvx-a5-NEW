package app.revanced.patches.youtube.misc.externalbrowser.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

// All three use the full literal "android.support.customtabs.action.CustomTabsService"
// (axga, ajjf and ajjm in 16.40.36)

// Old: ExternalBrowserPrimaryFingerprint (axga.w(Context)String in 16.40.36)
internal val BytecodePatchContext.externalBrowserPrimaryMethodMatch by composingFirstMethod(
    "android.support.customtabs.action.CustomTabsService"
) {
    returnType("L")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    opcodes(
        Opcode.CHECK_CAST,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.CONST_STRING,
    )
}

// Old: ExternalBrowserSecondaryFingerprint (ajjf.a(Laeu;)Object in 16.40.36)
internal val BytecodePatchContext.externalBrowserSecondaryMethodMatch by composingFirstMethod(
    "android.support.customtabs.action.CustomTabsService"
) {
    returnType("L")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    opcodes(
        Opcode.IPUT_OBJECT,
        Opcode.NEW_INSTANCE,
        Opcode.CONST_STRING,
    )
}

// Old: ExternalBrowserTertiaryFingerprint (ajjm.<init> in 16.40.36)
internal val BytecodePatchContext.externalBrowserTertiaryMethodMatch by composingFirstMethod(
    "android.support.customtabs.action.CustomTabsService"
) {
    returnType("V")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    opcodes(
        Opcode.CHECK_CAST,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.CONST_STRING,
    )
}
