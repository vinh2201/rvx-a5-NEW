package app.revanced.patches.youtube.video.quality.bytecode.patch

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.videoid.legacy.patch.injectCall
import app.revanced.patches.youtube.misc.videoid.legacy.patch.legacyVideoIdPatch
import app.revanced.patches.youtube.video.quality.bytecode.fingerprints.videoQualityReferenceMethodMatch
import app.revanced.patches.youtube.video.quality.bytecode.fingerprints.videoQualitySetterMethodMatch
import app.revanced.patches.youtube.video.quality.bytecode.fingerprints.videoUserQualityChangeMethodMatch
import app.revanced.shared.util.integrations.Constants.VIDEO_PATH
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/VideoQualityPatch;"

// Old name: default-video-quality-bytecode-patch (helper patch, never selectable)
val videoQualityBytecodePatch = bytecodePatch(
    description = "Hooks the video quality of the player.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(legacyVideoIdPatch)

    apply {
        val parentMatch = videoQualitySetterMethodMatch
        val parentClassDef = parentMatch.immutableClassDef

        val referenceMatch = parentClassDef.videoQualityReferenceMethodMatch
        val qualityFieldReference =
            referenceMatch.method.getInstruction<ReferenceInstruction>(0).reference as FieldReference

        parentMatch.method.addInstructions(
            0, """
                iget-object v0, p0, ${parentClassDef.type}->${qualityFieldReference.name}:${qualityFieldReference.type}
                invoke-static {p1, p2, v0}, $INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->setVideoQuality([Ljava/lang/Object;ILjava/lang/Object;)I
                move-result p2
            """,
        )

        parentClassDef.videoUserQualityChangeMethodMatch.method.addInstruction(
            0,
            "invoke-static {p3}, $INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->userChangedQuality(I)V"
        )

        injectCall("$INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;)V")
    }
}
