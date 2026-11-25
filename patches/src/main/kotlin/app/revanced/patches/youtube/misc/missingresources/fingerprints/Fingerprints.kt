package app.revanced.patches.youtube.misc.missingresources.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
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

// Old (rv6 / community): legacyIconEnumConverterFingerprint (hardcoded class names Lajft; / Laqca;).
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

// region Top bar buttons (subscriptions button on the Subscriptions tab, settings button on the You tab)
//
// The old 16.40.36 UI does not show the "all subscriptions" and "settings" buttons that newer layouts have in the top bar.
// The extension (TopBarButtonsPatch) adds them to the toolbar menu; it needs to know the selected tab of the bottom bar.

// Bottom bar controller (16.40.36: owc.pu(int previousIndex, int index, boolean reselected)): a tab of the bottom bar got
// selected. The id of the tab ("FEsubscriptions", "FElibrary", ...) is read from the Optional of the tab and cast to String.
internal val BytecodePatchContext.pivotTabSelectedMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("I", "I", "Z")
    opcodes(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL, // q(index): Optional of the tab
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL, // Optional.flatMap(): tab id
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CONST_STRING, // ""
        Opcode.INVOKE_VIRTUAL, // Optional.orElse("")
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST, // String
        Opcode.INVOKE_VIRTUAL,
    )
}

// Top bar controller (16.40.36: mhe.b(config, ...)): the top bar gets the configuration of a page. The first parameter is the
// configuration (title, custom view, buttons, colors and an int: 0 = no navigation icon = first page of a tab of the bottom
// bar, 1 = back arrow = a page that was opened from another page). The first parameter is a class of the app (any "L").
internal val BytecodePatchContext.actionBarConfigMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes(
        "L",
        "Lcom/google/android/apps/youtube/app/common/ui/actionbar/ActionBarColor;",
        "I",
        "Lcom/google/android/apps/youtube/app/common/ui/actionbar/ActionBarColor;",
        "I",
        "Lcom/google/android/apps/youtube/app/common/ui/actionbar/ActionBarColor;",
        "Z",
    )
}

// Controller of the "Settings" entry of the overflow menu (16.40.36: luw.m()): opens the settings of the app (it adds the account
// and starts the real settings activity without any intermediate window). It is the click action of the entry; the method
// has no parameter and always returns true. The gear button of the extension calls it.
internal val BytecodePatchContext.settingsMenuItemClickMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Z")
    parameterTypes()
    opcodes(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL, // Intent of the settings
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_INTERFACE, // account
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL, // account -> future
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.SGET_OBJECT,
        Opcode.NEW_INSTANCE, // callback that starts the settings
        Opcode.INVOKE_DIRECT,
        Opcode.INVOKE_STATIC,
        Opcode.CONST_4,
        Opcode.RETURN,
    )
}

// Top bar (16.40.36: fpm.l(Menu, MenuInflater, ?, SparseArray, int iconColor)): fills the menu of the toolbar with the buttons
// the server sent (bell, search, cast ...) and tints their icons. The buttons of the extension are added at its end.
internal val BytecodePatchContext.toolbarMenuMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returnType("V")
    parameterTypes("Landroid/view/Menu;", "Landroid/view/MenuInflater;", "L", "Landroid/util/SparseArray;", "I")
}

// endregion

// region Player menu (the "three dots" menu of the player)
//
// With a spoofed 20.x client the server sends the settings entries of the menu (quality, speed, captions, audio track, additional
// settings) as rows of its own, whose commands this old app does not know (a tap only closes the menu), and the app adds its own
// native rows below them: the same entries twice. The patch only needs the two classes of the menu (the app has two variants, which
// one is used depends on a server flag); the method that builds the adapter is found inside them from the two List fields.
// The class is located here through its click handler, the only method that calls the controller of the menu.

private const val PLAYER_OVERFLOW_CONTROLLER = "Lcom/google/android/apps/youtube/app/watch/bottomsheet/PlayerOverflowBottomSheetController;"

/** True when the method calls a method of the controller of the player menu (only the two classes of the menu do). */
private fun com.android.tools.smali.dexlib2.iface.Method.callsPlayerOverflowController(): Boolean =
    implementation?.instructions?.any {
        val reference = (it as? ReferenceInstruction)?.reference as? MethodReference
        reference != null && reference.definingClass == PLAYER_OVERFLOW_CONTROLLER
    } == true

// Menu variant 1 (16.40.36: class nwi, found by its onItemClick): the row comes from the adapter of the list (iget adapter, check-cast, get row).
internal val BytecodePatchContext.playerMenuSheet1MethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Landroid/widget/AdapterView;", "Landroid/view/View;", "I", "J")
    opcodes(
        Opcode.IGET_OBJECT, // list adapter
        Opcode.CHECK_CAST,
        Opcode.INVOKE_VIRTUAL, // row at the position
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INSTANCE_OF,
    )
    custom { callsPlayerOverflowController() }
}

// Menu variant 2 (16.40.36: class nwl, found by its onItemClick): the row comes from the ListView (iget list view, getAdapter, getItem).
internal val BytecodePatchContext.playerMenuSheet2MethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("V")
    parameterTypes("Landroid/widget/AdapterView;", "Landroid/view/View;", "I", "J")
    opcodes(
        Opcode.IGET_OBJECT, // list view
        Opcode.IF_NEZ,
        Opcode.RETURN_VOID,
        Opcode.INVOKE_VIRTUAL, // getAdapter
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_INTERFACE, // getItem
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.INSTANCE_OF,
    )
    custom { callsPlayerOverflowController() }
}

// endregion

// region Default color of the Litho text (dark theme)
//
// The text component of the feed (16.40.36: dyv, text spec stw) keeps a default color list that is always black (dzi.a) and
// paints everything that has no color of its own with it. Three static methods of the same helper class (16.40.36: dzi) take that
// color list together with the component context (the first parameter, 16.40.36: dmn, whose field b is the Android context of the view):
// the one that binds the text to the drawable at mount time, and the two that measure it (the second one is the variant with the
// sizes of the parent). The extension (MissingResourcesPatch.getDarkTextColor) swaps the color list on the dark theme.

// 16.40.36: dzi.f(Ldmn;Ldyz;ColorStateList;ZLssw;CharSequence;Layout;Float;ClickableSpan[];ImageSpan[])V
internal val BytecodePatchContext.lithoTextBindMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.STATIC)
    returnType("V")
    parameterTypes(
        "L", "L", "Landroid/content/res/ColorStateList;", "Z", "L", "Ljava/lang/CharSequence;", "Landroid/text/Layout;", "Ljava/lang/Float;",
        "[Landroid/text/style/ClickableSpan;", "[Landroid/text/style/ImageSpan;",
    )
}

// 16.40.36: dzi.h(Ldmn;Ldmr;CharSequence;TruncateAt;ZIFFFI ColorStateList;IIFII Typeface;II CharSequence;ZZ Layout;Layout;Integer;Integer; 5 x Lbaxk;)V
internal val BytecodePatchContext.lithoTextMeasureMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.STATIC)
    returnType("V")
    parameterTypes(
        "L", "L", "Ljava/lang/CharSequence;", "Landroid/text/TextUtils\$TruncateAt;", "Z", "I", "F", "F", "F", "I", "Landroid/content/res/ColorStateList;", "I", "I", "F", "I", "I",
        "Landroid/graphics/Typeface;", "I", "I", "Ljava/lang/CharSequence;", "Z", "Z", "Landroid/text/Layout;", "Landroid/text/Layout;", "Ljava/lang/Integer;", "Ljava/lang/Integer;",
        "L", "L", "L", "L", "L",
    )
}

// 16.40.36: dzi.i(Ldmn;Ldmr;IILdqx;CharSequence;TruncateAt;ZIFFFI ColorStateList;II CharSequence;FI Typeface;II 5 x Lbaxk;)V
internal val BytecodePatchContext.lithoTextMeasureWithSizeMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.STATIC)
    returnType("V")
    parameterTypes(
        "L", "L", "I", "I", "L", "Ljava/lang/CharSequence;", "Landroid/text/TextUtils\$TruncateAt;", "Z", "I", "F", "F", "F", "I", "Landroid/content/res/ColorStateList;", "I", "I",
        "Ljava/lang/CharSequence;", "F", "I", "Landroid/graphics/Typeface;", "I", "I", "L", "L", "L", "L", "L",
    )
}

// endregion

// region Resource name -> id (white icons on the dark theme)

/** True when the method asks the resources for an id by name (Resources.getIdentifier). */
private fun com.android.tools.smali.dexlib2.iface.Method.callsGetIdentifier(): Boolean =
    implementation?.instructions?.any { instruction ->
        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
        reference != null && reference.definingClass == "Landroid/content/res/Resources;" && reference.name == "getIdentifier"
    } ?: false

// 16.40.36: syu.a(Context, String type, String name)I (protected static): cached Resources.getIdentifier(name, type, package).
// The images of the elements (feed, Shorts player) are referred to by name through it (syu.b / syu.c, called by the image loader ryv).
internal val BytecodePatchContext.resourceByNameMethodMatch by composingFirstMethod {
    accessFlags(AccessFlags.PROTECTED, AccessFlags.STATIC)
    returnType("I")
    parameterTypes("Landroid/content/Context;", "Ljava/lang/String;", "Ljava/lang/String;")
    custom { callsGetIdentifier() }
}

// endregion
