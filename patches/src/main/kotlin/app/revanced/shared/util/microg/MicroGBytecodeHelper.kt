package app.revanced.shared.util.microg

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.CompositeMatch
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.PatchException
import app.revanced.shared.extensions.findMutableMethodOf
import app.revanced.shared.util.microg.Constants.ACTIONS
import app.revanced.shared.util.microg.Constants.AUTHORITIES
import app.revanced.shared.util.microg.Constants.MICROG_VENDOR
import app.revanced.shared.util.microg.Constants.PERMISSIONS
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference

/**
 * Helper class for applying bytecode patches needed for the microg-support patches.
 *
 * PORT-NOTE: the old helper received `MethodFingerprint`s and read `fingerprint.result`.
 * In patcher v22 a fingerprint is the [CompositeMatch] of an `xMethodMatch` property, so the helper receives
 * the already resolved matches instead.
 */
internal object MicroGBytecodeHelper {
    /**
     * Hook a method to check the availability of MicroG.
     *
     * @param match The match of the method to add the call for the notice in.
     */
    fun injectNotice(match: CompositeMatch) {
        match.method.addInstruction(
            0,
            "invoke-static {}, Lapp/revanced/integrations/patches/MicroGSupport;->checkAvailability()V"
        )
    }

    /**
     * Transform strings with package name out of [fromPackageName] and [toPackageName].
     *
     * @param fromPackageName Original package name.
     * @param toPackageName The package name to accept.
     **/
    fun packageNameTransform(fromPackageName: String, toPackageName: String): (String) -> String? {
        return { referencedString ->
            when (referencedString) {
                "$fromPackageName.SuggestionsProvider",
                "$fromPackageName.fileprovider" -> referencedString.replace(fromPackageName, toPackageName)

                else -> null
            }
        }
    }

    /**
     * Prime method data class for the [MicroGBytecodeHelper] class.
     *
     * @param primeMethodMatch The prime methods [CompositeMatch].
     * @param fromPackageName Original package name.
     * @param toPackageName The package name to accept.
     **/
    data class PrimeMethodTransformationData(
        val primeMethodMatch: CompositeMatch,
        val fromPackageName: String,
        val toPackageName: String
    ) {
        /**
         * Patch the prime method to accept the new package name.
         */
        fun transformPrimeMethodPackageName() {
            val primeMethod = primeMethodMatch.method
            val implementation = primeMethod.implementation!!

            var register = 2

            val index = implementation.instructions.indexOfFirst {
                if (it.opcode != Opcode.CONST_STRING) return@indexOfFirst false

                val instructionString = ((it as ReferenceInstruction).reference as StringReference).string
                if (instructionString != fromPackageName) return@indexOfFirst false

                register = (it as OneRegisterInstruction).registerA
                return@indexOfFirst true
            }

            if (index < 0) throw PatchException("Prime method does not contain the string $fromPackageName")

            primeMethod.replaceInstruction(
                index, "const-string v$register, \"$toPackageName\""
            )
        }
    }

    /**
     * Patch the bytecode to work with MicroG.
     * Note: this only handles string constants to gms (intent actions, authorities, ...).
     * If the app employs additional checks to validate the installed gms package, you'll have to handle those in the app- specific patch
     *
     * @param context The context.
     * @param additionalStringTransforms Additional transformations applied to all const-string references.
     * @param primeMethodTransformationData Data to patch the prime method.
     * @param earlyReturns List of [CompositeMatch] to return the matched methods early.
     */
    fun patchBytecode(
        context: BytecodePatchContext,
        additionalStringTransforms: Array<(str: String) -> String?>,
        primeMethodTransformationData: PrimeMethodTransformationData,
        earlyReturns: List<CompositeMatch>
    ) {
        earlyReturns.returnEarly()
        primeMethodTransformationData.transformPrimeMethodPackageName()

        val allTransforms = arrayOf(
            MicroGBytecodeHelper::commonTransform,
            MicroGBytecodeHelper::contentUrisTransform,
            *additionalStringTransforms
        )

        // transform all strings using all provided transforms, first match wins
        context.transformStringReferences transform@{
            for (transformFn in allTransforms) {
                val s = transformFn(it)
                if (s != null) return@transform s
            }

            return@transform null
        }
    }

    /**
     * const-string transform function for common gms string references.
     *
     * @param referencedString The string to transform.
     */
    private fun commonTransform(referencedString: String): String? =
        when (referencedString) {
            "com.google",
            "com.google.android.gms",
            in PERMISSIONS,
            in ACTIONS,
            in AUTHORITIES -> referencedString.replace("com.google", MICROG_VENDOR)

            // subscribedfeeds has no vendor prefix for whatever reason...
            "subscribedfeeds" -> "${MICROG_VENDOR}.subscribedfeeds"
            else -> null
        }

    /**
     * const-string transform function for strings containing gms content uris / authorities.
     */
    private fun contentUrisTransform(str: String): String? {
        // only when content:// uri
        if (str.startsWith("content://")) {
            // check if matches any authority
            for (authority in AUTHORITIES) {
                val uriPrefix = "content://$authority"
                if (str.startsWith(uriPrefix)) {
                    return str.replace(
                        uriPrefix,
                        "content://${authority.replace("com.google", MICROG_VENDOR)}"
                    )
                }
            }

            // gms also has a 'subscribedfeeds' authority, check for that one too
            val subFeedsUriPrefix = "content://subscribedfeeds"
            if (str.startsWith(subFeedsUriPrefix)) {
                return str.replace(subFeedsUriPrefix, "content://${MICROG_VENDOR}.subscribedfeeds")
            }
        }

        return null
    }

    /**
     * A const-string instruction whose string has to be replaced.
     */
    private class StringHit(val index: Int, val register: Int, val transformed: String)

    /**
     * Transform all constant string references using a transformation function.
     *
     * PORT-NOTE: the hits of a method are collected in a read-only pass first and applied afterwards, because
     * the classes are replaced by their mutable version while they are patched (patcher v22).
     * Replacing a const-string by a const-string never changes the instruction indices.
     *
     * @param transformFn string transformation function. if null, string is not changed.
     */
    private fun BytecodePatchContext.transformStringReferences(transformFn: (str: String) -> String?) {
        classDefs.toList().forEach { classDef ->
            // method -> hits, collected without mutating anything
            val methodHits = classDef.methods.mapNotNull { method ->
                val implementation = method.implementation ?: return@mapNotNull null

                val hits = mutableListOf<StringHit>()
                implementation.instructions.forEachIndexed { index, instruction ->
                    // skip all that are not const-string
                    if (instruction.opcode != Opcode.CONST_STRING) return@forEachIndexed

                    val str = ((instruction as ReferenceInstruction).reference as StringReference).string

                    // call transform function
                    val transformedStr = transformFn(str) ?: return@forEachIndexed

                    hits.add(StringHit(index, (instruction as OneRegisterInstruction).registerA, transformedStr))
                }

                if (hits.isEmpty()) null else method to hits
            }

            if (methodHits.isEmpty()) return@forEach

            // make class mutable, once per class
            val mutableClass = classDefs.getOrReplaceMutable(classDef)

            methodHits.forEach { (method, hits) ->
                val mutableMethod = mutableClass.findMutableMethodOf(method)

                hits.forEach { hit ->
                    // replace instruction with updated string
                    mutableMethod.implementation!!.replaceInstruction(
                        hit.index,
                        BuilderInstruction21c(
                            Opcode.CONST_STRING,
                            hit.register,
                            ImmutableStringReference(hit.transformed)
                        )
                    )
                }
            }
        }
    }

    /**
     * Return the matched methods of a list of [CompositeMatch] early.
     */
    private fun List<CompositeMatch>.returnEarly() {
        this.forEach { match ->
            val method: MutableMethod = match.method

            val stringInstructions = when (method.returnType.first()) {
                'L' -> """
                        const/4 v0, 0x0
                        return-object v0
                        """

                'V' -> "return-void"
                'I', 'Z' -> """
                        const/4 v0, 0x0
                        return v0
                        """

                else -> throw PatchException("Unsupported return type for returnEarly: ${method.returnType}")
            }

            method.addInstructions(
                0, stringInstructions
            )
        }
    }
}
