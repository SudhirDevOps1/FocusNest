package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.FocusNestApplication
import com.example.domain.model.LaunchPauseCoordinator
import com.example.domain.model.ReelsTargetApp
import com.example.ui.blocker.BlockedOverlayActivity
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Narrowly scoped, privacy-first AccessibilityService.
 * - Never logs, stores, or transmits screen text or user inputs.
 * - Only checks foreground package transitions against user-configured blocking rules,
 *   Mindful App-Launch Pause packages, and short-form video view IDs.
 */
class AppBlockerAccessibilityService : AccessibilityService() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var lastBlockedPackage: String = ""
  private var lastBlockedAtMs: Long = 0L
  private var lastShortFormAlertMs: Long = 0L

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    val pkg = event?.packageName?.toString() ?: return
    if (pkg == packageName || pkg == "com.android.systemui") return

    val app = applicationContext as? FocusNestApplication ?: return
    val now = System.currentTimeMillis()

    if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
      serviceScope.launch {
        val decision = app.container.blockerRepository.evaluatePackageDecision(pkg, now)
        if (decision.isBlocked) {
          if (pkg == lastBlockedPackage && now - lastBlockedAtMs < 2500L) return@launch
          lastBlockedPackage = pkg
          lastBlockedAtMs = now
          app.container.blockerRepository.recordBlockedAttempt(pkg)
          launchBlockedScreen(
            packageName = pkg,
            reason = decision.reason,
            profileName = decision.profileName ?: "FocusNest Shield",
            challengeType = decision.challengeType.name,
            isReelsPause = false,
            isLaunchPause = false,
            launchPauseSeconds = 8,
          )
          return@launch
        }

        val settings = app.container.preferences.settingsFlow.first()
        val lastProceed = launchPauseCooldowns[pkg]
        if (
          LaunchPauseCoordinator.shouldTriggerPause(
            packageName = pkg,
            launchPauseEnabled = settings.launchPauseEnabled,
            pausedPackages = settings.launchPausePackages,
            lastProceedEpochMs = lastProceed,
            nowEpochMs = now,
          )
        ) {
          if (pkg == lastBlockedPackage && now - lastBlockedAtMs < 2500L) return@launch
          lastBlockedPackage = pkg
          lastBlockedAtMs = now
          launchBlockedScreen(
            packageName = pkg,
            reason = "Mindful ${settings.launchPauseSeconds}s breath before entering",
            profileName = "Mindful App-Launch Pause",
            challengeType = "BREATHING",
            isReelsPause = false,
            isLaunchPause = true,
            launchPauseSeconds = settings.launchPauseSeconds,
          )
          return@launch
        }
      }
    }

    if (
      event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
        event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
    ) {
      val target = ReelsTargetApp.entries.firstOrNull { it.packageName == pkg } ?: return
      serviceScope.launch {
        val settings = app.container.preferences.settingsFlow.first()
        if (!settings.reelsReductionEnabled) return@launch
        val rule =
          app.container.database.shortFormRuleDao().getAll().firstOrNull {
            it.packageName == pkg && it.isEnabled
          } ?: return@launch

        val root = rootInActiveWindow ?: return@launch
        val detected = containsShortFormViewId(root, target.viewIdHints, depth = 0)
        if (detected && now - lastShortFormAlertMs > 12_000L) {
          lastShortFormAlertMs = now
          val updated = app.container.blockerRepository.recordShortFormInteraction(pkg)
          val count = updated?.interactionsToday ?: rule.interactionsToday
          val overLimit = count > rule.dailyInteractionLimit
          launchBlockedScreen(
            packageName = pkg,
            reason =
              if (overLimit) {
                "${target.displayName} ${target.featureName} daily cap ($count/${rule.dailyInteractionLimit}) reached"
              } else {
                "Mindful ${target.featureName} pause ($count/${rule.dailyInteractionLimit} today)"
              },
            profileName = "Reels & Shorts Reduction",
            challengeType = "REFLECTION_REMINDER",
            isReelsPause = true,
            isLaunchPause = false,
            launchPauseSeconds = 8,
          )
        }
      }
    }
  }

  private fun containsShortFormViewId(
    node: AccessibilityNodeInfo?,
    hints: List<String>,
    depth: Int,
  ): Boolean {
    if (node == null || depth > 8) return false
    val viewId = node.viewIdResourceName?.lowercase()
    if (viewId != null && hints.any { viewId.contains(it) }) {
      return true
    }
    for (i in 0 until node.childCount.coerceAtMost(12)) {
      val child = node.getChild(i)
      if (containsShortFormViewId(child, hints, depth + 1)) {
        return true
      }
    }
    return false
  }

  private fun launchBlockedScreen(
    packageName: String,
    reason: String,
    profileName: String,
    challengeType: String,
    isReelsPause: Boolean,
    isLaunchPause: Boolean,
    launchPauseSeconds: Int,
  ) {
    val intent =
      Intent(this, BlockedOverlayActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        putExtra(BlockedOverlayActivity.EXTRA_PACKAGE_NAME, packageName)
        putExtra(BlockedOverlayActivity.EXTRA_REASON, reason)
        putExtra(BlockedOverlayActivity.EXTRA_PROFILE_NAME, profileName)
        putExtra(BlockedOverlayActivity.EXTRA_CHALLENGE_TYPE, challengeType)
        putExtra(BlockedOverlayActivity.EXTRA_IS_REELS_PAUSE, isReelsPause)
        putExtra(BlockedOverlayActivity.EXTRA_IS_LAUNCH_PAUSE, isLaunchPause)
        putExtra(BlockedOverlayActivity.EXTRA_LAUNCH_PAUSE_SECONDS, launchPauseSeconds)
      }
    startActivity(intent)
  }

  override fun onInterrupt() = Unit

  override fun onDestroy() {
    super.onDestroy()
    serviceScope.cancel()
  }

  companion object {
    private val launchPauseCooldowns = ConcurrentHashMap<String, Long>()

    fun markLaunchPauseProceed(packageName: String, nowEpochMs: Long = System.currentTimeMillis()) {
      launchPauseCooldowns[packageName] = nowEpochMs
    }
  }
}
