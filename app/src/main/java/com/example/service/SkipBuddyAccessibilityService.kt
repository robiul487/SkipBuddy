package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.res.Configuration
import android.graphics.Path
import android.graphics.Rect
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Toast
import com.example.R
import com.example.data.DebugLogStore
import com.example.data.SkipBuddyRepository
import com.example.detection.AdState
import com.example.detection.DetectionEngine
import com.example.detection.NodeSnapshot
import com.example.detection.ScanAnalysis
import com.example.model.AdOutcome
import com.example.model.SkipBuddyPreferences
import com.example.model.SkipFeedbackMode
import com.example.model.SupportedApp
import com.example.util.AnnouncementAudioPlayer
import com.example.util.LocaleHelper
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Core Accessibility Service implementing Detection v2 + Language Priority Cascade:
 * - Strictly inspects ONLY com.google.android.youtube and (when enabled)
 *   com.google.android.apps.youtube.music.
 * - Adaptive polling: 500ms normal while a supported app is foreground, ~300ms while Ad State active/suspected.
 * - 900ms anti-flicker before ending an Ad Session.
 * - Exact per-ad-session counting (Detected +1 at start; Skipped / Unskippable / Failed at end with app source).
 * - Mute Unskippable Ads with exact previous STREAM_MUSIC volume save & restore (never overrides manual mute).
 * - Bundled MP3 voice announcement on USAGE_ASSISTANCE_ACCESSIBILITY stream (no TextToSpeech).
 * - Live dynamic reaction to every DataStore toggle (including YouTube Music ON/OFF) with zero restart required.
 */
class SkipBuddyAccessibilityService : AccessibilityService() {

    private lateinit var repository: SkipBuddyRepository
    private lateinit var audioManager: AudioManager
    private lateinit var announcementPlayer: AnnouncementAudioPlayer
    private lateinit var overlayController: FloatingOverlayController

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var currentPrefs: SkipBuddyPreferences = SkipBuddyPreferences()

    private var activeTargetPackage: String? = null
    private val isSupportedAppForeground: Boolean
        get() = activeTargetPackage != null

    // Ad Session State Machine
    private var inAdSession: Boolean = false
    private var sessionApp: SupportedApp = SupportedApp.YOUTUBE
    private var adSuspected: Boolean = false
    private var adAbsentSinceElapsedMs: Long = 0L
    private var skipEverAvailableInSession: Boolean = false
    private var announcementPlayedInSession: Boolean = false
    private var retryCountInSession: Int = 0
    private var lastClickElapsedMs: Long = 0L
    private var pendingClickRunnable: Runnable? = null
    private var pendingVerifyRunnable: Runnable? = null

    // Mute State (only ever restores a mute SkipBuddy itself applied)
    private var mutedBySkipBuddy: Boolean = false
    private var savedMusicVolume: Int = -1
    private var muteStartedElapsedMs: Long = 0L
    private var sessionMutedAccumulatedMs: Long = 0L

    // Debug log throttling
    private var lastDebugDumpElapsedMs: Long = 0L

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (!isSupportedAppForeground) return
            scanActiveTargetWindow(triggerSource = "poll")
            if (!isSupportedAppForeground) return
            val interval = if (inAdSession || adSuspected) POLL_INTERVAL_AD_MS else POLL_INTERVAL_NORMAL_MS
            mainHandler.postDelayed(this, interval)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        repository = SkipBuddyRepository.getInstance(applicationContext)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        announcementPlayer = AnnouncementAudioPlayer(applicationContext)
        overlayController = FloatingOverlayController(
            context = applicationContext,
            onToggleAutoSkip = {
                serviceScope.launch {
                    repository.setAutoSkipEnabled(!currentPrefs.autoSkipEnabled)
                }
            },
            onPositionCommitted = { side, yFraction ->
                serviceScope.launch {
                    repository.setFloatingPosition(side, yFraction)
                }
            }
        )

        repository.setServiceConnected(true)
        NotificationHelper.cancelServiceOffReminder(applicationContext)

        serviceScope.launch {
            repository.preferencesFlow.collectLatest { updated ->
                applyUpdatedPreferences(updated)
            }
        }
    }

    private fun applyUpdatedPreferences(updated: SkipBuddyPreferences) {
        val previous = currentPrefs
        currentPrefs = updated
        overlayController.updatePreferences(updated)

        // Dynamic live rule: turning YouTube Music OFF immediately removes it from the active gate
        if (!updated.youtubeMusicEnabled &&
            activeTargetPackage == DetectionEngine.YOUTUBE_MUSIC_PACKAGE
        ) {
            leaveSupportedForeground()
        }

        // Dynamic live rule: turning Mute OFF mid-ad IMMEDIATELY restores saved volume
        if (!updated.muteUnskippableAds && mutedBySkipBuddy) {
            restoreMusicVolumeIfMutedByUs()
        }

        // Dynamic live rule: turning Voice Announcement OFF stops any playing audio immediately
        if (!updated.voiceAnnouncementEnabled) {
            announcementPlayer.stop()
        }

        // Dynamic live rule: turning Ad Banner OFF hides it immediately; language change updates text live
        if (!updated.showAdBannerEnabled) {
            overlayController.setAdBannerVisible(false, "")
        } else if (inAdSession && previous.appLanguage != updated.appLanguage) {
            overlayController.setAdBannerVisible(true, updated.appLanguage.resolveAnnouncementText())
        }
    }

    private fun isAllowedPackage(pkg: String?): Boolean {
        return DetectionEngine.isPackageAllowed(pkg, currentPrefs.youtubeMusicEnabled)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString()

        // LAYER 1 PACKAGE GATE:
        // Never read or inspect nodes of any package outside YouTube (+ YouTube Music when enabled).
        if (!isAllowedPackage(pkg)) {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                handleNonAllowedWindowState(pkg)
            }
            return
        }

        // Supported app (YouTube or enabled YouTube Music) is foreground
        val allowedPkg = pkg!!
        if (activeTargetPackage != allowedPkg) {
            if (inAdSession) {
                finalizeAdSessionAsNonSkipped()
            }
            enterSupportedForeground(allowedPkg)
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            scanActiveTargetWindow(triggerSource = "event:${event.eventType}")
        }
    }

    private fun handleNonAllowedWindowState(pkg: String?) {
        if (!isSupportedAppForeground) return
        // Ignore transient SystemUI overlays (e.g. volume bar) ONLY if a supported app window is still active
        if (pkg == "com.android.systemui" || pkg == packageName) {
            val activeAllowed = findActiveAllowedApplicationPackage()
            if (activeAllowed != null) {
                activeTargetPackage = activeAllowed
                return
            }
        }
        leaveSupportedForeground()
    }

    private fun findActiveAllowedApplicationPackage(): String? {
        val activeRootPkg = rootInActiveWindow?.packageName?.toString()
        if (isAllowedPackage(activeRootPkg)) return activeRootPkg
        return runCatching {
            windows?.firstOrNull { win ->
                win.type == AccessibilityWindowInfo.TYPE_APPLICATION &&
                    isAllowedPackage(win.root?.packageName?.toString())
            }?.root?.packageName?.toString()
        }.getOrNull()
    }

    private fun enterSupportedForeground(pkg: String) {
        val wasForeground = isSupportedAppForeground
        activeTargetPackage = pkg
        if (!wasForeground) {
            overlayController.setSupportedAppForeground(true)
        }
        mainHandler.removeCallbacks(pollRunnable)
        mainHandler.postDelayed(pollRunnable, POLL_INTERVAL_NORMAL_MS)
    }

    private fun leaveSupportedForeground() {
        activeTargetPackage = null
        mainHandler.removeCallbacks(pollRunnable)
        cancelPendingClickAndVerify()
        overlayController.setSupportedAppForeground(false)
        announcementPlayer.stop()
        if (inAdSession) {
            finalizeAdSessionAsNonSkipped()
        } else {
            restoreMusicVolumeIfMutedByUs()
        }
        adSuspected = false
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::overlayController.isInitialized) {
            overlayController.onConfigurationChanged()
        }
    }

    override fun onInterrupt() {
        announcementPlayer.stop()
        restoreMusicVolumeIfMutedByUs()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(pollRunnable)
        cancelPendingClickAndVerify()
        if (::announcementPlayer.isInitialized) announcementPlayer.stop()
        restoreMusicVolumeIfMutedByUs()
        if (::overlayController.isInitialized) overlayController.destroy()
        if (::repository.isInitialized) repository.setServiceConnected(false)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun scanActiveTargetWindow(triggerSource: String) {
        val root = rootInActiveWindow
        val rootPkg = root?.packageName?.toString()
        if (root == null || !isAllowedPackage(rootPkg)) {
            if ((rootPkg == "com.android.systemui" || rootPkg == packageName) &&
                findActiveAllowedApplicationPackage() != null
            ) {
                return
            }
            leaveSupportedForeground()
            return
        }

        val allowedPkg = rootPkg!!
        if (activeTargetPackage != allowedPkg) {
            if (inAdSession) {
                finalizeAdSessionAsNonSkipped()
            }
            activeTargetPackage = allowedPkg
        }

        val liveNodes = ArrayList<AccessibilityNodeInfo>(64)
        val snapshots = ArrayList<NodeSnapshot>(64)
        collectNodeTree(root, parentIndex = -1, liveNodes = liveNodes, snapshots = snapshots)

        val prefs = currentPrefs
        val analysis = DetectionEngine.analyzeTree(
            nodes = snapshots,
            selectedLanguage = prefs.appLanguage
        )

        val nowElapsed = SystemClock.elapsedRealtime()
        maybeWriteDebugLog(triggerSource, analysis, snapshots, nowElapsed)
        processScanAnalysis(analysis, liveNodes, SupportedApp.fromPackage(allowedPkg), nowElapsed)
    }

    private fun collectNodeTree(
        node: AccessibilityNodeInfo,
        parentIndex: Int,
        liveNodes: MutableList<AccessibilityNodeInfo>,
        snapshots: MutableList<NodeSnapshot>
    ) {
        if (liveNodes.size >= MAX_TRAVERSAL_NODES) return
        val myIndex = liveNodes.size
        liveNodes.add(node)

        val rect = Rect()
        node.getBoundsInScreen(rect)
        snapshots.add(
            NodeSnapshot(
                nodeIndex = myIndex,
                parentIndex = parentIndex,
                packageName = node.packageName?.toString().orEmpty(),
                className = node.className?.toString().orEmpty(),
                viewIdResourceName = node.viewIdResourceName.orEmpty(),
                text = node.text?.toString().orEmpty(),
                contentDescription = node.contentDescription?.toString().orEmpty(),
                isVisibleToUser = node.isVisibleToUser,
                isClickable = node.isClickable,
                isEnabled = node.isEnabled,
                boundsLeft = rect.left,
                boundsTop = rect.top,
                boundsRight = rect.right,
                boundsBottom = rect.bottom
            )
        )

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            collectNodeTree(child, myIndex, liveNodes, snapshots)
        }
    }

    private fun processScanAnalysis(
        analysis: ScanAnalysis,
        liveNodes: List<AccessibilityNodeInfo>,
        currentApp: SupportedApp,
        nowElapsed: Long
    ) {
        val prefs = currentPrefs
        adSuspected = analysis.adState == AdState.AD_ACTIVE || analysis.weakSignals.isNotEmpty()

        // Sponsored ALONE (OVERLAY_SPONSORED) -> NO mute, NO announcement, NO action.
        if (analysis.adState == AdState.AD_ACTIVE) {
            adAbsentSinceElapsedMs = 0L
            if (!inAdSession) {
                startNewAdSession(currentApp)
            }

            if (analysis.hasClickableSkip) {
                skipEverAvailableInSession = true
                val candidate = analysis.skipCandidate!!
                if (prefs.autoSkipEnabled &&
                    retryCountInSession < MAX_RETRIES_PER_AD &&
                    pendingClickRunnable == null &&
                    pendingVerifyRunnable == null &&
                    (nowElapsed - lastClickElapsedMs) >= MIN_CLICK_DEBOUNCE_MS
                ) {
                    val targetNode = liveNodes.getOrNull(candidate.nodeIndex)
                    if (targetNode != null) {
                        scheduleSkipClick(targetNode, prefs.clickDelay.delayMs)
                    }
                }
            } else {
                // Ad is playing, and Skip button is NOT yet enabled/available (unskippable or countdown)
                if (prefs.muteUnskippableAds) {
                    muteMusicIfNeeded()
                }
                if (prefs.voiceAnnouncementEnabled && !announcementPlayedInSession) {
                    announcementPlayedInSession = true
                    announcementPlayer.playOnce(prefs.appLanguage)
                }
                if (prefs.showAdBannerEnabled) {
                    overlayController.setAdBannerVisible(
                        visible = true,
                        message = prefs.appLanguage.resolveAnnouncementText()
                    )
                }
            }
        } else if (inAdSession) {
            // Anti-flicker rule: end Ad Session only after signals have been absent for >= 900ms
            if (adAbsentSinceElapsedMs == 0L) {
                adAbsentSinceElapsedMs = nowElapsed
            } else if ((nowElapsed - adAbsentSinceElapsedMs) >= AD_END_ANTI_FLICKER_MS) {
                finalizeAdSessionAsNonSkipped()
            }
        }
    }

    private fun startNewAdSession(app: SupportedApp) {
        inAdSession = true
        sessionApp = app
        adAbsentSinceElapsedMs = 0L
        skipEverAvailableInSession = false
        announcementPlayedInSession = false
        retryCountInSession = 0
        sessionMutedAccumulatedMs = 0L

        // Per-ad-session counting: Detected +1 ONCE at session start (combined across both apps)
        serviceScope.launch {
            repository.recordAdSessionStart()
        }
    }

    private fun scheduleSkipClick(targetNode: AccessibilityNodeInfo, delayMs: Long) {
        val clickAction = Runnable {
            pendingClickRunnable = null
            if (!isSupportedAppForeground || !currentPrefs.autoSkipEnabled || !inAdSession) return@Runnable
            val now = SystemClock.elapsedRealtime()
            if ((now - lastClickElapsedMs) < MIN_CLICK_DEBOUNCE_MS) return@Runnable

            // Re-find fresh clickable Skip node if delayed so we never click a stale reference
            val clicked = if (delayMs > 0L) {
                findAndClickFreshSkipNode()
            } else {
                performThreeLevelClick(targetNode)
            }

            if (clicked) {
                lastClickElapsedMs = SystemClock.elapsedRealtime()
                retryCountInSession++
                schedulePostClickVerification()
            }
        }

        pendingClickRunnable = clickAction
        if (delayMs <= 0L) {
            clickAction.run()
        } else {
            mainHandler.postDelayed(clickAction, delayMs)
        }
    }

    private fun findAndClickFreshSkipNode(): Boolean {
        val root = rootInActiveWindow ?: return false
        if (!isAllowedPackage(root.packageName?.toString())) return false
        val liveNodes = ArrayList<AccessibilityNodeInfo>(64)
        val snapshots = ArrayList<NodeSnapshot>(64)
        collectNodeTree(root, -1, liveNodes, snapshots)
        val fresh = DetectionEngine.analyzeTree(snapshots, currentPrefs.appLanguage)
        if (!fresh.hasClickableSkip) return false
        val node = liveNodes.getOrNull(fresh.skipCandidate!!.nodeIndex) ?: return false
        return performThreeLevelClick(node)
    }

    /**
     * 3-level click strategy:
     * 1) ACTION_CLICK on node directly
     * 2) Walk up to nearest clickable ancestor (guarded against clicking root full-screen container)
     * 3) Fallback gesture tap at node bounds center (getBoundsInScreen + dispatchGesture)
     */
    private fun performThreeLevelClick(node: AccessibilityNodeInfo): Boolean {
        // Level 1: direct ACTION_CLICK
        if (node.isClickable && node.isEnabled && node.isVisibleToUser) {
            if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
        }

        // Level 2: clickable ancestor walk (max 4 hops, never a huge full-screen player container)
        val dm = resources.displayMetrics
        val maxSafeArea = (dm.widthPixels.toLong() * dm.heightPixels.toLong()) / 3L
        var ancestor = node.parent
        var hops = 0
        while (ancestor != null && hops < 4) {
            if (ancestor.isClickable && ancestor.isEnabled && ancestor.isVisibleToUser) {
                val r = Rect()
                ancestor.getBoundsInScreen(r)
                val area = r.width().toLong() * r.height().toLong()
                if (area in 1..maxSafeArea) {
                    if (ancestor.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        return true
                    }
                }
            }
            ancestor = ancestor.parent
            hops++
        }

        // Level 3: fallback dispatchGesture tap at node bounds center
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.isEmpty && bounds.width() > 0 && bounds.height() > 0) {
            return dispatchCenterTapGesture(bounds.exactCenterX(), bounds.exactCenterY())
        }
        return false
    }

    private fun dispatchCenterTapGesture(x: Float, y: Float): Boolean {
        if (x <= 0f || y <= 0f) return false
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 60L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    /**
     * Layer 6 Verify: re-check ~700ms after click.
     * - Ad markers gone -> Verified Skipped (count +1, log entry, feedback, restore volume).
     * - Still there -> allow retry up to max 3 times per ad session.
     */
    private fun schedulePostClickVerification() {
        pendingVerifyRunnable?.let { mainHandler.removeCallbacks(it) }
        val verify = Runnable {
            pendingVerifyRunnable = null
            if (!inAdSession) return@Runnable
            val root = rootInActiveWindow
            val stillAd = if (root != null && isAllowedPackage(root.packageName?.toString())) {
                val liveNodes = ArrayList<AccessibilityNodeInfo>(64)
                val snapshots = ArrayList<NodeSnapshot>(64)
                collectNodeTree(root, -1, liveNodes, snapshots)
                val check = DetectionEngine.analyzeTree(snapshots, currentPrefs.appLanguage)
                maybeWriteDebugLog("verify-700ms", check, snapshots, SystemClock.elapsedRealtime())
                check.adState == AdState.AD_ACTIVE
            } else {
                false
            }

            if (!stillAd) {
                finalizeAdSessionAsSkipped()
            }
            // If stillAd == true and retryCountInSession < 3, the 300ms poll loop will retry safely.
        }
        pendingVerifyRunnable = verify
        mainHandler.postDelayed(verify, VERIFY_AFTER_CLICK_MS)
    }

    private fun finalizeAdSessionAsSkipped() {
        cancelPendingClickAndVerify()
        announcementPlayer.stop()
        overlayController.setAdBannerVisible(false, "")
        restoreMusicVolumeIfMutedByUs()

        val mutedSecs = (sessionMutedAccumulatedMs / 1000L)
        val completedApp = sessionApp
        resetSessionFlags()

        triggerSkipFeedback()

        serviceScope.launch {
            repository.recordAdSessionOutcome(
                outcome = AdOutcome.SKIPPED,
                mutedSeconds = mutedSecs,
                app = completedApp
            )
        }
    }

    private fun finalizeAdSessionAsNonSkipped() {
        cancelPendingClickAndVerify()
        announcementPlayer.stop()
        overlayController.setAdBannerVisible(false, "")
        restoreMusicVolumeIfMutedByUs()

        val outcome = if (skipEverAvailableInSession) {
            // Skip appeared, retries failed (or auto-skip was paused) and session ended another way
            AdOutcome.FAILED
        } else {
            AdOutcome.UNSKIPPABLE
        }
        val mutedSecs = (sessionMutedAccumulatedMs / 1000L)
        val completedApp = sessionApp
        resetSessionFlags()

        serviceScope.launch {
            repository.recordAdSessionOutcome(
                outcome = outcome,
                mutedSeconds = mutedSecs,
                app = completedApp
            )
        }
    }

    private fun resetSessionFlags() {
        inAdSession = false
        adAbsentSinceElapsedMs = 0L
        skipEverAvailableInSession = false
        announcementPlayedInSession = false
        retryCountInSession = 0
        sessionMutedAccumulatedMs = 0L
    }

    private fun cancelPendingClickAndVerify() {
        pendingClickRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingClickRunnable = null
        pendingVerifyRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingVerifyRunnable = null
    }

    // --- Mute Unskippable Ads (exact STREAM_MUSIC save & restore) ---

    private fun muteMusicIfNeeded() {
        if (mutedBySkipBuddy) return
        runCatching {
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val alreadyMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC)
            // Never mute if user already muted manually!
            if (currentVol <= 0 || alreadyMuted) return
            savedMusicVolume = currentVol
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            mutedBySkipBuddy = true
            muteStartedElapsedMs = SystemClock.elapsedRealtime()
        }
    }

    private fun restoreMusicVolumeIfMutedByUs() {
        if (!mutedBySkipBuddy) return
        val now = SystemClock.elapsedRealtime()
        if (muteStartedElapsedMs > 0L) {
            sessionMutedAccumulatedMs += (now - muteStartedElapsedMs).coerceAtLeast(0L)
            muteStartedElapsedMs = 0L
        }
        val volumeToRestore = savedMusicVolume
        mutedBySkipBuddy = false
        savedMusicVolume = -1
        if (volumeToRestore > 0) {
            runCatching {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeToRestore, 0)
            }
        }
    }

    // --- Skip Feedback (Card 3) ---

    private fun triggerSkipFeedback() {
        val prefs = currentPrefs
        when (prefs.feedbackMode) {
            SkipFeedbackMode.NONE -> Unit
            SkipFeedbackMode.FLASH_CHECKMARK -> overlayController.triggerSkipCheckmarkFlash()
            SkipFeedbackMode.VIBRATE -> performTinyVibration()
            SkipFeedbackMode.SOUND -> playShortFeedbackBeep()
        }

        if (prefs.showAdSkippedMessage) {
            val localizedCtx = LocaleHelper.createLocalizedContext(applicationContext, prefs.appLanguage)
            Toast.makeText(
                localizedCtx,
                localizedCtx.getString(R.string.toast_ad_skipped),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun performTinyVibration() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(45L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(45L)
            }
        }
    }

    private fun playShortFeedbackBeep() {
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_ACCESSIBILITY, 65)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 90)
            mainHandler.postDelayed({ runCatching { tone.release() } }, 180L)
        }
    }

    private fun maybeWriteDebugLog(
        label: String,
        analysis: ScanAnalysis,
        snapshots: List<NodeSnapshot>,
        nowElapsed: Long
    ) {
        if (!currentPrefs.debugLogEnabled) return
        val isAdMoment = analysis.adState != AdState.NONE ||
            analysis.skipCandidate != null ||
            analysis.weakSignals.isNotEmpty()
        if (!isAdMoment) return
        if ((nowElapsed - lastDebugDumpElapsedMs) < 900L) return
        lastDebugDumpElapsedMs = nowElapsed

        val interestingNodes = snapshots.filter {
            it.isVisibleToUser && (
                it.text.isNotBlank() ||
                    it.contentDescription.isNotBlank() ||
                    it.viewIdResourceName.contains("ad", ignoreCase = true) ||
                    it.viewIdResourceName.contains("skip", ignoreCase = true)
                )
        }
        DebugLogStore.appendAdMoment(applicationContext, label, analysis, interestingNodes)
    }

    companion object {
        private const val POLL_INTERVAL_NORMAL_MS = 500L
        private const val POLL_INTERVAL_AD_MS = 300L
        private const val AD_END_ANTI_FLICKER_MS = 900L
        private const val VERIFY_AFTER_CLICK_MS = 700L
        private const val MIN_CLICK_DEBOUNCE_MS = 1_000L
        private const val MAX_RETRIES_PER_AD = 3
        private const val MAX_TRAVERSAL_NODES = 350
    }
}
