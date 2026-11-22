package com.example.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.database.AppLimitEntity
import com.example.core.database.AppUsageEntity
import com.example.core.database.BlockProfileEntity
import com.example.core.database.BlockingScheduleEntity
import com.example.core.database.DailyAnalyticsEntity
import com.example.core.database.FocusGoalEntity
import com.example.core.database.FocusSessionEntity
import com.example.core.database.HabitEntity
import com.example.core.datastore.FocusNestSettings
import com.example.core.datastore.PersistedTimerState
import com.example.core.design.AdaptiveTwoPane
import com.example.core.design.AnimatedGoalRing
import com.example.core.design.BarChartCard
import com.example.core.design.FocusNestEmptyState
import com.example.core.design.FocusNestLogoMark
import com.example.core.design.InstalledAppIcon
import com.example.core.design.MeasuredBadge
import com.example.core.design.MetricTile
import com.example.core.design.SanctuaryCard
import com.example.core.design.SectionHeader
import com.example.core.permissions.PermissionManager
import com.example.domain.model.FocusTimeUtils
import com.example.domain.model.PermissionSnapshot
import com.example.domain.model.SessionType
import com.example.domain.model.TimerMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeDashboardScreen(
  settings: FocusNestSettings,
  timerState: PersistedTimerState,
  permissions: PermissionSnapshot,
  todayUsage: List<AppUsageEntity>,
  weeklyUsage: List<AppUsageEntity>,
  todayAnalytics: DailyAnalyticsEntity?,
  focusSessions: List<FocusSessionEntity>,
  focusGoal: FocusGoalEntity,
  profiles: List<BlockProfileEntity>,
  schedules: List<BlockingScheduleEntity>,
  onRefreshUsage: () -> Unit,
  onQuickStartFocus: (SessionType, Int) -> Unit,
  onToggleQuickBlocker: (Boolean) -> Unit,
  onSaveDailyIntention: (String) -> Unit,
  onNavigateToFocus: () -> Unit,
  onNavigateToBlocker: () -> Unit,
  onNavigateToInsights: () -> Unit,
  yesterdayUsage: List<AppUsageEntity> = emptyList(),
  appLimits: List<AppLimitEntity> = emptyList(),
  habits: List<HabitEntity> = emptyList(),
  onNavigateToHabits: () -> Unit = {},
) {
  val context = LocalContext.current
  var showIntentionDialog by remember { mutableStateOf(false) }
  var intentionDraft by remember(settings.dailyIntention) { mutableStateOf(settings.dailyIntention) }

  val todayKey = FocusTimeUtils.todayKey()
  val totalScreenMinutes = remember(todayUsage) { (todayUsage.sumOf { it.foregroundTimeMs } / 60_000L).toInt() }
  val yesterdayScreenMinutes = remember(yesterdayUsage) { (yesterdayUsage.sumOf { it.foregroundTimeMs } / 60_000L).toInt() }
  val hasYesterdayData = remember(yesterdayUsage) { yesterdayUsage.isNotEmpty() }
  val limitsByPkg = remember(appLimits) { appLimits.associateBy { it.packageName } }
  val remainingMinutes = (focusGoal.dailyScreenTimeGoalMinutes - totalScreenMinutes).coerceAtLeast(0)
  val screenGoalProgress =
    if (focusGoal.dailyScreenTimeGoalMinutes > 0) {
      totalScreenMinutes.toFloat() / focusGoal.dailyScreenTimeGoalMinutes.toFloat()
    } else 0f

  val todaySessions = remember(focusSessions, todayKey) { focusSessions.filter { it.dateKey == todayKey } }
  val completedTodayCount = todaySessions.count { it.completed }
  val focusMinutesToday = (todaySessions.sumOf { it.elapsedSeconds } / 60L).toInt()
  val activeProfiles = profiles.filter { it.isActive }
  val blockedAttempts = todayAnalytics?.blockedAttemptsCount ?: 0
  val nextSchedule = schedules.firstOrNull { it.isEnabled }
  val hourNow = remember { LocalTime.now().hour }
  val greetingText =
    when (hourNow) {
      in 5..11 -> stringResource(R.string.greeting_morning)
      in 12..16 -> stringResource(R.string.greeting_afternoon)
      else -> stringResource(R.string.greeting_evening)
    }
  val dateFormatted =
    remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())) }

  val weeklyBarEntries =
    remember(weeklyUsage) {
      val byDay = weeklyUsage.groupBy { it.dateKey }
      (6 downTo 0).map { ago ->
        val d = LocalDate.now().minusDays(ago.toLong())
        val key = d.toString()
        val mins = (byDay[key]?.sumOf { it.foregroundTimeMs } ?: 0L) / 60_000f
        d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) to mins
      }
    }

  AdaptiveTwoPane(
    headerContent = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          FocusNestLogoMark(size = 38.dp)
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Text(
              "$greetingText • $dateFormatted • ${stringResource(R.string.app_tagline)}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        IconButton(
          onClick = onRefreshUsage,
          modifier = Modifier.testTag("home_refresh_btn"),
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Sync usage stats")
        }
      }
    },
    primaryPane = {

    if (!permissions.usageStatsGranted) {
      item {
        SanctuaryCard(highlight = true) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Usage Access Required for Screen Time", style = MaterialTheme.typography.titleMedium)
            Text(
              "Android requires you to grant Usage Access in system settings so FocusNest can measure real app time and launch counts. No placeholder numbers are shown.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
              onClick = {
                PermissionManager.safeStartSettings(
                  context,
                  PermissionManager.usageAccessIntent(context),
                )
              },
              modifier = Modifier.testTag("home_grant_usage_btn"),
            ) {
              Text("Grant Usage Access")
            }
          }
        }
      }
    }

    // Screen-Time Progress & Quick Controls
    item {
      SanctuaryCard {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Today's Screen Time", style = MaterialTheme.typography.titleLarge)
            MeasuredBadge(isMeasured = permissions.usageStatsGranted)
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            AnimatedGoalRing(
              progress = screenGoalProgress,
              centerTitle = FocusTimeUtils.formatDurationMinutes(totalScreenMinutes),
              centerSubtitle = "of ${FocusTimeUtils.formatDurationMinutes(focusGoal.dailyScreenTimeGoalMinutes)}",
              overLimit = totalScreenMinutes > focusGoal.dailyScreenTimeGoalMinutes,
              ringSize = 140.dp,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text =
                  if (totalScreenMinutes <= focusGoal.dailyScreenTimeGoalMinutes)
                    "${FocusTimeUtils.formatDurationMinutes(remainingMinutes)} remaining"
                  else
                    "${FocusTimeUtils.formatDurationMinutes(totalScreenMinutes - focusGoal.dailyScreenTimeGoalMinutes)} over goal",
                style = MaterialTheme.typography.titleMedium,
                color =
                  if (totalScreenMinutes <= focusGoal.dailyScreenTimeGoalMinutes)
                    MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.error,
              )
              LinearProgressIndicator(
                progress = { screenGoalProgress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
              )
              Text(
                "Daily Goal: ${FocusTimeUtils.formatDurationMinutes(focusGoal.dailyScreenTimeGoalMinutes)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              if (hasYesterdayData && yesterdayScreenMinutes > 0) {
                val diff = totalScreenMinutes - yesterdayScreenMinutes
                val diffLabel =
                  when {
                    diff < 0 -> "${FocusTimeUtils.formatDurationMinutes(-diff)} less than yesterday"
                    diff > 0 -> "+${FocusTimeUtils.formatDurationMinutes(diff)} more than yesterday"
                    else -> "Matching yesterday's usage"
                  }
                Text(
                  diffLabel,
                  style = MaterialTheme.typography.labelMedium,
                  color = if (diff <= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                )
              }
              Button(
                onClick = {
                  if (timerState.isRunning) onNavigateToFocus()
                  else onQuickStartFocus(SessionType.WORK, settings.pomodoroFocusMinutes)
                },
                modifier = Modifier.fillMaxWidth().testTag("home_quick_focus_btn"),
              ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (timerState.isRunning) "Open Timer" else "Quick Focus (${settings.pomodoroFocusMinutes}m)")
              }
            }
          }
        }
      }
    }

    // Quick Blocker Toggle & Daily Intention
    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SanctuaryCard(
          highlight = settings.quickBlockerEnabled,
          modifier = Modifier.weight(1f),
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Switch(
                checked = settings.quickBlockerEnabled,
                onCheckedChange = onToggleQuickBlocker,
                modifier = Modifier.testTag("home_quick_shield_switch"),
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("Quick Shield", style = MaterialTheme.typography.titleMedium)
            Text(
              if (settings.quickBlockerEnabled) "Blocking flagged apps" else "Tap to protect focus",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        SanctuaryCard(modifier = Modifier.weight(1f)) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Daily Intention", style = MaterialTheme.typography.titleMedium)
              IconButton(
                onClick = { showIntentionDialog = true },
                modifier = Modifier.size(32.dp).testTag("home_edit_intention_btn"),
              ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit daily intention", modifier = Modifier.size(18.dp))
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = settings.dailyIntention.ifBlank { "Set a clear focus intention for today." },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 3,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }

    // Quick Actions Strip (Focus, Shield, Habits, Insights)
    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = onNavigateToHabits,
          modifier = Modifier.weight(1f).testTag("home_quick_habits_btn"),
        ) {
          Icon(Icons.Default.SelfImprovement, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (habits.isEmpty()) "Habits" else "Habits (${habits.size})")
        }
        OutlinedButton(
          onClick = onNavigateToInsights,
          modifier = Modifier.weight(1f).testTag("home_quick_insights_btn"),
        ) {
          Icon(Icons.Default.QueryStats, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Insights")
        }
      }
    }
    },
    secondaryPane = {

    // 4 Metrics Grid
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          MetricTile(
            label = "Focus Minutes",
            value = "${focusMinutesToday}m",
            icon = Icons.Default.Timer,
            modifier = Modifier.weight(1f),
          )
          MetricTile(
            label = "Completed",
            value = "$completedTodayCount / ${focusGoal.dailySessionsGoal}",
            icon = Icons.Default.CheckCircle,
            modifier = Modifier.weight(1f),
          )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          MetricTile(
            label = "Active Profiles",
            value = "${activeProfiles.size}",
            icon = Icons.Default.Shield,
            modifier = Modifier.weight(1f),
          )
          MetricTile(
            label = "Blocked Attempts",
            value = "$blockedAttempts",
            icon = Icons.Default.Block,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    // Current Focus Session / Next Scheduled Restriction
    item {
      SanctuaryCard {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
              Icons.Default.Schedule,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text =
                  if (timerState.isRunning) "Current Focus Session: ${timerState.sessionType.label}"
                  else if (nextSchedule != null) "Next Scheduled Rule: ${nextSchedule.label}"
                  else "No Scheduled Restrictions Active",
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text =
                  if (timerState.isRunning) "${timerState.phase.label} • ${timerState.mode.label}"
                  else if (nextSchedule != null) {
                    "${FocusTimeUtils.formatMinuteOfDay(nextSchedule.startMinuteOfDay)} – ${FocusTimeUtils.formatMinuteOfDay(nextSchedule.endMinuteOfDay)}"
                  } else {
                    "Create a Study, Work, or Sleep schedule in Blocker"
                  },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          FilledTonalButton(
            onClick = if (timerState.isRunning) onNavigateToFocus else onNavigateToBlocker,
          ) {
            Text(if (timerState.isRunning) "View" else "Manage")
          }
        }
      }
    }

    // Weekly Comparison Chart
    item {
      BarChartCard(
        title = "Weekly Screen-Time Comparison",
        subtitle = "Last 7 days measured on-device (minutes)",
        entries = weeklyBarEntries,
        valueFormatter = { "${it.toInt()}m" },
      )
    }

    // Most-Used Apps
    item {
      SectionHeader(
        title = "Most-Used Apps Today",
        subtitle = "Time & measurable app openings",
        actionLabel = "Full Insights",
        onActionClick = onNavigateToInsights,
      )
    }

    if (todayUsage.isEmpty()) {
      item {
        FocusNestEmptyState(
          icon = Icons.Default.QueryStats,
          title = "No Measured App Usage Yet",
          message =
            if (permissions.usageStatsGranted) {
              "Android hasn't recorded app foreground events yet today, or tap Sync to check again."
            } else {
              "Grant Usage Access to view your top apps and opening counts, or start a Focus session anytime."
            },
          actionLabel = if (permissions.usageStatsGranted) "Sync Usage Now" else "Start Focus Session",
          actionTestTag = "home_empty_action_btn",
          onAction = if (permissions.usageStatsGranted) onRefreshUsage else onNavigateToFocus,
        )
      }
    } else {
      items(todayUsage.take(6), key = { it.packageName }) { app ->
        val limit = limitsByPkg[app.packageName]
        val totalMs = todayUsage.sumOf { it.foregroundTimeMs }.coerceAtLeast(1L)
        val pct = ((app.foregroundTimeMs.toFloat() / totalMs.toFloat()) * 100f).toInt()
        SanctuaryCard {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            InstalledAppIcon(packageName = app.packageName, appName = app.appName, size = 38.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(app.appName, style = MaterialTheme.typography.titleMedium)
              Text(
                buildString {
                  append("${app.category} • ${app.launchCount} openings • $pct%")
                  if (limit != null && limit.dailyLimitMinutes > 0) {
                    append(" • Cap ${limit.dailyLimitMinutes}m")
                  }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Text(
              FocusTimeUtils.formatDurationMillis(app.foregroundTimeMs),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    }
    },
  )

  if (showIntentionDialog) {
    AlertDialog(
      onDismissRequest = { showIntentionDialog = false },
      title = { Text("Today's Focus Intention") },
      text = {
        OutlinedTextField(
          value = intentionDraft,
          onValueChange = { intentionDraft = it },
          label = { Text("What matters most today?") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onSaveDailyIntention(intentionDraft)
            showIntentionDialog = false
          }
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showIntentionDialog = false }) { Text("Cancel") }
      },
    )
  }
}
