package com.example

import com.example.data.SkipBuddyRepository
import com.example.detection.AdState
import com.example.detection.DetectionEngine
import com.example.detection.NodeSnapshot
import com.example.model.AdOutcome
import com.example.model.AppLanguage
import com.example.model.DailyStats
import com.example.model.RecentActivityEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionEngineTest {

    private fun node(
        index: Int = 0,
        id: String = "",
        text: String = "",
        desc: String = "",
        visible: Boolean = true,
        clickable: Boolean = true,
        enabled: Boolean = true
    ) = NodeSnapshot(
        nodeIndex = index,
        parentIndex = -1,
        packageName = DetectionEngine.YOUTUBE_PACKAGE,
        className = "android.widget.Button",
        viewIdResourceName = id,
        text = text,
        contentDescription = desc,
        isVisibleToUser = visible,
        isClickable = clickable,
        isEnabled = enabled,
        boundsLeft = 100,
        boundsTop = 200,
        boundsRight = 320,
        boundsBottom = 280
    )

    @Test
    fun priorityCascadeOrder_matchesSelectedLanguage() {
        assertEquals(
            listOf(AppLanguage.BANGLA, AppLanguage.ENGLISH, AppLanguage.HINDI),
            DetectionEngine.buildCascadeOrder(AppLanguage.BANGLA)
        )
        assertEquals(
            listOf(AppLanguage.ENGLISH, AppLanguage.HINDI, AppLanguage.BANGLA),
            DetectionEngine.buildCascadeOrder(AppLanguage.ENGLISH)
        )
        assertEquals(
            listOf(AppLanguage.HINDI, AppLanguage.ENGLISH, AppLanguage.BANGLA),
            DetectionEngine.buildCascadeOrder(AppLanguage.HINDI)
        )
    }

    @Test
    fun skipStep0_viewIdFirst_matchesLanguageNeutralSkipButton() {
        val nodes = listOf(
            node(0, id = "com.google.android.youtube:id/modern_skip_ad_button", text = "Skip")
        )
        val result = DetectionEngine.analyzeTree(nodes, AppLanguage.BANGLA)
        assertEquals(AdState.AD_ACTIVE, result.adState)
        assertTrue(result.hasClickableSkip)
        assertEquals("STEP0_VIEW_ID", result.skipCandidate?.languageStep)
    }

    @Test
    fun skipLayer2_banglaSelected_matchesBanglaFirstWithEarlyExit() {
        val nodes = listOf(
            node(0, id = "", text = "বিজ্ঞাপন এড়িয়ে যান")
        )
        val result = DetectionEngine.analyzeTree(nodes, AppLanguage.BANGLA)
        assertEquals(AdState.AD_ACTIVE, result.adState)
        assertTrue(result.hasClickableSkip)
        assertEquals("STEP1_BANGLA", result.skipCandidate?.languageStep)
    }

    @Test
    fun skipLayer2_banglaSelected_fallsThroughToEnglishAndHindi() {
        val enNodes = listOf(node(0, id = "", text = "Skip Ads"))
        val enResult = DetectionEngine.analyzeTree(enNodes, AppLanguage.BANGLA)
        assertTrue(enResult.hasClickableSkip)
        assertEquals("STEP2_ENGLISH", enResult.skipCandidate?.languageStep)

        val hiNodes = listOf(node(0, id = "", text = "विज्ञापन छोड़ें"))
        val hiResult = DetectionEngine.analyzeTree(hiNodes, AppLanguage.BANGLA)
        assertTrue(hiResult.hasClickableSkip)
        assertEquals("STEP3_HINDI", hiResult.skipCandidate?.languageStep)
    }

    @Test
    fun countdownText_triggersAdStateButDoesNotClick() {
        val enCountdown = listOf(node(0, id = "com.google.android.youtube:id/skip_ad_button_text", text = "Skip ad in 5"))
        val res = DetectionEngine.analyzeTree(enCountdown, AppLanguage.ENGLISH)
        assertEquals(AdState.AD_ACTIVE, res.adState)
        assertFalse(res.hasClickableSkip)
        assertNotNull(res.skipCandidate)
        assertTrue(res.skipCandidate!!.isCountdownOnly)

        val bnCountdown = listOf(node(0, id = "", text = "৫ সেকেন্ড পর বিজ্ঞাপন এড়িয়ে যেতে পারবেন"))
        val bnRes = DetectionEngine.analyzeTree(bnCountdown, AppLanguage.BANGLA)
        assertEquals(AdState.AD_ACTIVE, bnRes.adState)
        assertFalse(bnRes.hasClickableSkip)
    }

    @Test
    fun sponsoredAlone_neverTriggersAdState() {
        val sponsoredOnly = listOf(node(0, text = "Sponsored"))
        val result = DetectionEngine.analyzeTree(sponsoredOnly, AppLanguage.ENGLISH)
        assertEquals(AdState.OVERLAY_SPONSORED, result.adState)
        assertFalse(result.hasClickableSkip)
    }

    @Test
    fun twoIndependentWeakSignals_triggersAdState() {
        val twoWeak = listOf(
            node(0, text = "Sponsored"),
            node(1, text = "Visit advertiser")
        )
        val result = DetectionEngine.analyzeTree(twoWeak, AppLanguage.ENGLISH)
        assertEquals(AdState.AD_ACTIVE, result.adState)
    }

    @Test
    fun downloadOrInstallAloneWithSponsored_doesNotTriggerAdState() {
        // Download and Install are excluded from advertiser CTA weak signals
        val nodes = listOf(
            node(0, text = "Sponsored"),
            node(1, text = "Download")
        )
        val result = DetectionEngine.analyzeTree(nodes, AppLanguage.ENGLISH)
        assertEquals(AdState.OVERLAY_SPONSORED, result.adState)
    }

    @Test
    fun safetyBlacklist_blocksPlaybackAndChannelControls() {
        val badNodes = listOf(
            node(0, text = "Subscribe"),
            node(1, text = "Like"),
            node(2, text = "Dislike"),
            node(3, text = "Next"),
            node(4, text = "Play"),
            node(5, text = "Pause"),
            node(6, text = "Share"),
            node(7, text = "Skip 10 seconds")
        )
        val result = DetectionEngine.analyzeTree(badNodes, AppLanguage.ENGLISH)
        assertEquals(AdState.NONE, result.adState)
        assertNull(result.skipCandidate)
    }

    @Test
    fun strongAdSignals_startAdStateWithoutSkipButton() {
        val adPod = listOf(node(0, text = "Ad 1 of 2"))
        assertEquals(AdState.AD_ACTIVE, DetectionEngine.analyzeTree(adPod, AppLanguage.ENGLISH).adState)

        val videoAfter = listOf(node(0, text = "Video will play after ad"))
        assertEquals(AdState.AD_ACTIVE, DetectionEngine.analyzeTree(videoAfter, AppLanguage.ENGLISH).adState)
    }

    @Test
    fun dailyStatsAndRecentActivity_roundTripSerialization() {
        val daily = mapOf(
            "2026-11-21" to DailyStats("2026-11-21", detected = 10, skipped = 7, unskippable = 2, failed = 1, mutedSeconds = 42L)
        )
        val encodedDaily = SkipBuddyRepository.encodeDailyStats(daily)
        assertEquals(daily, SkipBuddyRepository.decodeDailyStats(encodedDaily))

        val recent = listOf(
            RecentActivityEntry(1_700_000_000_000L, AdOutcome.SKIPPED),
            RecentActivityEntry(1_700_000_050_000L, AdOutcome.UNSKIPPABLE),
            RecentActivityEntry(1_700_000_100_000L, AdOutcome.FAILED)
        )
        val encodedRecent = SkipBuddyRepository.encodeRecentActivity(recent)
        assertEquals(recent, SkipBuddyRepository.decodeRecentActivity(encodedRecent))
    }
}
