package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.FocusNestApplication
import com.example.MainActivity
import com.example.R
import com.example.core.audio.AmbientSoundEngine
import com.example.core.database.FocusSessionEntity
import com.example.core.datastore.PersistedTimerState
import com.example.domain.model.AmbientSoundType
import com.example.domain.model.FocusTimeUtils
import com.example.domain.model.SessionType
import com.example.domain.model.TimerMode
import com.example.domain.model.TimerPhase
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusTimerService : Service() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var tickerJob: Job? = null
  private val ambientEngine = AmbientSoundEngine()

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val app = applicationContext as FocusNestApplication
    val action = intent?.action ?: ACTION_RESTORE

    when (action) {
      ACTION_START -> {
        val modeName = intent?.getStringExtra(EXTRA_MODE) ?: TimerMode.POMODORO.name
        val typeName = intent?.getStringExtra(EXTRA_SESSION_TYPE) ?: SessionType.STUDY.name
        val minutes = intent?.getIntExtra(EXTRA_MINUTES, 25) ?: 25
        val note = intent?.getStringExtra(EXTRA_NOTE) ?: ""
        val profileId = intent?.getLongExtra(EXTRA_PROFILE_ID, -1L)?.takeIf { it > 0L }
        serviceScope.launch {
          val mode = runCatching { TimerMode.valueOf(modeName) }.getOrDefault(TimerMode.POMODORO)
          val type = runCatching { SessionType.valueOf(typeName) }.getOrDefault(SessionType.STUDY)
          val now = System.currentTimeMillis()
          val elapsedNow = SystemClock.elapsedRealtime()
          val durationMs = minutes * 60_000L
          val state =
            PersistedTimerState(
              isRunning = true,
              phase = TimerPhase.FOCUS,
              mode = mode,
              sessionType = type,
              plannedMinutes = minutes,
              targetEndEpochMs = if (mode == TimerMode.STOPWATCH) 0L else now + durationMs,
              targetElapsedRealtimeMs = if (mode == TimerMode.STOPWATCH) 0L else elapsedNow + durationMs,
              startedAtEpochMs = now,
              pausedRemainingSeconds = minutes * 60L,
              stopwatchElapsedBaseSeconds = 0L,
              completedCycles = 0,
              sessionNote = note,
              linkedProfileId = profileId,
            )
          app.container.preferences.saveTimerState(state)
          startForegroundAndTicker(app, state)
        }
      }
      ACTION_PAUSE -> {
        serviceScope.launch {
          val current = app.container.preferences.timerStateFlow.first()
          if (current.isRunning) {
            val now = System.currentTimeMillis()
            val paused =
              if (current.mode == TimerMode.STOPWATCH) {
                val elapsed = current.stopwatchElapsedBaseSeconds + ((now - current.startedAtEpochMs) / 1000L)
                current.copy(isRunning = false, stopwatchElapsedBaseSeconds = elapsed)
              } else {
                val rem = remainingSecondsForState(current, now, SystemClock.elapsedRealtime())
                current.copy(isRunning = false, pausedRemainingSeconds = rem, targetElapsedRealtimeMs = 0L)
              }
            app.container.preferences.saveTimerState(paused)
          }
          tickerJob?.cancel()
          ambientEngine.stop()
          stopForegroundCompat()
          WidgetUpdater.refreshAll(applicationContext)
          stopSelf()
        }
      }
      ACTION_RESUME -> {
        serviceScope.launch {
          val current = app.container.preferences.timerStateFlow.first()
          if (!current.isRunning && current.phase != TimerPhase.IDLE) {
            val now = System.currentTimeMillis()
            val elapsedNow = SystemClock.elapsedRealtime()
            val remMs = current.pausedRemainingSeconds * 1000L
            val resumed =
              if (current.mode == TimerMode.STOPWATCH) {
                current.copy(isRunning = true, startedAtEpochMs = now)
              } else {
                current.copy(
                  isRunning = true,
                  targetEndEpochMs = now + remMs,
                  targetElapsedRealtimeMs = elapsedNow + remMs,
                )
              }
            app.container.preferences.saveTimerState(resumed)
            startForegroundAndTicker(app, resumed)
          } else {
            stopSelf()
          }
        }
      }
      ACTION_EXTEND -> {
        val deltaMinutes = (intent?.getIntExtra(EXTRA_DELTA_MINUTES, 5) ?: 5).coerceIn(1, 60)
        serviceScope.launch {
          val current = app.container.preferences.timerStateFlow.first()
          if (current.phase != TimerPhase.IDLE && current.mode != TimerMode.STOPWATCH) {
            val deltaMs = deltaMinutes * 60_000L
            val extended =
              if (current.isRunning) {
                current.copy(
                  plannedMinutes = current.plannedMinutes + deltaMinutes,
                  targetEndEpochMs = current.targetEndEpochMs + deltaMs,
                  targetElapsedRealtimeMs =
                    if (current.targetElapsedRealtimeMs > 0L) current.targetElapsedRealtimeMs + deltaMs else 0L,
                )
              } else {
                current.copy(
                  plannedMinutes = current.plannedMinutes + deltaMinutes,
                  pausedRemainingSeconds = current.pausedRemainingSeconds + deltaMinutes * 60L,
                )
              }
            app.container.preferences.saveTimerState(extended)
            if (extended.isRunning) {
              val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
              nm.notify(NOTIFICATION_ID, buildNotification(extended))
            }
            WidgetUpdater.refreshAll(applicationContext)
          }
        }
      }
      ACTION_RESTART -> {
        serviceScope.launch {
          val current = app.container.preferences.timerStateFlow.first()
          if (current.phase != TimerPhase.IDLE) {
            val now = System.currentTimeMillis()
            val elapsedNow = SystemClock.elapsedRealtime()
            val durationMs = current.plannedMinutes * 60_000L
            val restarted =
              current.copy(
                isRunning = true,
                targetEndEpochMs = if (current.mode == TimerMode.STOPWATCH) 0L else now + durationMs,
                targetElapsedRealtimeMs = if (current.mode == TimerMode.STOPWATCH) 0L else elapsedNow + durationMs,
                startedAtEpochMs = now,
                pausedRemainingSeconds = current.plannedMinutes * 60L,
                stopwatchElapsedBaseSeconds = 0L,
              )
            app.container.preferences.saveTimerState(restarted)
            startForegroundAndTicker(app, restarted)
          }
        }
      }
      ACTION_SKIP_BREAK -> {
        serviceScope.launch {
          val current = app.container.preferences.timerStateFlow.first()
          if (current.phase == TimerPhase.SHORT_BREAK || current.phase == TimerPhase.LONG_BREAK) {
            val settings = app.container.preferences.settingsFlow.first()
            val now = System.currentTimeMillis()
            val elapsedNow = SystemClock.elapsedRealtime()
            val focusMin = settings.pomodoroFocusMinutes
            val durationMs = focusMin * 60_000L
            val next =
              current.copy(
                isRunning = true,
                phase = TimerPhase.FOCUS,
                plannedMinutes = focusMin,
                targetEndEpochMs = now + durationMs,
                targetElapsedRealtimeMs = elapsedNow + durationMs,
                startedAtEpochMs = now,
                pausedRemainingSeconds = focusMin * 60L,
              )
            app.container.preferences.saveTimerState(next)
            startForegroundAndTicker(app, next)
          }
        }
      }
      ACTION_STOP -> {
        val completed = intent?.getBooleanExtra(EXTRA_COMPLETED, false) ?: false
        serviceScope.launch {
          val current = app.container.preferences.timerStateFlow.first()
          persistSessionRecord(app, current, completed = completed, interrupted = !completed)
          val settings = app.container.preferences.settingsFlow.first()
          app.container.preferences.saveTimerState(
            PersistedTimerState(
              isRunning = false,
              phase = TimerPhase.IDLE,
              mode = current.mode,
              sessionType = current.sessionType,
              plannedMinutes = settings.pomodoroFocusMinutes,
              pausedRemainingSeconds = settings.pomodoroFocusMinutes * 60L,
            )
          )
          tickerJob?.cancel()
          ambientEngine.stop()
          stopForegroundCompat()
          WidgetUpdater.refreshAll(applicationContext)
          stopSelf()
        }
      }
      ACTION_RESTORE -> {
        serviceScope.launch {
          val current = app.container.preferences.timerStateFlow.first()
          if (current.isRunning && current.phase != TimerPhase.IDLE) {
            // Re-anchor monotonic clock safely if device rebooted since targetElapsedRealtimeMs was saved
            val now = System.currentTimeMillis()
            val remSec = ((current.targetEndEpochMs - now) / 1000L).coerceAtLeast(0L)
            val reanchored =
              if (current.mode != TimerMode.STOPWATCH && current.targetEndEpochMs > now) {
                current.copy(targetElapsedRealtimeMs = SystemClock.elapsedRealtime() + remSec * 1000L)
              } else current
            app.container.preferences.saveTimerState(reanchored)
            startForegroundAndTicker(app, reanchored)
          } else {
            stopSelf()
          }
        }
      }
    }
    return START_STICKY
  }

  private fun startForegroundAndTicker(app: FocusNestApplication, initial: PersistedTimerState) {
    val notification = buildNotification(initial)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
    WidgetUpdater.refreshAll(applicationContext)

    tickerJob?.cancel()
    tickerJob =
      serviceScope.launch {
        while (isActive) {
          val state = app.container.preferences.timerStateFlow.first()
          if (!state.isRunning || state.phase == TimerPhase.IDLE) {
            ambientEngine.stop()
            break
          }
          val settings = app.container.preferences.settingsFlow.first()
          if (state.phase == TimerPhase.FOCUS && settings.ambientSoundType != AmbientSoundType.OFF) {
            ambientEngine.startOrUpdate(settings.ambientSoundType, settings.ambientSoundVolume)
          } else {
            ambientEngine.stop()
          }
          val now = System.currentTimeMillis()
          val elapsedNow = SystemClock.elapsedRealtime()
          val remainingSec = remainingSecondsForState(state, now, elapsedNow)
          if (state.mode != TimerMode.STOPWATCH && remainingSec <= 0L) {
            ambientEngine.stop()
            handlePhaseFinished(app, state)
            break
          }
          val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
          nm.notify(NOTIFICATION_ID, buildNotification(state))
          delay(1000L)
        }
      }
  }

  private suspend fun handlePhaseFinished(app: FocusNestApplication, state: PersistedTimerState) {
    val settings = app.container.preferences.settingsFlow.first()
    if (state.phase == TimerPhase.FOCUS) {
      persistSessionRecord(app, state, completed = true, interrupted = false)
    }
    alertCompletion(settings.soundEnabled, settings.vibrationEnabled)

    if (state.mode == TimerMode.POMODORO) {
      val now = System.currentTimeMillis()
      val nextCycles = if (state.phase == TimerPhase.FOCUS) state.completedCycles + 1 else state.completedCycles
      val nextPhase =
        if (state.phase == TimerPhase.FOCUS) {
          if (nextCycles % settings.pomodoroLongBreakInterval.coerceAtLeast(1) == 0) {
            TimerPhase.LONG_BREAK
          } else {
            TimerPhase.SHORT_BREAK
          }
        } else if (nextCycles < settings.pomodoroTargetCycles) {
          TimerPhase.FOCUS
        } else {
          TimerPhase.IDLE
        }

      if (nextPhase == TimerPhase.IDLE) {
        app.container.preferences.saveTimerState(
          PersistedTimerState(
            isRunning = false,
            phase = TimerPhase.IDLE,
            mode = state.mode,
            sessionType = state.sessionType,
            plannedMinutes = settings.pomodoroFocusMinutes,
            pausedRemainingSeconds = settings.pomodoroFocusMinutes * 60L,
            completedCycles = nextCycles,
          )
        )
        stopForegroundCompat()
        WidgetUpdater.refreshAll(applicationContext)
        stopSelf()
        return
      }

      val nextMinutes =
        when (nextPhase) {
          TimerPhase.SHORT_BREAK -> settings.pomodoroShortBreakMinutes
          TimerPhase.LONG_BREAK -> settings.pomodoroLongBreakMinutes
          else -> settings.pomodoroFocusMinutes
        }
      val nextState =
        state.copy(
          isRunning = settings.autoStartNextSession,
          phase = nextPhase,
          plannedMinutes = nextMinutes,
          targetEndEpochMs = if (settings.autoStartNextSession) now + nextMinutes * 60_000L else 0L,
          startedAtEpochMs = now,
          pausedRemainingSeconds = nextMinutes * 60L,
          completedCycles = nextCycles,
        )
      app.container.preferences.saveTimerState(nextState)
      WidgetUpdater.refreshAll(applicationContext)
      if (settings.autoStartNextSession) {
        startForegroundAndTicker(app, nextState)
      } else {
        stopForegroundCompat()
        stopSelf()
      }
    } else {
      app.container.preferences.saveTimerState(
        PersistedTimerState(
          isRunning = false,
          phase = TimerPhase.IDLE,
          mode = state.mode,
          sessionType = state.sessionType,
          plannedMinutes = state.plannedMinutes,
          pausedRemainingSeconds = state.plannedMinutes * 60L,
        )
      )
      stopForegroundCompat()
      WidgetUpdater.refreshAll(applicationContext)
      stopSelf()
    }
  }

  private suspend fun persistSessionRecord(
    app: FocusNestApplication,
    state: PersistedTimerState,
    completed: Boolean,
    interrupted: Boolean,
  ) {
    if (state.phase != TimerPhase.FOCUS || state.startedAtEpochMs <= 0L) return
    val now = System.currentTimeMillis()
    val elapsedSeconds =
      when {
        completed && state.mode != TimerMode.STOPWATCH -> state.plannedMinutes * 60L
        state.mode == TimerMode.STOPWATCH ->
          if (state.isRunning) {
            state.stopwatchElapsedBaseSeconds + ((now - state.startedAtEpochMs) / 1000L)
          } else {
            state.stopwatchElapsedBaseSeconds
          }
        else -> {
          val remaining =
            if (state.isRunning) ((state.targetEndEpochMs - now) / 1000L).coerceAtLeast(0L)
            else state.pausedRemainingSeconds
          (state.plannedMinutes * 60L - remaining).coerceAtLeast(0L)
        }
      }
    if (elapsedSeconds < 5L && !completed) return

    app.container.focusRepository.recordFocusSession(
      FocusSessionEntity(
        dateKey = FocusTimeUtils.todayKey(state.startedAtEpochMs),
        sessionType = state.sessionType.name,
        timerMode = state.mode.name,
        plannedMinutes = state.plannedMinutes,
        elapsedSeconds = elapsedSeconds,
        completed = completed,
        interrupted = interrupted,
        note = state.sessionNote,
        profileId = state.linkedProfileId,
        startedAtEpochMs = state.startedAtEpochMs,
        endedAtEpochMs = now,
      )
    )
  }

  private fun alertCompletion(sound: Boolean, vibration: Boolean) {
    if (sound) {
      runCatching {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        RingtoneManager.getRingtone(applicationContext, uri)?.play()
      }
    }
    if (vibration) {
      runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
          vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(350L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
          @Suppress("DEPRECATION")
          val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
          vibrator.vibrate(VibrationEffect.createOneShot(350L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
      }
    }
  }

  private fun buildNotification(state: PersistedTimerState): Notification {
    val openIntent =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )
    val now = System.currentTimeMillis()
    val elapsedNow = SystemClock.elapsedRealtime()
    val displayText =
      if (state.mode == TimerMode.STOPWATCH) {
        val elapsed = state.stopwatchElapsedBaseSeconds + ((now - state.startedAtEpochMs) / 1000L).coerceAtLeast(0L)
        "${state.sessionType.label} • ${FocusTimeUtils.formatTimerSeconds(elapsed)}"
      } else {
        val rem = remainingSecondsForState(state, now, elapsedNow)
        "${state.phase.label} (${state.sessionType.label}) • ${FocusTimeUtils.formatTimerSeconds(rem)}"
      }

    val pauseIntent =
      PendingIntent.getService(
        this,
        1,
        Intent(this, FocusTimerService::class.java).apply { action = ACTION_PAUSE },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )

    return NotificationCompat.Builder(this, FocusNestApplication.CHANNEL_TIMER)
      .setSmallIcon(R.drawable.ic_launcher_monochrome)
      .setContentTitle("FocusNest Active")
      .setContentText(displayText)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .addAction(0, "Pause", pauseIntent)
      .setContentIntent(openIntent)
      .build()
  }

  private fun stopForegroundCompat() {
    stopForeground(STOP_FOREGROUND_REMOVE)
  }

  override fun onDestroy() {
    ambientEngine.stop()
    super.onDestroy()
    serviceScope.cancel()
  }

  companion object {
    const val NOTIFICATION_ID = 4001
    const val ACTION_START = "com.example.focusnest.TIMER_START"
    const val ACTION_PAUSE = "com.example.focusnest.TIMER_PAUSE"
    const val ACTION_RESUME = "com.example.focusnest.TIMER_RESUME"
    const val ACTION_STOP = "com.example.focusnest.TIMER_STOP"
    const val ACTION_RESTORE = "com.example.focusnest.TIMER_RESTORE"
    const val ACTION_SKIP_BREAK = "com.example.focusnest.TIMER_SKIP_BREAK"
    const val ACTION_EXTEND = "com.example.focusnest.TIMER_EXTEND"
    const val ACTION_RESTART = "com.example.focusnest.TIMER_RESTART"

    const val EXTRA_MODE = "extra_mode"
    const val EXTRA_SESSION_TYPE = "extra_session_type"
    const val EXTRA_MINUTES = "extra_minutes"
    const val EXTRA_DELTA_MINUTES = "extra_delta_minutes"
    const val EXTRA_NOTE = "extra_note"
    const val EXTRA_PROFILE_ID = "extra_profile_id"
    const val EXTRA_COMPLETED = "extra_completed"

    fun remainingSecondsForState(
      state: PersistedTimerState,
      nowEpochMs: Long,
      elapsedRealtimeMs: Long,
    ): Long {
      if (!state.isRunning) return state.pausedRemainingSeconds.coerceAtLeast(0L)
      return if (state.targetElapsedRealtimeMs > 0L) {
        ((state.targetElapsedRealtimeMs - elapsedRealtimeMs) / 1000L).coerceAtLeast(0L)
      } else {
        ((state.targetEndEpochMs - nowEpochMs) / 1000L).coerceAtLeast(0L)
      }
    }

    fun startTimer(
      context: Context,
      mode: TimerMode,
      sessionType: SessionType,
      minutes: Int,
      note: String,
      profileId: Long?,
    ) {
      val intent =
        Intent(context, FocusTimerService::class.java).apply {
          action = ACTION_START
          putExtra(EXTRA_MODE, mode.name)
          putExtra(EXTRA_SESSION_TYPE, sessionType.name)
          putExtra(EXTRA_MINUTES, minutes)
          putExtra(EXTRA_NOTE, note)
          putExtra(EXTRA_PROFILE_ID, profileId ?: -1L)
        }
      context.startForegroundService(intent)
    }

    fun extendTimer(context: Context, deltaMinutes: Int = 5) {
      val intent =
        Intent(context, FocusTimerService::class.java).apply {
          action = ACTION_EXTEND
          putExtra(EXTRA_DELTA_MINUTES, deltaMinutes)
        }
      context.startService(intent)
    }

    fun sendAction(context: Context, action: String, completed: Boolean = false) {
      val intent =
        Intent(context, FocusTimerService::class.java).apply {
          this.action = action
          putExtra(EXTRA_COMPLETED, completed)
        }
      if (action == ACTION_RESUME || action == ACTION_SKIP_BREAK || action == ACTION_RESTART) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }
  }
}
