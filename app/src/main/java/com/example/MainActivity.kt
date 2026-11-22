package com.example

import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.domain.model.FocusTimeUtils
import com.example.domain.model.TimerMode
import com.example.feature.blocker.BlockerScreen
import com.example.feature.focus.FocusScreen
import com.example.feature.habits.HabitsScreen
import com.example.feature.home.HomeDashboardScreen
import com.example.feature.insights.InsightsScreen
import com.example.feature.more.MoreScreen
import com.example.feature.onboarding.OnboardingScreen
import com.example.ui.FocusNestViewModel
import com.example.ui.theme.FocusNestTheme
import java.util.Locale

sealed class MainTab(
  val route: String,
  @androidx.annotation.StringRes val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
) {
  data object Home : MainTab("home", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home)
  data object Focus : MainTab("focus", R.string.nav_focus, Icons.Filled.Timer, Icons.Outlined.Timer)
  data object Blocker : MainTab("blocker", R.string.nav_blocker, Icons.Filled.Shield, Icons.Outlined.Shield)
  data object Insights : MainTab("insights", R.string.nav_insights, Icons.Filled.Insights, Icons.Outlined.Insights)
  data object More : MainTab("more", R.string.nav_more, Icons.Filled.Menu, Icons.Outlined.Menu)
}

class MainActivity : FragmentActivity() {
  private val viewModel: FocusNestViewModel by viewModels()
  private var nfcAdapter: NfcAdapter? = null

  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    nfcAdapter = runCatching { NfcAdapter.getDefaultAdapter(this) }.getOrNull()
    handleNfcIntent(intent)

    setContent {
      val widthClass = calculateWindowSizeClass(this).widthSizeClass
      FocusNestAppRoot(viewModel = viewModel, useNavRail = widthClass != WindowWidthSizeClass.Compact)
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.refreshPermissionsAndSync()
    nfcAdapter?.let { adapter ->
      runCatching {
        val pending =
          PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE,
          )
        adapter.enableForegroundDispatch(this, pending, null, null)
      }
    }
  }

  override fun onPause() {
    super.onPause()
    nfcAdapter?.let { runCatching { it.disableForegroundDispatch(this) } }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleNfcIntent(intent)
  }

  private fun handleNfcIntent(intent: Intent?) {
    val tag: Tag? =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
      } else {
        @Suppress("DEPRECATION")
        intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG)
      }
    tag?.id?.let { bytes ->
      val uidHex = bytes.joinToString("") { "%02X".format(it) }
      viewModel.onNfcTagScanned(uidHex)
    }
  }
}

@Composable
fun FocusNestAppRoot(
  viewModel: FocusNestViewModel,
  useNavRail: Boolean,
) {
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val timerState by viewModel.timerState.collectAsStateWithLifecycle()
  val permissions by viewModel.permissions.collectAsStateWithLifecycle()
  val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
  val todayUsage by viewModel.todayUsage.collectAsStateWithLifecycle()
  val yesterdayUsage by viewModel.yesterdayUsage.collectAsStateWithLifecycle()
  val weeklyUsage by viewModel.weeklyUsage.collectAsStateWithLifecycle()
  val monthlyUsage by viewModel.monthlyUsage.collectAsStateWithLifecycle()
  val appLimits by viewModel.appLimits.collectAsStateWithLifecycle()
  val categoryLimits by viewModel.categoryLimits.collectAsStateWithLifecycle()
  val appGroups by viewModel.appGroups.collectAsStateWithLifecycle()
  val profiles by viewModel.profiles.collectAsStateWithLifecycle()
  val schedules by viewModel.schedules.collectAsStateWithLifecycle()
  val focusSessions by viewModel.focusSessions.collectAsStateWithLifecycle()
  val focusGoal by viewModel.focusGoal.collectAsStateWithLifecycle()
  val habits by viewModel.habits.collectAsStateWithLifecycle()
  val habitCompletions by viewModel.habitCompletions.collectAsStateWithLifecycle()
  val unlockHistory by viewModel.unlockHistory.collectAsStateWithLifecycle()
  val websiteRules by viewModel.websiteRules.collectAsStateWithLifecycle()
  val shortFormRules by viewModel.shortFormRules.collectAsStateWithLifecycle()
  val nfcTags by viewModel.nfcTags.collectAsStateWithLifecycle()
  val qrTokens by viewModel.qrTokens.collectAsStateWithLifecycle()
  val dailyAnalytics by viewModel.dailyAnalytics.collectAsStateWithLifecycle()
  val recentNotifications by viewModel.recentNotifications.collectAsStateWithLifecycle()
  val lastScannedNfcUid by viewModel.lastScannedNfcUid.collectAsStateWithLifecycle()
  val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

  val baseContext = LocalContext.current
  val localizedContext =
    remember(baseContext, settings.languageCode) {
      val targetLocale = Locale(settings.languageCode.ifBlank { "en" })
      val cfg = Configuration(baseContext.resources.configuration).apply { setLocale(targetLocale) }
      val localizedResources = baseContext.createConfigurationContext(cfg).resources
      object : ContextWrapper(baseContext) {
        override fun getResources(): Resources = localizedResources
      }
    }

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(statusBanner) {
    val msg = statusBanner
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusBanner()
    }
  }

  CompositionLocalProvider(LocalContext provides localizedContext) {
  FocusNestTheme(
    themeMode = settings.themeMode,
    accentPalette = settings.accentPalette,
    dynamicColor = settings.dynamicColorEnabled,
  ) {
    if (!settings.onboardingCompleted) {
      OnboardingScreen(
        permissions = permissions,
        installedApps = installedApps,
        onRefreshPermissions = viewModel::refreshPermissionsAndSync,
        onComplete = viewModel::completeOnboarding,
      )
      return@FocusNestTheme
    }

    val navController = rememberNavController()
    val tabs = listOf(MainTab.Home, MainTab.Focus, MainTab.Blocker, MainTab.Insights, MainTab.More)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination

    fun navigateTab(route: String) {
      navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
      }
    }

    Scaffold(
      snackbarHost = { SnackbarHost(snackbarHostState) },
      contentWindowInsets = WindowInsets.safeDrawing,
      bottomBar = {
        if (!useNavRail) {
          NavigationBar {
            tabs.forEach { tab ->
              val selected = currentDest?.hierarchy?.any { it.route == tab.route } == true
              val labelText = stringResource(tab.labelRes)
              NavigationBarItem(
                selected = selected,
                onClick = { navigateTab(tab.route) },
                icon = {
                  Icon(
                    if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = labelText,
                  )
                },
                label = { Text(labelText) },
                modifier = Modifier.testTag("nav_${tab.route}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useNavRail) {
          NavigationRail {
            tabs.forEach { tab ->
              val selected = currentDest?.hierarchy?.any { it.route == tab.route } == true
              val labelText = stringResource(tab.labelRes)
              NavigationRailItem(
                selected = selected,
                onClick = { navigateTab(tab.route) },
                icon = {
                  Icon(
                    if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = labelText,
                  )
                },
                label = { Text(labelText) },
                modifier = Modifier.testTag("nav_${tab.route}"),
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
          val todayAnalytics = dailyAnalytics.firstOrNull { it.dateKey == FocusTimeUtils.todayKey() }
          val streakDays = remember(focusSessions) { viewModel.computeStreakDays(focusSessions) }

          NavHost(navController = navController, startDestination = MainTab.Home.route) {
            composable(MainTab.Home.route) {
              HomeDashboardScreen(
                settings = settings,
                timerState = timerState,
                permissions = permissions,
                todayUsage = todayUsage,
                weeklyUsage = weeklyUsage,
                todayAnalytics = todayAnalytics,
                focusSessions = focusSessions,
                focusGoal = focusGoal,
                profiles = profiles,
                schedules = schedules,
                onRefreshUsage = viewModel::refreshUsageOnly,
                onQuickStartFocus = { type, mins ->
                  viewModel.startFocusSession(TimerMode.POMODORO, type, mins, "", null)
                  navigateTab(MainTab.Focus.route)
                },
                onToggleQuickBlocker = viewModel::toggleQuickBlocker,
                onSaveDailyIntention = viewModel::setDailyIntention,
                onNavigateToFocus = { navigateTab(MainTab.Focus.route) },
                onNavigateToBlocker = { navigateTab(MainTab.Blocker.route) },
                onNavigateToInsights = { navigateTab(MainTab.Insights.route) },
                yesterdayUsage = yesterdayUsage,
                appLimits = appLimits,
                habits = habits,
                onNavigateToHabits = { navController.navigate("habits") },
              )
            }
            composable(MainTab.Focus.route) {
              FocusScreen(
                settings = settings,
                timerState = timerState,
                sessions = focusSessions,
                goal = focusGoal,
                profiles = profiles,
                streakDays = streakDays,
                onStartTimer = viewModel::startFocusSession,
                onPauseTimer = viewModel::pauseFocusSession,
                onResumeTimer = viewModel::resumeFocusSession,
                onStopTimer = viewModel::stopFocusSession,
                onSavePomodoroConfig = viewModel::updatePomodoroSettings,
                onExtendTimer = viewModel::extendFocusSession,
                onRestartTimer = viewModel::restartFocusSession,
                onSkipBreak = viewModel::skipFocusBreak,
                onUpdateAmbientSound = viewModel::setAmbientSound,
              )
            }
            composable(MainTab.Blocker.route) {
              BlockerScreen(
                settings = settings,
                permissions = permissions,
                installedApps = installedApps,
                todayUsage = todayUsage,
                appLimits = appLimits,
                appGroups = appGroups,
                profiles = profiles,
                schedules = schedules,
                shortFormRules = shortFormRules,
                websiteRules = websiteRules,
                unlockHistory = unlockHistory,
                onRefreshPermissions = viewModel::refreshPermissionsAndSync,
                onToggleEmergencyWhitelist = viewModel::toggleEmergencyWhitelist,
                onSaveAppLimit = viewModel::saveAppLimit,
                onDeleteAppLimit = viewModel::deleteAppLimit,
                onSaveAppGroup = { name, pkgs, mins -> viewModel.saveAppGroup(name, pkgs, mins) },
                onDeleteAppGroup = viewModel::deleteAppGroup,
                onToggleProfileActive = viewModel::toggleProfileActive,
                onSaveProfile = viewModel::saveProfile,
                onDeleteProfile = viewModel::deleteProfile,
                onSaveSchedule = viewModel::saveSchedule,
                onDeleteSchedule = viewModel::deleteSchedule,
                onUpdateShortFormRule = viewModel::updateShortFormRule,
                onToggleReelsGlobal = viewModel::toggleReelsReductionGlobal,
                onAddWebsiteRule = viewModel::addWebsiteRule,
                onDeleteWebsiteRule = viewModel::deleteWebsiteRule,
                onInstallWebsitePack = viewModel::installWebsitePack,
                onStartVpn = viewModel::startLocalVpn,
                onStopVpn = viewModel::stopLocalVpn,
                categoryLimits = categoryLimits,
                onSaveCategoryLimit = viewModel::saveCategoryLimit,
                onDeleteCategoryLimit = viewModel::deleteCategoryLimit,
                onToggleBlockedNetworkPackage = viewModel::toggleBlockedNetworkPackage,
                onUpdateLaunchPauseConfig = viewModel::updateLaunchPauseConfig,
                onToggleLaunchPausePackage = viewModel::toggleLaunchPausePackage,
              )
            }
            composable(MainTab.Insights.route) {
              InsightsScreen(
                permissions = permissions,
                todayUsage = todayUsage,
                weeklyUsage = weeklyUsage,
                monthlyUsage = monthlyUsage,
                focusSessions = focusSessions,
                focusGoal = focusGoal,
                dailyAnalytics = dailyAnalytics,
                unlockHistory = unlockHistory,
                streakDays = streakDays,
                onComputeBestPeriod = viewModel::computeBestFocusPeriod,
                onBuildCsv = viewModel::buildCsvExport,
                onBuildJson = viewModel::buildJsonExport,
                onBuildPrintableHtml = viewModel::buildPrintableHtml,
                onShowMessage = viewModel::postMessage,
              )
            }
            composable(MainTab.More.route) {
              MoreScreen(
                settings = settings,
                permissions = permissions,
                focusGoal = focusGoal,
                profiles = profiles,
                nfcTags = nfcTags,
                qrTokens = qrTokens,
                lastScannedNfcUid = lastScannedNfcUid,
                recentNotifications = recentNotifications,
                onRefreshPermissions = viewModel::refreshPermissionsAndSync,
                onSaveNfcTag = viewModel::saveNfcTag,
                onDeleteNfcTag = viewModel::deleteNfcTag,
                onSaveQrToken = viewModel::saveQrOrBarcodeToken,
                onDeleteQrToken = viewModel::deleteQrToken,
                onEnableInvincible = viewModel::enableInvincibleMode,
                onRequestInvincibleUnlock = viewModel::requestInvincibleUnlockDelay,
                onCompleteInvincibleDisable = viewModel::completeInvincibleDisableIfReady,
                onEmergencyDisableInvincible = viewModel::emergencyDisableInvincibleWithPasscode,
                onUpdateMindful = viewModel::updateMindfulSettings,
                onUpdateAppearance = viewModel::updateAppearance,
                onUpdateGoals = viewModel::updateGoals,
                onBuildEncryptedBackup = viewModel::buildEncryptedBackup,
                onRestoreBackup = viewModel::restoreBackup,
                onDeleteAllData = viewModel::deleteAllData,
                onShowMessage = viewModel::postMessage,
                habitsCount = habits.size,
                onNavigateToHabits = { navController.navigate("habits") },
                onClearUsageHistory = viewModel::clearUsageHistory,
                onClearNotificationHistory = viewModel::clearNotificationHistory,
                onClearHabits = viewModel::clearHabits,
                onToggleWindDownStep = viewModel::toggleWindDownStep,
                onOpenReleases = viewModel::openReleasesInBrowser,
              )
            }
            composable("habits") {
              HabitsScreen(
                habits = habits,
                completions = habitCompletions,
                onSaveHabit = viewModel::saveHabit,
                onSeedStarterHabits = viewModel::seedStarterHabits,
                onDeleteHabit = viewModel::deleteHabit,
                onToggleHabitToday = viewModel::toggleHabitToday,
                onIncrementHabitToday = viewModel::incrementHabitToday,
                computeHabitStreak = viewModel::computeHabitStreak,
                computeWeeklyRates = viewModel::computeWeeklyHabitRates,
                onBack = { navController.popBackStack() },
              )
            }
          }
        }
      }
    }
  }
  }
}
