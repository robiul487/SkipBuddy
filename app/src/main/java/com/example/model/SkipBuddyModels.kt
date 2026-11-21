package com.example.model

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.example.R
import java.util.Locale

/**
 * Extensible 3-language registry.
 * Adding a future language requires only:
 * 1) a new res/values-<lang>/strings.xml
 * 2) a new res/raw/ad_wait_<lang>.mp3
 * 3) one enum entry below (zero detection/UI logic changes).
 */
enum class AppLanguage(
    val code: String,
    @StringRes val labelRes: Int,
    @RawRes val audioRes: Int?,
    val fallbackAnnouncementText: String?
) {
    BANGLA(
        code = "bn",
        labelRes = R.string.lang_bangla,
        audioRes = R.raw.ad_wait_bn,
        fallbackAnnouncementText = "বিজ্ঞাপন চলছে, একটু অপেক্ষা করুন"
    ),
    ENGLISH(
        code = "en",
        labelRes = R.string.lang_english,
        audioRes = R.raw.ad_wait_en,
        fallbackAnnouncementText = "Advertisement is playing, please wait a moment"
    ),
    HINDI(
        code = "hi",
        labelRes = R.string.lang_hindi,
        audioRes = R.raw.ad_wait_hi,
        fallbackAnnouncementText = "विज्ञापन चल रहा है, कृपया थोड़ी देर प्रतीक्षा करें"
    ),
    SYSTEM(
        code = "system",
        labelRes = R.string.lang_system,
        audioRes = null,
        fallbackAnnouncementText = null
    );

    fun resolveConcrete(systemLocale: Locale = Locale.getDefault()): AppLanguage {
        if (this != SYSTEM) return this
        return when (systemLocale.language.lowercase(Locale.ROOT)) {
            "bn" -> BANGLA
            "en" -> ENGLISH
            "hi" -> HINDI
            else -> ENGLISH
        }
    }

    fun toLocale(systemLocale: Locale = Locale.getDefault()): Locale {
        val concrete = resolveConcrete(systemLocale)
        return Locale(concrete.code)
    }

    @RawRes
    fun resolveAudioRes(systemLocale: Locale = Locale.getDefault()): Int {
        return resolveConcrete(systemLocale).audioRes ?: R.raw.ad_wait_en
    }

    fun resolveAnnouncementText(systemLocale: Locale = Locale.getDefault()): String {
        return resolveConcrete(systemLocale).fallbackAnnouncementText
            ?: ENGLISH.fallbackAnnouncementText!!
    }

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code == code } ?: ENGLISH
        }
    }
}

enum class ClickDelayOption(val delayMs: Long) {
    INSTANT(0L),
    HALF_SECOND(500L),
    ONE_SECOND(1000L);

    companion object {
        fun fromName(name: String?): ClickDelayOption =
            entries.firstOrNull { it.name == name } ?: INSTANT
    }
}

enum class FloatingVisibility {
    SHOW,
    HIDDEN,
    AUTO_HIDE;

    companion object {
        fun fromName(name: String?): FloatingVisibility =
            entries.firstOrNull { it.name == name } ?: AUTO_HIDE
    }
}

enum class FloatingSize(val dp: Int) {
    SMALL(44),
    MEDIUM(52),
    LARGE(62);

    companion object {
        fun fromName(name: String?): FloatingSize =
            entries.firstOrNull { it.name == name } ?: MEDIUM
    }
}

enum class FloatingSide {
    LEFT,
    RIGHT;

    companion object {
        fun fromName(name: String?): FloatingSide =
            entries.firstOrNull { it.name == name } ?: RIGHT
    }
}

enum class FloatingOpacity(val alpha: Float) {
    LOW(0.45f),
    MEDIUM(0.72f),
    FULL(1.0f);

    companion object {
        fun fromName(name: String?): FloatingOpacity =
            entries.firstOrNull { it.name == name } ?: MEDIUM
    }
}

enum class SkipFeedbackMode {
    NONE,
    VIBRATE,
    FLASH_CHECKMARK,
    SOUND;

    companion object {
        fun fromName(name: String?): SkipFeedbackMode =
            entries.firstOrNull { it.name == name } ?: FLASH_CHECKMARK
    }
}

enum class AdOutcome {
    SKIPPED,
    UNSKIPPABLE,
    FAILED;

    companion object {
        fun fromName(name: String?): AdOutcome =
            entries.firstOrNull { it.name == name } ?: SKIPPED
    }
}

data class RecentActivityEntry(
    val timestampMs: Long,
    val outcome: AdOutcome
)

data class DailyStats(
    val dateKey: String,
    val detected: Int = 0,
    val skipped: Int = 0,
    val unskippable: Int = 0,
    val failed: Int = 0,
    val mutedSeconds: Long = 0L
) {
    val couldNotSkip: Int get() = unskippable + failed
}

data class PermissionStatusState(
    val accessibilityGranted: Boolean = false,
    val overlayGranted: Boolean = false,
    val batteryOptimizationIgnored: Boolean = false,
    val notificationsGranted: Boolean = false
)

data class SkipBuddyPreferences(
    // Card 1: Auto-Skip
    val autoSkipEnabled: Boolean = true,
    val clickDelay: ClickDelayOption = ClickDelayOption.INSTANT,
    val muteUnskippableAds: Boolean = true,

    // Card 2: Floating Button
    val floatingVisibility: FloatingVisibility = FloatingVisibility.AUTO_HIDE,
    val floatingLocked: Boolean = true,
    val floatingSize: FloatingSize = FloatingSize.MEDIUM,
    val floatingSide: FloatingSide = FloatingSide.RIGHT,
    val floatingOpacity: FloatingOpacity = FloatingOpacity.MEDIUM,
    val floatingYFraction: Float = 0.5f,

    // Card 3: Feedback
    val feedbackMode: SkipFeedbackMode = SkipFeedbackMode.FLASH_CHECKMARK,
    val showAdSkippedMessage: Boolean = false,

    // Card 5: Language & Announcement (English default in V2)
    val appLanguage: AppLanguage = AppLanguage.ENGLISH,
    val voiceAnnouncementEnabled: Boolean = true,
    val showAdBannerEnabled: Boolean = true,

    // Setup Wizard & Debug Log (Card 6)
    val setupWizardCompleted: Boolean = false,
    val debugLogEnabled: Boolean = false,

    // Per-ad-session persisted statistics
    val dailyStats: Map<String, DailyStats> = emptyMap(),
    val totalDetected: Int = 0,
    val totalSkipped: Int = 0,
    val totalUnskippable: Int = 0,
    val totalFailed: Int = 0,
    val totalMutedSeconds: Long = 0L,
    val lastSkipTimeMs: Long = 0L,
    val lastAdTimeMs: Long = 0L,
    val recentActivity: List<RecentActivityEntry> = emptyList()
) {
    val totalCouldNotSkip: Int get() = totalUnskippable + totalFailed

    fun todayStats(todayKey: String): DailyStats =
        dailyStats[todayKey] ?: DailyStats(dateKey = todayKey)

    val allTimeSuccessPercent: Int?
        get() = if (totalDetected <= 0) null else ((totalSkipped * 100f) / totalDetected).toInt().coerceIn(0, 100)

    fun todaySuccessPercent(todayKey: String): Int? {
        val t = todayStats(todayKey)
        return if (t.detected <= 0) null else ((t.skipped * 100f) / t.detected).toInt().coerceIn(0, 100)
    }
}
