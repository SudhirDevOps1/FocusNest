package com.example.data.repository

import com.example.core.database.HabitCompletionEntity
import com.example.core.database.HabitDao
import com.example.core.database.HabitEntity
import com.example.domain.model.FocusTimeUtils
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class HabitRepository(
  private val habitDao: HabitDao,
) {
  val activeHabits: Flow<List<HabitEntity>> = habitDao.observeActiveHabits()
  val allCompletions: Flow<List<HabitCompletionEntity>> = habitDao.observeAllCompletions()

  suspend fun saveHabit(habit: HabitEntity): Long = habitDao.upsertHabit(habit)

  suspend fun deleteHabit(habit: HabitEntity) = habitDao.deleteHabit(habit)

  suspend fun recordHabitProgress(
    habit: HabitEntity,
    dateKey: String = FocusTimeUtils.todayKey(),
    delta: Int = 1,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): HabitCompletionEntity {
    val existing = habitDao.getCompletionForHabitDate(habit.id, dateKey)
    val nextCount = ((existing?.completedCount ?: 0) + delta).coerceAtLeast(0)
    val entry =
      HabitCompletionEntity(
        id = existing?.id ?: 0L,
        habitId = habit.id,
        dateKey = dateKey,
        completedCount = nextCount,
        completedAtEpochMs = nowEpochMs,
      )
    val savedId = habitDao.upsertCompletion(entry)
    return entry.copy(id = if (entry.id > 0L) entry.id else savedId)
  }

  suspend fun toggleHabitForDate(
    habit: HabitEntity,
    dateKey: String = FocusTimeUtils.todayKey(),
    nowEpochMs: Long = System.currentTimeMillis(),
  ): Boolean {
    val existing = habitDao.getCompletionForHabitDate(habit.id, dateKey)
    val currentlyDone = (existing?.completedCount ?: 0) >= habit.targetCount.coerceAtLeast(1)
    val nextCount = if (currentlyDone) 0 else habit.targetCount.coerceAtLeast(1)
    habitDao.upsertCompletion(
      HabitCompletionEntity(
        id = existing?.id ?: 0L,
        habitId = habit.id,
        dateKey = dateKey,
        completedCount = nextCount,
        completedAtEpochMs = nowEpochMs,
      )
    )
    return !currentlyDone
  }

  fun computeHabitStreakDays(
    habit: HabitEntity,
    completions: List<HabitCompletionEntity>,
    today: LocalDate = LocalDate.now(),
  ): Int {
    val target = habit.targetCount.coerceAtLeast(1)
    val doneDates =
      completions
        .filter { it.habitId == habit.id && it.completedCount >= target }
        .mapNotNull { runCatching { LocalDate.parse(it.dateKey) }.getOrNull() }
        .toSet()
    if (doneDates.isEmpty()) return 0

    var cursor = today
    if (cursor !in doneDates) {
      cursor = cursor.minusDays(1)
    }
    var streak = 0
    while (cursor in doneDates) {
      streak++
      cursor = cursor.minusDays(1)
    }
    return streak
  }

  fun weeklyCompletionRates(
    habits: List<HabitEntity>,
    completions: List<HabitCompletionEntity>,
    today: LocalDate = LocalDate.now(),
  ): List<Pair<String, Float>> {
    if (habits.isEmpty()) return emptyList()
    val byDateHabit = completions.groupBy { it.dateKey }
    return (6 downTo 0).map { ago ->
      val d = today.minusDays(ago.toLong())
      val key = d.toString()
      val dayRecords = byDateHabit[key].orEmpty().associateBy { it.habitId }
      val doneCount =
        habits.count { h ->
          (dayRecords[h.id]?.completedCount ?: 0) >= h.targetCount.coerceAtLeast(1)
        }
      val pct = (doneCount.toFloat() / habits.size.toFloat()) * 100f
      d.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() } to pct
    }
  }
}
