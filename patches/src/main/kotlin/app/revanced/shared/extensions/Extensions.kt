package app.revanced.shared.extensions

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableClassDef
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableField
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.toInstructions
import app.revanced.patcher.firstClassDefOrNull
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.PatchException
import app.revanced.shared.util.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.shared.util.integrations.Constants.PATCHES_PATH
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.util.MethodUtil
import org.w3c.dom.Node

internal fun MutableMethodImplementation.injectHideCall(
    index: Int,
    register: Int,
    patches: String,
    method: String
) {
    this.addInstruction(
        index,
        "invoke-static { v$register }, $PATCHES_PATH/$patches;->$method(Landroid/view/View;)V".toInstructions().first()
    )
}

internal fun MutableMethodImplementation.injectTheme(
    index: Int,
    register: Int,
    method: String
) {
    this.addInstruction(
        index,
        "invoke-static { v$register }, $INTEGRATIONS_PATH/utils/ThemeHelper;->$method(I)V".toInstructions().first()
    )
}

/**
 * Same as [MutableMethodImplementation.injectHideCall], but invoked directly on the [MutableMethod].
 */
internal fun MutableMethod.injectHideCall(
    index: Int,
    register: Int,
    patches: String,
    method: String
) {
    this.addInstruction(
        index,
        "invoke-static { v$register }, $PATCHES_PATH/$patches;->$method(Landroid/view/View;)V"
    )
}

/**
 * Same as [MutableMethodImplementation.injectTheme], but invoked directly on the [MutableMethod].
 */
internal fun MutableMethod.injectTheme(
    index: Int,
    register: Int,
    method: String
) {
    this.addInstruction(
        index,
        "invoke-static { v$register }, $INTEGRATIONS_PATH/utils/ThemeHelper;->$method(I)V"
    )
}

// PORT-NOTE: the old `MethodFingerprint.exception` extension was removed. In patcher v22 a method match
// throws a PatchException by itself when it cannot be resolved, so callers must not reference it anymore.

/**
 * Find the [MutableMethod] from a given [Method] in a [MutableClassDef].
 *
 * @param method The [Method] to find.
 * @return The [MutableMethod].
 */
fun MutableClassDef.findMutableMethodOf(method: Method) = this.methods.first {
    MethodUtil.methodSignaturesMatch(it, method)
}

/**
 * apply a transform to all methods of the class
 *
 * @param transform the transformation function. original method goes in, transformed method goes out
 */
fun MutableClassDef.transformMethods(transform: MutableMethod.() -> MutableMethod) {
    val transformedMethods = methods.map { it.transform() }
    methods.clear()
    methods.addAll(transformedMethods)
}

/**
 * apply a transform to all fields of the class
 *
 * @param transform the transformation function. original field goes in, transformed field goes out
 */
fun MutableClassDef.transformFields(transform: MutableField.() -> MutableField) {
    val transformedFields = fields.map { it.transform() }
    fields.clear()
    fields.addAll(transformedFields)
}

/**
 * traverse the class hierarchy starting from the given root class
 *
 * @param targetClass the class to start traversing the class hierarchy from
 * @param callback function that is called for every class in the hierarchy
 */
fun BytecodePatchContext.traverseClassHierarchy(targetClass: MutableClassDef, callback: MutableClassDef.() -> Unit) {
    callback(targetClass)

    val superclass = targetClass.superclass ?: return

    firstClassDefOrNull(superclass)?.let {
        traverseClassHierarchy(it, callback)
    }
}

internal fun Node.doRecursively(action: (Node) -> Unit) {
    action(this)
    for (i in 0 until this.childNodes.length) this.childNodes.item(i).doRecursively(action)
}

internal fun String.startsWithAny(vararg prefixes: String): Boolean {
    for (prefix in prefixes)
        if (this.startsWith(prefix))
            return true

    return false
}

internal fun toResult(errorIndex: Int) {
    if (errorIndex != -1)
        throw PatchException("Instruction not found: $errorIndex")
}
