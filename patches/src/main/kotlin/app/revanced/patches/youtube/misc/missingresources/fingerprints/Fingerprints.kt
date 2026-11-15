package app.revanced.patches.youtube.misc.missingresources.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val DRAWABLE = "Landroid/graphics/drawable/Drawable;"

/** True when the method body is exactly "invoke-virtual getDrawable; move-result-object; return-object". */
private fun com.android.tools.smali.dexlib2.iface.Method.isGetDrawableWrapperOf(definingClass: String): Boolean {
    val instructions = implementation?.instructions?.toList() ?: return false
    if (instructions.size != 3) return false

    val reference = (instructions[0] as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    return reference.definingClass == definingClass && reference.name == "getDrawable"
}

// Old (rv6): navigationBarGetDrawableFingerprint.
// 16.40.36: aik.a(Context, int) = ContextCompat.getDrawable() -> Context.getDrawable(int) (3 instructions).
// Called with resource id 0 when the server sends an icon type that the old icon table does not know.
// PORT-NOTE: the rv6 fingerprint only checked "name == a"; the body check is needed here because
// wfw.V(Context, int) has the same signature.
internal val BytecodePatchContext.contextGetDrawableMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returnType(DRAWABLE)
    parameterTypes("Landroid/content/Context;", "I")
    custom { isGetDrawableWrapperOf("Landroid/content/Context;") }
}

// New: fj.f(Resources, int, Theme) = ResourcesCompat.getDrawable() -> Resources.getDrawable(int, Theme).
// Same crash as above for callers that go through Resources instead of Context.
internal val BytecodePatchContext.resourcesGetDrawableMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returnType(DRAWABLE)
    parameterTypes("Landroid/content/res/Resources;", "I", "Landroid/content/res/Resources\$Theme;")
    custom { isGetDrawableWrapperOf("Landroid/content/res/Resources;") }
}

// Old (rv6): attributeResolverFingerprint (general/toolbar). 16.40.36: wfw.V(Context, int attr).
// Throws UnsupportedOperationException / IllegalArgumentException when the attribute id is 0 (missing toolbar attribute).
internal val BytecodePatchContext.attributeResolverMethodMatch by composingFirstMethod(
    "Type of attribute is not a reference to a drawable (attr = %d, value = %s)",
) {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returnType(DRAWABLE)
    parameterTypes("Landroid/content/Context;", "I")
}

// Old (rv6/morphe): legacyIconEnumConverterFingerprint (hardcoded class names Lajft; / Laqca;).
// The icon type enum (16.40.36: armu, proto number -> enum, 845 constants). Found by its enum constant names
// (all three are full literals of the enum's static constructor, "TAB_ACTIVITY" is unique to this class).
internal val BytecodePatchContext.iconTypeEnumMethodMatch by composingFirstMethod("TAB_ACTIVITY", "SEARCH", "NOTIFICATIONS") {
    name("<clinit>")
}

// armu.b(int): number -> enum. Returns null for unknown numbers (e.g. the new Cairo icon types of 20.x clients);
// all callers then fall back to UNKNOWN, whose drawable id is 0 -> getDrawable(0) -> crash / wrong icon.
internal val ClassDef.iconTypeFromNumberMethodMatch by ClassDefComposing.composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    parameterTypes("I")
    custom { returnType == definingClass }
}
