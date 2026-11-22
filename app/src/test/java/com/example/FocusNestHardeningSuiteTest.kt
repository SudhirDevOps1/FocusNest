package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.AppUsageEntity
import com.example.core.database.BlockProfileEntity
import com.example.core.database.BlockingScheduleEntity
import com.example.core.database.InstalledAppEntity
import com.example.core.database.WebsiteRuleEntity
import com.example.core.datastore.FocusNestPreferences
import com.example.core.datastore.FocusNestSettings
import com.example.core.datastore.PersistedTimerState
import com.example.core.permissions.PermissionKind
import com.example.core.permissions.PermissionStatusManager
import com.example.domain.blocker.BlockingScheduler
import com.example.domain.blocker.BlockingStateManager
import com.example.domain.blocker.EmergencyAccessManager
import com.example.domain.blocker.RuleEvaluationInput
import com.example.domain.blocker.RuleEvaluator
import com.example.domain.model.AppPackageCategory
import com.example.domain.model.FocusTimeUtils
import com.example.domain.model.PermissionSnapshot
import com.example.domain.model.StartupPhase
import com.example.domain.model.SystemEnvironmentSnapshot
import com.example.domain.model.SystemPackageClassifier
import com.example.domain.model.WebsiteRuleOutcome
import com.example.feature.challenges.YuvLuminanceExtractor
import com.example.ui.FocusNestViewModel
import java.nio.ByteBuffer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FocusNestHardeningSuiteTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun startupPhaseResolution_preventsReturningUserFlashAndSupportsRecovery() {
    assertEquals(
      StartupPhase.INITIALIZING,
      FocusNestPreferences.resolveStartupPhase(isLoaded = false, settings = FocusNestSettings()),
    )
    assertEquals(
      StartupPhase.FIRST_RUN,
      FocusNestPreferences.resolveStartupPhase(isLoaded = true, settings = FocusNestSettings(onboardingCompleted = false)),
    )
    assertEquals(
      StartupPhase.ONBOARDING,
      FocusNestPreferences.resolveStartupPhase(
        isLoaded = true,
        settings = FocusNestSettings(onboardingCompleted = false, onboardingVersion = 1, onboardingCompletedEpochMs = 100L),
      ),
    )
    assertEquals(
      StartupPhase.READY,
      FocusNestPreferences.resolveStartupPhase(
        isLoaded = true,
        settings = FocusNestSettings(onboardingCompleted = true, onboardingVersion = 2, onboardingCompletedEpochMs = 200L),
      ),
    )
    assertEquals(
      StartupPhase.RECOVERY,
      FocusNestPreferences.resolveStartupPhase(
        isLoaded = true,
        settings = FocusNestSettings(onboardingCompleted = true),
        readError = true,
      ),
    )
  }

  @Test
  fun onboardingCompletion_persistsVersionAndTimestampAcrossReload() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = FocusNestPreferences(context)
    prefs.clearAll()
    prefs.markOnboardingCompleted(version = 2, epochMs = 1_760_000_000_000L)
    val loaded = prefs.settingsFlow.first()
    assertTrue(loaded.onboardingCompleted)
    assertEquals(2, loaded.onboardingVersion)
    assertEquals(1_760_000_000_000L, loaded.onboardingCompletedEpochMs)
    assertEquals(StartupPhase.READY, FocusNestPreferences.resolveStartupPhase(true, loaded))
  }

  @Test
  fun permissionStatusManager_separatesRuntimeVsSpecialAndDetectsRevocations() {
    val prev = PermissionSnapshot(
      usageStatsGranted = true,
      accessibilityEnabled = true,
      cameraGranted = true,
      postNotificationsGranted = true,
    )
    val next = PermissionSnapshot(
      usageStatsGranted = false,
      accessibilityEnabled = true,
      cameraGranted = false,
      postNotificationsGranted = true,
    )
    val revoked = PermissionStatusManager.detectRevocations(prev, next)
    assertEquals(setOf("Usage Access", "Camera"), revoked)

    val diagnostics = PermissionStatusManager.buildDiagnostics(next.copy(revokedPermissions = revoked))
    assertEquals(PermissionKind.RUNTIME, diagnostics.first { it.id == "camera" }.kind)
    assertEquals(PermissionKind.SPECIAL_ACCESS, diagnostics.first { it.id == "usage_access" }.kind)
    assertTrue(diagnostics.first { it.id == "usage_access" }.wasRevoked)
    assertFalse(diagnostics.first { it.id == "accessibility" }.wasRevoked)
  }

  @Test
  fun yuvLuminanceExtractor_compactsRowStridePaddingAndPixelStride() {
    // 4x3 image with rowStride = 6 (2 padding bytes per row) and pixelStride = 1
    val raw = byteArrayOf(
      1, 2, 3, 4, 99, 99,
      5, 6, 7, 8, 99, 99,
      9, 10, 11, 12, 99, 99,
    )
    val extracted = YuvLuminanceExtractor.extractNormalizedLuminance(
      buffer = ByteBuffer.wrap(raw),
      width = 4,
      height = 3,
      rowStride = 6,
      pixelStride = 1,
    )!!
    assertEquals(12, extracted.size)
    assertEquals(listOf<Byte>(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), extracted.toList())

    // Interleaved pixelStride = 2 (3x2 image)
    val interleaved = byteArrayOf(
      10, 0, 20, 0, 30, 0, 0, 0,
      40, 0, 50, 0, 60, 0, 0, 0,
    )
    val compact2 = YuvLuminanceExtractor.extractNormalizedLuminance(
      buffer = ByteBuffer.wrap(interleaved),
      width = 3,
      height = 2,
      rowStride = 8,
      pixelStride = 2,
    )!!
    assertEquals(listOf<Byte>(10, 20, 30, 40, 50, 60), compact2.toList())
  }

  @Test
  fun systemPackageClassifier_excludesLauncherSystemUiAndImeByDefaultAndSupportsToggle() {
    val env = SystemEnvironmentSnapshot(
      selfPackage = "com.aistudio.focusnest.test",
      defaultHomePackage = "com.google.android.apps.nexuslauncher",
      homePackages = setOf("com.google.android.apps.nexuslauncher", "com.sec.android.app.launcher"),
      inputMethodPackages = setOf("com.google.android.inputmethod.latin"),
      permissionControllerPackages = setOf("com.google.android.permissioncontroller"),
    )
    assertEquals(
      AppPackageCategory.LAUNCHER_HOME,
      SystemPackageClassifier.classify("com.google.android.apps.nexuslauncher", isSystemFlag = true, env = env),
    )
    assertEquals(
      AppPackageCategory.INPUT_METHOD,
      SystemPackageClassifier.classify("com.google.android.inputmethod.latin", isSystemFlag = false, env = env),
    )
    assertEquals(
      AppPackageCategory.USER_APP,
      SystemPackageClassifier.classify("com.instagram.android", isSystemFlag = false, env = env),
    )

    val today = FocusTimeUtils.todayKey()
    val usage = listOf(
      AppUsageEntity(today, "com.google.android.apps.nexuslauncher", "Pixel Launcher", "OTHER", foregroundTimeMs = 900_000L),
      AppUsageEntity(today, "com.android.systemui", "System UI", "OTHER", foregroundTimeMs = 300_000L),
      AppUsageEntity(today, "com.instagram.android", "Instagram", "SOCIAL", foregroundTimeMs = 600_000L),
    )
    val filteredDefault = SystemPackageClassifier.filterUsageForDisplay(usage, emptyMap(), includeSystemApps = false, env = env)
    assertEquals(listOf("com.instagram.android"), filteredDefault.map { it.packageName })

    val shownAll = SystemPackageClassifier.filterUsageForDisplay(usage, emptyMap(), includeSystemApps = true, env = env)
    assertEquals(3, shownAll.size)

    // RuleEvaluator must NEVER block the home launcher even if a user accidentally saved a block rule
    val decision = RuleEvaluator.evaluateAppBlock(
      RuleEvaluationInput(
        packageName = "com.google.android.apps.nexuslauncher",
        nowEpochMs = System.currentTimeMillis(),
        settings = FocusNestSettings(quickBlockerEnabled = true),
        timer = PersistedTimerState(),
        installedApp = InstalledAppEntity("com.google.android.apps.nexuslauncher", "Pixel Launcher", "OTHER", isSelectedDistracting = true),
        appLimit = null,
        profiles = listOf(BlockProfileEntity(id = 1, name = "All", presetType = "Work", blockedPackages = listOf("com.google.android.apps.nexuslauncher"), isActive = true)),
        schedules = emptyList(),
        appGroups = emptyList(),
        categoryLimit = null,
        usageToday = emptyList(),
        usedMinutesWeek = 0,
        systemEnvironment = env,
      )
    )
    assertFalse(decision.isBlocked)
  }

  @Test
  fun blockerEngine_evaluatesDomainAllowlistOverrideSchedulesCooldownAndEmergencyPasscode() {
    val rules = listOf(
      WebsiteRuleEntity(domain = "youtube.com", category = "Video", isAllowlist = false, isEnabled = true),
      WebsiteRuleEntity(domain = "music.youtube.com", category = "Study Music", isAllowlist = true, isEnabled = true),
    )
    assertEquals(WebsiteRuleOutcome.BLOCKED, RuleEvaluator.evaluateDomain("https://m.youtube.com/shorts/123", rules).outcome)
    assertEquals(WebsiteRuleOutcome.ALLOWLISTED, RuleEvaluator.evaluateDomain("music.youtube.com", rules).outcome)
    assertEquals(WebsiteRuleOutcome.ALLOWED_NO_MATCH, RuleEvaluator.evaluateDomain("wikipedia.org", rules).outcome)

    // Overnight schedule 22:00 -> 06:00
    assertTrue(BlockingScheduler.isMinuteInWindow(23 * 60 + 30, 22 * 60, 6 * 60))
    assertTrue(BlockingScheduler.isMinuteInWindow(5 * 60, 22 * 60, 6 * 60))
    assertFalse(BlockingScheduler.isMinuteInWindow(12 * 60, 22 * 60, 6 * 60))

    // Cooldown overlay deduplication
    BlockingStateManager.resetForTesting()
    assertTrue(BlockingStateManager.shouldDispatchOverlay("com.app", 10_000L, cooldownMs = 2500L))
    assertFalse(BlockingStateManager.shouldDispatchOverlay("com.app", 11_000L, cooldownMs = 2500L))
    assertTrue(BlockingStateManager.shouldDispatchOverlay("com.app", 13_000L, cooldownMs = 2500L))

    // Emergency passcode check
    val hash = com.example.core.common.KeystoreAndBackupCrypto.sha256Hex("8492")
    assertTrue(EmergencyAccessManager.verifyPasscode("8492", hash))
    assertFalse(EmergencyAccessManager.verifyPasscode("0000", hash))
  }

  @Test
  fun replayTutorialWebsiteTesterAndSystemToggle_workInComposeUi() {
    val app = ApplicationProvider.getApplicationContext<FocusNestApplication>()
    kotlinx.coroutines.runBlocking {
      app.container.preferences.clearAll()
      app.container.preferences.markOnboardingCompleted()
    }
    val vm = FocusNestViewModel(app)

    composeTestRule.setContent {
      FocusNestAppRoot(viewModel = vm, useNavRail = false)
    }

    // Completed user skips splash directly to Home Dashboard without flashing Welcome onboarding
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithText("Today's Screen Time").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Today's Screen Time").assertIsDisplayed()

    // Verify website domain verification UI in Blocker -> Websites (VPN)
    composeTestRule.onNodeWithTag("nav_blocker").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("blocker_tab_3").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("website_lazy_list").performScrollToNode(hasTestTag("website_verify_input"))
    composeTestRule.onNodeWithTag("website_verify_input").performTextInput("https://wikipedia.org/wiki/Focus")
    composeTestRule.onNodeWithTag("website_verify_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("website_lazy_list").performScrollToNode(hasTestTag("website_verify_result"))
    composeTestRule.onNodeWithTag("website_verify_result").assertIsDisplayed()

    // Toggle system apps filter in Insights
    composeTestRule.onNodeWithTag("nav_insights").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("insights_show_system_chip").performClick()
    composeTestRule.waitForIdle()

    // Open More -> Settings -> Replay Onboarding Tutorial without clearing existing data
    composeTestRule.onNodeWithTag("nav_more").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("more_hub_lazy_list").performScrollToNode(hasTestTag("more_tile_settings"))
    composeTestRule.onNodeWithTag("more_tile_settings").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_lazy_list").performScrollToNode(hasTestTag("settings_replay_tutorial_btn"))
    composeTestRule.onNodeWithTag("settings_replay_tutorial_btn").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithText("Welcome to FocusNest").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Welcome to FocusNest").assertIsDisplayed()
    // Dismiss tutorial back to Settings
    composeTestRule.onNodeWithTag("onboarding_skip_all_btn").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodesWithTag("settings_lazy_list").fetchSemanticsNodes().isNotEmpty()
    }
  }
}
