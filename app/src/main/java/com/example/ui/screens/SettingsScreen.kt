package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.model.AppLanguage
import com.example.model.ClickDelayOption
import com.example.model.FloatingOpacity
import com.example.model.FloatingSide
import com.example.model.FloatingSize
import com.example.model.FloatingVisibility
import com.example.model.SkipFeedbackMode
import com.example.ui.SkipBuddyUiState
import com.example.ui.components.ElderlyChoiceRow
import com.example.ui.components.LargeToggleRow
import com.example.ui.components.NumberedSectionHeader
import com.example.ui.components.PermissionChecklistRow
import com.example.ui.components.SkipBuddyCard
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusOffRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Settings screen with EXACTLY 6 cards in the locked-in order:
 * 1. Auto-Skip
 * 2. Floating Button
 * 3. Feedback
 * 4. Permissions
 * 5. Language & Announcement
 * 6. Data & About
 */
@Composable
fun SettingsScreen(
    uiState: SkipBuddyUiState,
    onSetAutoSkip: (Boolean) -> Unit,
    onSetClickDelay: (ClickDelayOption) -> Unit,
    onSetMuteUnskippable: (Boolean) -> Unit,
    onSetFloatingVisibility: (FloatingVisibility) -> Unit,
    onSetFloatingLocked: (Boolean) -> Unit,
    onSetFloatingSize: (FloatingSize) -> Unit,
    onSetFloatingSide: (FloatingSide) -> Unit,
    onSetFloatingOpacity: (FloatingOpacity) -> Unit,
    onResetFloatingPosition: () -> Unit,
    onSetFeedbackMode: (SkipFeedbackMode) -> Unit,
    onSetShowAdSkippedMessage: (Boolean) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onSetAppLanguage: (AppLanguage) -> Unit,
    onSetVoiceAnnouncement: (Boolean) -> Unit,
    onSetShowAdBanner: (Boolean) -> Unit,
    onPlayTestSound: () -> Unit,
    onResetToday: () -> Unit,
    onResetAll: () -> Unit,
    onClearLog: () -> Unit,
    onSetDebugLogEnabled: (Boolean) -> Unit,
    onShareDebugLog: () -> Boolean,
    modifier: Modifier = Modifier
) {
    val prefs = uiState.preferences
    val perms = uiState.permissions
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 680.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // CARD 1: AUTO-SKIP
        item(key = "card_1_autoskip") {
            SkipBuddyCard(modifier = Modifier.testTag("settings_card_1")) {
                NumberedSectionHeader(1, stringResource(R.string.settings_card_autoskip))

                LargeToggleRow(
                    title = stringResource(R.string.settings_master_autoskip),
                    subtitle = stringResource(R.string.settings_master_autoskip_desc),
                    checked = prefs.autoSkipEnabled,
                    onCheckedChange = onSetAutoSkip,
                    testTag = "switch_master_autoskip"
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BorderHairline)

                // Locked ON and greyed out — safety invariant
                LargeToggleRow(
                    title = stringResource(R.string.settings_only_youtube),
                    subtitle = stringResource(R.string.settings_only_youtube_note),
                    checked = true,
                    enabled = false,
                    onCheckedChange = {},
                    testTag = "switch_youtube_only_locked"
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BorderHairline)

                Text(
                    text = stringResource(R.string.settings_click_delay),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                ElderlyChoiceRow(
                    options = listOf(
                        ClickDelayOption.INSTANT to stringResource(R.string.delay_instant),
                        ClickDelayOption.HALF_SECOND to stringResource(R.string.delay_half_sec),
                        ClickDelayOption.ONE_SECOND to stringResource(R.string.delay_one_sec)
                    ),
                    selected = prefs.clickDelay,
                    onSelect = onSetClickDelay,
                    testTagPrefix = "choice_delay"
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderHairline)

                LargeToggleRow(
                    title = stringResource(R.string.settings_mute_unskippable),
                    subtitle = stringResource(R.string.settings_mute_unskippable_desc),
                    checked = prefs.muteUnskippableAds,
                    onCheckedChange = onSetMuteUnskippable,
                    testTag = "switch_mute_unskippable"
                )
            }
        }

        // CARD 2: FLOATING BUTTON
        item(key = "card_2_floating") {
            SkipBuddyCard(modifier = Modifier.testTag("settings_card_2")) {
                NumberedSectionHeader(2, stringResource(R.string.settings_card_floating))

                Text(
                    text = stringResource(R.string.settings_floating_visibility),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                ElderlyChoiceRow(
                    options = listOf(
                        FloatingVisibility.SHOW to stringResource(R.string.visibility_show),
                        FloatingVisibility.HIDDEN to stringResource(R.string.visibility_hidden),
                        FloatingVisibility.AUTO_HIDE to stringResource(R.string.visibility_autohide)
                    ),
                    selected = prefs.floatingVisibility,
                    onSelect = onSetFloatingVisibility,
                    testTagPrefix = "choice_visibility",
                    stackedFullWidth = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_floating_autohide_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderHairline)

                LargeToggleRow(
                    title = stringResource(R.string.settings_floating_lock),
                    subtitle = stringResource(R.string.settings_floating_lock_hint),
                    checked = prefs.floatingLocked,
                    onCheckedChange = onSetFloatingLocked,
                    testTag = "switch_floating_lock"
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.guide_caregiver_note),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldPrimary
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderHairline)

                Text(
                    text = stringResource(R.string.settings_floating_size),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                ElderlyChoiceRow(
                    options = listOf(
                        FloatingSize.SMALL to stringResource(R.string.size_small),
                        FloatingSize.MEDIUM to stringResource(R.string.size_medium),
                        FloatingSize.LARGE to stringResource(R.string.size_large)
                    ),
                    selected = prefs.floatingSize,
                    onSelect = onSetFloatingSize,
                    testTagPrefix = "choice_size"
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.settings_floating_side),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                ElderlyChoiceRow(
                    options = listOf(
                        FloatingSide.LEFT to stringResource(R.string.side_left),
                        FloatingSide.RIGHT to stringResource(R.string.side_right)
                    ),
                    selected = prefs.floatingSide,
                    onSelect = onSetFloatingSide,
                    testTagPrefix = "choice_side"
                )

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        onResetFloatingPosition()
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_position_reset),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("button_reset_position"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, EmeraldPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = EmeraldPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.button_reset_position),
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.settings_floating_opacity),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                ElderlyChoiceRow(
                    options = listOf(
                        FloatingOpacity.LOW to stringResource(R.string.opacity_low),
                        FloatingOpacity.MEDIUM to stringResource(R.string.opacity_medium),
                        FloatingOpacity.FULL to stringResource(R.string.opacity_full)
                    ),
                    selected = prefs.floatingOpacity,
                    onSelect = onSetFloatingOpacity,
                    testTagPrefix = "choice_opacity"
                )
            }
        }

        // CARD 3: FEEDBACK
        item(key = "card_3_feedback") {
            SkipBuddyCard(modifier = Modifier.testTag("settings_card_3")) {
                NumberedSectionHeader(3, stringResource(R.string.settings_card_feedback))

                Text(
                    text = stringResource(R.string.settings_when_skipped),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                ElderlyChoiceRow(
                    options = listOf(
                        SkipFeedbackMode.NONE to stringResource(R.string.feedback_none),
                        SkipFeedbackMode.VIBRATE to stringResource(R.string.feedback_vibrate),
                        SkipFeedbackMode.FLASH_CHECKMARK to stringResource(R.string.feedback_flash),
                        SkipFeedbackMode.SOUND to stringResource(R.string.feedback_sound)
                    ),
                    selected = prefs.feedbackMode,
                    onSelect = onSetFeedbackMode,
                    testTagPrefix = "choice_feedback",
                    stackedFullWidth = true
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderHairline)

                LargeToggleRow(
                    title = stringResource(R.string.settings_show_ad_skipped_msg),
                    subtitle = stringResource(R.string.settings_show_ad_skipped_msg_hint),
                    checked = prefs.showAdSkippedMessage,
                    onCheckedChange = onSetShowAdSkippedMessage,
                    testTag = "switch_ad_skipped_msg"
                )
            }
        }

        // CARD 4: PERMISSIONS (in sync with Guide checklist)
        item(key = "card_4_permissions") {
            SkipBuddyCard(modifier = Modifier.testTag("settings_card_4")) {
                NumberedSectionHeader(4, stringResource(R.string.settings_card_permissions))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PermissionChecklistRow(
                        title = stringResource(R.string.perm_accessibility_title),
                        description = stringResource(R.string.perm_accessibility_desc),
                        granted = uiState.isServiceActive,
                        openButtonLabel = stringResource(R.string.button_open),
                        onOpenSettings = onOpenAccessibilitySettings,
                        testTag = "settings_perm_accessibility"
                    )
                    PermissionChecklistRow(
                        title = stringResource(R.string.perm_overlay_title),
                        description = stringResource(R.string.perm_overlay_desc),
                        granted = perms.overlayGranted,
                        openButtonLabel = stringResource(R.string.button_open),
                        onOpenSettings = onOpenOverlaySettings,
                        testTag = "settings_perm_overlay"
                    )
                    PermissionChecklistRow(
                        title = stringResource(R.string.perm_battery_title),
                        description = stringResource(R.string.perm_battery_desc),
                        granted = perms.batteryOptimizationIgnored,
                        openButtonLabel = stringResource(R.string.button_open),
                        onOpenSettings = onOpenBatterySettings,
                        testTag = "settings_perm_battery"
                    )
                    PermissionChecklistRow(
                        title = stringResource(R.string.perm_notifications_title),
                        description = stringResource(R.string.perm_notifications_desc),
                        granted = perms.notificationsGranted,
                        openButtonLabel = stringResource(R.string.button_open),
                        onOpenSettings = onOpenNotificationSettings,
                        testTag = "settings_perm_notifications"
                    )
                }
            }
        }

        // CARD 5: LANGUAGE & ANNOUNCEMENT (English first, then Bangla, Hindi, System Default)
        item(key = "card_5_language") {
            SkipBuddyCard(modifier = Modifier.testTag("settings_card_5")) {
                NumberedSectionHeader(5, stringResource(R.string.settings_card_language))

                Text(
                    text = stringResource(R.string.settings_language_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                ElderlyChoiceRow(
                    options = listOf(
                        AppLanguage.ENGLISH to stringResource(R.string.lang_english),
                        AppLanguage.BANGLA to stringResource(R.string.lang_bangla),
                        AppLanguage.HINDI to stringResource(R.string.lang_hindi),
                        AppLanguage.SYSTEM to stringResource(R.string.lang_system)
                    ),
                    selected = prefs.appLanguage,
                    onSelect = onSetAppLanguage,
                    testTagPrefix = "choice_language",
                    stackedFullWidth = true
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderHairline)

                LargeToggleRow(
                    title = stringResource(R.string.settings_voice_announcement),
                    subtitle = stringResource(R.string.settings_voice_announcement_desc),
                    checked = prefs.voiceAnnouncementEnabled,
                    onCheckedChange = onSetVoiceAnnouncement,
                    testTag = "switch_voice_announcement"
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BorderHairline)

                LargeToggleRow(
                    title = stringResource(R.string.settings_show_ad_banner),
                    subtitle = stringResource(R.string.settings_show_ad_banner_desc),
                    checked = prefs.showAdBannerEnabled,
                    onCheckedChange = onSetShowAdBanner,
                    testTag = "switch_ad_banner"
                )

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onPlayTestSound,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("button_play_test_sound"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.button_play_test_sound),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // CARD 6: DATA & ABOUT (including Detection Debug Log toggle + Share Debug Log)
        item(key = "card_6_data_about") {
            SkipBuddyCard(modifier = Modifier.testTag("settings_card_6")) {
                NumberedSectionHeader(6, stringResource(R.string.settings_card_data_about))

                OutlinedButton(
                    onClick = {
                        onResetToday()
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_today_reset),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("button_reset_today"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, BorderHairline)
                ) {
                    Text(
                        text = stringResource(R.string.button_reset_today),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        onResetAll()
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_all_reset),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("button_reset_all"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, StatusOffRed.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = stringResource(R.string.button_reset_all),
                        style = MaterialTheme.typography.labelLarge,
                        color = StatusOffRed
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        onClearLog()
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_log_cleared),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("button_clear_log"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, BorderHairline)
                ) {
                    Text(
                        text = stringResource(R.string.button_clear_log),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = BorderHairline)

                LargeToggleRow(
                    title = stringResource(R.string.settings_debug_log),
                    subtitle = stringResource(R.string.settings_debug_log_desc),
                    checked = prefs.debugLogEnabled,
                    onCheckedChange = onSetDebugLogEnabled,
                    testTag = "switch_debug_log"
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        val shared = onShareDebugLog()
                        if (!shared) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_debug_log_empty),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("button_share_debug_log"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, EmeraldPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        tint = EmeraldPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.button_share_debug_log),
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldPrimary
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = BorderHairline)

                Text(
                    text = stringResource(R.string.about_version_format, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.about_privacy_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}
