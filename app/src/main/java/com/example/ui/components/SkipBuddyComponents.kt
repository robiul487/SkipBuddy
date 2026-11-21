package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.HelpAmberAccent
import com.example.ui.theme.HelpAmberBg
import com.example.ui.theme.HelpAmberBorder
import com.example.ui.theme.StatusOffContainer
import com.example.ui.theme.StatusOffRed
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Self-sizing Material 3 rounded card. Never imposes a fixed height so content
 * in English, Bangla, or Hindi always breathes cleanly without clipping or overlap.
 */
@Composable
fun SkipBuddyCard(
    modifier: Modifier = Modifier,
    containerColor: Color = SurfaceCard,
    borderColor: Color = BorderHairline,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            content = content
        )
    }
}

/**
 * Clean numbered Settings section header: solid emerald step badge + full-width title.
 * Eliminates mixed-script numbering overlap/garbling across English, Bangla, and Hindi.
 */
@Composable
fun NumberedSectionHeader(
    number: Int,
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(EmeraldPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = EmeraldPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun LargeToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, role = Role.Switch) {
                onCheckedChange(!checked)
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) TextPrimary else TextSecondary
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = EmeraldPrimary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFCBD5E1),
                disabledCheckedTrackColor = EmeraldPrimary.copy(alpha = 0.45f),
                disabledCheckedThumbColor = Color.White
            )
        )
    }
}

/**
 * Full-width / 2-column graceful segmented choice group.
 * - Never squeezes 3 long labels onto one cramped row at 390dp width.
 * - Uses 2 equal wide columns per row (with an odd last item spanning full width cleanly),
 *   or full-width stacked radio pills when [stackedFullWidth] is true for multi-word labels.
 */
@Composable
fun <T> ElderlyChoiceRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    testTagPrefix: String,
    stackedFullWidth: Boolean = false
) {
    if (stackedFullWidth) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            options.forEach { (value, label) ->
                val isSelected = value == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) EmeraldContainer else SurfaceSubtle)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) EmeraldPrimary else BorderHairline,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable(role = Role.RadioButton) { onSelect(value) }
                        .testTag("${testTagPrefix}_${value.toString().lowercase()}")
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSelected) {
                            Icons.Rounded.CheckCircle
                        } else {
                            Icons.Rounded.RadioButtonUnchecked
                        },
                        contentDescription = null,
                        tint = if (isSelected) EmeraldPrimary else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) EmeraldOnContainer else TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        return
    }

    // 2-column balanced grid: every button gets >= 150dp on a 390dp phone (full width if odd 3rd item)
    val rows = options.chunked(2)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { (value, label) ->
                    val isSelected = value == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) EmeraldPrimary else SurfaceSubtle)
                            .border(
                                width = 1.5.dp,
                                color = if (isSelected) EmeraldPrimary else BorderHairline,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable(role = Role.RadioButton) { onSelect(value) }
                            .testTag("${testTagPrefix}_${value.toString().lowercase()}")
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) Color.White else TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Permission Checklist item with ZERO horizontal squeezing:
 * 1) Full-width permission title (icon + name)
 * 2) Dedicated status chip on its own line below
 * 3) Full-width description below
 * 4) Full-width Open Settings button below
 */
@Composable
fun PermissionChecklistRow(
    title: String,
    description: String,
    granted: Boolean,
    openButtonLabel: String,
    onOpenSettings: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (granted) EmeraldContainer.copy(alpha = 0.42f)
                else StatusOffContainer.copy(alpha = 0.72f)
            )
            .border(
                width = 1.dp,
                color = if (granted) EmeraldPrimary.copy(alpha = 0.35f) else StatusOffRed.copy(alpha = 0.32f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        // Line 1: Full-width icon + permission title (never squeezed by status badge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                contentDescription = if (granted) {
                    stringResource(R.string.permission_granted)
                } else {
                    stringResource(R.string.permission_not_granted)
                },
                tint = if (granted) EmeraldPrimary else StatusOffRed,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Line 2: Status chip on its own dedicated line
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (granted) EmeraldContainer else Color.White)
                .border(
                    width = 1.dp,
                    color = if (granted) EmeraldPrimary.copy(alpha = 0.4f) else StatusOffRed.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(50)
                )
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Text(
                text = stringResource(
                    if (granted) R.string.permission_granted else R.string.permission_not_granted
                ),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (granted) EmeraldOnContainer else StatusOffRed
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Line 3: Full-width description
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Line 4: Full-width action button
        if (!granted) {
            Button(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag(testTag),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = openButtonLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(testTag),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.2.dp, EmeraldPrimary.copy(alpha = 0.5f))
            ) {
                Text(
                    text = openButtonLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = EmeraldPrimary
                )
            }
        }
    }
}

/**
 * Android 13+ "Restricted setting" one-time allow help card.
 * Shown in 3 places:
 * (a) Setup Wizard Accessibility step (with Open App Settings button)
 * (b) Guide tab under the Permission Checklist
 * (c) Home tab under the Enable card
 */
@Composable
fun RestrictedSettingHelpCard(
    onOpenAppSettings: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    SkipBuddyCard(
        modifier = modifier.testTag(testTag),
        containerColor = HelpAmberBg,
        borderColor = HelpAmberBorder
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = stringResource(R.string.restricted_setting_title),
                tint = HelpAmberAccent,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.restricted_setting_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = HelpAmberAccent,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.restricted_setting_body),
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedButton(
            onClick = onOpenAppSettings,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("${testTag}_open_app_settings"),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, HelpAmberAccent)
        ) {
            Text(
                text = stringResource(R.string.button_open_app_settings),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = HelpAmberAccent
            )
        }
    }
}
