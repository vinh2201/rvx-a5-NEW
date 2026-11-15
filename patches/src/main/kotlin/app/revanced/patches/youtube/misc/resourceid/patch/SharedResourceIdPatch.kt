package app.revanced.patches.youtube.misc.resourceid.patch

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.shared.misc.mapping.ResourceType
import app.revanced.patches.shared.misc.mapping.resourceMappingPatch

// Former companion object of SharedResourcdIdPatch (names unchanged).
internal var infoCardAccessibilityTeaserVibrateDurationMsId: Long = -1
internal var inVideoProgrammingOverlayId: Long = -1
internal var playerHeadingViewContainerId: Long = -1
internal var abclistmenuitemLabelId: Long = -1
internal var accessibilityProgressTimeLabelId: Long = -1
internal var accountSwitcherAccessibilityLabelId: Long = -1
internal var appearanceStringId: Long = -1
internal var backgroundCategoryLabelId: Long = -1
internal var bottompaneloverlaytextLabelId: Long = -1
internal var bottomUiContainerResourceId: Long = -1
internal var controlsLayoutStubResourceId: Long = -1
internal var educationTextViewResourceId: Long = -1
internal var floatybarQueueLabelId: Long = -1
internal var imageOnlyTabId: Long = -1
internal var imageWithTextTabId: Long = -1
internal var layoutCircle: Long = -1
internal var layoutIcon: Long = -1
internal var layoutVideo: Long = -1
internal var liveChatButtonId: Long = -1
internal var timebarColorLabelId: Long = -1
internal var tooltipLabelId: Long = -1
internal var totalTimeId: Long = -1
internal var playerOverlaysId: Long = -1
internal var videoqualityfragmentLabelId: Long = -1

// Old name: shared-resource-id (helper patch, never selectable)
val sharedResourceIdPatch = resourcePatch(
    description = "Resolves the resource ids shared by several patches.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    dependsOn(resourceMappingPatch)

    apply {
        infoCardAccessibilityTeaserVibrateDurationMsId = ResourceType.INTEGER["info_card_accessibility_teaser_vibrate_duration_ms"]
        inVideoProgrammingOverlayId = ResourceType.LAYOUT["youtube_invideo_programming_overlay"]
        playerHeadingViewContainerId = ResourceType.LAYOUT["player_heading_view_container"]
        abclistmenuitemLabelId = ResourceType.LAYOUT["abc_list_menu_item_layout"]
        accessibilityProgressTimeLabelId = ResourceType.STRING["accessibility_player_progress_time"]
        accountSwitcherAccessibilityLabelId = ResourceType.STRING["account_switcher_accessibility_label"]
        appearanceStringId = ResourceType.STRING["app_theme_appearance_dark"]
        backgroundCategoryLabelId = ResourceType.STRING["pref_background_and_offline_category"]
        bottompaneloverlaytextLabelId = ResourceType.ID["bottom_panel_overlay_text"]
        bottomUiContainerResourceId = ResourceType.ID["bottom_ui_container_stub"]
        controlsLayoutStubResourceId = ResourceType.ID["controls_layout_stub"]
        educationTextViewResourceId = ResourceType.ID["user_education_text_view"]
        floatybarQueueLabelId = ResourceType.STRING["floaty_bar_queue_status"]
        imageOnlyTabId = ResourceType.LAYOUT["image_only_tab"]
        imageWithTextTabId = ResourceType.LAYOUT["image_with_text_tab"]
        layoutCircle = ResourceType.LAYOUT["endscreen_element_layout_circle"]
        layoutIcon = ResourceType.LAYOUT["endscreen_element_layout_icon"]
        layoutVideo = ResourceType.LAYOUT["endscreen_element_layout_video"]
        liveChatButtonId = ResourceType.ID["live_chat_overlay_button"]
        timebarColorLabelId = ResourceType.COLOR["inline_time_bar_progress_color"]
        tooltipLabelId = ResourceType.LAYOUT["tooltip_content_view"]
        totalTimeId = ResourceType.STRING["total_time"]
        playerOverlaysId = ResourceType.LAYOUT["player_overlays"]
        videoqualityfragmentLabelId = ResourceType.LAYOUT["video_quality_bottom_sheet_list_fragment_title"]
    }
}
