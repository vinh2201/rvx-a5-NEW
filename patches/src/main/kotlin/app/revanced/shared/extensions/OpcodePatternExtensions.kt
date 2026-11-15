package app.revanced.shared.extensions

import app.revanced.patcher.patch.PatchException
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

/**
 * Finds the first contiguous occurrence of [pattern] inside the instructions of this method.
 *
 * PORT-NOTE: replaces the old `MethodFingerprint.resolve(context, method, classDef)` that scanned a single, already
 * known method (e.g. the pivot bar method) for an opcode pattern: `scanResult.patternScanResult.startIndex/endIndex`
 * are now [IntRange.first] and [IntRange.last].
 *
 * @return the indices of the first and last instruction of the pattern, or `null` when it is not found.
 */
internal fun Method.findOpcodePattern(vararg pattern: Opcode): IntRange? {
    val opcodes = implementation?.instructions?.map { it.opcode } ?: return null
    if (pattern.isEmpty() || opcodes.size < pattern.size) return null

    for (start in 0..opcodes.size - pattern.size) {
        if (pattern.indices.all { opcodes[start + it] == pattern[it] }) {
            return start until start + pattern.size
        }
    }

    return null
}

/**
 * Same as [findOpcodePattern], but throws a [PatchException] when the pattern is not found.
 *
 * @param name name used in the exception message.
 */
internal fun Method.findOpcodePatternOrThrow(name: String, vararg pattern: Opcode): IntRange =
    findOpcodePattern(*pattern) ?: throw PatchException("$name failed")

/**
 * @return `true` when a `const-string` of exactly [value] exists in this method
 * (old `MethodFingerprint(strings = listOf(value))` inside a class-scoped fingerprint).
 */
internal fun Method.containsStringLiteral(value: String): Boolean =
    implementation?.instructions?.any {
        val reference = (it as? ReferenceInstruction)?.reference
        reference is StringReference && reference.string == value
    } == true
