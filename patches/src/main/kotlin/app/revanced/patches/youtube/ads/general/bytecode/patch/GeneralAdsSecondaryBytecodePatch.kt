package app.revanced.patches.youtube.ads.general.bytecode.patch

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.wideLiteral
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch
import app.revanced.shared.extensions.findMutableMethodOf
import app.revanced.shared.extensions.injectHideCall
import app.revanced.shared.extensions.toResult
import app.revanced.shared.util.integrations.Constants.ADS_PATH
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c

private class AdHit(
    val classDef: ClassDef,
    val method: Method,
    val kind: Int,
    val insertIndex: Int,
    val viewRegister: Int,
    val dummyRegister: Int,
)

private fun MutableMethod.addInjectCall(
    index: Int,
    dummyRegister: Int,
    viewRegister: Int,
    method: String
) {
    addInstructionsWithLabels(
        index + 1, """
            invoke-static {}, $ADS_PATH/GeneralAdsPatch;->$method()Z
            move-result v$dummyRegister
            if-eqz v$dummyRegister, :shown
            const v$viewRegister, 0x0
        """, ExternalLabel("shown", this.getInstruction(index + 1))
    )
}

// Old name: hide-general-ads-secondary-bytecode-patch (helper patch, never selectable)
val generalAdsSecondaryBytecodePatch = bytecodePatch(
    description = "Hides the ad attribution view, breaking news shelf, album cards and the paid content banner.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(resourceMappingPatch)

    apply {
        // 0: general ads, 1: breaking news, 2: album cards, 3: paid content banner
        val resourceIds = listOf(
            ResourceType.ID["ad_attribution"],
            ResourceType.LAYOUT["horizontal_card_list"],
            ResourceType.LAYOUT["album_card"],
            ResourceType.ID["reel_player_badge"],
        )
        val insertOffsets = intArrayOf(1, 4, 4, 3)
        val patchSuccessArray = Array(resourceIds.size) { false }

        // PORT-NOTE: the old code mutated classes while iterating over them. The hits are collected first
        // (read-only pass), then applied from the highest insert index to the lowest.
        val hits = mutableListOf<AdHit>()

        classDefs.toList().forEach { classDef ->
            classDef.methods.forEach { method ->
                val implementation = method.implementation ?: return@forEach
                val instructions = implementation.instructions

                instructions.forEachIndexed { index, instruction ->
                    if (instruction.opcode != Opcode.CONST) return@forEachIndexed

                    val literal = instruction.wideLiteral ?: return@forEachIndexed
                    val kind = resourceIds.indexOf(literal)
                    if (kind < 0) return@forEachIndexed

                    val insertIndex = index + insertOffsets[kind]
                    val target = instructions.elementAtOrNull(insertIndex) ?: return@forEachIndexed

                    val viewRegister: Int
                    if (kind == 0) { // general ads
                        if (target.opcode != Opcode.INVOKE_VIRTUAL) return@forEachIndexed
                        viewRegister = (target as Instruction35c).registerC
                    } else { // breaking news, album cards, paid content banner
                        if (target.opcode != Opcode.CHECK_CAST) return@forEachIndexed
                        viewRegister = (target as OneRegisterInstruction).registerA
                    }

                    val dummyRegister = (instruction as OneRegisterInstruction).registerA
                    hits.add(AdHit(classDef, method, kind, insertIndex, viewRegister, dummyRegister))
                }
            }
        }

        hits.sortedByDescending { it.insertIndex }.forEach { hit ->
            val mutableMethod = classDefs.getOrReplaceMutable(hit.classDef).findMutableMethodOf(hit.method)

            when (hit.kind) {
                0 -> mutableMethod.implementation!!.injectHideCall(
                    hit.insertIndex, hit.viewRegister, "ads/GeneralAdsPatch", "hideAdAttributionView"
                )

                1 -> mutableMethod.implementation!!.injectHideCall(
                    hit.insertIndex, hit.viewRegister, "ads/GeneralAdsPatch", "hideBreakingNewsShelf"
                )

                2 -> mutableMethod.implementation!!.injectHideCall(
                    hit.insertIndex, hit.viewRegister, "ads/GeneralAdsPatch", "hideAlbumCards"
                )

                3 -> mutableMethod.addInjectCall(
                    hit.insertIndex, hit.dummyRegister, hit.viewRegister, "hidePaidContentBanner"
                )
            }
            patchSuccessArray[hit.kind] = true
        }

        toResult(patchSuccessArray.indexOf(false))
    }
}
