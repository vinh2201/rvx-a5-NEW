package app.revanced.patches.youtube.misc.missingresources

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.integrations.patch.integrationsPatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.actionBarConfigMethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.attributeResolverMethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.contextGetDrawableMethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.iconTypeEnumMethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.iconTypeFromNumberMethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.pivotTabSelectedMethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.resourcesGetDrawableMethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.settingsMenuItemClickMethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.toolbarMenuMethodMatch
import app.revanced.shared.util.integrations.Constants.UTILS_PATH
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_CLASS_DESCRIPTOR = "$UTILS_PATH/MissingResourcesPatch;"
private const val TOP_BAR_BUTTONS_CLASS_DESCRIPTOR = "$UTILS_PATH/TopBarButtonsPatch;"

// Method that is added to the controller of the "Settings" entry of the app; the extension (TopBarButtonsPatch) calls it.
private const val OPEN_SETTINGS_METHOD = "rvxa5OpenSettings"

/**
 * Bytecode part of "Add missing resources (A5)".
 *
 * Why it exists: when the client is spoofed to a newer version (20.x) the server sends layouts/icon types that
 * this old app (16.40.36) does not know:
 *  - unknown icon type numbers (the new "Cairo" icons: bell, search, create, settings ...) -> enum lookup returns null
 *    -> UNKNOWN -> drawable id 0 -> Context.getDrawable(0) throws -> the app crashes or shows the wrong icon,
 *  - toolbar attributes that resolve to id 0.
 *
 * Comparison:
 *  - rv6 only remapped the three bottom-bar icons (home / shorts / subscriptions) and rescued everything else with a
 *    thread-local "last icon type" guess -> the top-bar bell/search icons were wrong. Not ported.
 *  - community remaps every Cairo icon type to the legacy icon type with the same meaning; that mapping is used here
 *    (see MissingResourcesPatch.getLegacyIconType) and the drawable guards only return a transparent drawable.
 *
 * Buttons that the old UI of 16.40.36 does not have are added here too (see TopBarButtonsPatch.java): the button
 * with the list of the subscribed channels (Subscriptions tab) and the settings gear (You tab) in the top bar.
 */
val addMissingResourcesBytecodePatch = bytecodePatch(
    description = "Fixes crashes and wrong icons caused by icon types and resources that are missing in this old version.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    // The extension class (MissingResourcesPatch) is merged by integrationsPatch.
    dependsOn(integrationsPatch)

    apply {
        // 1. Remap the icon type numbers of newer servers to the legacy numbers that exist in 16.40.36.
        iconTypeEnumMethodMatch.immutableClassDef.iconTypeFromNumberMethodMatch.method.addInstructions(
            0,
            """
                invoke-static {p0}, $EXTENSION_CLASS_DESCRIPTOR->getLegacyIconType(I)I
                move-result p0
            """
        )

        // 2. Context.getDrawable(0) -> transparent drawable (instead of Resources.NotFoundException).
        contextGetDrawableMethodMatch.method.apply {
            addInstructionsWithLabels(
                0,
                """
                    if-nez p1, :original
                    invoke-static {p0, p1}, $EXTENSION_CLASS_DESCRIPTOR->getDrawable(Landroid/content/Context;I)Landroid/graphics/drawable/Drawable;
                    move-result-object p0
                    return-object p0
                """,
                ExternalLabel("original", getInstruction(0))
            )
        }

        // 3. Resources.getDrawable(0, theme) -> transparent drawable.
        resourcesGetDrawableMethodMatch.method.apply {
            addInstructionsWithLabels(
                0,
                """
                    if-nez p1, :original
                    invoke-static {p0, p1, p2}, $EXTENSION_CLASS_DESCRIPTOR->getDrawable(Landroid/content/res/Resources;ILandroid/content/res/Resources${'$'}Theme;)Landroid/graphics/drawable/Drawable;
                    move-result-object p0
                    return-object p0
                """,
                ExternalLabel("original", getInstruction(0))
            )
        }

        // 4. Toolbar attribute resolver: attribute id 0 -> transparent drawable (instead of an exception).
        attributeResolverMethodMatch.method.apply {
            addInstructionsWithLabels(
                0,
                """
                    if-nez p1, :original
                    invoke-static {p0, p1}, $EXTENSION_CLASS_DESCRIPTOR->getDrawable(Landroid/content/Context;I)Landroid/graphics/drawable/Drawable;
                    move-result-object p0
                    return-object p0
                """,
                ExternalLabel("original", getInstruction(0))
            )
        }

        // region Top bar buttons

        // 5. A tab of the bottom bar got selected (the tab id "FEsubscriptions" / "FElibrary" ... was cast to String).
        pivotTabSelectedMethodMatch.let { match ->
            with(match.method) {
                val checkCastIndex = match[9]
                val checkCast = getInstruction(checkCastIndex)
                if (checkCast.opcode != Opcode.CHECK_CAST) {
                    throw IllegalStateException("Unexpected tab selection method")
                }

                val register = (checkCast as OneRegisterInstruction).registerA
                addInstruction(
                    checkCastIndex + 1,
                    "invoke-static {v$register}, $TOP_BAR_BUTTONS_CLASS_DESCRIPTOR->onPivotTabSelected(Ljava/lang/String;)V"
                )
            }
        }

        // 6. The top bar got the configuration of a page: its int field is the navigation mode (0 = first page of a tab,
        //    1 = back arrow). v0 is free at the start of the method and is overwritten before it is read.
        actionBarConfigMethodMatch.method.apply {
            val registers = implementation!!.registerCount - (parameterTypes.size + 1) // locals (this is not in parameterTypes)
            if (registers < 1) throw IllegalStateException("No free register in the top bar configuration method")

            val instructions = implementation!!.instructions
            val intFieldIndex = instructions.indexOfFirst {
                it.opcode == Opcode.IGET && ((it as ReferenceInstruction).reference as FieldReference).type == "I"
            }
            if (intFieldIndex < 0) throw IllegalStateException("Navigation mode field of the top bar configuration not found")

            val field = (instructions.elementAt(intFieldIndex) as ReferenceInstruction).reference as FieldReference
            addInstructions(
                0,
                """
                    iget v0, p1, ${field.definingClass}->${field.name}:${field.type}
                    invoke-static {v0}, $TOP_BAR_BUTTONS_CLASS_DESCRIPTOR->onActionBarChanged(I)V
                """
            )
        }

        // 7. The menu of the top bar was filled: add the buttons at its end (p0 = menu, p4 = color of the icons).
        toolbarMenuMethodMatch.method.apply {
            val returnIndex = implementation!!.instructions.count() - 1
            if (getInstruction(returnIndex).opcode != Opcode.RETURN_VOID) {
                throw IllegalStateException("Unexpected top bar menu method")
            }

            // The final return is the target of the jump that ends the loop over the buttons. A label stays with the
            // instruction it points to when something is inserted before it, so the hook replaces the return and a new
            // return is added behind it (every path then passes the hook).
            addInstruction(returnIndex + 1, "return-void")
            replaceInstruction(
                returnIndex,
                "invoke-static {p0, p4}, $TOP_BAR_BUTTONS_CLASS_DESCRIPTOR->onMenuPopulated(Landroid/view/Menu;I)V"
            )
        }

        // 8. The "Settings" entry of the overflow menu: its controller opens the settings of the app. The class gets a public
        //    method that calls the (obfuscated) click method, and the constructor hands the new controller to the extension.
        settingsMenuItemClickMethodMatch.let { match ->
            val controllerClass = match.classDef
            val clickMethod = match.method

            val openMethod = ImmutableMethod(
                controllerClass.type,
                OPEN_SETTINGS_METHOD,
                emptyList(),
                "Z",
                AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                null,
                null,
                MutableMethodImplementation(2) // v0 and p0 (this)
            ).toMutable()
            openMethod.addInstructions(
                0,
                """
                    invoke-virtual {p0}, ${controllerClass.type}->${clickMethod.name}()Z
                    move-result p0
                    return p0
                """
            )
            controllerClass.methods.add(openMethod)

            val controllerConstructor = controllerClass.methods.first { MethodUtil.isConstructor(it) }
            val returnIndex = controllerConstructor.implementation!!.instructions.count() - 1
            if (controllerConstructor.getInstruction(returnIndex).opcode != Opcode.RETURN_VOID) {
                throw IllegalStateException("Unexpected constructor of the settings menu controller")
            }

            controllerConstructor.addInstruction(
                returnIndex,
                "invoke-static {p0}, $TOP_BAR_BUTTONS_CLASS_DESCRIPTOR->onSettingsMenuControllerCreated(Ljava/lang/Object;)V"
            )
        }

        // endregion
    }
}
