package com.example.util

import android.content.Context
import android.content.res.Configuration
import com.example.model.AppLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocaleHelper {

    fun createLocalizedContext(base: Context, language: AppLanguage): Context {
        val targetLocale = language.toLocale()
        val config = Configuration(base.resources.configuration)
        config.setLocale(targetLocale)
        return base.createConfigurationContext(config)
    }

    fun formatDurationMmSs(totalSeconds: Long): String {
        val safe = totalSeconds.coerceAtLeast(0L)
        val mins = safe / 60
        val secs = safe % 60
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    fun formatTimeShort(timestampMs: Long, locale: Locale = Locale.getDefault()): String {
        if (timestampMs <= 0L) return ""
        val fmt = SimpleDateFormat("h:mm a", locale)
        return fmt.format(Date(timestampMs))
    }

    fun formatDayLabel(dateKey: String, locale: Locale = Locale.getDefault()): String {
        return runCatching {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = parser.parse(dateKey) ?: return dateKey.takeLast(2)
            SimpleDateFormat("EEE", locale).format(date)
        }.getOrDefault(dateKey.takeLast(2))
    }
}
