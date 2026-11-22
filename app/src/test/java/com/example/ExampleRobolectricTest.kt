package com.example

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Process
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.core.permissions.PermissionManager
import com.example.service.BootCompletedReceiver
import com.example.ui.FocusNestViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun appName_isFocusNest() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("FocusNest", context.getString(R.string.app_name))
  }

  @Test
  fun permissionChecks_handleMissingPermissionsAndNoNfcGracefully() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    Shadows.shadowOf(appOps)
      .setMode(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName,
        AppOpsManager.MODE_IGNORED,
      )
    val snapshot = PermissionManager.getSnapshot(context)
    assertFalse(snapshot.usageStatsGranted)
    assertFalse(snapshot.accessibilityEnabled)
    assertFalse(snapshot.notificationListenerEnabled)
    assertFalse(snapshot.vpnActive)
    assertFalse(snapshot.nfcSupported)
    runCatching { VpnService.prepare(context) }
  }

  @Test
  fun bootCompletedReceiver_handlesBootWithoutCrash() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val receiver = BootCompletedReceiver()
    receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
  }

  @Test
  fun onboardingToDashboard_andFiveTabNavigation_works() {
    val app = ApplicationProvider.getApplicationContext<FocusNestApplication>()
    val vm = FocusNestViewModel(app)

    composeTestRule.setContent {
      FocusNestAppRoot(viewModel = vm, useNavRail = false)
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Welcome to FocusNest").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_skip_all_btn").performClick()

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithText("Today's Screen Time").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Today's Screen Time").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_focus").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Focus Sanctuary").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_blocker").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Shield & Blocker").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_insights").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Insights & Reports").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_more").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("More & Sanctuary Controls").assertIsDisplayed()

    composeTestRule.onNodeWithTag("more_tile_habits").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("habits_seed_btn").performScrollTo().performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithText("7-Day Habit Completion Rate").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("habits_list").performScrollToNode(hasText("Morning Screen-Free Hour"))
    composeTestRule.onNodeWithText("Morning Screen-Free Hour").assertIsDisplayed()
    composeTestRule.onNodeWithTag("habits_list").performScrollToNode(hasText("0 / 3"))
    composeTestRule.onNodeWithText("0 / 3").assertIsDisplayed()
  }

  @Test
  fun hindiLocalizationResources_resolveAccurately() {
    val base = ApplicationProvider.getApplicationContext<Context>()
    val cfg = android.content.res.Configuration(base.resources.configuration).apply {
      setLocale(java.util.Locale.forLanguageTag("hi"))
    }
    val hiCtx = base.createConfigurationContext(cfg)
    assertEquals("होम", hiCtx.getString(R.string.nav_home))
    assertEquals("दैनिक और साप्ताहिक आदतें", hiCtx.getString(R.string.habits_title))
  }

  @Test
  fun launchPauseReportCardWindDownAndReleases_renderAndInteractInUi() {
    val app = ApplicationProvider.getApplicationContext<FocusNestApplication>()
    val vm = FocusNestViewModel(app)

    composeTestRule.setContent {
      FocusNestAppRoot(viewModel = vm, useNavRail = false)
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_skip_all_btn").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithText("Today's Screen Time").fetchSemanticsNodes().isNotEmpty()
    }

    // Focus tab has Offline Ambient Soundscapes
    composeTestRule.onNodeWithTag("nav_focus").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Offline Ambient Soundscape").performScrollTo().assertIsDisplayed()

    // Blocker tab has Mindful Launch Pause Curtain
    composeTestRule.onNodeWithTag("nav_blocker").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("blocker_launch_pause_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("blocker_launch_pause_switch").performClick()

    // Insights tab has Weekly Wellbeing Report Card Print button
    composeTestRule.onNodeWithTag("nav_insights").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("insights_print_btn").performScrollTo().assertIsDisplayed()

    // More -> Mindful Usage Tools -> Evening Wind-Down Checklist
    composeTestRule.onNodeWithTag("nav_more").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("more_tile_mindful").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("wind_down_step_charger_away").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("subpage_back_btn").performClick()

    // More -> Settings -> Check Releases on GitHub
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("more_tile_settings").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("more_check_updates_btn").performScrollTo().assertIsDisplayed()
  }

  @Test
  @Config(qualifiers = "w900dp-h1000dp")
  fun tabletWidth_rendersTwoPaneNavigationRailAndCanonicalLayouts() {
    val app = ApplicationProvider.getApplicationContext<FocusNestApplication>()
    val vm = FocusNestViewModel(app)

    composeTestRule.setContent {
      FocusNestAppRoot(viewModel = vm, useNavRail = true)
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_skip_all_btn").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithText("Today's Screen Time").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag("nav_more").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("More & Sanctuary Controls").assertIsDisplayed()
    // On two-pane width (>= 720dp), detail pane (Vault by default) is simultaneously visible beside Hub
    composeTestRule.onNodeWithText("Personal QR Codes & Barcodes (100% Offline)").assertIsDisplayed()
  }
}
