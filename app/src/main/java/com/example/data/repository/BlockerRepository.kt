package com.example.data.repository

import com.example.core.common.KeystoreAndBackupCrypto
import com.example.core.database.AppGroupDao
import com.example.core.database.AppGroupEntity
import com.example.core.database.AppLimitDao
import com.example.core.database.AppLimitEntity
import com.example.core.database.AppUsageDao
import com.example.core.database.BlockProfileDao
import com.example.core.database.BlockProfileEntity
import com.example.core.database.BlockingScheduleDao
import com.example.core.database.BlockingScheduleEntity
import com.example.core.database.CategoryLimitDao
import com.example.core.database.CategoryLimitEntity
import com.example.core.database.DailyAnalyticsDao
import com.example.core.database.DailyAnalyticsEntity
import com.example.core.database.InstalledAppDao
import com.example.core.database.NfcTagEntity
import com.example.core.database.QrTokenEntity
import com.example.core.database.ShortFormRuleDao
import com.example.core.database.ShortFormRuleEntity
import com.example.core.database.TokenVaultDao
import com.example.core.database.UnlockDao
import com.example.core.database.UnlockHistoryEntity
import com.example.core.database.UnlockRuleEntity
import com.example.core.database.WebsiteRuleDao
import com.example.core.database.WebsiteRuleEntity
import com.example.core.datastore.FocusNestPreferences
import com.example.domain.model.ActiveBlockDecision
import com.example.domain.model.FocusTimeUtils
import com.example.domain.model.LimitThresholdLevel
import com.example.domain.model.ReelsTargetApp
import com.example.domain.model.RulePriorityResolver
import com.example.domain.model.UnlockChallengeEngine
import com.example.domain.model.UnlockChallengeType
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first

class BlockerRepository(
  private val appLimitDao: AppLimitDao,
  private val appGroupDao: AppGroupDao,
  private val blockProfileDao: BlockProfileDao,
  private val blockingScheduleDao: BlockingScheduleDao,
  private val unlockDao: UnlockDao,
  private val websiteRuleDao: WebsiteRuleDao,
  private val tokenVaultDao: TokenVaultDao,
  private val shortFormRuleDao: ShortFormRuleDao,
  private val appUsageDao: AppUsageDao,
  private val dailyAnalyticsDao: DailyAnalyticsDao,
  private val installedAppDao: InstalledAppDao,
  private val preferences: FocusNestPreferences,
  private val categoryLimitDao: CategoryLimitDao? = null,
) {
  val appLimits: Flow<List<AppLimitEntity>> = appLimitDao.observeAll()
  val appGroups: Flow<List<AppGroupEntity>> = appGroupDao.observeAll()
  val categoryLimits: Flow<List<CategoryLimitEntity>> = categoryLimitDao?.observeAll() ?: emptyFlow()
  val profiles: Flow<List<BlockProfileEntity>> = blockProfileDao.observeAll()
  val schedules: Flow<List<BlockingScheduleEntity>> = blockingScheduleDao.observeAll()
  val unlockRules: Flow<List<UnlockRuleEntity>> = unlockDao.observeRules()
  val unlockHistory: Flow<List<UnlockHistoryEntity>> = unlockDao.observeHistory()
  val websiteRules: Flow<List<WebsiteRuleEntity>> = websiteRuleDao.observeAll()
  val nfcTags: Flow<List<NfcTagEntity>> = tokenVaultDao.observeNfcTags()
  val qrTokens: Flow<List<QrTokenEntity>> = tokenVaultDao.observeQrTokens()
  val shortFormRules: Flow<List<ShortFormRuleEntity>> = shortFormRuleDao.observeAll()

  suspend fun ensureStarterConfigurations() {
    if (blockProfileDao.getAll().isEmpty()) {
      val starterProfiles =
        listOf(
          BlockProfileEntity(
            name = "Study Sanctuary",
            presetType = "Study",
            focusDurationMinutes = 45,
            unlockPolicy = UnlockChallengeType.ADAPTIVE_MATH.name,
            priority = 8,
          ),
          BlockProfileEntity(
            name = "Deep Work",
            presetType = "Work",
            focusDurationMinutes = 50,
            unlockPolicy = UnlockChallengeType.INTENTION_ENTRY.name,
            priority = 9,
          ),
          BlockProfileEntity(
            name = "College Lecture",
            presetType = "College",
            focusDurationMinutes = 60,
            unlockPolicy = UnlockChallengeType.SENTENCE_TYPING.name,
            priority = 7,
          ),
          BlockProfileEntity(
            name = "Sleep Wind-Down",
            presetType = "Sleep",
            focusDurationMinutes = 0,
            unlockPolicy = UnlockChallengeType.STRICT_MODE.name,
            strictMode = true,
            priority = 10,
          ),
          BlockProfileEntity(
            name = "Quiet Reading",
            presetType = "Reading",
            focusDurationMinutes = 30,
            unlockPolicy = UnlockChallengeType.REFLECTION_REMINDER.name,
            priority = 6,
          ),
          BlockProfileEntity(
            name = "Weekend Reset",
            presetType = "Weekend",
            focusDurationMinutes = 25,
            unlockPolicy = UnlockChallengeType.TEMP_UNLOCK_DURATION.name,
            priority = 5,
          ),
        )
      starterProfiles.forEach { blockProfileDao.upsert(it) }
    }

    if (shortFormRuleDao.getAll().isEmpty()) {
      val today = FocusTimeUtils.todayKey()
      shortFormRuleDao.upsertAll(
        ReelsTargetApp.entries.map { target ->
          ShortFormRuleEntity(
            packageName = target.packageName,
            appName = target.displayName,
            featureLabel = target.featureName,
            isEnabled = false,
            dailyInteractionLimit = 8,
            interactionsToday = 0,
            lastResetDateKey = today,
            pauseSeconds = 8,
            requireReflection = true,
          )
        }
      )
    }

    if (unlockDao.getRules().isEmpty()) {
      unlockDao.upsertRule(
        UnlockRuleEntity(
          name = "Default Mindful Pause",
          challengeType = UnlockChallengeType.REFLECTION_REMINDER.name,
          cooldownSeconds = 20,
          tempUnlockMinutes = 5,
          dailyUnlockCap = 5,
          isEnabled = true,
        )
      )
    }
  }

  suspend fun saveAppLimit(limit: AppLimitEntity) = appLimitDao.upsert(limit)

  suspend fun deleteAppLimit(packageName: String) = appLimitDao.deleteByPackage(packageName)

  suspend fun saveCategoryLimit(limit: CategoryLimitEntity) {
    categoryLimitDao?.upsert(limit)
  }

  suspend fun deleteCategoryLimit(categoryName: String) {
    categoryLimitDao?.deleteByCategory(categoryName)
  }

  suspend fun saveAppGroup(group: AppGroupEntity): Long = appGroupDao.upsert(group)

  suspend fun deleteAppGroup(group: AppGroupEntity) = appGroupDao.delete(group)

  suspend fun saveProfile(profile: BlockProfileEntity): Long = blockProfileDao.upsert(profile)

  suspend fun setProfileActive(profileId: Long, active: Boolean) =
    blockProfileDao.setActive(profileId, active)

  suspend fun deleteProfile(profile: BlockProfileEntity) = blockProfileDao.delete(profile)

  suspend fun saveSchedule(schedule: BlockingScheduleEntity): Long =
    blockingScheduleDao.upsert(schedule)

  suspend fun deleteSchedule(schedule: BlockingScheduleEntity) =
    blockingScheduleDao.delete(schedule)

  suspend fun saveShortFormRule(rule: ShortFormRuleEntity) = shortFormRuleDao.upsert(rule)

  suspend fun recordShortFormInteraction(packageName: String): ShortFormRuleEntity? {
    val rules = shortFormRuleDao.getAll()
    val current = rules.firstOrNull { it.packageName == packageName } ?: return null
    val today = FocusTimeUtils.todayKey()
    val updated =
      if (current.lastResetDateKey != today) {
        current.copy(interactionsToday = 1, lastResetDateKey = today)
      } else {
        current.copy(interactionsToday = current.interactionsToday + 1)
      }
    shortFormRuleDao.upsert(updated)
    val analytics = dailyAnalyticsDao.getByDate(today) ?: DailyAnalyticsEntity(today)
    dailyAnalyticsDao.upsert(
      analytics.copy(
        shortFormInterventionsCount = analytics.shortFormInterventionsCount + 1,
        updatedAtEpochMs = System.currentTimeMillis(),
      )
    )
    return updated
  }

  suspend fun saveWebsiteRule(rule: WebsiteRuleEntity): Long = websiteRuleDao.upsert(rule)

  suspend fun deleteWebsiteRule(rule: WebsiteRuleEntity) = websiteRuleDao.delete(rule)

  suspend fun installOfflineWebsitePack(packName: String) {
    val domains =
      when (packName) {
        "Short-Form & Social" ->
          listOf("tiktok.com", "instagram.com", "facebook.com", "x.com", "reddit.com", "threads.net")
        "Streaming & Video" ->
          listOf("youtube.com", "twitch.tv", "netflix.com", "dailymotion.com")
        "Gaming & Streams" ->
          listOf("steampowered.com", "roblox.com", "epicgames.com", "itch.io")
        else -> listOf("news.ycombinator.com", "buzzfeed.com", "9gag.com")
      }
    val existing = websiteRuleDao.getAll().map { it.domain.lowercase() }.toSet()
    val toInsert =
      domains
        .filterNot { it.lowercase() in existing }
        .map { WebsiteRuleEntity(domain = it, category = packName, isAllowlist = false, isEnabled = true) }
    if (toInsert.isNotEmpty()) {
      websiteRuleDao.upsertAll(toInsert)
    }
  }

  suspend fun registerNfcTag(tag: NfcTagEntity) = tokenVaultDao.upsertNfcTag(tag)

  suspend fun deleteNfcTag(tag: NfcTagEntity) = tokenVaultDao.deleteNfcTag(tag)

  suspend fun findNfcTag(uidHex: String): NfcTagEntity? = tokenVaultDao.getNfcTag(uidHex)

  suspend fun registerQrOrBarcodeToken(token: QrTokenEntity): Long {
    val sig =
      if (token.signatureHex.isBlank()) {
        KeystoreAndBackupCrypto.sha256Hex("focusnest-qr|${token.tokenValue}|${token.createdAtEpochMs}")
      } else {
        token.signatureHex
      }
    return tokenVaultDao.upsertQrToken(token.copy(signatureHex = sig))
  }

  suspend fun deleteQrOrBarcodeToken(token: QrTokenEntity) = tokenVaultDao.deleteQrToken(token)

  suspend fun verifyQrOrBarcodeToken(scannedValue: String): QrTokenEntity? =
    verifyAndConsumeQrOrBarcodeToken(scannedValue).first

  suspend fun verifyAndConsumeQrOrBarcodeToken(
    scannedValue: String,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): Pair<QrTokenEntity?, String?> {
    val token = tokenVaultDao.findQrToken(scannedValue.trim())
      ?: return null to "Code not registered in FocusNest NFC & QR Vault."

    if (token.signatureHex.isNotBlank()) {
      val expectedSig =
        KeystoreAndBackupCrypto.sha256Hex("focusnest-qr|${token.tokenValue}|${token.createdAtEpochMs}")
      if (token.signatureHex != expectedSig) {
        return null to "Token signature verification failed."
      }
    }
    if (token.expiresAtEpochMs > 0L && nowEpochMs > token.expiresAtEpochMs) {
      return null to "This unlock token has expired."
    }
    if (token.isOneTime && token.isConsumed) {
      return null to "One-time token has already been used."
    }
    if (token.isOneTime) {
      tokenVaultDao.markQrTokenConsumed(token.id)
    }
    return token to null
  }

  suspend fun evaluateAndMarkAppLimitWarnings(
    nowEpochMs: Long = System.currentTimeMillis(),
  ): List<Pair<AppLimitEntity, LimitThresholdLevel>> {
    val today = FocusTimeUtils.todayKey(nowEpochMs)
    val usageMap = appUsageDao.getUsageForDate(today).associateBy { it.packageName }
    val limits = appLimitDao.getAll().filter { it.dailyLimitMinutes > 0 }
    val triggered = mutableListOf<Pair<AppLimitEntity, LimitThresholdLevel>>()

    for (limit in limits) {
      val usedMinutes = ((usageMap[limit.packageName]?.foregroundTimeMs ?: 0L) / 60_000L).toInt()
      val threshold = UnlockChallengeEngine.evaluateUsageThreshold(usedMinutes, limit.dailyLimitMinutes)
      when (threshold) {
        LimitThresholdLevel.WARN_100 ->
          if (limit.warned100DateKey != today) {
            val updated = limit.copy(warned100DateKey = today, warned90DateKey = today, warned80DateKey = today)
            appLimitDao.upsert(updated)
            triggered.add(updated to LimitThresholdLevel.WARN_100)
          }
        LimitThresholdLevel.WARN_90 ->
          if (limit.warned90DateKey != today) {
            val updated = limit.copy(warned90DateKey = today, warned80DateKey = today)
            appLimitDao.upsert(updated)
            triggered.add(updated to LimitThresholdLevel.WARN_90)
          }
        LimitThresholdLevel.WARN_80 ->
          if (limit.warned80DateKey != today) {
            val updated = limit.copy(warned80DateKey = today)
            appLimitDao.upsert(updated)
            triggered.add(updated to LimitThresholdLevel.WARN_80)
          }
        LimitThresholdLevel.NONE -> Unit
      }
    }
    return triggered
  }

  suspend fun recordBlockedAttempt(packageName: String) {
    val today = FocusTimeUtils.todayKey()
    val current = dailyAnalyticsDao.getByDate(today) ?: DailyAnalyticsEntity(today)
    dailyAnalyticsDao.upsert(
      current.copy(
        blockedAttemptsCount = current.blockedAttemptsCount + 1,
        updatedAtEpochMs = System.currentTimeMillis(),
      )
    )
  }

  suspend fun grantTemporaryUnlock(
    packageName: String,
    appName: String,
    challengeType: UnlockChallengeType,
    durationMinutes: Int,
    intentionText: String = "",
    dailyUnlockCap: Int = 5,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): Pair<Boolean, String> {
    val today = FocusTimeUtils.todayKey(nowEpochMs)
    val todayHistory = unlockDao.getHistoryForAppToday(today, packageName).filter { it.granted }
    val lastGrant = todayHistory.maxOfOrNull { it.timestampEpochMs }
    val (allowed, reason) =
      UnlockChallengeEngine.canGrantUnlock(
        unlocksUsedToday = todayHistory.size,
        dailyUnlockLimit = dailyUnlockCap,
        lastUnlockEpochMs = lastGrant,
        nowEpochMs = nowEpochMs,
      )
    if (!allowed) {
      unlockDao.insertHistory(
        UnlockHistoryEntity(
          dateKey = today,
          packageName = packageName,
          appName = appName,
          challengeType = challengeType.name,
          granted = false,
          grantedMinutes = 0,
          intentionText = intentionText,
          timestampEpochMs = nowEpochMs,
        )
      )
      return false to (reason ?: "Unlock denied")
    }

    val expiresAt = nowEpochMs + durationMinutes.coerceIn(1, 60) * 60_000L
    val existingLimit = appLimitDao.getByPackage(packageName)
    if (existingLimit != null) {
      appLimitDao.upsert(existingLimit.copy(tempUnlockUntilEpochMs = expiresAt))
    } else {
      appLimitDao.upsert(
        AppLimitEntity(
          packageName = packageName,
          appName = appName,
          dailyLimitMinutes = 0,
          tempUnlockUntilEpochMs = expiresAt,
        )
      )
    }

    unlockDao.insertHistory(
      UnlockHistoryEntity(
        dateKey = today,
        packageName = packageName,
        appName = appName,
        challengeType = challengeType.name,
        granted = true,
        grantedMinutes = durationMinutes,
        intentionText = intentionText,
        timestampEpochMs = nowEpochMs,
      )
    )

    val analytics = dailyAnalyticsDao.getByDate(today) ?: DailyAnalyticsEntity(today)
    dailyAnalyticsDao.upsert(
      analytics.copy(
        unlockCount = analytics.unlockCount + 1,
        updatedAtEpochMs = nowEpochMs,
      )
    )
    return true to "Unlocked for $durationMinutes min"
  }

  /**
   * Evaluates whether [packageName] should currently be blocked on this device.
   */
  suspend fun evaluatePackageDecision(
    packageName: String,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): ActiveBlockDecision {
    val installed = installedAppDao.getAll().firstOrNull { it.packageName == packageName }
    if (installed?.isEmergencyWhitelisted == true) {
      return ActiveBlockDecision(isBlocked = false, reason = "Emergency whitelisted")
    }

    val settings = preferences.settingsFlow.first()
    val timer = preferences.timerStateFlow.first()
    val limit = appLimitDao.getByPackage(packageName)
    val profiles = blockProfileDao.getAll()
    val schedules = blockingScheduleDao.getAll()
    val todayKey = FocusTimeUtils.todayKey(nowEpochMs)
    val usageToday = appUsageDao.getUsageForDate(todayKey)
    val usedMinutesToday =
      ((usageToday.firstOrNull { it.packageName == packageName }?.foregroundTimeMs ?: 0L) / 60_000L)
        .toInt()

    // Check shared group limits
    val groups = appGroupDao.getAll().filter { it.isEnabled && packageName in it.packageNames }
    val groupUsageMinutes =
      groups.maxOfOrNull { group ->
        usageToday.filter { it.packageName in group.packageNames }.sumOf { it.foregroundTimeMs } /
          60_000L
      }?.toInt() ?: 0
    val tightestGroupLimit =
      groups.filter { it.sharedDailyLimitMinutes > 0 }.minOfOrNull { it.sharedDailyLimitMinutes }

    // Check weekly usage if weekly limit exists
    val weekStartKey = FocusTimeUtils.dateKeyDaysAgo(6)
    val usedMinutesWeek =
      if ((limit?.weeklyLimitMinutes ?: 0) > 0) {
        (appUsageDao.getUsageInRange(weekStartKey, todayKey)
          .filter { it.packageName == packageName }
          .sumOf { it.foregroundTimeMs } / 60_000L)
          .toInt()
      } else {
        0
      }

    // Active focus session linked profile OR quick blocker distracting apps
    val focusLinkedProfile =
      if (timer.isRunning && timer.phase.name == "FOCUS") {
        profiles.firstOrNull { it.id == timer.linkedProfileId }
          ?: profiles.firstOrNull { it.isActive }
      } else {
        null
      }
    val activeFocusBlocks =
      timer.isRunning &&
        timer.phase.name == "FOCUS" &&
        ((focusLinkedProfile != null && packageName in focusLinkedProfile.blockedPackages) ||
          (focusLinkedProfile == null && installed?.isSelectedDistracting == true))

    // Active manual or scheduled profiles
    val zoned = Instant.ofEpochMilli(nowEpochMs).atZone(ZoneId.systemDefault())
    val dayOfWeek = zoned.dayOfWeek.value // 1=Mon..7=Sun
    val minuteOfDay = zoned.hour * 60 + zoned.minute

    val activeProfiles =
      profiles.filter { profile ->
        val scheduledNow =
          schedules.any { sched ->
            sched.profileId == profile.id &&
              sched.isEnabled &&
              dayOfWeek in sched.daysOfWeek &&
              isMinuteInWindow(minuteOfDay, sched.startMinuteOfDay, sched.endMinuteOfDay)
          }
        (profile.isActive || scheduledNow) &&
          packageName in profile.blockedPackages &&
          packageName !in profile.allowedPackages
      }
    val topProfile = activeProfiles.maxByOrNull { it.priority }

    val quickBlockerMatches =
      settings.quickBlockerEnabled &&
        (installed?.isSelectedDistracting == true || limit?.isBlockedManually == true)

    val matchedProfileName =
      topProfile?.name ?: if (quickBlockerMatches) "Quick Shield" else null
    val matchedChallenge =
      runCatching {
          UnlockChallengeType.valueOf(
            topProfile?.unlockPolicy
              ?: limit?.challengeType
              ?: UnlockChallengeType.REFLECTION_REMINDER.name
          )
        }
        .getOrDefault(UnlockChallengeType.REFLECTION_REMINDER)

    val effectiveDailyLimit =
      listOfNotNull(
          limit?.dailyLimitMinutes?.takeIf { it > 0 },
          tightestGroupLimit,
        )
        .minOrNull()
    val effectiveUsedToday = maxOf(usedMinutesToday, groupUsageMinutes)

    val appCategory =
      installed?.category?.takeIf { it.isNotBlank() } ?: limit?.categoryName?.takeIf { it.isNotBlank() }
    val catLimitEntity =
      if (appCategory != null) {
        categoryLimitDao?.getByCategory(appCategory)?.takeIf { it.isEnabled && it.dailyLimitMinutes > 0 }
      } else null
    val catUsedMinutes =
      if (catLimitEntity != null) {
        (usageToday.filter { it.category == catLimitEntity.categoryName }.sumOf { it.foregroundTimeMs } / 60_000L)
          .toInt()
      } else 0

    return RulePriorityResolver.resolve(
      packageName = packageName,
      tempUnlockExpiresAtEpochMs = limit?.tempUnlockUntilEpochMs,
      nowEpochMs = nowEpochMs,
      activeFocusBlocksPackage = activeFocusBlocks,
      activeFocusProfileName = focusLinkedProfile?.name ?: "Focus Session",
      matchingScheduledProfileName = matchedProfileName,
      matchingScheduledProfileId = topProfile?.id,
      matchingScheduledPriority = topProfile?.priority ?: 1,
      matchingChallenge = matchedChallenge,
      isStrictMode = settings.invincibleModeEnabled || (topProfile?.strictMode == true),
      usedMinutesToday = effectiveUsedToday,
      dailyLimitMinutes = effectiveDailyLimit,
      usedMinutesWeek = usedMinutesWeek,
      weeklyLimitMinutes = limit?.weeklyLimitMinutes?.takeIf { it > 0 },
      gracePeriodMinutes = limit?.gracePeriodMinutes ?: 0,
      categoryUsedMinutesToday = catUsedMinutes,
      categoryDailyLimitMinutes = catLimitEntity?.dailyLimitMinutes,
      categoryName = catLimitEntity?.categoryName,
    )
  }

  suspend fun recordMindfulLaunchProceed(
    packageName: String,
    appName: String,
    pauseSeconds: Int,
    nowEpochMs: Long = System.currentTimeMillis(),
  ) {
    val today = FocusTimeUtils.todayKey(nowEpochMs)
    unlockDao.insertHistory(
      UnlockHistoryEntity(
        dateKey = today,
        packageName = packageName,
        appName = appName,
        challengeType = UnlockChallengeType.BREATHING.name,
        granted = true,
        grantedMinutes = 5,
        intentionText = "Mindful ${pauseSeconds}s launch pause",
        timestampEpochMs = nowEpochMs,
      )
    )
    val analytics = dailyAnalyticsDao.getByDate(today) ?: DailyAnalyticsEntity(today)
    dailyAnalyticsDao.upsert(
      analytics.copy(
        unlockCount = analytics.unlockCount + 1,
        updatedAtEpochMs = nowEpochMs,
      )
    )
  }

  private fun isMinuteInWindow(current: Int, start: Int, end: Int): Boolean {
    return if (start <= end) {
      current in start..end
    } else {
      // Overnight schedule (e.g. 22:30 -> 07:00)
      current >= start || current <= end
    }
  }
}
