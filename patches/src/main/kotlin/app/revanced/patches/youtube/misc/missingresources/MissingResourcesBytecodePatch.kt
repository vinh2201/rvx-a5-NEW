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
import app.revanced.patches.youtube.misc.missingresources.fingerprints.blackIconTableMethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.blackIconTableWithFlagMethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.contextGetDrawableMethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.iconTypeEnumMethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.iconTypeFromNumberMethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.lithoTextBindMethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.lithoTextMeasureMethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.lithoTextMeasureWithSizeMethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.lithoTextRunColorMethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.resourceByNameMethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.pivotTabSelectedMethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.playerMenuSheet1MethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.playerMenuSheet2MethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.resourcesGetDrawableMethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.settingsMenuItemClickMethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.toolbarMenuMethodMatch
import app.revanced.shared.util.integrations.Constants.UTILS_PATH
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
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

        // region Player menu

        // 9. Popup of the three dots of the player. With a spoofed 20.x client the server also sends its own rows for the same
        //    things the app already has as native rows (quality, speed, captions, audio track, additional settings): the popup
        //    then shows both (a duplicate list), and the rows of the server do nothing because their commands belong to the
        //    new version. The adapter of the popup (two variants) reads the list of the server rows first and the list of the
        //    native rows after it; the server list is made null right after it is read, which the code already handles (it
        //    skips the loop), so only the native rows, the ones of the app, are shown, in the language of the app (the
        //    installed APK decides it; nothing is translated here). Nothing is hard coded and nothing depends on the labels
        //    or commands the server sends: the list fields are the List fields of the class and the method is the one
        //    that reads both of them. No option: without this the popup shows every entry twice and half of them are dead.
        listOf(playerMenuSheet1MethodMatch, playerMenuSheet2MethodMatch).forEach { match ->
            val sheetClass = match.classDef

            val listFieldNames = match.immutableClassDef.fields
                .filter { it.type == "Ljava/util/List;" && !AccessFlags.STATIC.isSet(it.accessFlags) }
                .map { it.name }
            if (listFieldNames.size != 2) throw IllegalStateException("Unexpected fields of the player menu")

            // Index of the instructions that read one of the two lists (iget-object v, p0, Sheet->list:List).
            fun listReadIndices(instructions: List<Instruction>) =
                instructions.indices.filter { index ->
                    val instruction = instructions[index]
                    instruction.opcode == Opcode.IGET_OBJECT &&
                        ((instruction as ReferenceInstruction).reference as? FieldReference)?.let { field ->
                            field.definingClass == sheetClass.type && field.name in listFieldNames
                        } == true
                }

            // The method that builds the adapter reads both lists.
            val builderMethods = sheetClass.methods.filter { method ->
                val instructions = method.implementation?.instructions?.toList() ?: return@filter false
                listReadIndices(instructions)
                    .map { ((instructions[it] as ReferenceInstruction).reference as FieldReference).name }
                    .toSet().size == 2
            }
            if (builderMethods.size != 1) throw IllegalStateException("Unexpected adapter builder of the player menu")

            val builder = builderMethods.single()
            val builderInstructions = builder.implementation!!.instructions.toList()

            // The first list that is read is the one of the server rows; its read is followed by the null check.
            val serverReadIndex = listReadIndices(builderInstructions).first()
            if (builderInstructions[serverReadIndex + 1].opcode != Opcode.IF_EQZ) {
                throw IllegalStateException("Unexpected adapter builder of the player menu (null check)")
            }
            val serverRegister = (builderInstructions[serverReadIndex] as OneRegisterInstruction).registerA
            if (serverRegister > 15) throw IllegalStateException("Unexpected register in the player menu")

            builder.addInstruction(serverReadIndex + 1, "const/4 v$serverRegister, 0x0")
        }

        // endregion

        // region Litho text color

        // 10. Default color of the text of the feed. The text component paints the text that has no color of its own with a fixed
        //     black default, and the text that a current server sends to this old app has no color (the server relies on the theme),
        //     so on the dark theme the video titles, the channel lines and the shelf titles were black on a dark background.
        //     Three static methods (bind, two measure variants) take the default color list as a parameter and the component
        //     context as the first parameter: at their start the color list is passed through the extension, which returns the
        //     primary text color of the theme when the theme is dark and nothing otherwise (the original list stays on the
        //     light theme). No option: the black text is not a style that anybody wants.
        val contextType = "Landroid/content/Context;"
        val colorListType = "Landroid/content/res/ColorStateList;"

        listOf(lithoTextBindMethodMatch, lithoTextMeasureMethodMatch, lithoTextMeasureWithSizeMethodMatch).forEach { match ->
            val method = match.method
            val parameters = method.parameterTypes.map { it.toString() }

            val colorIndex = parameters.indexOf(colorListType)
            if (colorIndex < 0 || parameters.lastIndexOf(colorListType) != colorIndex) {
                throw IllegalStateException("Unexpected parameters of the Litho text method")
            }
            // No wide (long / double) parameter before the color list, so the register of the parameter is its index.
            if (parameters.take(colorIndex).any { it == "J" || it == "D" }) {
                throw IllegalStateException("Unexpected wide parameter in the Litho text method")
            }

            // v0 is free at the start of a method (a local register is written before it is read; every method has at least one).
            val locals = method.implementation!!.registerCount - parameters.sumOf { if (it == "J" || it == "D") 2 else 1 }
            if (locals < 1) throw IllegalStateException("No free register in the Litho text method")

            // First parameter: the component context. Its class has exactly one instance field of the Android context type. The field
            // is read directly: the getter of that class returns the application context, which has no dark theme.
            val componentContextType = parameters.first()
            val componentContext = classDefs.toList().firstOrNull { it.type == componentContextType }
                ?: throw IllegalStateException("Component context class of the Litho text not found")
            val contextField = componentContext.fields.singleOrNull {
                it.type == contextType && !AccessFlags.STATIC.isSet(it.accessFlags)
            } ?: throw IllegalStateException("Android context field of the component context not found")

            // The extension returns the color list to use on a dark theme, or null (light theme: the original list stays).
            method.addInstructionsWithLabels(
                0,
                """
                    move-object/from16 v0, p0
                    iget-object v0, v0, $componentContextType->${contextField.name}:$contextType
                    invoke-static {v0}, $EXTENSION_CLASS_DESCRIPTOR->getDarkTextColor($contextType)$colorListType
                    move-result-object v0
                    if-eqz v0, :original
                    move-object/from16 p$colorIndex, v0
                """,
                ExternalLabel("original", method.getInstruction(0))
            )
        }

        // 10b. Color of the parts of a text that the server colors itself. The text of the posts and of the description of a video
        //      comes with the colors of the light theme (almost black text, dark browns on the tinted description panel), which
        //      the old app applies as they are (a ForegroundColorSpan per run), so the text was almost black on the dark theme.
        //      Right before the span is created the color goes through the extension: on a dark theme an almost black color becomes
        //      the primary text color of the theme; all other colors and the light theme stay as they are.
        lithoTextRunColorMethodMatch.method.apply {
            val parameters = parameterTypes.map { it.toString() }
            val componentContextType = parameters[1]
            val componentContext = classDefs.toList().firstOrNull { it.type == componentContextType }
                ?: throw IllegalStateException("Component context class of the Litho text run not found")
            val contextField = componentContext.fields.singleOrNull {
                it.type == contextType && !AccessFlags.STATIC.isSet(it.accessFlags)
            } ?: throw IllegalStateException("Android context field of the component context not found (text run)")

            val instructions = implementation!!.instructions.toList()
            val spanInitIndices = instructions.indices.filter { index ->
                val instruction = instructions[index]
                val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                instruction.opcode == Opcode.INVOKE_DIRECT &&
                    reference?.definingClass == "Landroid/text/style/ForegroundColorSpan;" && reference.name == "<init>"
            }
            if (spanInitIndices.size != 1) throw IllegalStateException("Unexpected number of color spans in the Litho text run method")
            val spanInitIndex = spanInitIndices.single()

            // invoke-direct {span, color}, ForegroundColorSpan-><init>(I)V, right after long-to-int color, runColor.
            val spanInit = instructions[spanInitIndex] as FiveRegisterInstruction
            if (spanInit.registerCount != 2) throw IllegalStateException("Unexpected color span constructor call")
            val colorRegister = spanInit.registerD

            val toInt = instructions[spanInitIndex - 1]
            if (toInt.opcode != Opcode.LONG_TO_INT || (toInt as TwoRegisterInstruction).registerA != colorRegister) {
                throw IllegalStateException("Unexpected color conversion before the color span")
            }
            // The register that held the long color is dead after the conversion: the original code overwrites it right after the
            // span is created (move-result-wide of the next call), so it is free to hold the context for the call.
            val tempRegister = toInt.registerB
            val afterCall = instructions[spanInitIndex + 1]
            val afterResult = instructions[spanInitIndex + 2]
            if (afterCall.opcode != Opcode.INVOKE_VIRTUAL ||
                afterResult.opcode != Opcode.MOVE_RESULT_WIDE ||
                (afterResult as OneRegisterInstruction).registerA != tempRegister
            ) {
                throw IllegalStateException("The register of the long color is not free after the color span")
            }
            if (colorRegister > 15 || tempRegister > 15 || colorRegister == tempRegister) {
                throw IllegalStateException("Unexpected registers around the color span")
            }

            addInstructions(
                spanInitIndex,
                """
                    move-object/from16 v$tempRegister, p1
                    iget-object v$tempRegister, v$tempRegister, $componentContextType->${contextField.name}:$contextType
                    invoke-static {v$colorRegister, v$tempRegister}, $EXTENSION_CLASS_DESCRIPTOR->adjustTextRunColor(I$contextType)I
                    move-result v$colorRegister
                """
            )
        }

        // endregion

        // region White icons

        // 11. The icons that the server asks for by name are the black ones; the old app does not paint them with the color of the
        //     theme, so on the dark theme they were black on a dark background (only the icons that happen to have a white sibling
        //     were white). The resource patch adds a white sibling for every black icon; at the start of the name -> id resolver
        //     the extension returns the id of the white sibling when the theme is dark, otherwise 0 and the app goes on as usual.
        //     No option, as the black icon on a dark background is not a style that anybody wants.
        resourceByNameMethodMatch.method.apply {
            // p0 = context, p1 = resource type, p2 = name; v0 is free at the start of the method.
            addInstructionsWithLabels(
                0,
                """
                    invoke-static {p0, p1, p2}, $EXTENSION_CLASS_DESCRIPTOR->getDarkDrawableId(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)I
                    move-result v0
                    if-eqz v0, :original
                    return v0
                """,
                ExternalLabel("original", getInstruction(0))
            )
        }

        // endregion

        // 12. White thumbs on the dark theme: the like button of the video (and every picture that is looked up by icon type)
        //     gets the black thumb drawable from the table of the black icons and is not painted with the color of the theme.
        //     Every return of the two lookup methods of that table goes through the extension, which returns the white sibling
        //     of a black thumb when the theme is dark (see MissingResourcesPatch.getDarkIconId); any other id is returned as it is.
        listOf(
            blackIconTableWithFlagMethodMatch.method,
            blackIconTableWithFlagMethodMatch.immutableClassDef.blackIconTableMethodMatch.method,
        ).forEach { method ->
            val returns = method.implementation!!.instructions.withIndex()
                .filter { it.value.opcode == Opcode.RETURN }
                .map { it.index to (it.value as OneRegisterInstruction).registerA }

            check(returns.isNotEmpty()) { "The black icon table method has no return instruction" }

            // From the last to the first, so that the indexes that are still to be used do not move.
            returns.asReversed().forEach { (index, register) ->
                method.addInstructions(
                    index,
                    """
                        invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->getDarkIconId(I)I
                        move-result v$register
                    """
                )
            }
        }
    }
}
