package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.PauseCircleFilled
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.SkipBuddyRepository
import com.example.model.AdOutcome
import com.example.model.DailyStats
import com.example.model.RecentActivityEntry
import com.example.ui.SkipBuddyUiState
import com.example.ui.components.RestrictedSettingHelpCard
import com.example.ui.components.SkipBuddyCard
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MetricBlue
import com.example.ui.theme.MetricBlueBg
import com.example.ui.theme.MetricBlueBorder
import com.example.ui.theme.MetricGreen
import com.example.ui.theme.MetricGreenBg
import com.example.ui.theme.MetricGreenBorder
import com.example.ui.theme.MetricOrange
import com.example.ui.theme.MetricOrangeBg
import com.example.ui.theme.MetricOrangeBorder
import com.example.ui.theme.MetricRed
import com.example.ui.theme.MetricRedBg
import com.example.ui.theme.MetricRedBorder
import com.example.ui.theme.StatusOffBorder
import com.example.ui.theme.StatusOffContainer
import com.example.ui.theme.StatusOffRed
import com.example.ui.theme.StatusPausedContainer
import com.example.ui.theme.StatusPausedSlate
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.LocaleHelper
import java.util.Locale

@Composable
fun HomeScreen(
    uiState: SkipBuddyUiState,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onToggleAutoSkip: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = uiState.preferences
    val todayKey = remember { SkipBuddyRepository.dateKeyFor() }
    val todayStats = prefs.todayStats(todayKey)
    val last7Keys = remember(todayKey) { SkipBuddyRepository.lastSevenDateKeys() }
    val displayLocale = prefs.appLanguage.toLocale()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 680.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. STATUS CARD AT THE VERY TOP (green if ON, red/grey if OFF)
        item(key = "status_card") {
            ServiceStatusCard(
                serviceOn = uiState.isServiceActive,
                autoSkipEnabled = prefs.autoSkipEnabled
            )
        }

        // 2. CONDITIONAL WARNING + HUGE "ENABLE SKIPBUDDY" BUTTON + RESTRICTED SETTING HELP (hidden when ON)
        if (!uiState.isServiceActive) {
            item(key = "enable_warning_card") {
                EnableWarningCard(onEnableClick = onOpenAccessibilitySettings)
            }
            item(key = "home_restricted_setting_help") {
                RestrictedSettingHelpCard(
                    onOpenAppSettings = onOpenAppSettings,
                    testTag = "home_restricted_help"
                )
            }
        }

        // 3. HUGE PAUSE / RESUME AUTO-SKIP TOGGLE
        item(key = "pause_resume_card") {
            PauseResumeAutoSkipCard(
                autoSkipEnabled = prefs.autoSkipEnabled,
                onToggle = onToggleAutoSkip
            )
        }

        // 4. STATS GRID (2-column rounded cards + full-width Success Rate + Last Activity)
        item(key = "stats_grid") {
            DashboardStatsSection(
                todayStats = todayStats,
                totalDetected = prefs.totalDetected,
                totalSkipped = prefs.totalSkipped,
                totalCouldNotSkip = prefs.totalCouldNotSkip,
                totalUnskippable = prefs.totalUnskippable,
                totalFailed = prefs.totalFailed,
                totalMutedSeconds = prefs.totalMutedSeconds,
                allTimeSuccessPercent = prefs.allTimeSuccessPercent,
                todaySuccessPercent = prefs.todaySuccessPercent(todayKey),
                lastSkipTimeMs = prefs.lastSkipTimeMs,
                lastAdTimeMs = prefs.lastAdTimeMs,
                locale = displayLocale
            )
        }

        // 5. LAST 7 DAYS MINI BAR CHART (pure Compose bars, no chart library)
        item(key = "last_7_days_chart") {
            Last7DaysBarChartCard(
                dateKeys = last7Keys,
                todayKey = todayKey,
                dailyStatsMap = prefs.dailyStats,
                locale = displayLocale
            )
        }

        // 6. RECENT ACTIVITY LIST WITH RESULT ICONS
        item(key = "recent_header") {
            Text(
                text = stringResource(R.string.recent_activity_title),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }

        if (prefs.recentActivity.isEmpty()) {
            item(key = "recent_empty") {
                SkipBuddyCard {
                    Text(
                        text = stringResource(R.string.recent_activity_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            items(
                items = prefs.recentActivity.take(20),
                key = { "${it.timestampMs}_${it.outcome.name}" }
            ) { entry ->
                RecentActivityRowCard(entry = entry, locale = displayLocale)
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun ServiceStatusCard(
    serviceOn: Boolean,
    autoSkipEnabled: Boolean
) {
    val bgColor = when {
        !serviceOn -> StatusOffContainer
        autoSkipEnabled -> EmeraldContainer
        else -> StatusPausedContainer
    }
    val borderColor = when {
        !serviceOn -> StatusOffBorder
        autoSkipEnabled -> EmeraldPrimary.copy(alpha = 0.5f)
        else -> BorderHairline
    }
    val accentColor = when {
        !serviceOn -> StatusOffRed
        autoSkipEnabled -> EmeraldPrimary
        else -> StatusPausedSlate
    }
    val titleRes = when {
        !serviceOn -> R.string.service_status_off_title
        autoSkipEnabled -> R.string.service_status_on_title
        else -> R.string.service_status_paused_title
    }
    val subtitleRes = when {
        !serviceOn -> R.string.service_status_off_subtitle
        autoSkipEnabled -> R.string.service_status_on_subtitle
        else -> R.string.service_status_paused_subtitle
    }

    SkipBuddyCard(
        containerColor = bgColor,
        borderColor = borderColor,
        modifier = Modifier.testTag("home_status_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        !serviceOn -> Icons.Rounded.ErrorOutline
                        autoSkipEnabled -> Icons.Rounded.Shield
                        else -> Icons.Rounded.PauseCircleFilled
                    },
                    contentDescription = stringResource(titleRes),
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (serviceOn && autoSkipEnabled) EmeraldOnContainer else accentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(subtitleRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun EnableWarningCard(onEnableClick: () -> Unit) {
    SkipBuddyCard(
        containerColor = StatusOffContainer,
        borderColor = StatusOffRed.copy(alpha = 0.45f),
        modifier = Modifier.testTag("home_enable_warning_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = stringResource(R.string.warning_enable_title),
                tint = StatusOffRed,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.warning_enable_title),
                style = MaterialTheme.typography.titleLarge,
                color = StatusOffRed,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.warning_enable_body),
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onEnableClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .testTag("enable_skipbuddy_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                contentColor = Color.White
            )
        ) {
            Text(
                text = stringResource(R.string.button_enable_skipbuddy),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PauseResumeAutoSkipCard(
    autoSkipEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    SkipBuddyCard(modifier = Modifier.testTag("home_pause_resume_card")) {
        Text(
            text = stringResource(
                if (autoSkipEnabled) R.string.toggle_autoskip_active else R.string.toggle_autoskip_paused
            ),
            style = MaterialTheme.typography.titleLarge,
            color = if (autoSkipEnabled) EmeraldPrimary else StatusOffRed
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = { onToggle(!autoSkipEnabled) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .testTag("home_pause_resume_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (autoSkipEnabled) StatusPausedSlate else EmeraldPrimary,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = if (autoSkipEnabled) {
                    Icons.Rounded.PauseCircleFilled
                } else {
                    Icons.Rounded.PlayCircleFilled
                },
                contentDescription = null,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(
                    if (autoSkipEnabled) R.string.button_pause_autoskip else R.string.button_resume_autoskip
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DashboardStatsSection(
    todayStats: DailyStats,
    totalDetected: Int,
    totalSkipped: Int,
    totalCouldNotSkip: Int,
    totalUnskippable: Int,
    totalFailed: Int,
    totalMutedSeconds: Long,
    allTimeSuccessPercent: Int?,
    todaySuccessPercent: Int?,
    lastSkipTimeMs: Long,
    lastAdTimeMs: Long,
    locale: Locale
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Row 1: Ads Detected (Blue) | Ads Skipped (Green)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricTileCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.stat_ads_detected),
                todayValue = todayStats.detected.toString(),
                totalValue = totalDetected.toString(),
                detailLine = null,
                icon = Icons.Rounded.Radar,
                accentColor = MetricBlue,
                bgColor = MetricBlueBg,
                borderColor = MetricBlueBorder,
                testTag = "stat_card_detected"
            )
            MetricTileCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.stat_ads_skipped),
                todayValue = todayStats.skipped.toString(),
                totalValue = totalSkipped.toString(),
                detailLine = null,
                icon = Icons.Rounded.FastForward,
                accentColor = MetricGreen,
                bgColor = MetricGreenBg,
                borderColor = MetricGreenBorder,
                testTag = "stat_card_skipped"
            )
        }

        // Row 2: Could Not Skip (Red) | Unskippable Muted (Orange)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricTileCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.stat_could_not_skip),
                todayValue = todayStats.couldNotSkip.toString(),
                totalValue = totalCouldNotSkip.toString(),
                detailLine = stringResource(
                    R.string.stat_could_not_skip_split,
                    totalUnskippable,
                    totalFailed
                ),
                icon = Icons.Rounded.Cancel,
                accentColor = MetricRed,
                bgColor = MetricRedBg,
                borderColor = MetricRedBorder,
                testTag = "stat_card_could_not_skip"
            )
            MetricTileCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.stat_unskippable_muted),
                todayValue = todayStats.unskippable.toString(),
                totalValue = totalUnskippable.toString(),
                detailLine = stringResource(
                    R.string.stat_muted_time_label,
                    LocaleHelper.formatDurationMmSs(totalMutedSeconds)
                ),
                icon = Icons.AutoMirrored.Rounded.VolumeOff,
                accentColor = MetricOrange,
                bgColor = MetricOrangeBg,
                borderColor = MetricOrangeBorder,
                testTag = "stat_card_muted"
            )
        }

        // Full-width SUCCESS RATE card + Last Activity lines
        SkipBuddyCard(modifier = Modifier.testTag("stat_card_success_rate")) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.stat_success_rate),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            R.string.stat_success_rate_all_time,
                            totalSkipped,
                            totalDetected
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Text(
                    text = allTimeSuccessPercent?.let { "$it%" } ?: "—",
                    style = MaterialTheme.typography.displaySmall,
                    color = EmeraldPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (allTimeSuccessPercent ?: 0) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(50)),
                color = EmeraldPrimary,
                trackColor = SurfaceSubtle,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(
                    R.string.stat_success_rate_today,
                    todaySuccessPercent?.let { "$it%" } ?: "—"
                ),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderHairline)
            Spacer(modifier = Modifier.height(10.dp))

            val neverText = stringResource(R.string.stat_never)
            val lastSkipStr = if (lastSkipTimeMs > 0L) {
                LocaleHelper.formatTimeShort(lastSkipTimeMs, locale)
            } else {
                neverText
            }
            val lastAdStr = if (lastAdTimeMs > 0L) {
                LocaleHelper.formatTimeShort(lastAdTimeMs, locale)
            } else {
                neverText
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.stat_last_activity_skip, lastSkipStr),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = stringResource(R.string.stat_last_activity_ad, lastAdStr),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * 2-column metric tile: icon badge on top, full-width title below so labels never split mid-word at 390dp.
 */
@Composable
private fun MetricTileCard(
    modifier: Modifier,
    title: String,
    todayValue: String,
    totalValue: String,
    detailLine: String?,
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color,
    borderColor: Color,
    testTag: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(14.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = stringResource(R.string.stat_today_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = todayValue,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.stat_total_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = totalValue,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
        if (!detailLine.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = detailLine,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun Last7DaysBarChartCard(
    dateKeys: List<String>,
    todayKey: String,
    dailyStatsMap: Map<String, DailyStats>,
    locale: Locale
) {
    val counts = dateKeys.map { key -> dailyStatsMap[key]?.skipped ?: 0 }
    val maxVal = (counts.maxOrNull() ?: 0).coerceAtLeast(1)

    SkipBuddyCard(modifier = Modifier.testTag("chart_last_7_days")) {
        Text(
            text = stringResource(R.string.chart_last_7_days_title),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(14.dp))
        // No fixed outer height — sizes cleanly to bars + localized labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            dateKeys.forEachIndexed { idx, key ->
                val count = counts[idx]
                val barFraction = if (count <= 0) 0.08f else (count.toFloat() / maxVal).coerceIn(0.16f, 1f)
                val isToday = key == todayKey
                val dayLabel = if (isToday) {
                    stringResource(R.string.chart_today_short)
                } else {
                    LocaleHelper.formatDayLabel(key, locale)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) EmeraldPrimary else TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .height(88.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((88f * barFraction).dp)
                                .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                                .background(
                                    when {
                                        count == 0 -> SurfaceSubtle
                                        isToday -> EmeraldPrimary
                                        else -> EmeraldPrimary.copy(alpha = 0.55f)
                                    }
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) EmeraldPrimary else TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentActivityRowCard(
    entry: RecentActivityEntry,
    locale: Locale
) {
    val (icon, tint, bg, labelRes) = when (entry.outcome) {
        AdOutcome.SKIPPED -> Quadruple(
            Icons.Rounded.CheckCircle,
            MetricGreen,
            MetricGreenBg,
            R.string.outcome_skipped
        )
        AdOutcome.UNSKIPPABLE -> Quadruple(
            Icons.AutoMirrored.Rounded.VolumeOff,
            MetricOrange,
            MetricOrangeBg,
            R.string.outcome_unskippable
        )
        AdOutcome.FAILED -> Quadruple(
            Icons.Rounded.Cancel,
            MetricRed,
            MetricRedBg,
            R.string.outcome_failed
        )
    }

    SkipBuddyCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).padding(end = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = stringResource(labelRes),
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
            }
            Text(
                text = LocaleHelper.formatTimeShort(entry.timestampMs, locale),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
