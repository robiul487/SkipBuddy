package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppLanguage
import com.example.ui.SkipBuddyUiState
import com.example.ui.components.ElderlyChoiceRow
import com.example.ui.components.RestrictedSettingHelpCard
import com.example.ui.components.SkipBuddyCard
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusOffContainer
import com.example.ui.theme.StatusOffRed
import com.example.ui.theme.SurfaceBackground
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * First-launch Setup Wizard (skippable, re-openable from Guide).
 * Exact step order:
 * 1) Accessibility (+ Android 13+ Restricted Setting help block & Open App Settings button)
 * 2) Display over other apps
 * 3) Battery optimization
 * 4) Notifications
 * 5) Xiaomi/Samsung Autostart guide
 * 6) Play Test Sound
 * 7) Finish prompt: Hide + Lock floating button
 */
@Composable
fun SetupWizardScreen(
    uiState: SkipBuddyUiState,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onRequestOrOpenNotifications: () -> Unit,
    onOpenAutostartSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onSetAppLanguage: (AppLanguage) -> Unit,
    onPlayTestSound: () -> Unit,
    onSkipWizard: () -> Unit,
    onFinishHideAndLock: () -> Unit,
    onFinishKeepAutoHideLocked: () -> Unit
) {
    val totalSteps = 7
    var currentStep by rememberSaveable { mutableIntStateOf(0) }

    BackHandler {
        if (currentStep > 0) {
            currentStep--
        } else {
            onSkipWizard()
        }
    }

    Scaffold(
        containerColor = SurfaceBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Header row with title + Skip Setup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = stringResource(R.string.wizard_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.wizard_step_counter, currentStep + 1, totalSteps),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    TextButton(
                        onClick = onSkipWizard,
                        modifier = Modifier.testTag("wizard_skip_button")
                    ) {
                        Text(
                            text = stringResource(R.string.wizard_skip),
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (currentStep + 1).toFloat() / totalSteps.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50)),
                    color = EmeraldPrimary,
                    trackColor = SurfaceSubtle,
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (currentStep) {
                        0 -> {
                            WizardPermissionStep(
                                icon = Icons.Rounded.AccessibilityNew,
                                title = stringResource(R.string.wizard_step_accessibility_title),
                                body = stringResource(R.string.wizard_step_accessibility_body),
                                granted = uiState.isServiceActive,
                                actionLabel = stringResource(R.string.button_open_settings),
                                onAction = onOpenAccessibilitySettings,
                                testTag = "wizard_action_accessibility"
                            )
                            RestrictedSettingHelpCard(
                                onOpenAppSettings = onOpenAppSettings,
                                testTag = "wizard_restricted_help"
                            )
                        }

                        1 -> WizardPermissionStep(
                            icon = Icons.Rounded.Layers,
                            title = stringResource(R.string.wizard_step_overlay_title),
                            body = stringResource(R.string.wizard_step_overlay_body),
                            granted = uiState.permissions.overlayGranted,
                            actionLabel = stringResource(R.string.button_open_settings),
                            onAction = onOpenOverlaySettings,
                            testTag = "wizard_action_overlay"
                        )

                        2 -> WizardPermissionStep(
                            icon = Icons.Rounded.BatteryChargingFull,
                            title = stringResource(R.string.wizard_step_battery_title),
                            body = stringResource(R.string.wizard_step_battery_body),
                            granted = uiState.permissions.batteryOptimizationIgnored,
                            actionLabel = stringResource(R.string.button_open_settings),
                            onAction = onOpenBatterySettings,
                            testTag = "wizard_action_battery"
                        )

                        3 -> WizardPermissionStep(
                            icon = Icons.Rounded.NotificationsActive,
                            title = stringResource(R.string.wizard_step_notifications_title),
                            body = stringResource(R.string.wizard_step_notifications_body),
                            granted = uiState.permissions.notificationsGranted,
                            actionLabel = stringResource(R.string.button_open_settings),
                            onAction = onRequestOrOpenNotifications,
                            testTag = "wizard_action_notifications"
                        )

                        4 -> SkipBuddyCard {
                            WizardStepHeader(
                                icon = Icons.Rounded.Smartphone,
                                title = stringResource(R.string.wizard_step_autostart_title)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = stringResource(R.string.wizard_step_autostart_xiaomi),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = stringResource(R.string.wizard_step_autostart_samsung),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = onOpenAutostartSettings,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .testTag("wizard_action_autostart"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.wizard_open_app_info),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        5 -> SkipBuddyCard {
                            WizardStepHeader(
                                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                                title = stringResource(R.string.wizard_step_sound_title)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.wizard_step_sound_body),
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            ElderlyChoiceRow(
                                options = listOf(
                                    AppLanguage.ENGLISH to stringResource(R.string.lang_english),
                                    AppLanguage.BANGLA to stringResource(R.string.lang_bangla),
                                    AppLanguage.HINDI to stringResource(R.string.lang_hindi),
                                    AppLanguage.SYSTEM to stringResource(R.string.lang_system)
                                ),
                                selected = uiState.preferences.appLanguage,
                                onSelect = onSetAppLanguage,
                                testTagPrefix = "wizard_lang",
                                stackedFullWidth = true
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onPlayTestSound,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 58.dp)
                                    .testTag("wizard_play_test_sound"),
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
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        else -> SkipBuddyCard(
                            containerColor = EmeraldContainer.copy(alpha = 0.6f),
                            borderColor = EmeraldPrimary.copy(alpha = 0.45f)
                        ) {
                            WizardStepHeader(
                                icon = Icons.Rounded.Lock,
                                title = stringResource(R.string.wizard_step_finish_title)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.wizard_step_finish_body),
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.guide_caregiver_note),
                                style = MaterialTheme.typography.bodyMedium,
                                color = EmeraldOnContainer
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = onFinishHideAndLock,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp)
                                    .testTag("wizard_hide_and_lock_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.wizard_button_hide_and_lock),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = onFinishKeepAutoHideLocked,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 54.dp)
                                    .testTag("wizard_keep_autohide_button"),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, EmeraldPrimary)
                            ) {
                                Text(
                                    text = stringResource(R.string.wizard_button_keep_autohide),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Bottom Back / Next controls
                if (currentStep < totalSteps - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (currentStep > 0) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 56.dp)
                                    .testTag("wizard_back_button"),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, BorderHairline)
                            ) {
                                Text(
                                    text = stringResource(R.string.wizard_back),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextPrimary
                                )
                            }
                        }
                        Button(
                            onClick = { currentStep++ },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
                                .testTag("wizard_next_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.wizard_next),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WizardStepHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(EmeraldContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = EmeraldPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun WizardPermissionStep(
    icon: ImageVector,
    title: String,
    body: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    testTag: String
) {
    SkipBuddyCard {
        WizardStepHeader(icon = icon, title = title)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Dedicated full-width live status banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (granted) EmeraldContainer else StatusOffContainer)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = if (granted) EmeraldPrimary else StatusOffRed,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(
                    if (granted) R.string.permission_granted else R.string.permission_not_granted
                ),
                style = MaterialTheme.typography.titleSmall,
                color = if (granted) EmeraldOnContainer else StatusOffRed
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .testTag(testTag),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                contentColor = Color.White
            )
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
