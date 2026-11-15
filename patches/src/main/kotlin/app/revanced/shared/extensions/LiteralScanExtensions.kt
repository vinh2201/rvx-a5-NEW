package app.revanced.shared.extensions

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.wideLiteral
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.PatchException
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

/**
 * A literal instruction (usually `const` of a resource id) found while scanning all classes.
 *
 * @param index index of the literal instruction inside [method] (index of the *original*, not yet mutated method).
 * @param register destination register of the literal instruction.
 */
internal data class LiteralHit(
    val classDef: ClassDef,
    val method: Method,
    val index: Int,
    val register: Int
)

/**
 * Read-only pass over all classes, collecting every [opcode] instruction whose literal equals [literal].
 *
 * PORT-NOTE: the old patches mutated classes while iterating over them (`context.proxy(classDef)`).
 * With patcher v22 the hits are collected first and applied afterwards, see [mutableMethodOf].
 */
internal fun BytecodePatchContext.findLiteralHits(
    literal: Long,
    opcode: Opcode = Opcode.CONST
): List<LiteralHit> {
    val hits = mutableListOf<LiteralHit>()

    classDefs.toList().forEach { classDef ->
        classDef.methods.forEach { method ->
            val implementation = method.implementation ?: return@forEach

            implementation.instructions.forEachIndexed { index, instruction ->
                if (instruction.opcode != opcode) return@forEachIndexed
                if (instruction.wideLiteral != literal) return@forEachIndexed

                hits.add(LiteralHit(classDef, method, index, (instruction as OneRegisterInstruction).registerA))
            }
        }
    }

    return hits
}

/**
 * @return the mutable version of the method of a [LiteralHit].
 */
internal fun BytecodePatchContext.mutableMethodOf(hit: LiteralHit): MutableMethod =
    classDefs.getOrReplaceMutable(hit.classDef).findMutableMethodOf(hit.method)

/**
 * Applies [action] to every hit, from the highest to the lowest index, so that instructions inserted for one hit
 * never shift the index of a hit that is still to be processed in the same method.
 *
 * @param name name used in the exception message when nothing was applied (old `toResult(patchSuccessArray.indexOf(false))`).
 * @param action receives the hit and returns `true` when it was applied.
 */
internal fun BytecodePatchContext.applyToLiteralHits(
    hits: List<LiteralHit>,
    name: String,
    action: (hit: LiteralHit, method: MutableMethod) -> Boolean
) {
    var applied = false

    hits.sortedByDescending { it.index }.forEach { hit ->
        if (action(hit, mutableMethodOf(hit))) applied = true
    }

    if (!applied) throw PatchException("Instruction not found: $name")
}
