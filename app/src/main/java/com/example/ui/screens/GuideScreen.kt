package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.SkipBuddyUiState
import com.example.ui.components.PermissionChecklistRow
import com.example.ui.components.RestrictedSettingHelpCard
import com.example.ui.components.SkipBuddyCard
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GuideScreen(
    uiState: SkipBuddyUiState,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onReopenSetupWizard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val perms = uiState.permissions

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 680.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Caregiver one-time setup note + Re-open Setup Wizard button
        item(key = "caregiver_banner") {
            SkipBuddyCard(
                containerColor = EmeraldContainer.copy(alpha = 0.7f),
                borderColor = EmeraldPrimary.copy(alpha = 0.4f),
                modifier = Modifier.testTag("guide_caregiver_card")
            ) {
                Text(
                    text = stringResource(R.string.guide_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = EmeraldOnContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.guide_caregiver_note),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onReopenSetupWizard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("guide_reopen_wizard_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.button_reopen_wizard),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3 Simple Steps ONLY (self-sizing, zero fixed heights)
        item(key = "guide_steps") {
            SkipBuddyCard(modifier = Modifier.testTag("guide_steps_card")) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    GuideStepItem(
                        icon = ImageVector.vectorResource(R.drawable.ic_accessibility),
                        title = stringResource(R.string.guide_step_1_title),
                        body = stringResource(R.string.guide_step_1_body)
                    )
                    GuideStepItem(
                        icon = ImageVector.vectorResource(R.drawable.ic_ondemand_video),
                        title = stringResource(R.string.guide_step_2_title),
                        body = stringResource(R.string.guide_step_2_body)
                    )
                    GuideStepItem(
                        icon = ImageVector.vectorResource(R.drawable.ic_fast_forward),
                        title = stringResource(R.string.guide_step_3_title),
                        body = stringResource(R.string.guide_step_3_body)
                    )
                }
            }
        }

        // Permission Checklist (in sync with Settings Card 4)
        item(key = "permission_checklist") {
            SkipBuddyCard(modifier = Modifier.testTag("guide_permissions_card")) {
                Text(
                    text = stringResource(R.string.permissions_checklist_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PermissionChecklistRow(
                        title = stringResource(R.string.perm_accessibility_title),
                        description = stringResource(R.string.perm_accessibility_desc),
                        granted = uiState.isServiceActive,
                        openButtonLabel = stringResource(R.string.button_open_settings),
                        onOpenSettings = onOpenAccessibilitySettings,
                        testTag = "guide_perm_accessibility"
                    )
                    PermissionChecklistRow(
                        title = stringResource(R.string.perm_overlay_title),
                        description = stringResource(R.string.perm_overlay_desc),
                        granted = perms.overlayGranted,
                        openButtonLabel = stringResource(R.string.button_open_settings),
                        onOpenSettings = onOpenOverlaySettings,
                        testTag = "guide_perm_overlay"
                    )
                    PermissionChecklistRow(
                        title = stringResource(R.string.perm_battery_title),
                        description = stringResource(R.string.perm_battery_desc),
                        granted = perms.batteryOptimizationIgnored,
                        openButtonLabel = stringResource(R.string.button_open_settings),
                        onOpenSettings = onOpenBatterySettings,
                        testTag = "guide_perm_battery"
                    )
                    PermissionChecklistRow(
                        title = stringResource(R.string.perm_notifications_title),
                        description = stringResource(R.string.perm_notifications_desc),
                        granted = perms.notificationsGranted,
                        openButtonLabel = stringResource(R.string.button_open_settings),
                        onOpenSettings = onOpenNotificationSettings,
                        testTag = "guide_perm_notifications"
                    )
                }
            }
        }

        // Android 13+ Restricted Setting Help Block directly under Permission Checklist
        item(key = "guide_restricted_setting_help") {
            RestrictedSettingHelpCard(
                onOpenAppSettings = onOpenAppSettings,
                testTag = "guide_restricted_help"
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun GuideStepItem(
    icon: ImageVector,
    title: String,
    body: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(EmeraldContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = EmeraldPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
