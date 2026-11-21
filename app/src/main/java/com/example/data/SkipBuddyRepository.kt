package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AdOutcome
import com.example.model.AppLanguage
import com.example.model.ClickDelayOption
import com.example.model.DailyStats
import com.example.model.FloatingOpacity
import com.example.model.FloatingSide
import com.example.model.FloatingSize
import com.example.model.FloatingVisibility
import com.example.model.RecentActivityEntry
import com.example.model.SkipBuddyPreferences
import com.example.model.SkipFeedbackMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Context.skipBuddyDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "skipbuddy_prefs"
)

class SkipBuddyRepository private constructor(private val appContext: Context) {

    private val dataStore = appContext.skipBuddyDataStore

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    fun setServiceConnected(connected: Boolean) {
        _isServiceConnected.value = connected
    }

    val preferencesFlow: Flow<SkipBuddyPreferences> = dataStore.data.map { prefs ->
        SkipBuddyPreferences(
            autoSkipEnabled = prefs[KEY_AUTO_SKIP] ?: true,
            clickDelay = ClickDelayOption.fromName(prefs[KEY_CLICK_DELAY]),
            muteUnskippableAds = prefs[KEY_MUTE_UNSKIPPABLE] ?: true,

            floatingVisibility = FloatingVisibility.fromName(prefs[KEY_FLOATING_VISIBILITY]),
            floatingLocked = prefs[KEY_FLOATING_LOCKED] ?: true,
            floatingSize = FloatingSize.fromName(prefs[KEY_FLOATING_SIZE]),
            floatingSide = FloatingSide.fromName(prefs[KEY_FLOATING_SIDE]),
            floatingOpacity = FloatingOpacity.fromName(prefs[KEY_FLOATING_OPACITY]),
            floatingYFraction = (prefs[KEY_FLOATING_Y_FRACTION] ?: 0.5f).coerceIn(0.12f, 0.88f),

            feedbackMode = SkipFeedbackMode.fromName(prefs[KEY_FEEDBACK_MODE]),
            showAdSkippedMessage = prefs[KEY_SHOW_AD_SKIPPED_MSG] ?: false,

            appLanguage = AppLanguage.fromCode(prefs[KEY_APP_LANGUAGE]),
            voiceAnnouncementEnabled = prefs[KEY_VOICE_ANNOUNCEMENT] ?: true,
            showAdBannerEnabled = prefs[KEY_SHOW_AD_BANNER] ?: true,

            setupWizardCompleted = prefs[KEY_WIZARD_COMPLETED] ?: false,
            debugLogEnabled = prefs[KEY_DEBUG_LOG_ENABLED] ?: false,

            dailyStats = decodeDailyStats(prefs[KEY_DAILY_STATS_SERIALIZED]),
            totalDetected = prefs[KEY_TOTAL_DETECTED] ?: 0,
            totalSkipped = prefs[KEY_TOTAL_SKIPPED] ?: 0,
            totalUnskippable = prefs[KEY_TOTAL_UNSKIPPABLE] ?: 0,
            totalFailed = prefs[KEY_TOTAL_FAILED] ?: 0,
            totalMutedSeconds = prefs[KEY_TOTAL_MUTED_SECONDS] ?: 0L,
            lastSkipTimeMs = prefs[KEY_LAST_SKIP_TIME] ?: 0L,
            lastAdTimeMs = prefs[KEY_LAST_AD_TIME] ?: 0L,
            recentActivity = decodeRecentActivity(prefs[KEY_RECENT_ACTIVITY])
        )
    }

    suspend fun setAutoSkipEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_SKIP] = enabled }
    }

    suspend fun setClickDelay(option: ClickDelayOption) {
        dataStore.edit { it[KEY_CLICK_DELAY] = option.name }
    }

    suspend fun setMuteUnskippableAds(enabled: Boolean) {
        dataStore.edit { it[KEY_MUTE_UNSKIPPABLE] = enabled }
    }

    suspend fun setFloatingVisibility(visibility: FloatingVisibility) {
        dataStore.edit { it[KEY_FLOATING_VISIBILITY] = visibility.name }
    }

    suspend fun setFloatingLocked(locked: Boolean) {
        dataStore.edit { it[KEY_FLOATING_LOCKED] = locked }
    }

    suspend fun setFloatingSize(size: FloatingSize) {
        dataStore.edit { it[KEY_FLOATING_SIZE] = size.name }
    }

    suspend fun setFloatingSide(side: FloatingSide) {
        dataStore.edit { it[KEY_FLOATING_SIDE] = side.name }
    }

    suspend fun setFloatingOpacity(opacity: FloatingOpacity) {
        dataStore.edit { it[KEY_FLOATING_OPACITY] = opacity.name }
    }

    suspend fun setFloatingPosition(side: FloatingSide, yFraction: Float) {
        dataStore.edit {
            it[KEY_FLOATING_SIDE] = side.name
            it[KEY_FLOATING_Y_FRACTION] = yFraction.coerceIn(0.12f, 0.88f)
        }
    }

    suspend fun resetFloatingPosition() {
        dataStore.edit {
            it[KEY_FLOATING_SIDE] = FloatingSide.RIGHT.name
            it[KEY_FLOATING_Y_FRACTION] = 0.5f
        }
    }

    suspend fun setFeedbackMode(mode: SkipFeedbackMode) {
        dataStore.edit { it[KEY_FEEDBACK_MODE] = mode.name }
    }

    suspend fun setShowAdSkippedMessage(show: Boolean) {
        dataStore.edit { it[KEY_SHOW_AD_SKIPPED_MSG] = show }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        dataStore.edit { it[KEY_APP_LANGUAGE] = language.code }
    }

    suspend fun setVoiceAnnouncementEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_VOICE_ANNOUNCEMENT] = enabled }
    }

    suspend fun setShowAdBannerEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_SHOW_AD_BANNER] = enabled }
    }

    suspend fun setSetupWizardCompleted(completed: Boolean) {
        dataStore.edit { it[KEY_WIZARD_COMPLETED] = completed }
    }

    suspend fun setDebugLogEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_DEBUG_LOG_ENABLED] = enabled }
    }

    suspend fun finishWizardHideAndLock() {
        dataStore.edit {
            it[KEY_FLOATING_VISIBILITY] = FloatingVisibility.HIDDEN.name
            it[KEY_FLOATING_LOCKED] = true
            it[KEY_WIZARD_COMPLETED] = true
        }
    }

    suspend fun finishWizardKeepAutoHideLocked() {
        dataStore.edit {
            it[KEY_FLOATING_VISIBILITY] = FloatingVisibility.AUTO_HIDE.name
            it[KEY_FLOATING_LOCKED] = true
            it[KEY_WIZARD_COMPLETED] = true
        }
    }

    /**
     * Per-ad-session counting rule:
     * Called ONCE when Ad State transitions OFF -> ON.
     */
    suspend fun recordAdSessionStart(nowMs: Long = System.currentTimeMillis()) {
        val dateKey = dateKeyFor(nowMs)
        dataStore.edit { prefs ->
            val map = decodeDailyStats(prefs[KEY_DAILY_STATS_SERIALIZED]).toMutableMap()
            val current = map[dateKey] ?: DailyStats(dateKey = dateKey)
            map[dateKey] = current.copy(detected = current.detected + 1)
            pruneOldDays(map)
            prefs[KEY_DAILY_STATS_SERIALIZED] = encodeDailyStats(map)
            prefs[KEY_TOTAL_DETECTED] = (prefs[KEY_TOTAL_DETECTED] ?: 0) + 1
            prefs[KEY_LAST_AD_TIME] = nowMs
        }
    }

    /**
     * Per-ad-session counting rule:
     * Called ONCE when the ad session ends (verified Skip, Unskippable, or Failed).
     */
    suspend fun recordAdSessionOutcome(
        outcome: AdOutcome,
        mutedSeconds: Long,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val dateKey = dateKeyFor(nowMs)
        val safeMuted = mutedSeconds.coerceAtLeast(0L)
        dataStore.edit { prefs ->
            val map = decodeDailyStats(prefs[KEY_DAILY_STATS_SERIALIZED]).toMutableMap()
            val current = map[dateKey] ?: DailyStats(dateKey = dateKey)
            val updated = when (outcome) {
                AdOutcome.SKIPPED -> current.copy(
                    skipped = current.skipped + 1,
                    mutedSeconds = current.mutedSeconds + safeMuted
                )
                AdOutcome.UNSKIPPABLE -> current.copy(
                    unskippable = current.unskippable + 1,
                    mutedSeconds = current.mutedSeconds + safeMuted
                )
                AdOutcome.FAILED -> current.copy(
                    failed = current.failed + 1,
                    mutedSeconds = current.mutedSeconds + safeMuted
                )
            }
            map[dateKey] = updated
            pruneOldDays(map)
            prefs[KEY_DAILY_STATS_SERIALIZED] = encodeDailyStats(map)

            when (outcome) {
                AdOutcome.SKIPPED -> {
                    prefs[KEY_TOTAL_SKIPPED] = (prefs[KEY_TOTAL_SKIPPED] ?: 0) + 1
                    prefs[KEY_LAST_SKIP_TIME] = nowMs
                }
                AdOutcome.UNSKIPPABLE -> {
                    prefs[KEY_TOTAL_UNSKIPPABLE] = (prefs[KEY_TOTAL_UNSKIPPABLE] ?: 0) + 1
                }
                AdOutcome.FAILED -> {
                    prefs[KEY_TOTAL_FAILED] = (prefs[KEY_TOTAL_FAILED] ?: 0) + 1
                }
            }
            if (safeMuted > 0L) {
                prefs[KEY_TOTAL_MUTED_SECONDS] = (prefs[KEY_TOTAL_MUTED_SECONDS] ?: 0L) + safeMuted
            }

            val recent = decodeRecentActivity(prefs[KEY_RECENT_ACTIVITY]).toMutableList()
            recent.add(0, RecentActivityEntry(timestampMs = nowMs, outcome = outcome))
            val trimmed = recent.take(MAX_RECENT_ENTRIES)
            prefs[KEY_RECENT_ACTIVITY] = encodeRecentActivity(trimmed)
        }
    }

    /**
     * Resets today's aggregate counters cleanly.
     */
    suspend fun resetTodayStats(nowMs: Long = System.currentTimeMillis()) {
        val todayKey = dateKeyFor(nowMs)
        dataStore.edit { prefs ->
            val map = decodeDailyStats(prefs[KEY_DAILY_STATS_SERIALIZED]).toMutableMap()
            map.remove(todayKey)
            prefs[KEY_DAILY_STATS_SERIALIZED] = encodeDailyStats(map)
        }
    }

    /**
     * Resets ALL counters, daily history, totals, last activity times, and recent log.
     */
    suspend fun resetAllStats() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_DAILY_STATS_SERIALIZED)
            prefs[KEY_TOTAL_DETECTED] = 0
            prefs[KEY_TOTAL_SKIPPED] = 0
            prefs[KEY_TOTAL_UNSKIPPABLE] = 0
            prefs[KEY_TOTAL_FAILED] = 0
            prefs[KEY_TOTAL_MUTED_SECONDS] = 0L
            prefs[KEY_LAST_SKIP_TIME] = 0L
            prefs[KEY_LAST_AD_TIME] = 0L
            prefs.remove(KEY_RECENT_ACTIVITY)
        }
    }

    suspend fun clearRecentActivity() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_RECENT_ACTIVITY)
        }
    }

    companion object {
        private const val MAX_RECENT_ENTRIES = 40
        private const val MAX_DAILY_HISTORY_DAYS = 30

        private val KEY_AUTO_SKIP = booleanPreferencesKey("auto_skip_enabled")
        private val KEY_CLICK_DELAY = stringPreferencesKey("click_delay")
        private val KEY_MUTE_UNSKIPPABLE = booleanPreferencesKey("mute_unskippable_ads")

        private val KEY_FLOATING_VISIBILITY = stringPreferencesKey("floating_visibility")
        private val KEY_FLOATING_LOCKED = booleanPreferencesKey("floating_locked")
        private val KEY_FLOATING_SIZE = stringPreferencesKey("floating_size")
        private val KEY_FLOATING_SIDE = stringPreferencesKey("floating_side")
        private val KEY_FLOATING_OPACITY = stringPreferencesKey("floating_opacity")
        private val KEY_FLOATING_Y_FRACTION = floatPreferencesKey("floating_y_fraction")

        private val KEY_FEEDBACK_MODE = stringPreferencesKey("feedback_mode")
        private val KEY_SHOW_AD_SKIPPED_MSG = booleanPreferencesKey("show_ad_skipped_msg")

        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_VOICE_ANNOUNCEMENT = booleanPreferencesKey("voice_announcement_enabled")
        private val KEY_SHOW_AD_BANNER = booleanPreferencesKey("show_ad_banner_enabled")

        private val KEY_WIZARD_COMPLETED = booleanPreferencesKey("setup_wizard_completed")
        private val KEY_DEBUG_LOG_ENABLED = booleanPreferencesKey("debug_log_enabled")

        private val KEY_DAILY_STATS_SERIALIZED = stringPreferencesKey("daily_stats_v2")
        private val KEY_TOTAL_DETECTED = intPreferencesKey("total_detected")
        private val KEY_TOTAL_SKIPPED = intPreferencesKey("total_skipped")
        private val KEY_TOTAL_UNSKIPPABLE = intPreferencesKey("total_unskippable")
        private val KEY_TOTAL_FAILED = intPreferencesKey("total_failed")
        private val KEY_TOTAL_MUTED_SECONDS = longPreferencesKey("total_muted_seconds")
        private val KEY_LAST_SKIP_TIME = longPreferencesKey("last_skip_time_ms")
        private val KEY_LAST_AD_TIME = longPreferencesKey("last_ad_time_ms")
        private val KEY_RECENT_ACTIVITY = stringPreferencesKey("recent_activity_v2")

        @Volatile
        private var INSTANCE: SkipBuddyRepository? = null

        fun getInstance(context: Context): SkipBuddyRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SkipBuddyRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun dateKeyFor(timeMs: Long = System.currentTimeMillis()): String {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return fmt.format(Date(timeMs))
        }

        fun lastSevenDateKeys(nowMs: Long = System.currentTimeMillis()): List<String> {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
            val result = mutableListOf<String>()
            cal.add(Calendar.DAY_OF_YEAR, -6)
            repeat(7) {
                result.add(fmt.format(cal.time))
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return result
        }

        private fun pruneOldDays(map: MutableMap<String, DailyStats>) {
            if (map.size <= MAX_DAILY_HISTORY_DAYS) return
            val sortedKeys = map.keys.sorted()
            val toRemove = sortedKeys.take(map.size - MAX_DAILY_HISTORY_DAYS)
            toRemove.forEach { map.remove(it) }
        }

        internal fun encodeDailyStats(map: Map<String, DailyStats>): String {
            return map.values.sortedBy { it.dateKey }.joinToString(";") { d ->
                "${d.dateKey},${d.detected},${d.skipped},${d.unskippable},${d.failed},${d.mutedSeconds}"
            }
        }

        internal fun decodeDailyStats(raw: String?): Map<String, DailyStats> {
            if (raw.isNullOrBlank()) return emptyMap()
            val result = LinkedHashMap<String, DailyStats>()
            raw.split(";").forEach { entry ->
                val parts = entry.split(",")
                if (parts.size >= 6) {
                    val key = parts[0]
                    result[key] = DailyStats(
                        dateKey = key,
                        detected = parts[1].toIntOrNull() ?: 0,
                        skipped = parts[2].toIntOrNull() ?: 0,
                        unskippable = parts[3].toIntOrNull() ?: 0,
                        failed = parts[4].toIntOrNull() ?: 0,
                        mutedSeconds = parts[5].toLongOrNull() ?: 0L
                    )
                }
            }
            return result
        }

        internal fun encodeRecentActivity(list: List<RecentActivityEntry>): String {
            return list.joinToString(";") { "${it.timestampMs}:${it.outcome.name}" }
        }

        internal fun decodeRecentActivity(raw: String?): List<RecentActivityEntry> {
            if (raw.isNullOrBlank()) return emptyList()
            return raw.split(";").mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val ts = parts[0].toLongOrNull() ?: return@mapNotNull null
                    RecentActivityEntry(ts, AdOutcome.fromName(parts[1]))
                } else {
                    null
                }
            }
        }
    }
}
