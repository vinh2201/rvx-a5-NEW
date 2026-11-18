package app.revanced.patches.youtube.misc.layoutupdates.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val CONFIG_PREFIX = "com.google.android.libraries.youtube.innertube."

// Old (rv6): cronetHeaderFingerprint (header "Accept-Encoding" of the Cronet request).
// PORT-NOTE: 16.40.36 does not send the layout configuration as HTTP headers (there is no X-Youtube-*-Config-* string
// at all). It is put into the request body (config info of the client context) by aatu.get() together with the hash data
// that is read from the app preferences. The hook is therefore placed there.
internal val BytecodePatchContext.requestConfigInfoMethodMatch by composingFirstMethod(
    CONFIG_PREFIX + "hot_hash_data",
    CONFIG_PREFIX + "cold_hash_data",
) {
    name("get")
    returnType("Ljava/lang/Object;")
    parameterTypes()
}

// Old (rv6): hotConfigPreferenceFingerprint + coldConfigPreferenceFingerprint (two methods in 17.34.36).
// 16.40.36: both configurations are loaded from the preferences in one method, aauq.run().
internal val BytecodePatchContext.configLoadMethodMatch by composingFirstMethod(
    CONFIG_PREFIX + "cold_config_group",
    CONFIG_PREFIX + "cold_stored_timestamp",
    CONFIG_PREFIX + "cold_hash_data",
    CONFIG_PREFIX + "hot_config_group",
    CONFIG_PREFIX + "hot_stored_timestamp",
    CONFIG_PREFIX + "hot_hash_data",
) {
    name("run")
    returnType("V")
    parameterTypes()
}

// Fix for the crash on every launch (16.40.36): the constructor of the client event type config (actm) reads the stored
// layout configuration and returns EARLY when it finds a client event type that this old app does not know
// (`Larwp;->a(I)` returns null). The field that is set at the very end of the constructor stays null and the app throws a
// NullPointerException the first time it is used. Matched pattern: move-result-object -> if-nez -> return-void.
internal val BytecodePatchContext.clientEventTypeConfigMethodMatch by composingFirstMethod {
    name("<init>")
    returnType("V")
    parameterTypes("L", "L")
    opcodes(
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IF_NEZ,
        Opcode.RETURN_VOID,
    )
    custom {
        // Two EnumSet.noneOf(...) calls build the two event type sets of the config.
        implementation?.instructions?.count {
            val reference = (it as? com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction)?.reference
            reference is MethodReference &&
                reference.definingClass == "Ljava/util/EnumSet;" &&
                reference.name == "noneOf"
        } == 2
    }
}
