package com.example.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AccentPalette
import com.example.domain.model.AmbientSoundType
import com.example.domain.model.SessionType
import com.example.domain.model.ThemeMode
import com.example.domain.model.TimerMode
import com.example.domain.model.TimerPhase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.focusNestDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "focusnest_preferences")

data class FocusNestSettings(
  val onboardingCompleted: Boolean = false,
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val accentPalette: AccentPalette = AccentPalette.EMERALD_SAGE,
  val dynamicColorEnabled: Boolean = false,
  val languageCode: String = "en",
  val dailyIntention: String = "",
  val quickBlockerEnabled: Boolean = false,
  val pomodoroFocusMinutes: Int = 25,
  val pomodoroShortBreakMinutes: Int = 5,
  val pomodoroLongBreakMinutes: Int = 15,
  val pomodoroLongBreakInterval: Int = 4,
  val pomodoroTargetCycles: Int = 4,
  val autoStartNextSession: Boolean = false,
  val soundEnabled: Boolean = true,
  val vibrationEnabled: Boolean = true,
  val invincibleModeEnabled: Boolean = false,
  val invincibleDelayMinutes: Int = 15,
  val invincibleBiometricRequired: Boolean = false,
  val invinciblePendingUnlockAtEpochMs: Long = 0L,
  val emergencyPasscodeHash: String = "",
  val bedtimeEnabled: Boolean = false,
  val bedtimeStartMinute: Int = 22 * 60 + 30,
  val bedtimeEndMinute: Int = 7 * 60,
  val bedtimeDndEnabled: Boolean = false,
  val breakReminderEnabled: Boolean = false,
  val breakReminderIntervalMinutes: Int = 45,
  val vpnBlockingEnabled: Boolean = false,
  val blockedNetworkPackages: Set<String> = emptySet(),
  val reelsReductionEnabled: Boolean = false,
  val notificationFilterEnabled: Boolean = false,
  val appOpeningCooldownSeconds: Int = 10,
  val ambientSoundType: AmbientSoundType = AmbientSoundType.OFF,
  val ambientSoundVolume: Float = 0.45f,
  val launchPauseEnabled: Boolean = false,
  val launchPauseSeconds: Int = 8,
  val launchPausePackages: Set<String> = emptySet(),
  val windDownDateKey: String = "",
  val windDownCompletedIds: Set<String> = emptySet(),
)

data class PersistedTimerState(
  val isRunning: Boolean = false,
  val phase: TimerPhase = TimerPhase.IDLE,
  val mode: TimerMode = TimerMode.POMODORO,
  val sessionType: SessionType = SessionType.STUDY,
  val plannedMinutes: Int = 25,
  val targetEndEpochMs: Long = 0L,
  val targetElapsedRealtimeMs: Long = 0L,
  val startedAtEpochMs: Long = 0L,
  val pausedRemainingSeconds: Long = 25 * 60L,
  val stopwatchElapsedBaseSeconds: Long = 0L,
  val completedCycles: Int = 0,
  val sessionNote: String = "",
  val linkedProfileId: Long? = null,
)

class FocusNestPreferences(private val context: Context) {
  private object Keys {
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val LANGUAGE_CODE = stringPreferencesKey("language_code")
    val DAILY_INTENTION = stringPreferencesKey("daily_intention")
    val QUICK_BLOCKER_ENABLED = booleanPreferencesKey("quick_blocker_enabled")

    val POMO_FOCUS_MIN = intPreferencesKey("pomo_focus_min")
    val POMO_SHORT_BREAK_MIN = intPreferencesKey("pomo_short_break_min")
    val POMO_LONG_BREAK_MIN = intPreferencesKey("pomo_long_break_min")
    val POMO_LONG_BREAK_INTERVAL = intPreferencesKey("pomo_long_break_interval")
    val POMO_TARGET_CYCLES = intPreferencesKey("pomo_target_cycles")
    val AUTO_START_NEXT = booleanPreferencesKey("auto_start_next")
    val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")

    val INVINCIBLE_ENABLED = booleanPreferencesKey("invincible_enabled")
    val INVINCIBLE_DELAY_MIN = intPreferencesKey("invincible_delay_min")
    val INVINCIBLE_BIOMETRIC = booleanPreferencesKey("invincible_biometric")
    val INVINCIBLE_PENDING_UNLOCK = longPreferencesKey("invincible_pending_unlock")
    val EMERGENCY_PASSCODE_HASH = stringPreferencesKey("emergency_passcode_hash")

    val BEDTIME_ENABLED = booleanPreferencesKey("bedtime_enabled")
    val BEDTIME_START = intPreferencesKey("bedtime_start")
    val BEDTIME_END = intPreferencesKey("bedtime_end")
    val BEDTIME_DND = booleanPreferencesKey("bedtime_dnd")
    val BREAK_REMINDER_ENABLED = booleanPreferencesKey("break_reminder_enabled")
    val BREAK_REMINDER_MIN = intPreferencesKey("break_reminder_min")
    val VPN_BLOCKING_ENABLED = booleanPreferencesKey("vpn_blocking_enabled")
    val BLOCKED_NETWORK_PACKAGES = stringPreferencesKey("blocked_network_packages")
    val REELS_REDUCTION_ENABLED = booleanPreferencesKey("reels_reduction_enabled")
    val NOTIFICATION_FILTER_ENABLED = booleanPreferencesKey("notification_filter_enabled")
    val APP_COOLDOWN_SEC = intPreferencesKey("app_cooldown_sec")
    val AMBIENT_SOUND_TYPE = stringPreferencesKey("ambient_sound_type")
    val AMBIENT_SOUND_VOLUME = floatPreferencesKey("ambient_sound_volume")
    val LAUNCH_PAUSE_ENABLED = booleanPreferencesKey("launch_pause_enabled")
    val LAUNCH_PAUSE_SECONDS = intPreferencesKey("launch_pause_seconds")
    val LAUNCH_PAUSE_PACKAGES = stringPreferencesKey("launch_pause_packages")
    val WINDDOWN_DATE_KEY = stringPreferencesKey("winddown_date_key")
    val WINDDOWN_COMPLETED_IDS = stringPreferencesKey("winddown_completed_ids")

    // Active timer persistence across process kills & reboots
    val TIMER_RUNNING = booleanPreferencesKey("timer_running")
    val TIMER_PHASE = stringPreferencesKey("timer_phase")
    val TIMER_MODE = stringPreferencesKey("timer_mode")
    val TIMER_SESSION_TYPE = stringPreferencesKey("timer_session_type")
    val TIMER_PLANNED_MIN = intPreferencesKey("timer_planned_min")
    val TIMER_TARGET_END = longPreferencesKey("timer_target_end")
    val TIMER_TARGET_ELAPSED = longPreferencesKey("timer_target_elapsed")
    val TIMER_STARTED_AT = longPreferencesKey("timer_started_at")
    val TIMER_PAUSED_REMAINING = longPreferencesKey("timer_paused_remaining")
    val TIMER_STOPWATCH_BASE = longPreferencesKey("timer_stopwatch_base")
    val TIMER_CYCLES = intPreferencesKey("timer_cycles")
    val TIMER_NOTE = stringPreferencesKey("timer_note")
    val TIMER_LINKED_PROFILE = longPreferencesKey("timer_linked_profile")
  }

  val settingsFlow: Flow<FocusNestSettings> =
    context.focusNestDataStore.data.map { prefs ->
      FocusNestSettings(
        onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
        themeMode =
          runCatching { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: "SYSTEM") }
            .getOrDefault(ThemeMode.SYSTEM),
        accentPalette =
          runCatching { AccentPalette.valueOf(prefs[Keys.ACCENT_PALETTE] ?: "EMERALD_SAGE") }
            .getOrDefault(AccentPalette.EMERALD_SAGE),
        dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR] ?: false,
        languageCode = prefs[Keys.LANGUAGE_CODE] ?: "en",
        dailyIntention = prefs[Keys.DAILY_INTENTION] ?: "",
        quickBlockerEnabled = prefs[Keys.QUICK_BLOCKER_ENABLED] ?: false,
        pomodoroFocusMinutes = prefs[Keys.POMO_FOCUS_MIN] ?: 25,
        pomodoroShortBreakMinutes = prefs[Keys.POMO_SHORT_BREAK_MIN] ?: 5,
        pomodoroLongBreakMinutes = prefs[Keys.POMO_LONG_BREAK_MIN] ?: 15,
        pomodoroLongBreakInterval = prefs[Keys.POMO_LONG_BREAK_INTERVAL] ?: 4,
        pomodoroTargetCycles = prefs[Keys.POMO_TARGET_CYCLES] ?: 4,
        autoStartNextSession = prefs[Keys.AUTO_START_NEXT] ?: false,
        soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
        vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
        invincibleModeEnabled = prefs[Keys.INVINCIBLE_ENABLED] ?: false,
        invincibleDelayMinutes = prefs[Keys.INVINCIBLE_DELAY_MIN] ?: 15,
        invincibleBiometricRequired = prefs[Keys.INVINCIBLE_BIOMETRIC] ?: false,
        invinciblePendingUnlockAtEpochMs = prefs[Keys.INVINCIBLE_PENDING_UNLOCK] ?: 0L,
        emergencyPasscodeHash = prefs[Keys.EMERGENCY_PASSCODE_HASH] ?: "",
        bedtimeEnabled = prefs[Keys.BEDTIME_ENABLED] ?: false,
        bedtimeStartMinute = prefs[Keys.BEDTIME_START] ?: (22 * 60 + 30),
        bedtimeEndMinute = prefs[Keys.BEDTIME_END] ?: (7 * 60),
        bedtimeDndEnabled = prefs[Keys.BEDTIME_DND] ?: false,
        breakReminderEnabled = prefs[Keys.BREAK_REMINDER_ENABLED] ?: false,
        breakReminderIntervalMinutes = prefs[Keys.BREAK_REMINDER_MIN] ?: 45,
        vpnBlockingEnabled = prefs[Keys.VPN_BLOCKING_ENABLED] ?: false,
        blockedNetworkPackages =
          (prefs[Keys.BLOCKED_NETWORK_PACKAGES] ?: "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet(),
        reelsReductionEnabled = prefs[Keys.REELS_REDUCTION_ENABLED] ?: false,
        notificationFilterEnabled = prefs[Keys.NOTIFICATION_FILTER_ENABLED] ?: false,
        appOpeningCooldownSeconds = prefs[Keys.APP_COOLDOWN_SEC] ?: 10,
        ambientSoundType =
          runCatching { AmbientSoundType.valueOf(prefs[Keys.AMBIENT_SOUND_TYPE] ?: "OFF") }
            .getOrDefault(AmbientSoundType.OFF),
        ambientSoundVolume = (prefs[Keys.AMBIENT_SOUND_VOLUME] ?: 0.45f).coerceIn(0f, 1f),
        launchPauseEnabled = prefs[Keys.LAUNCH_PAUSE_ENABLED] ?: false,
        launchPauseSeconds = (prefs[Keys.LAUNCH_PAUSE_SECONDS] ?: 8).coerceIn(5, 15),
        launchPausePackages =
          (prefs[Keys.LAUNCH_PAUSE_PACKAGES] ?: "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet(),
        windDownDateKey = prefs[Keys.WINDDOWN_DATE_KEY] ?: "",
        windDownCompletedIds =
          (prefs[Keys.WINDDOWN_COMPLETED_IDS] ?: "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet(),
      )
    }

  val timerStateFlow: Flow<PersistedTimerState> =
    context.focusNestDataStore.data.map { prefs ->
      val linked = prefs[Keys.TIMER_LINKED_PROFILE] ?: -1L
      PersistedTimerState(
        isRunning = prefs[Keys.TIMER_RUNNING] ?: false,
        phase =
          runCatching { TimerPhase.valueOf(prefs[Keys.TIMER_PHASE] ?: "IDLE") }
            .getOrDefault(TimerPhase.IDLE),
        mode =
          runCatching { TimerMode.valueOf(prefs[Keys.TIMER_MODE] ?: "POMODORO") }
            .getOrDefault(TimerMode.POMODORO),
        sessionType =
          runCatching { SessionType.valueOf(prefs[Keys.TIMER_SESSION_TYPE] ?: "STUDY") }
            .getOrDefault(SessionType.STUDY),
        plannedMinutes = prefs[Keys.TIMER_PLANNED_MIN] ?: 25,
        targetEndEpochMs = prefs[Keys.TIMER_TARGET_END] ?: 0L,
        targetElapsedRealtimeMs = prefs[Keys.TIMER_TARGET_ELAPSED] ?: 0L,
        startedAtEpochMs = prefs[Keys.TIMER_STARTED_AT] ?: 0L,
        pausedRemainingSeconds = prefs[Keys.TIMER_PAUSED_REMAINING] ?: (25 * 60L),
        stopwatchElapsedBaseSeconds = prefs[Keys.TIMER_STOPWATCH_BASE] ?: 0L,
        completedCycles = prefs[Keys.TIMER_CYCLES] ?: 0,
        sessionNote = prefs[Keys.TIMER_NOTE] ?: "",
        linkedProfileId = if (linked > 0L) linked else null,
      )
    }

  suspend fun updateSettings(transform: (FocusNestSettings) -> FocusNestSettings) {
    context.focusNestDataStore.edit { prefs ->
      val current =
        FocusNestSettings(
          onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
          themeMode =
            runCatching { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: "SYSTEM") }
              .getOrDefault(ThemeMode.SYSTEM),
          accentPalette =
            runCatching { AccentPalette.valueOf(prefs[Keys.ACCENT_PALETTE] ?: "EMERALD_SAGE") }
              .getOrDefault(AccentPalette.EMERALD_SAGE),
          dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR] ?: false,
          languageCode = prefs[Keys.LANGUAGE_CODE] ?: "en",
          dailyIntention = prefs[Keys.DAILY_INTENTION] ?: "",
          quickBlockerEnabled = prefs[Keys.QUICK_BLOCKER_ENABLED] ?: false,
          pomodoroFocusMinutes = prefs[Keys.POMO_FOCUS_MIN] ?: 25,
          pomodoroShortBreakMinutes = prefs[Keys.POMO_SHORT_BREAK_MIN] ?: 5,
          pomodoroLongBreakMinutes = prefs[Keys.POMO_LONG_BREAK_MIN] ?: 15,
          pomodoroLongBreakInterval = prefs[Keys.POMO_LONG_BREAK_INTERVAL] ?: 4,
          pomodoroTargetCycles = prefs[Keys.POMO_TARGET_CYCLES] ?: 4,
          autoStartNextSession = prefs[Keys.AUTO_START_NEXT] ?: false,
          soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
          vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
          invincibleModeEnabled = prefs[Keys.INVINCIBLE_ENABLED] ?: false,
          invincibleDelayMinutes = prefs[Keys.INVINCIBLE_DELAY_MIN] ?: 15,
          invincibleBiometricRequired = prefs[Keys.INVINCIBLE_BIOMETRIC] ?: false,
          invinciblePendingUnlockAtEpochMs = prefs[Keys.INVINCIBLE_PENDING_UNLOCK] ?: 0L,
          emergencyPasscodeHash = prefs[Keys.EMERGENCY_PASSCODE_HASH] ?: "",
          bedtimeEnabled = prefs[Keys.BEDTIME_ENABLED] ?: false,
          bedtimeStartMinute = prefs[Keys.BEDTIME_START] ?: (22 * 60 + 30),
          bedtimeEndMinute = prefs[Keys.BEDTIME_END] ?: (7 * 60),
          bedtimeDndEnabled = prefs[Keys.BEDTIME_DND] ?: false,
          breakReminderEnabled = prefs[Keys.BREAK_REMINDER_ENABLED] ?: false,
          breakReminderIntervalMinutes = prefs[Keys.BREAK_REMINDER_MIN] ?: 45,
          vpnBlockingEnabled = prefs[Keys.VPN_BLOCKING_ENABLED] ?: false,
          blockedNetworkPackages =
            (prefs[Keys.BLOCKED_NETWORK_PACKAGES] ?: "")
              .split(",")
              .map { it.trim() }
              .filter { it.isNotEmpty() }
              .toSet(),
          reelsReductionEnabled = prefs[Keys.REELS_REDUCTION_ENABLED] ?: false,
          notificationFilterEnabled = prefs[Keys.NOTIFICATION_FILTER_ENABLED] ?: false,
          appOpeningCooldownSeconds = prefs[Keys.APP_COOLDOWN_SEC] ?: 10,
          ambientSoundType =
            runCatching { AmbientSoundType.valueOf(prefs[Keys.AMBIENT_SOUND_TYPE] ?: "OFF") }
              .getOrDefault(AmbientSoundType.OFF),
          ambientSoundVolume = (prefs[Keys.AMBIENT_SOUND_VOLUME] ?: 0.45f).coerceIn(0f, 1f),
          launchPauseEnabled = prefs[Keys.LAUNCH_PAUSE_ENABLED] ?: false,
          launchPauseSeconds = (prefs[Keys.LAUNCH_PAUSE_SECONDS] ?: 8).coerceIn(5, 15),
          launchPausePackages =
            (prefs[Keys.LAUNCH_PAUSE_PACKAGES] ?: "")
              .split(",")
              .map { it.trim() }
              .filter { it.isNotEmpty() }
              .toSet(),
          windDownDateKey = prefs[Keys.WINDDOWN_DATE_KEY] ?: "",
          windDownCompletedIds =
            (prefs[Keys.WINDDOWN_COMPLETED_IDS] ?: "")
              .split(",")
              .map { it.trim() }
              .filter { it.isNotEmpty() }
              .toSet(),
        )
      val next = transform(current)
      prefs[Keys.ONBOARDING_COMPLETED] = next.onboardingCompleted
      prefs[Keys.THEME_MODE] = next.themeMode.name
      prefs[Keys.ACCENT_PALETTE] = next.accentPalette.name
      prefs[Keys.DYNAMIC_COLOR] = next.dynamicColorEnabled
      prefs[Keys.LANGUAGE_CODE] = next.languageCode
      prefs[Keys.DAILY_INTENTION] = next.dailyIntention
      prefs[Keys.QUICK_BLOCKER_ENABLED] = next.quickBlockerEnabled
      prefs[Keys.POMO_FOCUS_MIN] = next.pomodoroFocusMinutes
      prefs[Keys.POMO_SHORT_BREAK_MIN] = next.pomodoroShortBreakMinutes
      prefs[Keys.POMO_LONG_BREAK_MIN] = next.pomodoroLongBreakMinutes
      prefs[Keys.POMO_LONG_BREAK_INTERVAL] = next.pomodoroLongBreakInterval
      prefs[Keys.POMO_TARGET_CYCLES] = next.pomodoroTargetCycles
      prefs[Keys.AUTO_START_NEXT] = next.autoStartNextSession
      prefs[Keys.SOUND_ENABLED] = next.soundEnabled
      prefs[Keys.VIBRATION_ENABLED] = next.vibrationEnabled
      prefs[Keys.INVINCIBLE_ENABLED] = next.invincibleModeEnabled
      prefs[Keys.INVINCIBLE_DELAY_MIN] = next.invincibleDelayMinutes
      prefs[Keys.INVINCIBLE_BIOMETRIC] = next.invincibleBiometricRequired
      prefs[Keys.INVINCIBLE_PENDING_UNLOCK] = next.invinciblePendingUnlockAtEpochMs
      prefs[Keys.EMERGENCY_PASSCODE_HASH] = next.emergencyPasscodeHash
      prefs[Keys.BEDTIME_ENABLED] = next.bedtimeEnabled
      prefs[Keys.BEDTIME_START] = next.bedtimeStartMinute
      prefs[Keys.BEDTIME_END] = next.bedtimeEndMinute
      prefs[Keys.BEDTIME_DND] = next.bedtimeDndEnabled
      prefs[Keys.BREAK_REMINDER_ENABLED] = next.breakReminderEnabled
      prefs[Keys.BREAK_REMINDER_MIN] = next.breakReminderIntervalMinutes
      prefs[Keys.VPN_BLOCKING_ENABLED] = next.vpnBlockingEnabled
      prefs[Keys.BLOCKED_NETWORK_PACKAGES] = next.blockedNetworkPackages.joinToString(",")
      prefs[Keys.REELS_REDUCTION_ENABLED] = next.reelsReductionEnabled
      prefs[Keys.NOTIFICATION_FILTER_ENABLED] = next.notificationFilterEnabled
      prefs[Keys.APP_COOLDOWN_SEC] = next.appOpeningCooldownSeconds
      prefs[Keys.AMBIENT_SOUND_TYPE] = next.ambientSoundType.name
      prefs[Keys.AMBIENT_SOUND_VOLUME] = next.ambientSoundVolume.coerceIn(0f, 1f)
      prefs[Keys.LAUNCH_PAUSE_ENABLED] = next.launchPauseEnabled
      prefs[Keys.LAUNCH_PAUSE_SECONDS] = next.launchPauseSeconds.coerceIn(5, 15)
      prefs[Keys.LAUNCH_PAUSE_PACKAGES] = next.launchPausePackages.joinToString(",")
      prefs[Keys.WINDDOWN_DATE_KEY] = next.windDownDateKey
      prefs[Keys.WINDDOWN_COMPLETED_IDS] = next.windDownCompletedIds.joinToString(",")
    }
  }

  suspend fun saveTimerState(state: PersistedTimerState) {
    context.focusNestDataStore.edit { prefs ->
      prefs[Keys.TIMER_RUNNING] = state.isRunning
      prefs[Keys.TIMER_PHASE] = state.phase.name
      prefs[Keys.TIMER_MODE] = state.mode.name
      prefs[Keys.TIMER_SESSION_TYPE] = state.sessionType.name
      prefs[Keys.TIMER_PLANNED_MIN] = state.plannedMinutes
      prefs[Keys.TIMER_TARGET_END] = state.targetEndEpochMs
      prefs[Keys.TIMER_TARGET_ELAPSED] = state.targetElapsedRealtimeMs
      prefs[Keys.TIMER_STARTED_AT] = state.startedAtEpochMs
      prefs[Keys.TIMER_PAUSED_REMAINING] = state.pausedRemainingSeconds
      prefs[Keys.TIMER_STOPWATCH_BASE] = state.stopwatchElapsedBaseSeconds
      prefs[Keys.TIMER_CYCLES] = state.completedCycles
      prefs[Keys.TIMER_NOTE] = state.sessionNote
      prefs[Keys.TIMER_LINKED_PROFILE] = state.linkedProfileId ?: -1L
    }
  }

  suspend fun clearAll() {
    context.focusNestDataStore.edit { it.clear() }
  }
}
