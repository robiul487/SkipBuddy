package com.example

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.model.AdOutcome
import com.example.model.AppLanguage
import com.example.model.PermissionStatusState
import com.example.model.RecentActivityEntry
import com.example.model.SkipBuddyPreferences
import com.example.model.SupportedApp
import com.example.ui.SkipBuddyUiState
import com.example.ui.screens.GuideScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SetupWizardScreen
import com.example.ui.theme.SkipBuddyTheme
import com.example.util.LocaleHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies V2 fixes at 390dp phone width in BOTH English and Bangla:
 * - English is the default app language and resolves to ad_wait_en.
 * - Zero vertical overlap between cards on Home, Guide, Settings, and Setup Wizard.
 * - Restricted Setting help block with Open App Settings button is present in all 3 required places.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class LayoutVerification390dpTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun defaultLanguage_isEnglish_withEnglishAudio_andYoutubeMusicEnabledByDefault() {
        val prefs = SkipBuddyPreferences()
        assertEquals(AppLanguage.ENGLISH, prefs.appLanguage)
        assertEquals(R.raw.ad_wait_en, prefs.appLanguage.resolveAudioRes())
        assertTrue(prefs.youtubeMusicEnabled)
    }

    @Test
    fun v3Strings_referenceYoutubeAndYoutubeMusicInAllThreeLanguages() {
        val baseCtx = ApplicationProvider.getApplicationContext<Context>()
        for (lang in listOf(AppLanguage.ENGLISH, AppLanguage.BANGLA, AppLanguage.HINDI)) {
            val ctx = LocaleHelper.createLocalizedContext(baseCtx, lang)
            val step2Title = ctx.getString(R.string.guide_step_2_title)
            val aboutPrivacy = ctx.getString(R.string.about_privacy_text)
            val supportedApps = ctx.getString(R.string.settings_only_youtube)
            assertTrue("Guide step 2 in $lang must mention YouTube Music: $step2Title", step2Title.contains("YouTube Music"))
            assertTrue("About privacy in $lang must mention YouTube Music: $aboutPrivacy", aboutPrivacy.contains("YouTube Music"))
            assertTrue("About privacy in $lang must list both packages: $aboutPrivacy", aboutPrivacy.contains("com.google.android.apps.youtube.music"))
            assertTrue("Supported apps row in $lang must mention YouTube + YouTube Music: $supportedApps", supportedApps.contains("YouTube + YouTube Music"))
        }
    }

    @Test
    fun restrictedSettingWording_usesUpdatedFirstSentenceInAllLanguages() {
        val baseCtx = ApplicationProvider.getApplicationContext<Context>()
        val enBody = LocaleHelper.createLocalizedContext(baseCtx, AppLanguage.ENGLISH)
            .getString(R.string.restricted_setting_body)
        val bnBody = LocaleHelper.createLocalizedContext(baseCtx, AppLanguage.BANGLA)
            .getString(R.string.restricted_setting_body)
        val hiBody = LocaleHelper.createLocalizedContext(baseCtx, AppLanguage.HINDI)
            .getString(R.string.restricted_setting_body)

        assertTrue(enBody.startsWith("Some phones (Android 13 and newer) may block this once for sideloaded apps"))
        assertTrue(bnBody.contains("কিছু ফোনে (Android 13 বা নতুন)"))
        assertTrue(hiBody.contains("कुछ फोन (Android 13 या नए)"))
    }

    @Test
    fun guideScreen_390dp_english_noCardOverlap_andHasRestrictedHelp() {
        verifyGuideScreen(AppLanguage.ENGLISH)
    }

    @Test
    fun guideScreen_390dp_bangla_noCardOverlap_andHasRestrictedHelp() {
        verifyGuideScreen(AppLanguage.BANGLA)
    }

    private fun verifyGuideScreen(lang: AppLanguage) {
        val baseCtx = ApplicationProvider.getApplicationContext<Context>()
        val locCtx = LocaleHelper.createLocalizedContext(baseCtx, lang)
        val state = SkipBuddyUiState(
            preferences = SkipBuddyPreferences(appLanguage = lang),
            permissions = PermissionStatusState(),
            preferencesLoaded = true
        )

        composeRule.setContent {
            CompositionLocalProvider(LocalContext provides locCtx) {
                SkipBuddyTheme {
                    GuideScreen(
                        uiState = state,
                        onOpenAccessibilitySettings = {},
                        onOpenOverlaySettings = {},
                        onOpenBatterySettings = {},
                        onOpenNotificationSettings = {},
                        onOpenAppSettings = {},
                        onReopenSetupWizard = {},
                        modifier = Modifier.testTag("guide_list")
                    )
                }
            }
        }
        composeRule.waitForIdle()

        val caregiverBounds = composeRule.onNodeWithTag("guide_caregiver_card").getUnclippedBoundsInRoot()
        val stepsBounds = composeRule.onNodeWithTag("guide_steps_card").getUnclippedBoundsInRoot()
        assertTrue(
            "Caregiver card must sit strictly above Steps card in $lang: $caregiverBounds vs $stepsBounds",
            caregiverBounds.bottom <= stepsBounds.top
        )
        assertTrue(
            "Caregiver card must fit within 390dp width in $lang: ${caregiverBounds.right}",
            caregiverBounds.right <= 391.dp
        )

        composeRule.onNodeWithTag("guide_list")
            .performScrollToNode(hasTestTag("guide_restricted_help"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("guide_restricted_help").assertIsDisplayed()
        composeRule.onNodeWithTag("guide_restricted_help_open_app_settings").assertIsDisplayed()
    }

    @Test
    fun homeScreen_390dp_english_showsRestrictedHelpUnderEnableCard_andNoOverlap() {
        verifyHomeScreen(AppLanguage.ENGLISH)
    }

    @Test
    fun homeScreen_390dp_bangla_showsRestrictedHelpUnderEnableCard_andNoOverlap() {
        verifyHomeScreen(AppLanguage.BANGLA)
    }

    private fun verifyHomeScreen(lang: AppLanguage) {
        val baseCtx = ApplicationProvider.getApplicationContext<Context>()
        val locCtx = LocaleHelper.createLocalizedContext(baseCtx, lang)
        val state = SkipBuddyUiState(
            preferences = SkipBuddyPreferences(
                appLanguage = lang,
                recentActivity = listOf(
                    RecentActivityEntry(1_700_000_000_000L, AdOutcome.SKIPPED, SupportedApp.YOUTUBE_MUSIC),
                    RecentActivityEntry(1_700_000_050_000L, AdOutcome.UNSKIPPABLE, SupportedApp.YOUTUBE)
                )
            ),
            permissions = PermissionStatusState(accessibilityGranted = false),
            preferencesLoaded = true
        )

        composeRule.setContent {
            CompositionLocalProvider(LocalContext provides locCtx) {
                SkipBuddyTheme {
                    HomeScreen(
                        uiState = state,
                        onOpenAccessibilitySettings = {},
                        onOpenAppSettings = {},
                        onToggleAutoSkip = {},
                        modifier = Modifier.testTag("home_list")
                    )
                }
            }
        }
        composeRule.waitForIdle()

        val statusBounds = composeRule.onNodeWithTag("home_status_card").getUnclippedBoundsInRoot()
        val enableBounds = composeRule.onNodeWithTag("home_enable_warning_card").getUnclippedBoundsInRoot()
        assertTrue("Status card above Enable card in $lang", statusBounds.bottom <= enableBounds.top)

        composeRule.onNodeWithTag("home_list")
            .performScrollToNode(hasTestTag("home_restricted_help"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_restricted_help").assertIsDisplayed()
        composeRule.onNodeWithTag("home_restricted_help_open_app_settings").assertIsDisplayed()

        composeRule.onNodeWithTag("home_list")
            .performScrollToNode(hasTestTag("recent_activity_row_0"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("recent_activity_row_0").assertIsDisplayed()
    }

    @Test
    fun setupWizardStep1_390dp_english_showsRestrictedHelpAndOpenAppSettings() {
        verifySetupWizardStep1(AppLanguage.ENGLISH)
    }

    @Test
    fun setupWizardStep1_390dp_bangla_showsRestrictedHelpAndOpenAppSettings() {
        verifySetupWizardStep1(AppLanguage.BANGLA)
    }

    private fun verifySetupWizardStep1(lang: AppLanguage) {
        val baseCtx = ApplicationProvider.getApplicationContext<Context>()
        val locCtx = LocaleHelper.createLocalizedContext(baseCtx, lang)
        val state = SkipBuddyUiState(
            preferences = SkipBuddyPreferences(appLanguage = lang),
            permissions = PermissionStatusState(accessibilityGranted = false),
            preferencesLoaded = true
        )

        composeRule.setContent {
            CompositionLocalProvider(LocalContext provides locCtx) {
                SkipBuddyTheme {
                    SetupWizardScreen(
                        uiState = state,
                        onOpenAccessibilitySettings = {},
                        onOpenOverlaySettings = {},
                        onOpenBatterySettings = {},
                        onRequestOrOpenNotifications = {},
                        onOpenAutostartSettings = {},
                        onOpenAppSettings = {},
                        onSetAppLanguage = {},
                        onPlayTestSound = {},
                        onSkipWizard = {},
                        onFinishHideAndLock = {},
                        onFinishKeepAutoHideLocked = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("wizard_action_accessibility").assertIsDisplayed()
        composeRule.onNodeWithTag("wizard_restricted_help").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("wizard_restricted_help_open_app_settings").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_390dp_english_allSixCardsCleanlyOrdered() {
        verifySettingsScreen(AppLanguage.ENGLISH)
    }

    @Test
    fun settingsScreen_390dp_bangla_allSixCardsCleanlyOrdered() {
        verifySettingsScreen(AppLanguage.BANGLA)
    }

    private fun verifySettingsScreen(lang: AppLanguage) {
        val baseCtx = ApplicationProvider.getApplicationContext<Context>()
        val locCtx = LocaleHelper.createLocalizedContext(baseCtx, lang)
        val state = SkipBuddyUiState(
            preferences = SkipBuddyPreferences(appLanguage = lang),
            permissions = PermissionStatusState(),
            preferencesLoaded = true
        )

        composeRule.setContent {
            CompositionLocalProvider(LocalContext provides locCtx) {
                SkipBuddyTheme {
                    SettingsScreen(
                        uiState = state,
                        onSetAutoSkip = {},
                        onSetYoutubeMusicEnabled = {},
                        onSetClickDelay = {},
                        onSetMuteUnskippable = {},
                        onSetFloatingVisibility = {},
                        onSetFloatingLocked = {},
                        onSetFloatingSize = {},
                        onSetFloatingSide = {},
                        onSetFloatingOpacity = {},
                        onResetFloatingPosition = {},
                        onSetFeedbackMode = {},
                        onSetShowAdSkippedMessage = {},
                        onOpenAccessibilitySettings = {},
                        onOpenOverlaySettings = {},
                        onOpenBatterySettings = {},
                        onOpenNotificationSettings = {},
                        onSetAppLanguage = {},
                        onSetVoiceAnnouncement = {},
                        onSetShowAdBanner = {},
                        onPlayTestSound = {},
                        onResetToday = {},
                        onResetAll = {},
                        onClearLog = {},
                        onSetDebugLogEnabled = {},
                        onShareDebugLog = { false },
                        modifier = Modifier.testTag("settings_list")
                    )
                }
            }
        }
        composeRule.waitForIdle()

        val card1Bounds = composeRule.onNodeWithTag("settings_card_1").getUnclippedBoundsInRoot()
        assertTrue("Settings Card 1 fits 390dp width in $lang", card1Bounds.right <= 391.dp)
        composeRule.onNodeWithTag("switch_youtube_music").assertIsDisplayed()

        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_card_4"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_card_4").assertIsDisplayed()

        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_card_5"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_card_5").assertIsDisplayed()

        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_card_6"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_card_6").assertIsDisplayed()
    }
}
