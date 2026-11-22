package com.example.feature.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.database.HabitCompletionEntity
import com.example.core.database.HabitEntity
import com.example.core.design.AdaptiveTwoPane
import com.example.core.design.BarChartCard
import com.example.core.design.FocusNestEmptyState
import com.example.core.design.MetricTile
import com.example.core.design.SanctuaryCard
import com.example.core.design.SectionHeader
import com.example.domain.model.FocusTimeUtils
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitsScreen(
  habits: List<HabitEntity>,
  completions: List<HabitCompletionEntity>,
  onSaveHabit: (title: String, description: String, frequencyType: String, targetCount: Int, unitLabel: String) -> Unit,
  onSeedStarterHabits: () -> Unit,
  onDeleteHabit: (HabitEntity) -> Unit,
  onToggleHabitToday: (HabitEntity) -> Unit,
  onIncrementHabitToday: (HabitEntity, Int) -> Unit,
  computeHabitStreak: (HabitEntity, List<HabitCompletionEntity>) -> Int,
  computeWeeklyRates: (List<HabitEntity>, List<HabitCompletionEntity>) -> List<Pair<String, Float>>,
  onBack: () -> Unit,
) {
  var showCreateDialog by remember { mutableStateOf(false) }
  val todayKey = FocusTimeUtils.todayKey()
  val todayMap =
    remember(completions, todayKey) {
      completions.filter { it.dateKey == todayKey }.associateBy { it.habitId }
    }
  val weeklyRates =
    remember(habits, completions) { computeWeeklyRates(habits, completions) }
  val completedToday =
    remember(habits, todayMap) {
      habits.count { h -> (todayMap[h.id]?.completedCount ?: 0) >= h.targetCount.coerceAtLeast(1) }
    }
  val bestStreak =
    remember(habits, completions) {
      habits.maxOfOrNull { computeHabitStreak(it, completions) } ?: 0
    }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showCreateDialog = true },
        modifier = Modifier.testTag("habits_add_btn"),
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Habit")
      }
    }
  ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("habits_back_btn")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(stringResource(R.string.habits_title), style = MaterialTheme.typography.headlineMedium)
          Text(
            stringResource(R.string.habits_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      AdaptiveTwoPane(
        listTestTag = "habits_list",
        primaryPane = {
          item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              MetricTile(
                label = "Done Today",
                value = "$completedToday / ${habits.size}",
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f),
              )
              MetricTile(
                label = "Best Streak",
                value = "${bestStreak}d",
                icon = Icons.Default.LocalFireDepartment,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f),
              )
            }
          }

          if (habits.isNotEmpty()) {
            item {
              BarChartCard(
                title = "7-Day Habit Completion Rate",
                subtitle = "Percentage of active habits completed each day",
                entries = weeklyRates,
                valueFormatter = { "${it.toInt()}%" },
              )
            }
          }
        },
        secondaryPane = {
          item {
            SectionHeader(
              title = "Active Offline Routines",
              subtitle = "100% local Room tracking • Never synced externally",
              actionLabel = if (habits.isEmpty()) "Starter Pack" else null,
              onActionClick = if (habits.isEmpty()) onSeedStarterHabits else null,
            )
          }

        if (habits.isEmpty()) {
          item {
            FocusNestEmptyState(
              icon = Icons.Default.SelfImprovement,
              title = "Build Your Offline Routine",
              message = "Create daily or weekly habits like screen-free mornings, offline reading, or docking your phone before bed.",
              actionLabel = "Add Starter Habits",
              actionTestTag = "habits_seed_btn",
              onAction = onSeedStarterHabits,
            )
          }
          item {
            OutlinedButton(
              onClick = { showCreateDialog = true },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text("Create Custom Habit")
            }
          }
        } else {
          items(habits, key = { it.id }) { habit ->
            val currentCount = todayMap[habit.id]?.completedCount ?: 0
            val target = habit.targetCount.coerceAtLeast(1)
            val isDone = currentCount >= target
            val streak = computeHabitStreak(habit, completions)

            SanctuaryCard(
              highlight = isDone,
              modifier = Modifier.testTag("habit_card_${habit.id}"),
            ) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Checkbox(
                    checked = isDone,
                    onCheckedChange = { onToggleHabitToday(habit) },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(habit.title, style = MaterialTheme.typography.titleMedium)
                    if (habit.description.isNotBlank()) {
                      Text(
                        habit.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape,
                      ) {
                        Text(
                          "${habit.frequencyType} • $currentCount/$target ${habit.unitLabel}",
                          style = MaterialTheme.typography.labelSmall,
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                      }
                      if (streak > 0) {
                        Surface(
                          color = MaterialTheme.colorScheme.tertiaryContainer,
                          shape = CircleShape,
                        ) {
                          Text(
                            "${streak}d streak",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                          )
                        }
                      }
                    }
                  }
                  IconButton(onClick = { onDeleteHabit(habit) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete habit")
                  }
                }

                if (target > 1) {
                  LinearProgressIndicator(
                    progress = { (currentCount.toFloat() / target.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                  )
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    OutlinedButton(
                      enabled = currentCount > 0,
                      onClick = { onIncrementHabitToday(habit, -1) },
                      modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                      Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                      onClick = { onIncrementHabitToday(habit, 1) },
                      modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                      Icon(Icons.Default.Add, contentDescription = "Increase")
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("+1 ${habit.unitLabel}")
                    }
                  }
                }

                // 7-day mini calendar strip
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  val habitComp = completions.filter { it.habitId == habit.id }.associateBy { it.dateKey }
                  (6 downTo 0).forEach { ago ->
                    val d = LocalDate.now().minusDays(ago.toLong())
                    val doneOnDay = (habitComp[d.toString()]?.completedCount ?: 0) >= target
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                      Text(
                        d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                      Spacer(modifier = Modifier.height(3.dp))
                      Box(
                        modifier =
                          Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                              if (doneOnDay) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                        contentAlignment = Alignment.Center,
                      ) {
                        if (doneOnDay) {
                          Icon(
                            Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp),
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
        },
      )
    }
  }

  if (showCreateDialog) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("DAILY") }
    var targetCount by remember { mutableIntStateOf(1) }
    var unitLabel by remember { mutableStateOf("times") }

    AlertDialog(
      onDismissRequest = { showCreateDialog = false },
      title = { Text("Create Mindful Habit") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Habit Title (e.g. Evening Walk)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("habit_title_input"),
          )
          OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Short Note (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("DAILY", "WEEKLY").forEach { freq ->
              FilterChip(
                selected = frequency == freq,
                onClick = { frequency = freq },
                label = { Text(freq) },
              )
            }
          }
          Text("Target per period: $targetCount $unitLabel")
          Slider(
            value = targetCount.toFloat(),
            onValueChange = { targetCount = it.toInt() },
            valueRange = 1f..30f,
          )
          OutlinedTextField(
            value = unitLabel,
            onValueChange = { unitLabel = it },
            label = { Text("Unit (times, pages, mins)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          enabled = title.isNotBlank(),
          onClick = {
            onSaveHabit(title, description, frequency, targetCount, unitLabel)
            showCreateDialog = false
          },
          modifier = Modifier.testTag("habit_save_btn"),
        ) { Text("Save") }
      },
      dismissButton = {
        TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
      },
    )
  }
}
