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
import app.revanced.patches.youtube.misc.missingresources.fingerprints.playerMenuItemClickMethodMatch
import app.revanced.patches.youtube.misc.missingresources.fingerprints.playerMenuListViewItemClickMethodMatch
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
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_CLASS_DESCRIPTOR = "$UTILS_PATH/MissingResourcesPatch;"
private const val TOP_BAR_BUTTONS_CLASS_DESCRIPTOR = "$UTILS_PATH/TopBarButtonsPatch;"
private const val PLAYER_MENU_CLASS_DESCRIPTOR = "$UTILS_PATH/PlayerMenuFixPatch;"

// Controller of the player menu: its static method turns the item of a row of the server into the command of the row.
private const val PLAYER_OVERFLOW_CONTROLLER_DESCRIPTOR =
    "Lcom/google/android/apps/youtube/app/watch/bottomsheet/PlayerOverflowBottomSheetController;"

// Method that is added to the two classes of the player menu: it handles the tap on a row that came from the server.
private const val HANDLE_SERVER_ROW_METHOD = "rvxa5HandleServerRow"

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

        // 9. The settings rows of the player menu (quality, playback speed, captions, audio track, additional settings) that a
        //    spoofed 20.x client gets from the server do nothing when tapped: the menu only closes, because the command behind
        //    the row is one of the new version. The click handler of the menu (two variants) asks the extension first: it
        //    recognises such a row and the handler clicks the native row of the app that does the same. There is no option
        //    for this (without it the entries of the menu are dead). Everything that is needed (types, fields, the call that
        //    makes the command) is read from the click handler itself, so nothing is hard coded.
        listOf(playerMenuItemClickMethodMatch, playerMenuListViewItemClickMethodMatch).forEach { match ->
            val sheetClass = match.classDef
            val clickMethod = match.method
            val instructions = clickMethod.implementation!!.instructions.toList()

            // The first instruction reads the list (variant 1: its adapter, variant 2: the ListView).
            if (instructions[0].opcode != Opcode.IGET_OBJECT) {
                throw IllegalStateException("Unexpected player menu click handler")
            }
            val listField = (instructions[0] as ReferenceInstruction).reference as FieldReference
            val listIsListView = listField.type == "Landroid/widget/ListView;"

            // "instance-of X, if-eqz, check-cast X" appears twice: for a row of the server (first) and for a native row (second).
            val castIndices = instructions.indices.filter { i ->
                i + 2 < instructions.size &&
                    instructions[i].opcode == Opcode.INSTANCE_OF &&
                    instructions[i + 1].opcode == Opcode.IF_EQZ &&
                    instructions[i + 2].opcode == Opcode.CHECK_CAST &&
                    ((instructions[i] as ReferenceInstruction).reference as TypeReference).type ==
                    ((instructions[i + 2] as ReferenceInstruction).reference as TypeReference).type
            }.map { it + 2 }
            if (castIndices.size != 2) throw IllegalStateException("Unexpected player menu click handler (rows)")

            val serverCastIndex = castIndices[0]
            val nativeCastIndex = castIndices[1]
            val serverRowType = ((instructions[serverCastIndex] as ReferenceInstruction).reference as TypeReference).type
            val nativeRowType = ((instructions[nativeCastIndex] as ReferenceInstruction).reference as TypeReference).type

            // Right after the cast of the server row: iget-object item, row, ServerRow->item (the item that holds the command).
            if (instructions[serverCastIndex + 1].opcode != Opcode.IGET_OBJECT) {
                throw IllegalStateException("Unexpected player menu click handler (item)")
            }
            val itemField = (instructions[serverCastIndex + 1] as ReferenceInstruction).reference as FieldReference
            if (itemField.definingClass != serverRowType) throw IllegalStateException("Unexpected player menu click handler (item type)")

            // Right after the cast of the native row: invoke-virtual {row}, NativeRow->click()V.
            if (instructions[nativeCastIndex + 1].opcode != Opcode.INVOKE_VIRTUAL) {
                throw IllegalStateException("Unexpected player menu click handler (native click)")
            }
            val nativeClick = (instructions[nativeCastIndex + 1] as ReferenceInstruction).reference as MethodReference
            if (nativeClick.definingClass != nativeRowType || nativeClick.parameterTypes.isNotEmpty() || nativeClick.returnType != "V") {
                throw IllegalStateException("Unexpected player menu click handler (native click method)")
            }

            // The static method of the controller that turns the item into the command of the row.
            val commandMethod = instructions
                .filter { it.opcode == Opcode.INVOKE_STATIC }
                .map { (it as ReferenceInstruction).reference as MethodReference }
                .first { it.definingClass == PLAYER_OVERFLOW_CONTROLLER_DESCRIPTOR && it.parameterTypes.size == 1 }
            val commandCall = "${commandMethod.definingClass}->${commandMethod.name}" +
                "(${commandMethod.parameterTypes.joinToString("")})${commandMethod.returnType}"

            val dismissMethod = instructions
                .filter { it.opcode == Opcode.INVOKE_VIRTUAL }
                .map { (it as ReferenceInstruction).reference as MethodReference }
                .first { it.name == "dismiss" && it.parameterTypes.isEmpty() }

            // The list with the native rows of the menu is the first of the two List fields (the second one holds the rows of the server).
            val listFields = match.immutableClassDef.fields
                .filter { it.type == "Ljava/util/List;" && !AccessFlags.STATIC.isSet(it.accessFlags) }
                .sortedBy { it.name }
            if (listFields.size != 2) throw IllegalStateException("Unexpected fields of the player menu")
            val nativeRowsField = listFields[0]

            val readAdapter = if (listIsListView) {
                """
                    iget-object v0, p0, ${listField.definingClass}->${listField.name}:${listField.type}
                    if-eqz v0, :no_adapter
                    invoke-virtual {v0}, Landroid/widget/ListView;->getAdapter()Landroid/widget/ListAdapter;
                    move-result-object v0
                    :no_adapter
                """
            } else {
                "iget-object v0, p0, ${listField.definingClass}->${listField.name}:${listField.type}"
            }

            // boolean rvxa5HandleServerRow(Object row): true when the tap was handled (the caller only has to close the menu).
            val handleMethod = ImmutableMethod(
                sheetClass.type,
                HANDLE_SERVER_ROW_METHOD,
                listOf(ImmutableMethodParameter("Ljava/lang/Object;", null, null)),
                "Z",
                AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                null,
                null,
                MutableMethodImplementation(6) // v0..v3, p0 (this), p1 (row)
            ).toMutable()
            handleMethod.addInstructionsWithLabels(
                0,
                """
                    $readAdapter
                    iget-object v1, p0, ${sheetClass.type}->${nativeRowsField.name}:Ljava/util/List;
                    move-object v2, p1
                    check-cast v2, $serverRowType
                    iget-object v2, v2, ${itemField.definingClass}->${itemField.name}:${itemField.type}
                    if-eqz v2, :not_handled
                    invoke-static {v2}, $commandCall
                    move-result-object v2
                    invoke-static {p1, v2, v0, v1}, $PLAYER_MENU_CLASS_DESCRIPTOR->resolveRowClick(Ljava/lang/Object;Ljava/lang/Object;Landroid/widget/ListAdapter;Ljava/util/List;)I
                    move-result v2
                    const/4 v3, -0x1
                    if-eq v2, v3, :not_handled
                    if-ltz v2, :handled
                    invoke-interface {v1, v2}, Ljava/util/List;->get(I)Ljava/lang/Object;
                    move-result-object v3
                    check-cast v3, $nativeRowType
                    invoke-virtual {v3}, ${nativeClick.definingClass}->${nativeClick.name}()V
                    :handled
                    const/4 v0, 0x1
                    return v0
                    :not_handled
                    const/4 v0, 0x0
                    return v0
                """
            )
            sheetClass.methods.add(handleMethod)

            // Hook: right after the cast of the server row (p3 is free there; the instance-of result is not needed any more).
            clickMethod.addInstructionsWithLabels(
                serverCastIndex + 1,
                """
                    invoke-virtual {p0, p1}, ${sheetClass.type}->$HANDLE_SERVER_ROW_METHOD(Ljava/lang/Object;)Z
                    move-result p3
                    if-eqz p3, :rvxa5_original
                    invoke-virtual {p0}, ${dismissMethod.definingClass}->dismiss()V
                    return-void
                """,
                ExternalLabel("rvxa5_original", clickMethod.getInstruction(serverCastIndex + 1))
            )
        }

        // endregion
    }
}
