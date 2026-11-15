package app.revanced.patches.youtube.video.speed.bytecode.patch

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.toInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.videocpn.patch.injectCall
import app.revanced.patches.youtube.misc.videocpn.patch.videoCpnPatch
import app.revanced.patches.youtube.video.speed.bytecode.fingerprints.videoSpeedChangedMethodMatch
import app.revanced.patches.youtube.video.speed.bytecode.fingerprints.videoSpeedParentMethodMatch
import app.revanced.patches.youtube.video.speed.bytecode.fingerprints.videoSpeedSetterMethodMatch
import app.revanced.shared.util.integrations.Constants.VIDEO_PATH
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR =
    "$VIDEO_PATH/VideoSpeedPatch;"

// Old name: default-video-speed-bytecode-patch (helper patch, never selectable)
val videoSpeedBytecodePatch = bytecodePatch(
    description = "Hooks the video speed of the player.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(videoCpnPatch)

    apply {
        val parentMatch = videoSpeedParentMethodMatch
        val parentClassDef = parentMatch.immutableClassDef

        // old companion state (startIndex/endIndex/firstRef/secondRef/thirdRef) is now local
        val firstRef: FieldReference
        val secondRef: FieldReference
        val thirdRef: MethodReference

        parentClassDef.videoSpeedChangedMethodMatch.let { changedMatch ->
            val startIndex = changedMatch[0]
            val endIndex = changedMatch[-1]

            with(changedMatch.method) {
                firstRef = getInstruction<ReferenceInstruction>(startIndex).reference as FieldReference
                secondRef = getInstruction<ReferenceInstruction>(endIndex - 1).reference as FieldReference
                thirdRef = getInstruction<ReferenceInstruction>(endIndex).reference as MethodReference

                val register = getInstruction<FiveRegisterInstruction>(endIndex).registerD

                addInstruction(
                    endIndex,
                    "invoke-static { v$register }, $INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR" +
                            "->" +
                            "userChangedSpeed(F)V"
                )
            }
        }

        val thirdRefDescriptor = "${thirdRef.definingClass}->${thirdRef.name}(" +
                "${thirdRef.parameterTypes.joinToString("")})${thirdRef.returnType}"

        val parentMutableClass = parentMatch.classDef
        parentMutableClass.methods.add(
            ImmutableMethod(
                parentMutableClass.type,
                "overrideSpeed",
                listOf(ImmutableMethodParameter("F", null, null)),
                "V",
                AccessFlags.PRIVATE.value,
                null,
                null,
                ImmutableMethodImplementation(
                    4, """
                        const/4 v0, 0x0
                        cmpg-float v0, v3, v0
                        if-lez v0, :cond_0
                        iget-object v0, v2, ${parentClassDef.type}->${firstRef.name}:${firstRef.type}
                        check-cast v0, ${secondRef.definingClass}
                        iget-object v1, v0, ${secondRef.definingClass}->${secondRef.name}:${secondRef.type}
                        invoke-virtual {v1, v3}, $thirdRefDescriptor
                        :cond_0
                        return-void
                    """.toInstructions(), null, null
                )
            ).toMutable()
        )

        videoSpeedSetterMethodMatch.let {
            it.method.addInstructions(
                0, """
                    invoke-static {}, $INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR->getSpeedValue()F
                    move-result v0
                    invoke-direct {p0, v0}, ${it.immutableClassDef.type}->overrideSpeed(F)V
                """,
            )
        }

        injectCall("$INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Z)V")
    }
}
