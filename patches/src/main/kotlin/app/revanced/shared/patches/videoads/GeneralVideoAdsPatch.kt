package app.revanced.shared.patches.videoads

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.shared.fingerprints.legacyVideoAdsMethodMatch
import app.revanced.shared.fingerprints.mainstreamVideoAdsMethodMatch
import app.revanced.shared.fingerprints.mainstreamVideoAdsParentMethodMatch

// Former companion object of GeneralVideoAdsPatch.
private var insertIndex: Int = 0
private lateinit var legacyVideoAdsMethod: MutableMethod
private lateinit var mainstreamVideoAdsMethod: MutableMethod

fun injectLegacyAds(
    descriptor: String
) {
    legacyVideoAdsMethod.addInstructions(
        0, """
            invoke-static {}, $descriptor
            move-result v1
    """
    )
}

fun injectMainstreamAds(
    descriptor: String
) {
    mainstreamVideoAdsMethod.addInstructionsWithLabels(
        insertIndex, """
            invoke-static {}, $descriptor
            move-result v1
            if-nez v1, :show_video_ads
            return-void
    """, ExternalLabel("show_video_ads", mainstreamVideoAdsMethod.getInstruction(insertIndex))
    )
}

// Old name: general-video-ads-patch (helper patch, never selectable)
val generalVideoAdsPatch = bytecodePatch(
    description = "Hooks the legacy and mainstream video ads methods.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    apply {
        // PORT-NOTE: old: toMethodWalker(method).nextMethod(13, true) -> navigate(method).to(13).stop()
        legacyVideoAdsMethod = navigate(legacyVideoAdsMethodMatch.immutableMethod).to(13).stop()

        mainstreamVideoAdsParentMethodMatch.immutableClassDef.mainstreamVideoAdsMethodMatch.let {
            mainstreamVideoAdsMethod = it.method
            insertIndex = it[-1]
        }
    }
}
