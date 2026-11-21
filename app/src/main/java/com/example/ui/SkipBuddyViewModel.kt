package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.DebugLogStore
import com.example.data.SkipBuddyRepository
import com.example.model.AppLanguage
import com.example.model.ClickDelayOption
import com.example.model.FloatingOpacity
import com.example.model.FloatingSide
import com.example.model.FloatingSize
import com.example.model.FloatingVisibility
import com.example.model.PermissionStatusState
import com.example.model.SkipBuddyPreferences
import com.example.model.SkipFeedbackMode
import com.example.util.AnnouncementAudioPlayer
import com.example.util.PermissionChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab {
    HOME,
    GUIDE,
    SETTINGS
}

data class SkipBuddyUiState(
    val preferences: SkipBuddyPreferences = SkipBuddyPreferences(),
    val permissions: PermissionStatusState = PermissionStatusState(),
    val isServiceConnected: Boolean = false,
    val showWizardManually: Boolean = false,
    val preferencesLoaded: Boolean = false
) {
    val isServiceActive: Boolean
        get() = permissions.accessibilityGranted || isServiceConnected

    val shouldShowWizard: Boolean
        get() = preferencesLoaded && (!preferences.setupWizardCompleted || showWizardManually)
}

class SkipBuddyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SkipBuddyRepository.getInstance(application)
    private val audioPlayer = AnnouncementAudioPlayer(application)

    private val _permissions = MutableStateFlow(PermissionChecker.checkAll(application))
    private val _showWizardManually = MutableStateFlow(false)
    private val _prefsLoaded = MutableStateFlow(false)

    val uiState: StateFlow<SkipBuddyUiState> = combine(
        repository.preferencesFlow,
        _permissions,
        repository.isServiceConnected,
        _showWizardManually
    ) { prefs, perms, connected, manualWizard ->
        _prefsLoaded.value = true
        SkipBuddyUiState(
            preferences = prefs,
            permissions = perms,
            isServiceConnected = connected,
            showWizardManually = manualWizard,
            preferencesLoaded = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SkipBuddyUiState(
            permissions = _permissions.value,
            preferencesLoaded = false
        )
    )

    fun refreshPermissions() {
        _permissions.value = PermissionChecker.checkAll(getApplication())
    }

    fun openSetupWizard() {
        _showWizardManually.value = true
    }

    fun skipOrCloseWizard() {
        _showWizardManually.value = false
        viewModelScope.launch {
            repository.setSetupWizardCompleted(true)
        }
    }

    fun finishWizardHideAndLock() {
        _showWizardManually.value = false
        viewModelScope.launch {
            repository.finishWizardHideAndLock()
        }
    }

    fun finishWizardKeepAutoHideLocked() {
        _showWizardManually.value = false
        viewModelScope.launch {
            repository.finishWizardKeepAutoHideLocked()
        }
    }

    // Card 1: Auto-Skip
    fun setAutoSkipEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoSkipEnabled(enabled) }
    }

    fun setClickDelay(option: ClickDelayOption) {
        viewModelScope.launch { repository.setClickDelay(option) }
    }

    fun setMuteUnskippableAds(enabled: Boolean) {
        viewModelScope.launch { repository.setMuteUnskippableAds(enabled) }
    }

    // Card 2: Floating Button
    fun setFloatingVisibility(visibility: FloatingVisibility) {
        viewModelScope.launch { repository.setFloatingVisibility(visibility) }
    }

    fun setFloatingLocked(locked: Boolean) {
        viewModelScope.launch { repository.setFloatingLocked(locked) }
    }

    fun setFloatingSize(size: FloatingSize) {
        viewModelScope.launch { repository.setFloatingSize(size) }
    }

    fun setFloatingSide(side: FloatingSide) {
        viewModelScope.launch { repository.setFloatingSide(side) }
    }

    fun setFloatingOpacity(opacity: FloatingOpacity) {
        viewModelScope.launch { repository.setFloatingOpacity(opacity) }
    }

    fun resetFloatingPosition() {
        viewModelScope.launch { repository.resetFloatingPosition() }
    }

    // Card 3: Feedback
    fun setFeedbackMode(mode: SkipFeedbackMode) {
        viewModelScope.launch { repository.setFeedbackMode(mode) }
    }

    fun setShowAdSkippedMessage(show: Boolean) {
        viewModelScope.launch { repository.setShowAdSkippedMessage(show) }
    }

    // Card 5: Language & Announcement
    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch { repository.setAppLanguage(language) }
    }

    fun setVoiceAnnouncementEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setVoiceAnnouncementEnabled(enabled) }
    }

    fun setShowAdBannerEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setShowAdBannerEnabled(enabled) }
    }

    fun playTestSound() {
        audioPlayer.playOnce(uiState.value.preferences.appLanguage)
    }

    // Card 6: Data & Debug Log
    fun resetTodayStats() {
        viewModelScope.launch { repository.resetTodayStats() }
    }

    fun resetAllStats() {
        viewModelScope.launch { repository.resetAllStats() }
    }

    fun clearRecentActivity() {
        viewModelScope.launch { repository.clearRecentActivity() }
    }

    fun setDebugLogEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setDebugLogEnabled(enabled) }
    }

    fun shareDebugLog(localizedContext: Context): Boolean {
        val logText = DebugLogStore.readLog(getApplication())
        if (logText.isBlank()) return false
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "SkipBuddy Detection Debug Log")
            putExtra(Intent.EXTRA_TEXT, logText)
        }
        val chooser = Intent.createChooser(
            sendIntent,
            localizedContext.getString(R.string.share_debug_log_title)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        localizedContext.startActivity(chooser)
        return true
    }

    override fun onCleared() {
        audioPlayer.stop()
        super.onCleared()
    }
}
