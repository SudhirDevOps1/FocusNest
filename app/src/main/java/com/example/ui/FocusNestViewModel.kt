package com.example.ui

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FocusNestApplication
import com.example.core.common.KeystoreAndBackupCrypto
import com.example.core.database.AppGroupEntity
import com.example.core.database.AppLimitEntity
import com.example.core.database.AppUsageEntity
import com.example.core.database.BlockProfileEntity
import com.example.core.database.BlockingScheduleEntity
import com.example.core.database.CategoryLimitEntity
import com.example.core.database.DailyAnalyticsEntity
import com.example.core.database.FocusGoalEntity
import com.example.core.database.FocusSessionEntity
import com.example.core.database.HabitCompletionEntity
import com.example.core.database.HabitEntity
import com.example.core.database.InstalledAppEntity
import com.example.core.database.NfcTagEntity
import com.example.core.database.NotificationRecordEntity
import com.example.core.database.QrTokenEntity
import com.example.core.database.ReminderEntity
import com.example.core.database.ShortFormRuleEntity
import com.example.core.database.UnlockHistoryEntity
import com.example.core.database.UnlockRuleEntity
import com.example.core.database.WebsiteRuleEntity
import com.example.core.datastore.FocusNestSettings
import com.example.core.datastore.PersistedTimerState
import com.example.core.permissions.PermissionManager
import com.example.domain.model.AccentPalette
import com.example.domain.model.AmbientSoundType
import com.example.domain.model.FocusTimeUtils
import com.example.domain.model.PermissionSnapshot
import com.example.domain.model.SessionType
import com.example.domain.model.ThemeMode
import com.example.domain.model.TimerMode
import com.example.domain.model.UnlockChallengeType
import com.example.domain.model.UpdateReleaseConfig
import com.example.service.FocusNestVpnService
import com.example.service.FocusTimerService
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FocusNestViewModel(application: Application) : AndroidViewModel(application) {
  private val app = application as FocusNestApplication
  private val container = app.container
  private val prefs = container.preferences
  private val usageRepo = container.usageStatsRepository
  private val blockerRepo = container.blockerRepository
  private val focusRepo = container.focusRepository
  private val habitRepo = container.habitRepository
  private val insightsRepo = container.insightsRepository

  val settings: StateFlow<FocusNestSettings> =
    prefs.settingsFlow.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      FocusNestSettings(),
    )

  val timerState: StateFlow<PersistedTimerState> =
    prefs.timerStateFlow.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      PersistedTimerState(),
    )

  private val _permissions = MutableStateFlow(PermissionManager.getSnapshot(app))
  val permissions: StateFlow<PermissionSnapshot> = _permissions.asStateFlow()

  private val _statusBanner = MutableStateFlow<String?>(null)
  val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

  val installedApps: StateFlow<List<InstalledAppEntity>> =
    usageRepo.installedApps.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val todayUsage: StateFlow<List<AppUsageEntity>> =
    usageRepo
      .observeUsageForDate(FocusTimeUtils.todayKey())
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val yesterdayUsage: StateFlow<List<AppUsageEntity>> =
    usageRepo
      .observeUsageForDate(FocusTimeUtils.dateKeyDaysAgo(1))
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val weeklyUsage: StateFlow<List<AppUsageEntity>> =
    usageRepo
      .observeUsageInRange(FocusTimeUtils.dateKeyDaysAgo(6), FocusTimeUtils.todayKey())
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val monthlyUsage: StateFlow<List<AppUsageEntity>> =
    usageRepo
      .observeUsageInRange(FocusTimeUtils.dateKeyDaysAgo(29), FocusTimeUtils.todayKey())
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val appLimits: StateFlow<List<AppLimitEntity>> =
    blockerRepo.appLimits.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val categoryLimits: StateFlow<List<CategoryLimitEntity>> =
    blockerRepo.categoryLimits.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val appGroups: StateFlow<List<AppGroupEntity>> =
    blockerRepo.appGroups.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val profiles: StateFlow<List<BlockProfileEntity>> =
    blockerRepo.profiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val schedules: StateFlow<List<BlockingScheduleEntity>> =
    blockerRepo.schedules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val focusSessions: StateFlow<List<FocusSessionEntity>> =
    focusRepo.allSessions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val focusGoal: StateFlow<FocusGoalEntity> =
    focusRepo.goal
      .map { it ?: FocusGoalEntity() }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FocusGoalEntity())

  val habits: StateFlow<List<HabitEntity>> =
    habitRepo.activeHabits.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val habitCompletions: StateFlow<List<HabitCompletionEntity>> =
    habitRepo.allCompletions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val unlockRules: StateFlow<List<UnlockRuleEntity>> =
    blockerRepo.unlockRules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val unlockHistory: StateFlow<List<UnlockHistoryEntity>> =
    blockerRepo.unlockHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val websiteRules: StateFlow<List<WebsiteRuleEntity>> =
    blockerRepo.websiteRules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val shortFormRules: StateFlow<List<ShortFormRuleEntity>> =
    blockerRepo.shortFormRules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val nfcTags: StateFlow<List<NfcTagEntity>> =
    blockerRepo.nfcTags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val qrTokens: StateFlow<List<QrTokenEntity>> =
    blockerRepo.qrTokens.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val dailyAnalytics: StateFlow<List<DailyAnalyticsEntity>> =
    insightsRepo.recentDailyAnalytics.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      emptyList(),
    )

  val reminders: StateFlow<List<ReminderEntity>> =
    insightsRepo.reminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val recentNotifications: StateFlow<List<NotificationRecordEntity>> =
    insightsRepo.recentNotifications.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      emptyList(),
    )

  private val _lastScannedNfcUid = MutableStateFlow<String?>(null)
  val lastScannedNfcUid: StateFlow<String?> = _lastScannedNfcUid.asStateFlow()

  init {
    refreshPermissionsAndSync()
  }

  fun clearStatusBanner() {
    _statusBanner.value = null
  }

  fun postMessage(msg: String) {
    _statusBanner.value = msg
  }

  fun refreshPermissionsAndSync() {
    _permissions.value = PermissionManager.getSnapshot(app)
    viewModelScope.launch {
      usageRepo.syncInstalledApps()
      usageRepo.syncTodayUsageFromSystem()
      WidgetUpdater.refreshAll(app)
    }
  }

  fun refreshUsageOnly() {
    viewModelScope.launch {
      val ok = usageRepo.syncTodayUsageFromSystem()
      _permissions.value = PermissionManager.getSnapshot(app)
      _statusBanner.value =
        if (ok) "Usage stats synced from Android system."
        else "Usage Access not granted yet. Enable it in Settings to measure screen time."
      WidgetUpdater.refreshAll(app)
    }
  }

  fun completeOnboarding(
    dailyScreenGoalMinutes: Int,
    dailyFocusGoalMinutes: Int,
    distractingPackages: Set<String>,
    firstProfileName: String,
    firstFocusMinutes: Int,
  ) {
    viewModelScope.launch {
      focusRepo.updateGoals(
        dailyScreenTimeMinutes = dailyScreenGoalMinutes,
        dailyFocusMinutes = dailyFocusGoalMinutes,
        dailySessionsGoal = (dailyFocusGoalMinutes / 25).coerceAtLeast(1),
        weeklyFocusMinutes = dailyFocusGoalMinutes * 7,
      )
      distractingPackages.forEach { pkg -> usageRepo.setAppDistracting(pkg, true) }
      if (firstProfileName.isNotBlank()) {
        blockerRepo.saveProfile(
          BlockProfileEntity(
            name = firstProfileName.trim(),
            presetType = "Custom",
            blockedPackages = distractingPackages.toList(),
            focusDurationMinutes = firstFocusMinutes,
            unlockPolicy = UnlockChallengeType.REFLECTION_REMINDER.name,
            isActive = false,
            priority = 8,
          )
        )
      }
      prefs.updateSettings { it.copy(onboardingCompleted = true) }
    }
  }

  fun setDailyIntention(text: String) {
    viewModelScope.launch {
      prefs.updateSettings { it.copy(dailyIntention = text.trim()) }
    }
  }

  fun toggleQuickBlocker(enabled: Boolean) {
    viewModelScope.launch {
      if (!enabled && settings.value.invincibleModeEnabled) {
        _statusBanner.value = "Invincible Mode is active. Request unlock delay in More > Invincible Mode."
        return@launch
      }
      prefs.updateSettings { it.copy(quickBlockerEnabled = enabled) }
      WidgetUpdater.refreshAll(app)
    }
  }

  // Focus Timer Controls
  fun startFocusSession(
    mode: TimerMode,
    sessionType: SessionType,
    minutes: Int,
    note: String,
    profileId: Long?,
  ) {
    FocusTimerService.startTimer(
      context = app,
      mode = mode,
      sessionType = sessionType,
      minutes = minutes,
      note = note,
      profileId = profileId,
    )
  }

  fun pauseFocusSession() {
    FocusTimerService.sendAction(app, FocusTimerService.ACTION_PAUSE)
  }

  fun resumeFocusSession() {
    FocusTimerService.sendAction(app, FocusTimerService.ACTION_RESUME)
  }

  fun stopFocusSession(markCompleted: Boolean) {
    FocusTimerService.sendAction(app, FocusTimerService.ACTION_STOP, completed = markCompleted)
  }

  fun extendFocusSession(deltaMinutes: Int = 5) {
    FocusTimerService.extendTimer(app, deltaMinutes)
    _statusBanner.value = "Added +${deltaMinutes}m to active session."
  }

  fun restartFocusSession() {
    FocusTimerService.sendAction(app, FocusTimerService.ACTION_RESTART)
    _statusBanner.value = "Session timer restarted."
  }

  fun skipFocusBreak() {
    FocusTimerService.sendAction(app, FocusTimerService.ACTION_SKIP_BREAK)
    _statusBanner.value = "Break skipped — next focus cycle started."
  }

  fun updatePomodoroSettings(
    focusMin: Int,
    shortBreakMin: Int,
    longBreakMin: Int,
    interval: Int,
    cycles: Int,
    autoNext: Boolean,
    sound: Boolean,
    vibration: Boolean,
  ) {
    viewModelScope.launch {
      prefs.updateSettings {
        it.copy(
          pomodoroFocusMinutes = focusMin.coerceIn(5, 180),
          pomodoroShortBreakMinutes = shortBreakMin.coerceIn(1, 30),
          pomodoroLongBreakMinutes = longBreakMin.coerceIn(5, 60),
          pomodoroLongBreakInterval = interval.coerceIn(2, 8),
          pomodoroTargetCycles = cycles.coerceIn(1, 12),
          autoStartNextSession = autoNext,
          soundEnabled = sound,
          vibrationEnabled = vibration,
        )
      }
      _statusBanner.value = "Focus timer settings saved."
    }
  }

  fun setAmbientSound(type: AmbientSoundType, volume: Float) {
    viewModelScope.launch {
      prefs.updateSettings {
        it.copy(
          ambientSoundType = type,
          ambientSoundVolume = volume.coerceIn(0f, 1f),
        )
      }
    }
  }

  fun updateLaunchPauseConfig(enabled: Boolean? = null, seconds: Int? = null) {
    viewModelScope.launch {
      prefs.updateSettings { cur ->
        cur.copy(
          launchPauseEnabled = enabled ?: cur.launchPauseEnabled,
          launchPauseSeconds = (seconds ?: cur.launchPauseSeconds).coerceIn(5, 15),
        )
      }
    }
  }

  fun toggleLaunchPausePackage(packageName: String, paused: Boolean) {
    viewModelScope.launch {
      prefs.updateSettings { cur ->
        val next =
          if (paused) cur.launchPausePackages + packageName
          else cur.launchPausePackages - packageName
        cur.copy(launchPausePackages = next)
      }
    }
  }

  fun toggleWindDownStep(stepId: String) {
    viewModelScope.launch {
      val todayKey = FocusTimeUtils.todayKey()
      prefs.updateSettings { cur ->
        val baseIds = if (cur.windDownDateKey == todayKey) cur.windDownCompletedIds else emptySet()
        val nextIds = if (stepId in baseIds) baseIds - stepId else baseIds + stepId
        cur.copy(windDownDateKey = todayKey, windDownCompletedIds = nextIds)
      }
    }
  }

  fun openReleasesInBrowser() {
    val opened = PermissionManager.openExternalUrl(app, UpdateReleaseConfig.RELEASES_URL)
    if (!opened) {
      _statusBanner.value = "No browser app found. Visit ${UpdateReleaseConfig.RELEASES_URL} manually."
    }
  }

  fun updateGoals(
    screenGoalMin: Int,
    focusGoalMin: Int,
    sessionsGoal: Int,
    weeklyFocusGoalMin: Int,
  ) {
    viewModelScope.launch {
      focusRepo.updateGoals(screenGoalMin, focusGoalMin, sessionsGoal, weeklyFocusGoalMin)
      WidgetUpdater.refreshAll(app)
      _statusBanner.value = "Daily and weekly goals updated."
    }
  }

  // Blocker & Profiles
  fun toggleAppDistracting(pkg: String, distracting: Boolean) {
    viewModelScope.launch { usageRepo.setAppDistracting(pkg, distracting) }
  }

  fun toggleEmergencyWhitelist(pkg: String, whitelisted: Boolean) {
    viewModelScope.launch { usageRepo.setAppEmergencyWhitelisted(pkg, whitelisted) }
  }

  fun saveAppLimit(
    pkg: String,
    appName: String,
    dailyMinutes: Int,
    weeklyMinutes: Int,
    manualBlock: Boolean,
    challengeType: UnlockChallengeType,
    gracePeriodMinutes: Int = 0,
    categoryName: String = "",
  ) {
    viewModelScope.launch {
      if (settings.value.invincibleModeEnabled && !manualBlock) {
        _statusBanner.value = "Invincible Mode locks rule relaxations until cooldown finishes."
        return@launch
      }
      blockerRepo.saveAppLimit(
        AppLimitEntity(
          packageName = pkg,
          appName = appName,
          dailyLimitMinutes = dailyMinutes.coerceAtLeast(0),
          weeklyLimitMinutes = weeklyMinutes.coerceAtLeast(0),
          isBlockedManually = manualBlock,
          challengeType = challengeType.name,
          categoryName = categoryName,
          gracePeriodMinutes = gracePeriodMinutes.coerceIn(0, 30),
        )
      )
      _statusBanner.value = "Saved rule for $appName."
    }
  }

  fun saveCategoryLimit(
    categoryName: String,
    dailyLimitMinutes: Int,
    isEnabled: Boolean,
    challengeType: UnlockChallengeType,
  ) {
    viewModelScope.launch {
      blockerRepo.saveCategoryLimit(
        CategoryLimitEntity(
          categoryName = categoryName,
          dailyLimitMinutes = dailyLimitMinutes.coerceAtLeast(5),
          isEnabled = isEnabled,
          challengeType = challengeType.name,
        )
      )
      _statusBanner.value = "Category limit saved for $categoryName."
    }
  }

  fun deleteCategoryLimit(categoryName: String) {
    viewModelScope.launch {
      if (settings.value.invincibleModeEnabled) {
        _statusBanner.value = "Invincible Mode locks rule removals until cooldown finishes."
        return@launch
      }
      blockerRepo.deleteCategoryLimit(categoryName)
    }
  }

  fun deleteAppLimit(pkg: String) {
    viewModelScope.launch {
      if (settings.value.invincibleModeEnabled) {
        _statusBanner.value = "Cannot remove limits while Invincible Mode is active."
        return@launch
      }
      blockerRepo.deleteAppLimit(pkg)
    }
  }

  fun saveAppGroup(name: String, packages: List<String>, sharedMinutes: Int, existingId: Long = 0L) {
    viewModelScope.launch {
      if (name.isBlank() || packages.isEmpty()) return@launch
      blockerRepo.saveAppGroup(
        AppGroupEntity(
          id = existingId,
          name = name.trim(),
          packageNames = packages,
          sharedDailyLimitMinutes = sharedMinutes.coerceAtLeast(5),
          isEnabled = true,
        )
      )
      _statusBanner.value = "App group \"$name\" saved."
    }
  }

  fun deleteAppGroup(group: AppGroupEntity) {
    viewModelScope.launch { blockerRepo.deleteAppGroup(group) }
  }

  fun toggleProfileActive(profile: BlockProfileEntity, active: Boolean) {
    viewModelScope.launch {
      if (!active && settings.value.invincibleModeEnabled) {
        _statusBanner.value = "Invincible Mode is protecting active profiles."
        return@launch
      }
      blockerRepo.setProfileActive(profile.id, active)
      WidgetUpdater.refreshAll(app)
    }
  }

  fun saveProfile(profile: BlockProfileEntity) {
    viewModelScope.launch {
      blockerRepo.saveProfile(profile)
      _statusBanner.value = "Profile \"${profile.name}\" saved."
    }
  }

  fun deleteProfile(profile: BlockProfileEntity) {
    viewModelScope.launch {
      if (settings.value.invincibleModeEnabled) {
        _statusBanner.value = "Cannot delete profiles while Invincible Mode is active."
        return@launch
      }
      blockerRepo.deleteProfile(profile)
    }
  }

  fun saveSchedule(schedule: BlockingScheduleEntity) {
    viewModelScope.launch {
      blockerRepo.saveSchedule(schedule)
      _statusBanner.value = "Schedule \"${schedule.label}\" saved."
    }
  }

  fun deleteSchedule(schedule: BlockingScheduleEntity) {
    viewModelScope.launch { blockerRepo.deleteSchedule(schedule) }
  }

  // Reels & Shorts Reduction
  fun updateShortFormRule(rule: ShortFormRuleEntity) {
    viewModelScope.launch {
      blockerRepo.saveShortFormRule(rule)
    }
  }

  fun toggleReelsReductionGlobal(enabled: Boolean) {
    viewModelScope.launch {
      prefs.updateSettings { it.copy(reelsReductionEnabled = enabled) }
    }
  }

  // Website Blocking (Local VPN)
  fun addWebsiteRule(domain: String, category: String, isAllowlist: Boolean) {
    viewModelScope.launch {
      val clean = domain.trim().lowercase().removePrefix("https://").removePrefix("http://").substringBefore("/")
      if (clean.isBlank() || !clean.contains('.')) {
        _statusBanner.value = "Enter a valid domain such as youtube.com"
        return@launch
      }
      blockerRepo.saveWebsiteRule(
        WebsiteRuleEntity(domain = clean, category = category, isAllowlist = isAllowlist, isEnabled = true)
      )
    }
  }

  fun deleteWebsiteRule(rule: WebsiteRuleEntity) {
    viewModelScope.launch { blockerRepo.deleteWebsiteRule(rule) }
  }

  fun installWebsitePack(packName: String) {
    viewModelScope.launch {
      blockerRepo.installOfflineWebsitePack(packName)
      _statusBanner.value = "Installed offline pack: $packName"
    }
  }

  fun stopLocalVpn() {
    FocusNestVpnService.stop(app)
    _permissions.value = PermissionManager.getSnapshot(app)
  }

  fun startLocalVpn() {
    FocusNestVpnService.start(app)
    _permissions.value = PermissionManager.getSnapshot(app)
  }

  fun toggleBlockedNetworkPackage(pkg: String, blocked: Boolean) {
    viewModelScope.launch {
      prefs.updateSettings { cur ->
        val next =
          if (blocked) cur.blockedNetworkPackages + pkg
          else cur.blockedNetworkPackages - pkg
        cur.copy(blockedNetworkPackages = next)
      }
    }
  }

  // Habits & Offline Routines
  fun saveHabit(
    title: String,
    description: String,
    frequencyType: String,
    targetCount: Int,
    unitLabel: String,
    existingId: Long = 0L,
  ) {
    viewModelScope.launch {
      if (title.isBlank()) return@launch
      habitRepo.saveHabit(
        HabitEntity(
          id = existingId,
          title = title.trim(),
          description = description.trim(),
          frequencyType = frequencyType,
          targetCount = targetCount.coerceIn(1, 50),
          unitLabel = unitLabel.trim().ifEmpty { "times" },
        )
      )
      _statusBanner.value = "Habit \"${title.trim()}\" saved."
    }
  }

  fun seedStarterHabits() {
    viewModelScope.launch {
      if (habits.value.isNotEmpty()) return@launch
      listOf(
        HabitEntity(
          title = "Morning Screen-Free Hour",
          description = "Keep phone away for the first 60 minutes after waking",
          frequencyType = "DAILY",
          targetCount = 1,
          unitLabel = "day",
        ),
        HabitEntity(
          title = "Focused Reading / Study",
          description = "Read or study offline without interruptions",
          frequencyType = "DAILY",
          targetCount = 20,
          unitLabel = "pages",
        ),
        HabitEntity(
          title = "Device Dock Before Sleep",
          description = "Place phone outside reach 30m before bedtime",
          frequencyType = "DAILY",
          targetCount = 1,
          unitLabel = "night",
        ),
      ).forEach { habitRepo.saveHabit(it) }
      _statusBanner.value = "Added starter offline habits."
    }
  }

  fun deleteHabit(habit: HabitEntity) {
    viewModelScope.launch { habitRepo.deleteHabit(habit) }
  }

  fun toggleHabitToday(habit: HabitEntity) {
    viewModelScope.launch { habitRepo.toggleHabitForDate(habit, FocusTimeUtils.todayKey()) }
  }

  fun incrementHabitToday(habit: HabitEntity, delta: Int) {
    viewModelScope.launch { habitRepo.recordHabitProgress(habit, FocusTimeUtils.todayKey(), delta) }
  }

  fun computeHabitStreak(habit: HabitEntity, completions: List<HabitCompletionEntity>): Int =
    habitRepo.computeHabitStreakDays(habit, completions)

  fun computeWeeklyHabitRates(
    habitList: List<HabitEntity>,
    completions: List<HabitCompletionEntity>,
  ): List<Pair<String, Float>> = habitRepo.weeklyCompletionRates(habitList, completions)

  // NFC / QR / Barcode Tokens
  fun onNfcTagScanned(uidHex: String) {
    _lastScannedNfcUid.value = uidHex
    viewModelScope.launch {
      val matched = blockerRepo.findNfcTag(uidHex)
      if (matched != null) {
        when (matched.actionType) {
          "START_FOCUS" -> {
            startFocusSession(
              mode = TimerMode.POMODORO,
              sessionType = SessionType.WORK,
              minutes = matched.focusMinutes,
              note = "Triggered by NFC tag ${matched.label}",
              profileId = matched.targetProfileId,
            )
            _statusBanner.value = "NFC tag \"${matched.label}\" started a ${matched.focusMinutes}m focus session."
          }
          "TOGGLE_BLOCKER" -> {
            val next = !settings.value.quickBlockerEnabled
            toggleQuickBlocker(next)
            _statusBanner.value = "NFC tag \"${matched.label}\" toggled Shield ${if (next) "ON" else "OFF"}."
          }
          else -> {
            matched.targetProfileId?.let { pid ->
              blockerRepo.setProfileActive(pid, false)
            }
            _statusBanner.value = "NFC tag \"${matched.label}\" verified."
          }
        }
      } else {
        _statusBanner.value = "Scanned new NFC tag ($uidHex). Save it in NFC & QR Vault."
      }
    }
  }

  fun saveNfcTag(uidHex: String, label: String, actionType: String, profileId: Long?, focusMinutes: Int) {
    viewModelScope.launch {
      blockerRepo.registerNfcTag(
        NfcTagEntity(
          tagUidHex = uidHex.trim(),
          label = label.trim().ifEmpty { "Focus Tag" },
          actionType = actionType,
          targetProfileId = profileId,
          focusMinutes = focusMinutes,
        )
      )
      _statusBanner.value = "NFC tag saved."
    }
  }

  fun deleteNfcTag(tag: NfcTagEntity) {
    viewModelScope.launch { blockerRepo.deleteNfcTag(tag) }
  }

  fun saveQrOrBarcodeToken(
    value: String,
    label: String,
    format: String,
    profileId: Long?,
    isOneTime: Boolean = false,
    expiresInHours: Int = 0,
  ) {
    viewModelScope.launch {
      if (value.isBlank()) return@launch
      val now = System.currentTimeMillis()
      val expiresAt = if (expiresInHours > 0) now + expiresInHours * 3_600_000L else 0L
      blockerRepo.registerQrOrBarcodeToken(
        QrTokenEntity(
          tokenValue = value.trim(),
          label = label.trim().ifEmpty { "FocusNest Token" },
          tokenFormat = format,
          targetProfileId = profileId,
          createdAtEpochMs = now,
          isOneTime = isOneTime,
          expiresAtEpochMs = expiresAt,
        )
      )
      _statusBanner.value = "$format token \"$label\" saved."
    }
  }

  fun deleteQrToken(token: QrTokenEntity) {
    viewModelScope.launch { blockerRepo.deleteQrOrBarcodeToken(token) }
  }

  // Invincible Mode & Mindful Settings
  fun enableInvincibleMode(delayMinutes: Int, requireBiometric: Boolean, emergencyPasscode: String) {
    viewModelScope.launch {
      val hash =
        if (emergencyPasscode.isNotBlank()) KeystoreAndBackupCrypto.sha256Hex(emergencyPasscode.trim())
        else settings.value.emergencyPasscodeHash
      prefs.updateSettings {
        it.copy(
          invincibleModeEnabled = true,
          invincibleDelayMinutes = delayMinutes.coerceIn(5, 120),
          invincibleBiometricRequired = requireBiometric,
          invinciblePendingUnlockAtEpochMs = 0L,
          emergencyPasscodeHash = hash,
        )
      }
      _statusBanner.value = "Invincible Mode enabled with ${delayMinutes}m rule-change delay."
    }
  }

  fun requestInvincibleUnlockDelay() {
    viewModelScope.launch {
      val delayMs = settings.value.invincibleDelayMinutes * 60_000L
      val readyAt = System.currentTimeMillis() + delayMs
      prefs.updateSettings { it.copy(invinciblePendingUnlockAtEpochMs = readyAt) }
      _statusBanner.value = "Cooldown started. Invincible Mode can be turned off in ${settings.value.invincibleDelayMinutes}m."
    }
  }

  fun completeInvincibleDisableIfReady(): Boolean {
    val s = settings.value
    if (s.invinciblePendingUnlockAtEpochMs in 1..System.currentTimeMillis()) {
      viewModelScope.launch {
        prefs.updateSettings {
          it.copy(invincibleModeEnabled = false, invinciblePendingUnlockAtEpochMs = 0L)
        }
        _statusBanner.value = "Invincible Mode disabled after cooldown."
      }
      return true
    }
    _statusBanner.value = "Cooldown still active."
    return false
  }

  fun emergencyDisableInvincibleWithPasscode(passcode: String): Boolean {
    val s = settings.value
    val ok =
      s.emergencyPasscodeHash.isBlank() ||
        KeystoreAndBackupCrypto.sha256Hex(passcode.trim()) == s.emergencyPasscodeHash
    if (ok) {
      viewModelScope.launch {
        prefs.updateSettings {
          it.copy(invincibleModeEnabled = false, invinciblePendingUnlockAtEpochMs = 0L)
        }
        _statusBanner.value = "Emergency owner recovery verified. Invincible Mode turned off."
      }
    } else {
      _statusBanner.value = "Incorrect emergency recovery passcode."
    }
    return ok
  }

  fun updateMindfulSettings(
    bedtimeEnabled: Boolean? = null,
    bedtimeStart: Int? = null,
    bedtimeEnd: Int? = null,
    bedtimeDnd: Boolean? = null,
    breakEnabled: Boolean? = null,
    breakInterval: Int? = null,
    notifFilterEnabled: Boolean? = null,
    cooldownSec: Int? = null,
  ) {
    viewModelScope.launch {
      prefs.updateSettings { cur ->
        cur.copy(
          bedtimeEnabled = bedtimeEnabled ?: cur.bedtimeEnabled,
          bedtimeStartMinute = bedtimeStart ?: cur.bedtimeStartMinute,
          bedtimeEndMinute = bedtimeEnd ?: cur.bedtimeEndMinute,
          bedtimeDndEnabled = bedtimeDnd ?: cur.bedtimeDndEnabled,
          breakReminderEnabled = breakEnabled ?: cur.breakReminderEnabled,
          breakReminderIntervalMinutes = breakInterval ?: cur.breakReminderIntervalMinutes,
          notificationFilterEnabled = notifFilterEnabled ?: cur.notificationFilterEnabled,
          appOpeningCooldownSeconds = cooldownSec ?: cur.appOpeningCooldownSeconds,
        )
      }
      if (bedtimeDnd == true && PermissionManager.isDndPolicyAccessGranted(app)) {
        runCatching {
          val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
          nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        }
      } else if (bedtimeDnd == false && PermissionManager.isDndPolicyAccessGranted(app)) {
        runCatching {
          val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
          nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
      }
    }
  }

  // Theme & Appearance
  fun updateAppearance(
    themeMode: ThemeMode? = null,
    accentPalette: AccentPalette? = null,
    dynamicColor: Boolean? = null,
    languageCode: String? = null,
  ) {
    viewModelScope.launch {
      prefs.updateSettings { cur ->
        cur.copy(
          themeMode = themeMode ?: cur.themeMode,
          accentPalette = accentPalette ?: cur.accentPalette,
          dynamicColorEnabled = dynamicColor ?: cur.dynamicColorEnabled,
          languageCode = languageCode ?: cur.languageCode,
        )
      }
    }
  }

  // Exports & Backups
  suspend fun buildCsvExport(startKey: String, endKey: String): String =
    insightsRepo.exportUsageAndFocusCsv(startKey, endKey)

  suspend fun buildJsonExport(): String = insightsRepo.exportFullJson()

  suspend fun buildEncryptedBackup(passphrase: String): String =
    insightsRepo.exportEncryptedBackup(passphrase)

  suspend fun restoreBackup(payload: String, passphrase: String?): Result<Int> =
    runCatching { insightsRepo.restoreFromBackup(payload, passphrase) }

  suspend fun buildPrintableHtml(startKey: String, endKey: String): String =
    insightsRepo.buildPrintableReportHtml(startKey, endKey)

  fun clearUsageHistory() {
    viewModelScope.launch {
      insightsRepo.clearUsageHistory()
      _statusBanner.value = "Local usage history cleared."
    }
  }

  fun clearNotificationHistory() {
    viewModelScope.launch {
      insightsRepo.clearNotificationHistory()
      _statusBanner.value = "Notification log cleared."
    }
  }

  fun clearHabits() {
    viewModelScope.launch {
      insightsRepo.clearHabits()
      _statusBanner.value = "All habit records cleared."
    }
  }

  fun deleteAllData() {
    viewModelScope.launch {
      insightsRepo.deleteAllUserData()
      focusRepo.ensureDefaultGoal()
      blockerRepo.ensureStarterConfigurations()
      _statusBanner.value = "All local FocusNest data deleted."
    }
  }

  fun computeStreakDays(sessions: List<FocusSessionEntity>): Int =
    focusRepo.computeCurrentStreakDays(sessions)

  suspend fun computeBestFocusPeriod(sessions: List<FocusSessionEntity>): String =
    insightsRepo.computeBestFocusPeriod(sessions)
}
