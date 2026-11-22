package com.example.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.random.Random

enum class SessionType(val label: String, val defaultMinutes: Int) {
  STUDY("Study", 45),
  WORK("Work", 50),
  READING("Reading", 30),
  CODING("Coding", 60),
  CREATIVE("Creative", 40),
  CUSTOM("Custom", 25),
}

enum class TimerMode(val label: String) {
  POMODORO("Pomodoro"),
  COUNTDOWN("Countdown"),
  STOPWATCH("Stopwatch"),
}

enum class TimerPhase(val label: String) {
  IDLE("Ready"),
  FOCUS("Focus"),
  SHORT_BREAK("Short Break"),
  LONG_BREAK("Long Break"),
}

enum class UnlockChallengeType(val label: String, val summary: String) {
  STRICT_MODE("Strict Mode", "No bypass until schedule or session ends (owner emergency only)"),
  FIXED_COOLDOWN("Fixed Cooldown", "Wait a mandatory cooldown countdown before opening"),
  TEMP_UNLOCK_DURATION("Temporary Unlock", "Choose a short 1–15 min mindful unlock window"),
  QR_SCAN("QR Code Scan", "Scan a registered FocusNest QR code offline"),
  BARCODE_SCAN("Barcode Scan", "Scan a registered product/book barcode token"),
  NFC_SCAN("NFC Tag Tap", "Tap a physical NFC tag paired to this profile"),
  ADAPTIVE_MATH("Adaptive Math", "Solve multi-step arithmetic calibrated to attempt count"),
  SENTENCE_TYPING("Sentence Typing", "Type an intentional commitment sentence verbatim"),
  INTENTION_ENTRY("Personal Intention", "Write why you need this app right now"),
  DAILY_UNLOCK_LIMIT("Daily Unlock Limit", "Caps maximum unlocks per app per day"),
  REFLECTION_REMINDER("Reflection Pause", "Guided breathing & reflection prompt before entry"),
  BREATHING("Breathing Challenge", "Complete a guided inhale-hold-exhale cycle before entry"),
}

enum class AppCategory(val label: String) {
  SOCIAL("Social"),
  VIDEO("Video & Shorts"),
  GAMING("Gaming"),
  PRODUCTIVITY("Productivity"),
  EDUCATION("Education"),
  COMMUNICATION("Communication"),
  BROWSER("Browsers"),
  OTHER("Other"),
}

enum class AccentPalette(val label: String) {
  EMERALD_SAGE("Sage Sanctuary"),
  INDIGO_FOCUS("Indigo Focus"),
  WARM_AMBER("Warm Amber"),
  OCEAN_SLATE("Ocean Slate"),
  LAVENDER_DUSK("Calm Dusk"),
  TERRACOTTA_CALM("Earthy Clay"),
}

enum class ThemeMode(val label: String) {
  SYSTEM("System"),
  LIGHT("Light"),
  DARK("Dark"),
}

enum class ReelsTargetApp(
  val packageName: String,
  val displayName: String,
  val featureName: String,
  val viewIdHints: List<String>,
) {
  INSTAGRAM_REELS(
    packageName = "com.instagram.android",
    displayName = "Instagram",
    featureName = "Reels",
    viewIdHints = listOf("clips_viewer", "reel_viewer", "clips_video_container"),
  ),
  YOUTUBE_SHORTS(
    packageName = "com.google.android.youtube",
    displayName = "YouTube",
    featureName = "Shorts",
    viewIdHints = listOf("reel_watch_player", "shorts_player", "reel_recycler"),
  ),
  FACEBOOK_REELS(
    packageName = "com.facebook.katana",
    displayName = "Facebook",
    featureName = "Reels",
    viewIdHints = listOf("reels_viewer", "fb_shorts"),
  ),
  TIKTOK(
    packageName = "com.zhiliaoapp.musically",
    displayName = "TikTok",
    featureName = "For You Feed",
    viewIdHints = listOf("viewpager", "feed_item"),
  ),
}

data class PermissionSnapshot(
  val usageStatsGranted: Boolean = false,
  val accessibilityEnabled: Boolean = false,
  val notificationListenerEnabled: Boolean = false,
  val vpnPrepared: Boolean = false,
  val vpnActive: Boolean = false,
  val postNotificationsGranted: Boolean = true,
  val cameraGranted: Boolean = false,
  val dndPolicyAccessGranted: Boolean = false,
  val ignoringBatteryOptimizations: Boolean = false,
  val nfcSupported: Boolean = false,
  val nfcEnabled: Boolean = false,
)

data class ActiveBlockDecision(
  val isBlocked: Boolean,
  val reason: String,
  val profileId: Long? = null,
  val profileName: String? = null,
  val priorityRank: Int = 0,
  val challengeType: UnlockChallengeType = UnlockChallengeType.REFLECTION_REMINDER,
  val remainingAllowanceMinutes: Int? = null,
)

data class MathChallenge(
  val prompt: String,
  val expectedAnswer: Int,
  val difficultyLevel: Int,
)

enum class LimitThresholdLevel {
  NONE,
  WARN_80,
  WARN_90,
  WARN_100,
}

enum class AmbientSoundType(val label: String, val description: String) {
  OFF("Off", "Silent focus session"),
  BROWN_NOISE("Brown Noise", "Warm low-frequency blanket"),
  SOFT_RAIN("Soft Rain", "Gentle offline rainfall"),
  WARM_DRONE("Warm Drone", "Calming ambient fifths"),
  STUDY_MURMUR("Study Wash", "Soft filtered room hush"),
}

data class WindDownStep(
  val id: String,
  val title: String,
  val subtitle: String,
)

object BedtimeChecklistDefaults {
  val steps =
    listOf(
      WindDownStep(
        id = "charger_away",
        title = "Dock phone charger away from bed",
        subtitle = "Keep night scrolling out of arm's reach",
      ),
      WindDownStep(
        id = "grayscale_night",
        title = "Enable Bedtime Grayscale / Night Light",
        subtitle = "Reduce dopamine stimulation and blue glare",
      ),
      WindDownStep(
        id = "dim_alerts",
        title = "Dim brightness & silence non-urgent alerts",
        subtitle = "Protect uninterrupted restorative sleep",
      ),
      WindDownStep(
        id = "tomorrow_intention",
        title = "Set tomorrow's first mindful morning focus",
        subtitle = "Decide what matters before waking up",
      ),
    )
}

object LaunchPauseCoordinator {
  const val DEFAULT_COOLDOWN_MS: Long = 5 * 60_000L

  fun shouldTriggerPause(
    packageName: String,
    launchPauseEnabled: Boolean,
    pausedPackages: Set<String>,
    lastProceedEpochMs: Long?,
    nowEpochMs: Long,
    cooldownMs: Long = DEFAULT_COOLDOWN_MS,
  ): Boolean {
    if (!launchPauseEnabled) return false
    if (packageName !in pausedPackages) return false
    if (lastProceedEpochMs != null && (nowEpochMs - lastProceedEpochMs) < cooldownMs) {
      return false
    }
    return true
  }
}

object UpdateReleaseConfig {
  const val GITHUB_OWNER_REPO = "OWNER/FocusNest"
  const val REPO_URL = "https://github.com/$GITHUB_OWNER_REPO"
  const val RELEASES_URL = "https://github.com/$GITHUB_OWNER_REPO/releases/latest"
  const val PAGES_URL = "https://OWNER.github.io/FocusNest/"

  val latestHighlights =
    listOf(
      "Adaptive foldable & tablet two-pane Sanctuary layouts",
      "Offline synthesized ambient focus soundscapes",
      "Mindful app-launch pause curtain with breathing delay",
      "Weekly printable/PDF wellbeing report card & bedtime checklist",
    )
}

object UnlockChallengeEngine {
  val predefinedIntentionReasons =
    listOf(
      "Reply to an urgent message",
      "Check a study/work resource",
      "Two-factor verification or utility",
      "Planned break check-in (5 min max)",
    )

  private val commitmentSentences =
    listOf(
      "I choose intentional presence over mindless scrolling right now.",
      "My attention is valuable and I protect my deep work time.",
      "One conscious pause keeps me aligned with my goals today.",
      "I am opening this app deliberately and will close it on time.",
    )

  fun generateMathChallenge(attemptCountToday: Int, random: Random = Random.Default): MathChallenge {
    val level = (attemptCountToday / 2).coerceIn(1, 5)
    return when (level) {
      1 -> {
        val a = random.nextInt(12, 35)
        val b = random.nextInt(11, 29)
        MathChallenge("$a + $b = ?", a + b, level)
      }
      2 -> {
        val a = random.nextInt(40, 95)
        val b = random.nextInt(12, 38)
        val c = random.nextInt(3, 9)
        MathChallenge("($a - $b) × $c = ?", (a - b) * c, level)
      }
      3 -> {
        val divisor = random.nextInt(3, 12)
        val quotient = random.nextInt(4, 16)
        val dividend = divisor * quotient
        val bonus = random.nextInt(5, 25)
        MathChallenge("($dividend ÷ $divisor) + $bonus = ?", quotient + bonus, level)
      }
      4 -> {
        val a = random.nextInt(11, 24)
        val b = random.nextInt(6, 15)
        val c = random.nextInt(15, 45)
        MathChallenge("$a × $b + $c = ?", a * b + c, level)
      }
      else -> {
        val a = random.nextInt(14, 29)
        val b = random.nextInt(12, 22)
        val c = random.nextInt(25, 65)
        MathChallenge("$a × $b - $c = ?", a * b - c, level)
      }
    }
  }

  fun evaluateUsageThreshold(usedMinutes: Int, limitMinutes: Int): LimitThresholdLevel {
    if (limitMinutes <= 0) return LimitThresholdLevel.NONE
    val pct = (usedMinutes * 100) / limitMinutes
    return when {
      pct >= 100 -> LimitThresholdLevel.WARN_100
      pct >= 90 -> LimitThresholdLevel.WARN_90
      pct >= 80 -> LimitThresholdLevel.WARN_80
      else -> LimitThresholdLevel.NONE
    }
  }

  fun pickSentence(seedIndex: Int): String =
    commitmentSentences[Math.abs(seedIndex) % commitmentSentences.size]

  fun validateSentence(expected: String, typed: String): Boolean =
    expected.trim().equals(typed.trim(), ignoreCase = false)

  fun validateIntention(intention: String, minWords: Int = 3): Boolean {
    val words = intention.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return words.size >= minWords && intention.trim().length >= 12
  }

  fun canGrantUnlock(
    unlocksUsedToday: Int,
    dailyUnlockLimit: Int,
    lastUnlockEpochMs: Long?,
    nowEpochMs: Long,
    minDuplicateGuardMs: Long = 10_000L,
  ): Pair<Boolean, String?> {
    if (lastUnlockEpochMs != null && (nowEpochMs - lastUnlockEpochMs) < minDuplicateGuardMs) {
      return false to "Duplicate unlock prevented. Wait a moment before retrying."
    }
    if (dailyUnlockLimit > 0 && unlocksUsedToday >= dailyUnlockLimit) {
      return false to "Daily unlock limit ($dailyUnlockLimit) reached for this rule."
    }
    return true to null
  }
}

object RulePriorityResolver {
  /**
   * Predictable deterministic rule priority:
   * 100 - Active temporary unlock window (explicitly unlocked) -> ALLOW
   * 90  - Active Focus Session blocking
   * 80  - Invincible / Strict manual block
   * 70  - Active Scheduled Profile (sorted by profile priority)
   * 60  - App, Group, or Category Daily/Weekly Limit exceeded (after optional grace period)
   */
  fun resolve(
    packageName: String,
    tempUnlockExpiresAtEpochMs: Long?,
    nowEpochMs: Long,
    activeFocusBlocksPackage: Boolean,
    activeFocusProfileName: String?,
    matchingScheduledProfileName: String?,
    matchingScheduledProfileId: Long?,
    matchingScheduledPriority: Int,
    matchingChallenge: UnlockChallengeType,
    isStrictMode: Boolean,
    usedMinutesToday: Int,
    dailyLimitMinutes: Int?,
    usedMinutesWeek: Int = 0,
    weeklyLimitMinutes: Int? = null,
    gracePeriodMinutes: Int = 0,
    categoryUsedMinutesToday: Int = 0,
    categoryDailyLimitMinutes: Int? = null,
    categoryName: String? = null,
  ): ActiveBlockDecision {
    if (tempUnlockExpiresAtEpochMs != null && tempUnlockExpiresAtEpochMs > nowEpochMs) {
      val remMin = max(1, ((tempUnlockExpiresAtEpochMs - nowEpochMs) / 60_000L).toInt())
      return ActiveBlockDecision(
        isBlocked = false,
        reason = "Temporary unlock active ($remMin min left)",
        remainingAllowanceMinutes = remMin,
      )
    }

    if (activeFocusBlocksPackage) {
      return ActiveBlockDecision(
        isBlocked = true,
        reason = "Blocked by active Focus session",
        profileName = activeFocusProfileName ?: "Focus Session",
        priorityRank = 90,
        challengeType = if (isStrictMode) UnlockChallengeType.STRICT_MODE else matchingChallenge,
      )
    }

    if (matchingScheduledProfileName != null) {
      return ActiveBlockDecision(
        isBlocked = true,
        reason = "Restricted by profile: $matchingScheduledProfileName",
        profileId = matchingScheduledProfileId,
        profileName = matchingScheduledProfileName,
        priorityRank = 70 + matchingScheduledPriority.coerceIn(0, 19),
        challengeType = if (isStrictMode) UnlockChallengeType.STRICT_MODE else matchingChallenge,
      )
    }

    val effectiveDailyHardBlock =
      if (dailyLimitMinutes != null && dailyLimitMinutes > 0) {
        dailyLimitMinutes + gracePeriodMinutes.coerceAtLeast(0)
      } else null

    if (effectiveDailyHardBlock != null && usedMinutesToday >= effectiveDailyHardBlock) {
      return ActiveBlockDecision(
        isBlocked = true,
        reason =
          if (gracePeriodMinutes > 0) {
            "Daily limit + ${gracePeriodMinutes}m grace ended (${usedMinutesToday}m / ${dailyLimitMinutes}m)"
          } else {
            "Daily limit reached (${usedMinutesToday}m / ${dailyLimitMinutes}m)"
          },
        priorityRank = 60,
        challengeType = if (isStrictMode) UnlockChallengeType.STRICT_MODE else matchingChallenge,
        remainingAllowanceMinutes = 0,
      )
    }

    if (
      categoryDailyLimitMinutes != null &&
        categoryDailyLimitMinutes > 0 &&
        categoryUsedMinutesToday >= categoryDailyLimitMinutes
    ) {
      return ActiveBlockDecision(
        isBlocked = true,
        reason = "${categoryName ?: "Category"} daily limit reached (${categoryUsedMinutesToday}m / ${categoryDailyLimitMinutes}m)",
        priorityRank = 58,
        challengeType = if (isStrictMode) UnlockChallengeType.STRICT_MODE else matchingChallenge,
        remainingAllowanceMinutes = 0,
      )
    }

    if (weeklyLimitMinutes != null && weeklyLimitMinutes > 0 && usedMinutesWeek >= weeklyLimitMinutes) {
      return ActiveBlockDecision(
        isBlocked = true,
        reason = "Weekly limit reached (${usedMinutesWeek}m / ${weeklyLimitMinutes}m)",
        priorityRank = 55,
        challengeType = if (isStrictMode) UnlockChallengeType.STRICT_MODE else matchingChallenge,
        remainingAllowanceMinutes = 0,
      )
    }

    val remaining =
      if (dailyLimitMinutes != null && dailyLimitMinutes > 0) {
        max(0, dailyLimitMinutes - usedMinutesToday)
      } else {
        null
      }

    val inGraceNote =
      if (dailyLimitMinutes != null && dailyLimitMinutes > 0 && usedMinutesToday >= dailyLimitMinutes && gracePeriodMinutes > 0) {
        "Grace period (${effectiveDailyHardBlock!! - usedMinutesToday}m left)"
      } else {
        "Allowed"
      }

    return ActiveBlockDecision(
      isBlocked = false,
      reason = inGraceNote,
      remainingAllowanceMinutes = remaining,
    )
  }
}

object FocusTimeUtils {
  private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  fun todayKey(nowEpochMs: Long = System.currentTimeMillis()): String =
    dateKeyFromEpoch(nowEpochMs)

  fun dateKeyFromEpoch(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)

  fun dateKeyDaysAgo(daysAgo: Int): String =
    LocalDate.now().minusDays(daysAgo.toLong()).format(dateFormatter)

  fun formatDurationMinutes(totalMinutes: Int): String {
    val safe = max(0, totalMinutes)
    val h = safe / 60
    val m = safe % 60
    return when {
      h > 0 && m > 0 -> "${h}h ${m}m"
      h > 0 -> "${h}h"
      else -> "${m}m"
    }
  }

  fun formatDurationMillis(millis: Long): String {
    val totalMinutes = (max(0L, millis) / 60_000L).toInt()
    return formatDurationMinutes(totalMinutes)
  }

  fun formatTimerSeconds(totalSeconds: Long): String {
    val safe = max(0L, totalSeconds)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return if (hours > 0) {
      String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
      String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
  }

  fun formatMinuteOfDay(minuteOfDay: Int): String {
    val clamped = minuteOfDay.coerceIn(0, 1439)
    val h = clamped / 60
    val m = clamped % 60
    return String.format(Locale.US, "%02d:%02d", h, m)
  }
}
