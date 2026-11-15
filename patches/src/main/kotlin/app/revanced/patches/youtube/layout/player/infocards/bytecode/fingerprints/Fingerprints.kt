package app.revanced.patches.youtube.layout.player.infocards.bytecode.fingerprints

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patches.youtube.misc.resourceid.patch.infoCardAccessibilityTeaserVibrateDurationMsId
import app.revanced.shared.extensions.containsStringLiteral
import app.revanced.util.containsLiteralInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.ClassDef

// Old: InfocardsIncognitoParentFingerprint (kpc.<init> in 16.40.36)
// (resource id is read lazily when the match is resolved, i.e. after sharedResourceIdPatch ran)
internal val BytecodePatchContext.infocardsIncognitoParentMethodMatch by composingFirstMethod {
    returnType("V")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    custom { containsLiteralInstruction(infoCardAccessibilityTeaserVibrateDurationMsId) }
}

// Old: InfocardsIncognitoFingerprint (was resolved inside the class of the parent; kpc.a(Larol;JJ)Boolean in 16.40.36)
internal val ClassDef.infocardsIncognitoMethodMatch by ClassDefComposing.composingFirstMethod {
    returnType("Ljava/lang/Boolean;")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    custom { containsStringLiteral("vibrator") }
}
