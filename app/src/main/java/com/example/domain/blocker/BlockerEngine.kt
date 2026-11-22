package com.example.domain.blocker

import com.example.core.common.KeystoreAndBackupCrypto
import com.example.core.database.AppGroupEntity
import com.example.core.database.AppLimitEntity
import com.example.core.database.AppUsageEntity
import com.example.core.database.BlockProfileEntity
import com.example.core.database.BlockingScheduleEntity
import com.example.core.database.CategoryLimitEntity
import com.example.core.database.InstalledAppEntity
import com.example.core.database.WebsiteRuleEntity
import com.example.core.datastore.FocusNestSettings
import com.example.core.datastore.PersistedTimerState
import com.example.domain.model.ActiveBlockDecision
import com.example.domain.model.AppPackageCategory
import com.example.domain.model.RulePriorityResolver
import com.example.domain.model.SystemEnvironmentSnapshot
import com.example.domain.model.SystemPackageClassifier
import com.example.domain.model.UnlockChallengeType
import com.example.domain.model.WebsiteRuleEvaluation
import com.example.domain.model.WebsiteRuleOutcome
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

data class RuleEvaluationInput(
  val packageName: String,
  val nowEpochMs: Long,
  val settings: FocusNestSettings,
  val timer: PersistedTimerState,
  val installedApp: InstalledAppEntity?,
  val appLimit: AppLimitEntity?,
  val profiles: List<BlockProfileEntity>,
  val schedules: List<BlockingScheduleEntity>,
  val appGroups: List<AppGroupEntity>,
  val categoryLimit: CategoryLimitEntity?,
  val usageToday: List<AppUsageEntity>,
  val usedMinutesWeek: Int,
  val systemEnvironment: SystemEnvironmentSnapshot? = null,
)

/**
 * Pure, deterministic evaluator for app blocking, category caps, schedules, and website domain rules.
 */
object RuleEvaluator {
  fun evaluateAppBlock(input: RuleEvaluationInput): ActiveBlockDecision {
    val pkg = input.packageName
    // Safety guard: never lock FocusNest, active Home launcher, SystemUI, IME, or PermissionController
    if (input.systemEnvironment != null) {
      val category =
        SystemPackageClassifier.classify(
          packageName = pkg,
          isSystemFlag = input.installedApp?.isSystemApp ?: false,
          env = input.systemEnvironment,
        )
      if (
        category == AppPackageCategory.SELF_APP ||
          category == AppPackageCategory.LAUNCHER_HOME ||
          category == AppPackageCategory.SYSTEM_UI_INFRASTRUCTURE ||
          category == AppPackageCategory.INPUT_METHOD ||
          category == AppPackageCategory.PERMISSION_CONTROLLER
      ) {
        return ActiveBlockDecision(isBlocked = false, reason = "${category.label} protected")
      }
    }

    if (input.installedApp?.isEmergencyWhitelisted == true) {
      return ActiveBlockDecision(isBlocked = false, reason = "Emergency whitelisted")
    }

    val usedMinutesToday =
      ((input.usageToday.firstOrNull { it.packageName == pkg }?.foregroundTimeMs ?: 0L) / 60_000L)
        .toInt()

    val matchingGroups = input.appGroups.filter { it.isEnabled && pkg in it.packageNames }
    val groupUsageMinutes =
      matchingGroups.maxOfOrNull { group ->
        input.usageToday.filter { it.packageName in group.packageNames }.sumOf { it.foregroundTimeMs } /
          60_000L
      }?.toInt() ?: 0
    val tightestGroupLimit =
      matchingGroups.filter { it.sharedDailyLimitMinutes > 0 }.minOfOrNull { it.sharedDailyLimitMinutes }

    val focusLinkedProfile =
      if (input.timer.isRunning && input.timer.phase.name == "FOCUS") {
        input.profiles.firstOrNull { it.id == input.timer.linkedProfileId }
          ?: input.profiles.firstOrNull { it.isActive }
      } else {
        null
      }

    val activeFocusBlocks =
      input.timer.isRunning &&
        input.timer.phase.name == "FOCUS" &&
        ((focusLinkedProfile != null && pkg in focusLinkedProfile.blockedPackages) ||
          (focusLinkedProfile == null && input.installedApp?.isSelectedDistracting == true))

    val activeProfiles =
      BlockingScheduler.activeProfilesAt(
        profiles = input.profiles,
        schedules = input.schedules,
        packageName = pkg,
        nowEpochMs = input.nowEpochMs,
      )
    val topProfile = activeProfiles.maxByOrNull { it.priority }

    val quickBlockerMatches =
      input.settings.quickBlockerEnabled &&
        (input.installedApp?.isSelectedDistracting == true || input.appLimit?.isBlockedManually == true)

    val matchedProfileName =
      topProfile?.name ?: if (quickBlockerMatches) "Quick Shield" else null
    val matchedChallenge =
      runCatching {
        UnlockChallengeType.valueOf(
          topProfile?.unlockPolicy
            ?: input.appLimit?.challengeType
            ?: UnlockChallengeType.REFLECTION_REMINDER.name
        )
      }.getOrDefault(UnlockChallengeType.REFLECTION_REMINDER)

    val effectiveDailyLimit =
      listOfNotNull(
        input.appLimit?.dailyLimitMinutes?.takeIf { it > 0 },
        tightestGroupLimit,
      ).minOrNull()
    val effectiveUsedToday = maxOf(usedMinutesToday, groupUsageMinutes)

    val catUsedMinutes =
      if (input.categoryLimit != null && input.categoryLimit.isEnabled && input.categoryLimit.dailyLimitMinutes > 0) {
        (input.usageToday
          .filter { it.category == input.categoryLimit.categoryName }
          .sumOf { it.foregroundTimeMs } / 60_000L)
          .toInt()
      } else {
        0
      }

    return RulePriorityResolver.resolve(
      packageName = pkg,
      tempUnlockExpiresAtEpochMs = input.appLimit?.tempUnlockUntilEpochMs,
      nowEpochMs = input.nowEpochMs,
      activeFocusBlocksPackage = activeFocusBlocks,
      activeFocusProfileName = focusLinkedProfile?.name ?: "Focus Session",
      matchingScheduledProfileName = matchedProfileName,
      matchingScheduledProfileId = topProfile?.id,
      matchingScheduledPriority = topProfile?.priority ?: 1,
      matchingChallenge = matchedChallenge,
      isStrictMode = input.settings.invincibleModeEnabled || (topProfile?.strictMode == true),
      usedMinutesToday = effectiveUsedToday,
      dailyLimitMinutes = effectiveDailyLimit,
      usedMinutesWeek = input.usedMinutesWeek,
      weeklyLimitMinutes = input.appLimit?.weeklyLimitMinutes?.takeIf { it > 0 },
      gracePeriodMinutes = input.appLimit?.gracePeriodMinutes ?: 0,
      categoryUsedMinutesToday = catUsedMinutes,
      categoryDailyLimitMinutes =
        input.categoryLimit?.takeIf { it.isEnabled && it.dailyLimitMinutes > 0 }?.dailyLimitMinutes,
      categoryName = input.categoryLimit?.categoryName,
    )
  }

  fun normalizeDomainInput(raw: String): String {
    return raw
      .trim()
      .lowercase()
      .removePrefix("https://")
      .removePrefix("http://")
      .substringBefore("/")
      .substringBefore("?")
      .substringBefore("#")
      .substringBefore(":")
      .removePrefix("*.")
      .trimEnd('.')
  }

  fun domainMatches(queried: String, ruleDomain: String): Boolean {
    val q = normalizeDomainInput(queried)
    val r = normalizeDomainInput(ruleDomain)
    if (q.isEmpty() || r.isEmpty()) return false
    return q == r || q.endsWith(".$r")
  }

  fun evaluateDomain(rawQuery: String, rules: List<WebsiteRuleEntity>): WebsiteRuleEvaluation {
    val normalized = normalizeDomainInput(rawQuery)
    val enabledRules = rules.filter { it.isEnabled }
    if (normalized.isEmpty()) {
      return WebsiteRuleEvaluation(
        rawQuery = rawQuery,
        normalizedDomain = normalized,
        outcome = WebsiteRuleOutcome.ALLOWED_NO_MATCH,
        evaluatedRuleCount = enabledRules.size,
      )
    }
    val allowMatch = enabledRules.firstOrNull { it.isAllowlist && domainMatches(normalized, it.domain) }
    if (allowMatch != null) {
      return WebsiteRuleEvaluation(
        rawQuery = rawQuery,
        normalizedDomain = normalized,
        outcome = WebsiteRuleOutcome.ALLOWLISTED,
        matchedRuleDomain = allowMatch.domain,
        matchedRuleCategory = allowMatch.category,
        evaluatedRuleCount = enabledRules.size,
      )
    }
    val blockMatch = enabledRules.firstOrNull { !it.isAllowlist && domainMatches(normalized, it.domain) }
    if (blockMatch != null) {
      return WebsiteRuleEvaluation(
        rawQuery = rawQuery,
        normalizedDomain = normalized,
        outcome = WebsiteRuleOutcome.BLOCKED,
        matchedRuleDomain = blockMatch.domain,
        matchedRuleCategory = blockMatch.category,
        evaluatedRuleCount = enabledRules.size,
      )
    }
    return WebsiteRuleEvaluation(
      rawQuery = rawQuery,
      normalizedDomain = normalized,
      outcome = WebsiteRuleOutcome.ALLOWED_NO_MATCH,
      evaluatedRuleCount = enabledRules.size,
    )
  }
}

/**
 * Evaluates weekly and overnight blocking schedule windows cleanly.
 */
object BlockingScheduler {
  fun isMinuteInWindow(currentMinuteOfDay: Int, startMinuteOfDay: Int, endMinuteOfDay: Int): Boolean {
    val cur = currentMinuteOfDay.coerceIn(0, 1439)
    val start = startMinuteOfDay.coerceIn(0, 1439)
    val end = endMinuteOfDay.coerceIn(0, 1439)
    return if (start <= end) {
      cur in start..end
    } else {
      cur >= start || cur <= end
    }
  }

  fun isScheduleActiveAt(schedule: BlockingScheduleEntity, nowEpochMs: Long): Boolean {
    if (!schedule.isEnabled) return false
    val zoned = Instant.ofEpochMilli(nowEpochMs).atZone(ZoneId.systemDefault())
    val dayOfWeek = zoned.dayOfWeek.value // 1=Mon..7=Sun
    val minuteOfDay = zoned.hour * 60 + zoned.minute
    return dayOfWeek in schedule.daysOfWeek &&
      isMinuteInWindow(minuteOfDay, schedule.startMinuteOfDay, schedule.endMinuteOfDay)
  }

  fun activeProfilesAt(
    profiles: List<BlockProfileEntity>,
    schedules: List<BlockingScheduleEntity>,
    packageName: String,
    nowEpochMs: Long,
  ): List<BlockProfileEntity> {
    return profiles.filter { profile ->
      val scheduledNow = schedules.any { sched -> sched.profileId == profile.id && isScheduleActiveAt(sched, nowEpochMs) }
      (profile.isActive || scheduledNow) &&
        packageName in profile.blockedPackages &&
        packageName !in profile.allowedPackages
    }
  }
}

/**
 * Deduplicates rapid Accessibility + UsageEvents fallback triggers so the overlay does not flicker or loop.
 */
object BlockingStateManager {
  private val lastTriggerEpochByPkg = ConcurrentHashMap<String, Long>()

  fun shouldDispatchOverlay(
    packageName: String,
    nowEpochMs: Long,
    cooldownMs: Long = 2500L,
  ): Boolean {
    val last = lastTriggerEpochByPkg[packageName] ?: 0L
    if (nowEpochMs - last < cooldownMs) return false
    lastTriggerEpochByPkg[packageName] = nowEpochMs
    return true
  }

  fun resetForTesting() {
    lastTriggerEpochByPkg.clear()
  }
}

/**
 * Handles owner emergency recovery passcode checks and invincible cooldown timing.
 */
object EmergencyAccessManager {
  fun verifyPasscode(enteredPasscode: String, storedHash: String): Boolean {
    if (storedHash.isBlank()) return true
    return KeystoreAndBackupCrypto.sha256Hex(enteredPasscode.trim()) == storedHash
  }

  fun isCooldownReady(pendingUnlockAtEpochMs: Long, nowEpochMs: Long = System.currentTimeMillis()): Boolean {
    return pendingUnlockAtEpochMs in 1L..nowEpochMs
  }
}
