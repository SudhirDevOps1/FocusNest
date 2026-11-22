package com.example.feature.focus

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.database.BlockProfileEntity
import com.example.core.database.FocusGoalEntity
import com.example.core.database.FocusSessionEntity
import com.example.core.datastore.FocusNestSettings
import com.example.core.datastore.PersistedTimerState
import com.example.core.design.AdaptiveTwoPane
import com.example.core.design.AnimatedGoalRing
import com.example.core.design.BarChartCard
import com.example.core.design.FocusNestEmptyState
import com.example.core.design.MetricTile
import com.example.core.design.SanctuaryCard
import com.example.core.design.SectionHeader
import com.example.domain.model.AmbientSoundType
import com.example.domain.model.FocusTimeUtils
import com.example.domain.model.SessionType
import com.example.domain.model.TimerMode
import com.example.domain.model.TimerPhase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
  settings: FocusNestSettings,
  timerState: PersistedTimerState,
  sessions: List<FocusSessionEntity>,
  goal: FocusGoalEntity,
  profiles: List<BlockProfileEntity>,
  streakDays: Int,
  onStartTimer: (TimerMode, SessionType, Int, String, Long?) -> Unit,
  onPauseTimer: () -> Unit,
  onResumeTimer: () -> Unit,
  onStopTimer: (Boolean) -> Unit,
  onSavePomodoroConfig: (Int, Int, Int, Int, Int, Boolean, Boolean, Boolean) -> Unit,
  onExtendTimer: (Int) -> Unit = {},
  onRestartTimer: () -> Unit = {},
  onSkipBreak: () -> Unit = {},
  onUpdateAmbientSound: (AmbientSoundType, Float) -> Unit = { _, _ -> },
) {
  var selectedMode by rememberSaveable { mutableStateOf(timerState.mode) }
  var selectedType by rememberSaveable { mutableStateOf(timerState.sessionType) }
  var customMinutes by rememberSaveable { mutableIntStateOf(settings.pomodoroFocusMinutes) }
  var sessionNote by rememberSaveable { mutableStateOf(timerState.sessionNote) }
  var selectedProfileId by rememberSaveable { mutableStateOf(timerState.linkedProfileId) }
  var showPomodoroDialog by remember { mutableStateOf(false) }

  // Tick every 500ms purely for UI rendering; source of truth is epoch timestamps in DataStore.
  var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(timerState.isRunning) {
    while (timerState.isRunning) {
      nowMs = System.currentTimeMillis()
      delay(500L)
    }
    nowMs = System.currentTimeMillis()
  }

  val activeMode = if (timerState.phase != TimerPhase.IDLE) timerState.mode else selectedMode
  val displaySeconds =
    when {
      timerState.phase == TimerPhase.IDLE -> customMinutes * 60L
      activeMode == TimerMode.STOPWATCH ->
        if (timerState.isRunning) {
          timerState.stopwatchElapsedBaseSeconds + ((nowMs - timerState.startedAtEpochMs) / 1000L).coerceAtLeast(0L)
        } else {
          timerState.stopwatchElapsedBaseSeconds
        }
      timerState.isRunning -> ((timerState.targetEndEpochMs - nowMs) / 1000L).coerceAtLeast(0L)
      else -> timerState.pausedRemainingSeconds
    }

  val totalTargetSeconds = (timerState.plannedMinutes.coerceAtLeast(1) * 60f)
  val progress =
    when {
      timerState.phase == TimerPhase.IDLE -> 0f
      activeMode == TimerMode.STOPWATCH -> ((displaySeconds % 3600) / 3600f)
      else -> 1f - (displaySeconds / totalTargetSeconds).coerceIn(0f, 1f)
    }

  val totalFocusMinutes = remember(sessions) { (sessions.sumOf { it.elapsedSeconds } / 60L).toInt() }
  val completedCount = remember(sessions) { sessions.count { it.completed } }
  val weeklyFocusBars =
    remember(sessions) {
      val byDate = sessions.groupBy { it.dateKey }
      (6 downTo 0).map { ago ->
        val d = LocalDate.now().minusDays(ago.toLong())
        val mins = (byDate[d.toString()]?.sumOf { it.elapsedSeconds } ?: 0L) / 60f
        d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) to mins
      }
    }

  val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }

  AdaptiveTwoPane(
    headerContent = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Focus Sanctuary", style = MaterialTheme.typography.headlineMedium)
          Text(
            "Reboot-persistent Pomodoro, Countdown & Stopwatch",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(
          onClick = { showPomodoroDialog = true },
          modifier = Modifier.testTag("focus_pomodoro_settings_btn"),
        ) {
          Icon(Icons.Default.Tune, contentDescription = "Configure Pomodoro")
        }
      }
    },
    primaryPane = {
    item {
      PrimaryTabRow(selectedTabIndex = activeMode.ordinal) {
        TimerMode.entries.forEach { mode ->
          Tab(
            selected = activeMode == mode,
            enabled = timerState.phase == TimerPhase.IDLE,
            onClick = { selectedMode = mode },
            text = { Text(mode.label) },
          )
        }
      }
    }

    item {
      SanctuaryCard(highlight = timerState.isRunning) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          AnimatedGoalRing(
            progress = progress,
            centerTitle = FocusTimeUtils.formatTimerSeconds(displaySeconds),
            centerSubtitle =
              if (timerState.phase == TimerPhase.IDLE) "${selectedType.label} • ${selectedMode.label}"
              else "${timerState.phase.label} • Cycle ${timerState.completedCycles + 1}",
            ringSize = 200.dp,
          )

          Spacer(modifier = Modifier.height(16.dp))

          if (timerState.phase == TimerPhase.IDLE) {
            Text("Session Type", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              SessionType.entries.forEach { type ->
                FilterChip(
                  selected = selectedType == type,
                  onClick = {
                    selectedType = type
                    if (selectedMode == TimerMode.COUNTDOWN) {
                      customMinutes = type.defaultMinutes
                    }
                  },
                  label = { Text(type.label) },
                )
              }
            }

            if (selectedMode != TimerMode.STOPWATCH) {
              Spacer(modifier = Modifier.height(12.dp))
              Text("Duration: $customMinutes min", style = MaterialTheme.typography.titleMedium)
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                listOf(15, 25, 45, 60, 90).forEach { preset ->
                  FilterChip(
                    selected = customMinutes == preset,
                    onClick = { customMinutes = preset },
                    label = { Text("${preset}m") },
                  )
                }
              }
              Slider(
                value = customMinutes.toFloat(),
                onValueChange = { customMinutes = (it.toInt() / 5) * 5 },
                valueRange = 5f..120f,
              )
            }

            if (profiles.isNotEmpty()) {
              Spacer(modifier = Modifier.height(8.dp))
              Text("Shield Profile During Focus", style = MaterialTheme.typography.labelLarge)
              Spacer(modifier = Modifier.height(6.dp))
              FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                  selected = selectedProfileId == null,
                  onClick = { selectedProfileId = null },
                  label = { Text("Flagged Apps") },
                )
                profiles.take(4).forEach { profile ->
                  FilterChip(
                    selected = selectedProfileId == profile.id,
                    onClick = { selectedProfileId = profile.id },
                    label = { Text(profile.name) },
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
              value = sessionNote,
              onValueChange = { sessionNote = it },
              label = { Text("Session note or intention (optional)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("focus_note_input"),
            )

            Spacer(modifier = Modifier.height(14.dp))
            Button(
              onClick = {
                onStartTimer(selectedMode, selectedType, customMinutes, sessionNote, selectedProfileId)
              },
              modifier = Modifier.fillMaxWidth().height(52.dp).testTag("focus_start_btn"),
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Start ${selectedType.label} Session")
            }
          } else {
            if (timerState.phase == TimerPhase.SHORT_BREAK || timerState.phase == TimerPhase.LONG_BREAK) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 10.dp),
              ) {
                Image(
                  painter = painterResource(R.drawable.ic_focus_meditate),
                  contentDescription = "Mindful break rest",
                  modifier = Modifier.height(48.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text("Mindful Break Window", style = MaterialTheme.typography.titleMedium)
                  Text(
                    "Stretch, hydrate, and rest your eyes before the next cycle.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                OutlinedButton(
                  onClick = onSkipBreak,
                  modifier = Modifier.testTag("focus_skip_break_btn"),
                ) {
                  Icon(Icons.Default.SkipNext, contentDescription = null)
                  Text("Skip")
                }
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Button(
                onClick = { if (timerState.isRunning) onPauseTimer() else onResumeTimer() },
                modifier = Modifier.weight(1f).height(50.dp).testTag("focus_pause_resume_btn"),
              ) {
                Icon(
                  if (timerState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                  contentDescription = null,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (timerState.isRunning) "Pause" else "Resume")
              }
              OutlinedButton(
                onClick = { onStopTimer(true) },
                modifier = Modifier.weight(1f).height(50.dp).testTag("focus_finish_btn"),
              ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Finish")
              }
              OutlinedButton(
                onClick = { onStopTimer(false) },
                modifier = Modifier.height(50.dp).testTag("focus_cancel_btn"),
              ) {
                Icon(Icons.Default.Stop, contentDescription = "Stop session")
              }
            }
            if (activeMode != TimerMode.STOPWATCH) {
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                OutlinedButton(
                  onClick = { onExtendTimer(5) },
                  modifier = Modifier.weight(1f).testTag("focus_extend_btn"),
                ) {
                  Icon(Icons.Default.Add, contentDescription = null)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("+5m Extend")
                }
                OutlinedButton(
                  onClick = onRestartTimer,
                  modifier = Modifier.weight(1f).testTag("focus_restart_btn"),
                ) {
                  Icon(Icons.Default.Replay, contentDescription = null)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Restart")
                }
              }
            }
          }
        }
      }
    }

    // Offline Synthesized Ambient Soundscape Console
    item {
      SanctuaryCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
              Text("Offline Ambient Soundscape", style = MaterialTheme.typography.titleMedium)
              Text(
                settings.ambientSoundType.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            AmbientSoundType.entries.forEach { sound ->
              FilterChip(
                selected = settings.ambientSoundType == sound,
                onClick = { onUpdateAmbientSound(sound, settings.ambientSoundVolume) },
                label = { Text(sound.label) },
                modifier = Modifier.testTag("focus_ambient_chip_${sound.name}"),
              )
            }
          }
          if (settings.ambientSoundType != AmbientSoundType.OFF) {
            Text(
              "Volume: ${(settings.ambientSoundVolume * 100).toInt()}% • Synthesized locally during Focus",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            Slider(
              value = settings.ambientSoundVolume,
              onValueChange = { onUpdateAmbientSound(settings.ambientSoundType, it) },
              valueRange = 0.05f..1f,
              modifier = Modifier.testTag("focus_ambient_volume_slider"),
            )
          }
        }
      }
    }
    },
    secondaryPane = {

    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricTile(
          label = "Focus Streak",
          value = "$streakDays d",
          icon = Icons.Default.LocalFireDepartment,
          tint = MaterialTheme.colorScheme.tertiary,
          modifier = Modifier.weight(1f),
        )
        MetricTile(
          label = "Total Minutes",
          value = "${totalFocusMinutes}m",
          icon = Icons.Default.Timer,
          modifier = Modifier.weight(1f),
        )
        MetricTile(
          label = "Completed",
          value = "$completedCount",
          icon = Icons.Default.Check,
          modifier = Modifier.weight(1f),
        )
      }
    }

    item {
      BarChartCard(
        title = "Weekly Focus Minutes",
        subtitle = "Weekly target: ${FocusTimeUtils.formatDurationMinutes(goal.weeklyFocusGoalMinutes)}",
        entries = weeklyFocusBars,
        valueFormatter = { "${it.toInt()}m" },
      )
    }

    item {
      SectionHeader(
        title = "Session Timeline & History",
        subtitle = "Completed and interrupted sessions stored locally",
      )
    }

    if (sessions.isEmpty()) {
      item {
        FocusNestEmptyState(
          icon = Icons.Default.Timer,
          title = "No Focus Sessions Recorded Yet",
          message = "Start your first Pomodoro, Countdown, or Stopwatch session above to build your streak.",
        )
      }
    } else {
      items(sessions.take(20), key = { it.id }) { s ->
        val startStr =
          Instant.ofEpochMilli(s.startedAtEpochMs).atZone(ZoneId.systemDefault()).format(timeFmt)
        SanctuaryCard {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("${s.sessionType} • ${s.timerMode}", style = MaterialTheme.typography.titleMedium)
              Text(
                "${s.dateKey} at $startStr • ${if (s.completed) "Completed" else "Interrupted"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              if (s.note.isNotBlank()) {
                Text(s.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
              }
            }
            Text(
              text = "${(s.elapsedSeconds / 60L)}m ${(s.elapsedSeconds % 60L)}s",
              style = MaterialTheme.typography.labelLarge,
              color = if (s.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
    },
  )

  if (showPomodoroDialog) {
    var focusMin by remember { mutableIntStateOf(settings.pomodoroFocusMinutes) }
    var shortMin by remember { mutableIntStateOf(settings.pomodoroShortBreakMinutes) }
    var longMin by remember { mutableIntStateOf(settings.pomodoroLongBreakMinutes) }
    var interval by remember { mutableIntStateOf(settings.pomodoroLongBreakInterval) }
    var cycles by remember { mutableIntStateOf(settings.pomodoroTargetCycles) }
    var autoNext by remember { mutableStateOf(settings.autoStartNextSession) }
    var sound by remember { mutableStateOf(settings.soundEnabled) }
    var vib by remember { mutableStateOf(settings.vibrationEnabled) }

    AlertDialog(
      onDismissRequest = { showPomodoroDialog = false },
      title = { Text("Pomodoro Configuration") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Focus: $focusMin min")
          Slider(value = focusMin.toFloat(), onValueChange = { focusMin = (it.toInt() / 5) * 5 }, valueRange = 10f..90f)
          Text("Short Break: $shortMin min")
          Slider(value = shortMin.toFloat(), onValueChange = { shortMin = it.toInt() }, valueRange = 2f..20f)
          Text("Long Break: $longMin min (every $interval cycles)")
          Slider(value = longMin.toFloat(), onValueChange = { longMin = (it.toInt() / 5) * 5 }, valueRange = 10f..45f)
          Text("Target Cycles: $cycles")
          Slider(value = cycles.toFloat(), onValueChange = { cycles = it.toInt() }, valueRange = 1f..8f)
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Auto-start next phase")
            Switch(checked = autoNext, onCheckedChange = { autoNext = it })
          }
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Completion Sound")
            Switch(checked = sound, onCheckedChange = { sound = it })
          }
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Vibration")
            Switch(checked = vib, onCheckedChange = { vib = it })
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onSavePomodoroConfig(focusMin, shortMin, longMin, interval, cycles, autoNext, sound, vib)
            customMinutes = focusMin
            showPomodoroDialog = false
          }
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showPomodoroDialog = false }) { Text("Cancel") }
      },
    )
  }
}
