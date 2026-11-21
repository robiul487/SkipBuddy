package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainTab
import com.example.ui.SkipBuddyViewModel
import com.example.ui.screens.GuideScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SetupWizardScreen
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SkipBuddyTheme
import com.example.ui.theme.SurfaceBackground
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextSecondary
import com.example.util.LocaleHelper
import com.example.util.PermissionChecker

class MainActivity : ComponentActivity() {

    private val viewModel: SkipBuddyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val baseContext = LocalContext.current

            // Refresh all permissions automatically every time user returns from Android Settings
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }

            // Dynamic zero-restart per-app locale Context wrapping all Compose stringResource() calls
            val localizedContext = remember(uiState.preferences.appLanguage) {
                LocaleHelper.createLocalizedContext(baseContext, uiState.preferences.appLanguage)
            }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ ->
                viewModel.refreshPermissions()
            }

            val requestOrOpenNotifications: () -> Unit = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !uiState.permissions.notificationsGranted
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    PermissionChecker.openNotificationSettings(baseContext)
                }
            }

            CompositionLocalProvider(LocalContext provides localizedContext) {
                SkipBuddyTheme {
                    if (uiState.shouldShowWizard) {
                        SetupWizardScreen(
                            uiState = uiState,
                            onOpenAccessibilitySettings = {
                                PermissionChecker.openAccessibilitySettings(baseContext)
                            },
                            onOpenOverlaySettings = {
                                PermissionChecker.openOverlaySettings(baseContext)
                            },
                            onOpenBatterySettings = {
                                PermissionChecker.openBatteryOptimizationSettings(baseContext)
                            },
                            onRequestOrOpenNotifications = requestOrOpenNotifications,
                            onOpenAutostartSettings = {
                                PermissionChecker.openAutostartOrAppInfo(baseContext)
                            },
                            onOpenAppSettings = {
                                PermissionChecker.openAppDetailsSettings(baseContext)
                            },
                            onSetAppLanguage = viewModel::setAppLanguage,
                            onPlayTestSound = viewModel::playTestSound,
                            onSkipWizard = viewModel::skipOrCloseWizard,
                            onFinishHideAndLock = viewModel::finishWizardHideAndLock,
                            onFinishKeepAutoHideLocked = viewModel::finishWizardKeepAutoHideLocked
                        )
                    } else {
                        SkipBuddyMainShell(
                            uiState = uiState,
                            viewModel = viewModel,
                            onOpenAccessibilitySettings = {
                                PermissionChecker.openAccessibilitySettings(baseContext)
                            },
                            onOpenOverlaySettings = {
                                PermissionChecker.openOverlaySettings(baseContext)
                            },
                            onOpenBatterySettings = {
                                PermissionChecker.openBatteryOptimizationSettings(baseContext)
                            },
                            onOpenNotificationSettings = requestOrOpenNotifications,
                            onOpenAppSettings = {
                                PermissionChecker.openAppDetailsSettings(baseContext)
                            },
                            onShareDebugLog = { viewModel.shareDebugLog(localizedContext) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkipBuddyMainShell(
    uiState: com.example.ui.SkipBuddyUiState,
    viewModel: SkipBuddyViewModel,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onShareDebugLog: () -> Boolean
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }

    // Hardware/gesture back on secondary tabs returns cleanly to Home
    BackHandler(enabled = selectedTab != MainTab.HOME) {
        selectedTab = MainTab.HOME
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 700.dp

        Scaffold(
            containerColor = SurfaceBackground,
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SurfaceCard
                    )
                )
            },
            bottomBar = {
                if (!useRail) {
                    Surface(
                        color = SurfaceCard,
                        shadowElevation = 3.dp
                    ) {
                        NavigationBar(
                            containerColor = SurfaceCard,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            MainTab.entries.forEach { tab ->
                                val selected = tab == selectedTab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { selectedTab = tab },
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                                    icon = {
                                        Icon(
                                            imageVector = tabIcon(tab, selected),
                                            contentDescription = stringResource(tabLabelRes(tab)),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(tabLabelRes(tab)),
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = EmeraldPrimary,
                                        selectedTextColor = EmeraldPrimary,
                                        indicatorColor = EmeraldContainer,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = SurfaceCard,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = tab == selectedTab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}"),
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = stringResource(tabLabelRes(tab)),
                                        modifier = Modifier.size(26.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tabLabelRes(tab)),
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = EmeraldPrimary,
                                    selectedTextColor = EmeraldPrimary,
                                    indicatorColor = EmeraldContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (selectedTab) {
                        MainTab.HOME -> HomeScreen(
                            uiState = uiState,
                            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                            onOpenAppSettings = onOpenAppSettings,
                            onToggleAutoSkip = viewModel::setAutoSkipEnabled
                        )

                        MainTab.GUIDE -> GuideScreen(
                            uiState = uiState,
                            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                            onOpenOverlaySettings = onOpenOverlaySettings,
                            onOpenBatterySettings = onOpenBatterySettings,
                            onOpenNotificationSettings = onOpenNotificationSettings,
                            onOpenAppSettings = onOpenAppSettings,
                            onReopenSetupWizard = viewModel::openSetupWizard
                        )

                        MainTab.SETTINGS -> SettingsScreen(
                            uiState = uiState,
                            onSetAutoSkip = viewModel::setAutoSkipEnabled,
                            onSetClickDelay = viewModel::setClickDelay,
                            onSetMuteUnskippable = viewModel::setMuteUnskippableAds,
                            onSetFloatingVisibility = viewModel::setFloatingVisibility,
                            onSetFloatingLocked = viewModel::setFloatingLocked,
                            onSetFloatingSize = viewModel::setFloatingSize,
                            onSetFloatingSide = viewModel::setFloatingSide,
                            onSetFloatingOpacity = viewModel::setFloatingOpacity,
                            onResetFloatingPosition = viewModel::resetFloatingPosition,
                            onSetFeedbackMode = viewModel::setFeedbackMode,
                            onSetShowAdSkippedMessage = viewModel::setShowAdSkippedMessage,
                            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                            onOpenOverlaySettings = onOpenOverlaySettings,
                            onOpenBatterySettings = onOpenBatterySettings,
                            onOpenNotificationSettings = onOpenNotificationSettings,
                            onSetAppLanguage = viewModel::setAppLanguage,
                            onSetVoiceAnnouncement = viewModel::setVoiceAnnouncementEnabled,
                            onSetShowAdBanner = viewModel::setShowAdBannerEnabled,
                            onPlayTestSound = viewModel::playTestSound,
                            onResetToday = viewModel::resetTodayStats,
                            onResetAll = viewModel::resetAllStats,
                            onClearLog = viewModel::clearRecentActivity,
                            onSetDebugLogEnabled = viewModel::setDebugLogEnabled,
                            onShareDebugLog = onShareDebugLog
                        )
                    }
                }
            }
        }
    }
}

private fun tabLabelRes(tab: MainTab): Int = when (tab) {
    MainTab.HOME -> R.string.nav_home
    MainTab.GUIDE -> R.string.nav_guide
    MainTab.SETTINGS -> R.string.nav_settings
}

private fun tabIcon(tab: MainTab, selected: Boolean) = when (tab) {
    MainTab.HOME -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
    MainTab.GUIDE -> if (selected) Icons.AutoMirrored.Rounded.MenuBook else Icons.AutoMirrored.Outlined.MenuBook
    MainTab.SETTINGS -> if (selected) Icons.Rounded.Settings else Icons.Outlined.Settings
}
