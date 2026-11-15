package app.revanced.patches.youtube.misc.returnyoutubedislike.bytecode.patch

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.returnyoutubedislike.bytecode.fingerprints.dislikeMethodMatch
import app.revanced.patches.youtube.misc.returnyoutubedislike.bytecode.fingerprints.likeMethodMatch
import app.revanced.patches.youtube.misc.returnyoutubedislike.bytecode.fingerprints.removeLikeMethodMatch
import app.revanced.patches.youtube.misc.returnyoutubedislike.bytecode.fingerprints.shortsTextComponentParentMethodMatch
import app.revanced.patches.youtube.misc.returnyoutubedislike.bytecode.fingerprints.textComponentSpecMethodMatch
import app.revanced.patches.youtube.misc.videoid.mainstream.patch.injectCall
import app.revanced.patches.youtube.misc.videoid.mainstream.patch.mainstreamVideoIdPatch
import app.revanced.shared.util.integrations.Constants.UTILS_PATH
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val INTEGRATIONS_RYD_CLASS_DESCRIPTOR = "$UTILS_PATH/ReturnYouTubeDislikePatch;"

private enum class Vote(val value: Int) {
    LIKE(1),
    DISLIKE(-1),
    REMOVE_LIKE(0)
}

// Old name: return-youtube-dislike-bytecode-patch (helper patch, never selectable)
val returnYouTubeDislikeBytecodePatch = bytecodePatch(
    description = "Shows the dislike count of videos using the Return YouTube Dislike API (bytecode).",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(mainstreamVideoIdPatch)

    apply {
        listOf(
            likeMethodMatch to Vote.LIKE,
            dislikeMethodMatch to Vote.DISLIKE,
            removeLikeMethodMatch to Vote.REMOVE_LIKE
        ).forEach { (match, vote) ->
            match.method.addInstructions(
                0,
                """
                    const/4 v0, ${vote.value}
                    invoke-static {v0}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->sendVote(I)V
                """
            )
        }

        shortsTextComponentParentMethodMatch.let {
            // old: toMethodWalker(it.method).nextMethod(patternScanResult.endIndex, true)
            // (gnk.e -> adax.aq(asum)Spanned in 16.40.36)
            with(navigate(it.immutableMethod).to(it[-1]).stop()) {
                val insertIndex = implementation!!.instructions.size - 1
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->onShortsComponentCreated(Landroid/text/Spanned;)Landroid/text/Spanned;
                        move-result-object v$insertRegister
                    """
                )
            }
        }

        injectCall("$INTEGRATIONS_RYD_CLASS_DESCRIPTOR->newVideoLoaded(Ljava/lang/String;)V")

        with(textComponentSpecMethodMatch.method) {
            // stw.e(): p5 is the ConversionContext (szu, contains "ConversionContext{" in toString) and the
            // second to last parameter is the AtomicReference holding the text (15 parameters in 16.40.36);
            // the method has 13 locals, so v7 and v8 are free.
            val conversionContextParam = 5
            val textRefParam = parameterTypes.size - 2

            addInstructions(
                0,
                """
                    move-object/from16 v7, p$conversionContextParam
                    move-object/from16 v8, p$textRefParam
                    invoke-static {v7, v8}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->onComponentCreated(Ljava/lang/Object;Ljava/util/concurrent/atomic/AtomicReference;)V
                """
            )
        }
    }
}
