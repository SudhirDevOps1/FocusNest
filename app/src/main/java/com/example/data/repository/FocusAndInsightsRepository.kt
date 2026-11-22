package com.example.data.repository

import com.example.core.common.KeystoreAndBackupCrypto
import com.example.core.database.AppGroupDao
import com.example.core.database.AppLimitDao
import com.example.core.database.AppLimitEntity
import com.example.core.database.AppUsageDao
import com.example.core.database.BlockProfileDao
import com.example.core.database.BlockProfileEntity
import com.example.core.database.BlockingScheduleDao
import com.example.core.database.CategoryLimitDao
import com.example.core.database.CategoryLimitEntity
import com.example.core.database.DailyAnalyticsDao
import com.example.core.database.DailyAnalyticsEntity
import com.example.core.database.FocusGoalDao
import com.example.core.database.FocusGoalEntity
import com.example.core.database.FocusSessionDao
import com.example.core.database.FocusSessionEntity
import com.example.core.database.HabitCompletionEntity
import com.example.core.database.HabitDao
import com.example.core.database.HabitEntity
import com.example.core.database.NotificationRecordDao
import com.example.core.database.NotificationRecordEntity
import com.example.core.database.ReminderDao
import com.example.core.database.ReminderEntity
import com.example.core.database.UnlockDao
import com.example.core.database.WebsiteRuleDao
import com.example.core.database.WebsiteRuleEntity
import com.example.core.datastore.FocusNestPreferences
import com.example.domain.model.FocusTimeUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

class FocusRepository(
  private val focusSessionDao: FocusSessionDao,
  private val focusGoalDao: FocusGoalDao,
  private val dailyAnalyticsDao: DailyAnalyticsDao,
) {
  val allSessions: Flow<List<FocusSessionEntity>> = focusSessionDao.observeAll()
  val goal: Flow<FocusGoalEntity?> = focusGoalDao.observeGoal()

  fun observeTodaySessions(): Flow<List<FocusSessionEntity>> =
    focusSessionDao.observeByDate(FocusTimeUtils.todayKey())

  suspend fun ensureDefaultGoal() {
    if (focusGoalDao.getGoal() == null) {
      focusGoalDao.upsert(FocusGoalEntity())
    }
  }

  suspend fun updateGoals(
    dailyScreenTimeMinutes: Int,
    dailyFocusMinutes: Int,
    dailySessionsGoal: Int,
    weeklyFocusMinutes: Int,
  ) {
    focusGoalDao.upsert(
      FocusGoalEntity(
        id = 1,
        dailyScreenTimeGoalMinutes = dailyScreenTimeMinutes.coerceAtLeast(15),
        dailyFocusGoalMinutes = dailyFocusMinutes.coerceAtLeast(10),
        dailySessionsGoal = dailySessionsGoal.coerceAtLeast(1),
        weeklyFocusGoalMinutes = weeklyFocusMinutes.coerceAtLeast(30),
        updatedAtEpochMs = System.currentTimeMillis(),
      )
    )
  }

  suspend fun recordFocusSession(session: FocusSessionEntity): Long {
    val id = focusSessionDao.insert(session)
    val today = session.dateKey
    val current = dailyAnalyticsDao.getByDate(today) ?: DailyAnalyticsEntity(today)
    val addedMin = (session.elapsedSeconds / 60L).toInt()
    dailyAnalyticsDao.upsert(
      current.copy(
        totalFocusMinutes = current.totalFocusMinutes + addedMin,
        completedFocusSessions =
          current.completedFocusSessions + if (session.completed) 1 else 0,
        updatedAtEpochMs = System.currentTimeMillis(),
      )
    )
    return id
  }

  fun computeCurrentStreakDays(sessions: List<FocusSessionEntity>): Int {
    val completedDays =
      sessions
        .filter { it.completed }
        .mapNotNull { runCatching { LocalDate.parse(it.dateKey) }.getOrNull() }
        .toSet()
    if (completedDays.isEmpty()) return 0
    var cursor = LocalDate.now()
    if (cursor !in completedDays) {
      cursor = cursor.minusDays(1)
    }
    var streak = 0
    while (cursor in completedDays) {
      streak++
      cursor = cursor.minusDays(1)
    }
    return streak
  }
}

class InsightsAndBackupRepository(
  private val appUsageDao: AppUsageDao,
  private val dailyAnalyticsDao: DailyAnalyticsDao,
  private val focusSessionDao: FocusSessionDao,
  private val focusGoalDao: FocusGoalDao,
  private val appLimitDao: AppLimitDao,
  private val appGroupDao: AppGroupDao,
  private val blockProfileDao: BlockProfileDao,
  private val blockingScheduleDao: BlockingScheduleDao,
  private val websiteRuleDao: WebsiteRuleDao,
  private val unlockDao: UnlockDao,
  private val reminderDao: ReminderDao,
  private val notificationRecordDao: NotificationRecordDao,
  private val preferences: FocusNestPreferences,
  private val habitDao: HabitDao? = null,
  private val categoryLimitDao: CategoryLimitDao? = null,
) {
  val recentDailyAnalytics: Flow<List<DailyAnalyticsEntity>> = dailyAnalyticsDao.observeRecent()
  val reminders: Flow<List<ReminderEntity>> = reminderDao.observeAll()
  val recentNotifications: Flow<List<NotificationRecordEntity>> =
    notificationRecordDao.observeRecent()

  suspend fun saveReminder(reminder: ReminderEntity): Long = reminderDao.upsert(reminder)

  suspend fun deleteReminder(reminder: ReminderEntity) = reminderDao.delete(reminder)

  suspend fun computeBestFocusPeriod(sessions: List<FocusSessionEntity>): String {
    val completed = sessions.filter { it.completed }
    if (completed.isEmpty()) return "Not enough focus sessions yet"
    val buckets = mutableMapOf("Morning (5am–12pm)" to 0L, "Afternoon (12pm–5pm)" to 0L, "Evening (5pm–9pm)" to 0L, "Night (9pm–5am)" to 0L)
    completed.forEach { session ->
      val hour = Instant.ofEpochMilli(session.startedAtEpochMs).atZone(ZoneId.systemDefault()).hour
      val key =
        when (hour) {
          in 5..11 -> "Morning (5am–12pm)"
          in 12..16 -> "Afternoon (12pm–5pm)"
          in 17..20 -> "Evening (5pm–9pm)"
          else -> "Night (9pm–5am)"
        }
      buckets[key] = (buckets[key] ?: 0L) + session.elapsedSeconds
    }
    return buckets.maxByOrNull { it.value }?.key ?: "Morning (5am–12pm)"
  }

  suspend fun exportUsageAndFocusCsv(startDateKey: String, endDateKey: String): String {
    val usage = appUsageDao.getUsageInRange(startDateKey, endDateKey)
    val sessions = focusSessionDao.getAll().filter { it.dateKey in startDateKey..endDateKey }
    val sb = StringBuilder()
    sb.appendLine("section,date,name_or_type,category_or_mode,minutes,launches_or_completed,measured")
    usage.forEach { u ->
      val mins = u.foregroundTimeMs / 60_000L
      val safeName = u.appName.replace("\"", "\"\"")
      sb.appendLine("app_usage,${u.dateKey},\"$safeName\",${u.category},$mins,${u.launchCount},${u.isMeasured}")
    }
    sessions.forEach { s ->
      val mins = s.elapsedSeconds / 60L
      sb.appendLine("focus_session,${s.dateKey},\"${s.sessionType}\",${s.timerMode},$mins,${s.completed},true")
    }
    return sb.toString()
  }

  suspend fun exportFullJson(): String {
    val goal = focusGoalDao.getGoal() ?: FocusGoalEntity()
    val limits = appLimitDao.getAll()
    val profiles = blockProfileDao.getAll()
    val websites = websiteRuleDao.getAll()
    val sessions = focusSessionDao.getAll()
    val analytics = dailyAnalyticsDao.getAll()
    val settings = preferences.settingsFlow.first()

    val root = JSONObject()
    root.put("schema", "focusnest-backup-v1")
    root.put("exportedAtEpochMs", System.currentTimeMillis())
    root.put(
      "goals",
      JSONObject()
        .put("dailyScreenTimeGoalMinutes", goal.dailyScreenTimeGoalMinutes)
        .put("dailyFocusGoalMinutes", goal.dailyFocusGoalMinutes)
        .put("dailySessionsGoal", goal.dailySessionsGoal)
        .put("weeklyFocusGoalMinutes", goal.weeklyFocusGoalMinutes),
    )
    root.put(
      "settings",
      JSONObject()
        .put("themeMode", settings.themeMode.name)
        .put("accentPalette", settings.accentPalette.name)
        .put("dailyIntention", settings.dailyIntention)
        .put("pomodoroFocusMinutes", settings.pomodoroFocusMinutes)
        .put("pomodoroShortBreakMinutes", settings.pomodoroShortBreakMinutes)
        .put("pomodoroLongBreakMinutes", settings.pomodoroLongBreakMinutes),
    )

    val profilesArray = JSONArray()
    profiles.forEach { p ->
      profilesArray.put(
        JSONObject()
          .put("name", p.name)
          .put("presetType", p.presetType)
          .put("blockedPackages", JSONArray(p.blockedPackages))
          .put("allowedPackages", JSONArray(p.allowedPackages))
          .put("focusDurationMinutes", p.focusDurationMinutes)
          .put("unlockPolicy", p.unlockPolicy)
          .put("strictMode", p.strictMode)
          .put("isActive", p.isActive)
          .put("priority", p.priority)
      )
    }
    root.put("profiles", profilesArray)

    val limitsArray = JSONArray()
    limits.forEach { l ->
      limitsArray.put(
        JSONObject()
          .put("packageName", l.packageName)
          .put("appName", l.appName)
          .put("dailyLimitMinutes", l.dailyLimitMinutes)
          .put("weeklyLimitMinutes", l.weeklyLimitMinutes)
          .put("isBlockedManually", l.isBlockedManually)
          .put("challengeType", l.challengeType)
      )
    }
    root.put("appLimits", limitsArray)

    val websitesArray = JSONArray()
    websites.forEach { w ->
      websitesArray.put(
        JSONObject()
          .put("domain", w.domain)
          .put("category", w.category)
          .put("isAllowlist", w.isAllowlist)
          .put("isEnabled", w.isEnabled)
      )
    }
    root.put("websiteRules", websitesArray)

    val sessionsArray = JSONArray()
    sessions.forEach { s ->
      sessionsArray.put(
        JSONObject()
          .put("dateKey", s.dateKey)
          .put("sessionType", s.sessionType)
          .put("timerMode", s.timerMode)
          .put("plannedMinutes", s.plannedMinutes)
          .put("elapsedSeconds", s.elapsedSeconds)
          .put("completed", s.completed)
          .put("note", s.note)
          .put("startedAtEpochMs", s.startedAtEpochMs)
          .put("endedAtEpochMs", s.endedAtEpochMs)
      )
    }
    root.put("focusSessions", sessionsArray)

    val analyticsArray = JSONArray()
    analytics.forEach { a ->
      analyticsArray.put(
        JSONObject()
          .put("dateKey", a.dateKey)
          .put("totalScreenTimeMs", a.totalScreenTimeMs)
          .put("totalFocusMinutes", a.totalFocusMinutes)
          .put("completedFocusSessions", a.completedFocusSessions)
          .put("blockedAttemptsCount", a.blockedAttemptsCount)
          .put("unlockCount", a.unlockCount)
      )
    }
    root.put("dailyAnalytics", analyticsArray)

    val catLimits = categoryLimitDao?.getAll().orEmpty()
    val catArray = JSONArray()
    catLimits.forEach { c ->
      catArray.put(
        JSONObject()
          .put("categoryName", c.categoryName)
          .put("dailyLimitMinutes", c.dailyLimitMinutes)
          .put("isEnabled", c.isEnabled)
          .put("challengeType", c.challengeType)
      )
    }
    root.put("categoryLimits", catArray)

    val habits = habitDao?.getAllHabits().orEmpty()
    val habitsArray = JSONArray()
    habits.forEach { h ->
      habitsArray.put(
        JSONObject()
          .put("id", h.id)
          .put("title", h.title)
          .put("description", h.description)
          .put("frequencyType", h.frequencyType)
          .put("targetCount", h.targetCount)
          .put("unitLabel", h.unitLabel)
          .put("colorHexTag", h.colorHexTag)
          .put("isArchived", h.isArchived)
      )
    }
    root.put("habits", habitsArray)

    val completions = habitDao?.getAllCompletions().orEmpty()
    val compArray = JSONArray()
    completions.forEach { c ->
      compArray.put(
        JSONObject()
          .put("habitId", c.habitId)
          .put("dateKey", c.dateKey)
          .put("completedCount", c.completedCount)
          .put("completedAtEpochMs", c.completedAtEpochMs)
      )
    }
    root.put("habitCompletions", compArray)
    return root.toString(2)
  }

  suspend fun exportEncryptedBackup(passphrase: String): String {
    val plain = exportFullJson()
    return KeystoreAndBackupCrypto.encryptPortableBackup(plain, passphrase)
  }

  suspend fun restoreFromBackup(rawPayload: String, passphrase: String? = null): Int {
    val jsonText =
      if (rawPayload.contains("focusnest-encrypted-backup")) {
        require(!passphrase.isNullOrBlank()) { "Passphrase required to decrypt backup." }
        KeystoreAndBackupCrypto.decryptPortableBackup(rawPayload, passphrase)
      } else {
        rawPayload
      }
    val root = JSONObject(jsonText)
    require(root.optString("schema") == "focusnest-backup-v1") {
      "Unsupported backup schema."
    }

    var restoredItems = 0
    root.optJSONObject("goals")?.let { g ->
      focusGoalDao.upsert(
        FocusGoalEntity(
          id = 1,
          dailyScreenTimeGoalMinutes = g.optInt("dailyScreenTimeGoalMinutes", 180),
          dailyFocusGoalMinutes = g.optInt("dailyFocusGoalMinutes", 60),
          dailySessionsGoal = g.optInt("dailySessionsGoal", 2),
          weeklyFocusGoalMinutes = g.optInt("weeklyFocusGoalMinutes", 420),
        )
      )
      restoredItems++
    }

    root.optJSONArray("profiles")?.let { arr ->
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val blocked = o.optJSONArray("blockedPackages")?.let { a -> List(a.length()) { a.getString(it) } }.orEmpty()
        val allowed = o.optJSONArray("allowedPackages")?.let { a -> List(a.length()) { a.getString(it) } }.orEmpty()
        blockProfileDao.upsert(
          BlockProfileEntity(
            name = o.getString("name"),
            presetType = o.optString("presetType", "Custom"),
            blockedPackages = blocked,
            allowedPackages = allowed,
            focusDurationMinutes = o.optInt("focusDurationMinutes", 25),
            unlockPolicy = o.optString("unlockPolicy", "REFLECTION_REMINDER"),
            strictMode = o.optBoolean("strictMode", false),
            isActive = o.optBoolean("isActive", false),
            priority = o.optInt("priority", 5),
          )
        )
        restoredItems++
      }
    }

    root.optJSONArray("appLimits")?.let { arr ->
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        appLimitDao.upsert(
          AppLimitEntity(
            packageName = o.getString("packageName"),
            appName = o.optString("appName", o.getString("packageName")),
            dailyLimitMinutes = o.optInt("dailyLimitMinutes", 0),
            weeklyLimitMinutes = o.optInt("weeklyLimitMinutes", 0),
            isBlockedManually = o.optBoolean("isBlockedManually", false),
            challengeType = o.optString("challengeType", "REFLECTION_REMINDER"),
            categoryName = o.optString("categoryName", ""),
            gracePeriodMinutes = o.optInt("gracePeriodMinutes", 0),
          )
        )
        restoredItems++
      }
    }

    root.optJSONArray("categoryLimits")?.let { arr ->
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        categoryLimitDao?.upsert(
          CategoryLimitEntity(
            categoryName = o.getString("categoryName"),
            dailyLimitMinutes = o.optInt("dailyLimitMinutes", 60),
            isEnabled = o.optBoolean("isEnabled", true),
            challengeType = o.optString("challengeType", "REFLECTION_REMINDER"),
          )
        )
        restoredItems++
      }
    }

    val habitIdMap = mutableMapOf<Long, Long>()
    root.optJSONArray("habits")?.let { arr ->
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val oldId = o.optLong("id", 0L)
        val newId =
          habitDao?.upsertHabit(
            HabitEntity(
              title = o.getString("title"),
              description = o.optString("description", ""),
              frequencyType = o.optString("frequencyType", "DAILY"),
              targetCount = o.optInt("targetCount", 1),
              unitLabel = o.optString("unitLabel", "times"),
              colorHexTag = o.optString("colorHexTag", "#155E50"),
              isArchived = o.optBoolean("isArchived", false),
            )
          ) ?: 0L
        if (oldId > 0L && newId > 0L) habitIdMap[oldId] = newId
        restoredItems++
      }
    }

    root.optJSONArray("habitCompletions")?.let { arr ->
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val oldHabitId = o.optLong("habitId", 0L)
        val mappedId = habitIdMap[oldHabitId] ?: oldHabitId
        if (mappedId > 0L) {
          habitDao?.upsertCompletion(
            HabitCompletionEntity(
              habitId = mappedId,
              dateKey = o.getString("dateKey"),
              completedCount = o.optInt("completedCount", 1),
              completedAtEpochMs = o.optLong("completedAtEpochMs", System.currentTimeMillis()),
            )
          )
          restoredItems++
        }
      }
    }

    root.optJSONArray("websiteRules")?.let { arr ->
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        websiteRuleDao.upsert(
          WebsiteRuleEntity(
            domain = o.getString("domain"),
            category = o.optString("category", "Restored"),
            isAllowlist = o.optBoolean("isAllowlist", false),
            isEnabled = o.optBoolean("isEnabled", true),
          )
        )
        restoredItems++
      }
    }

    root.optJSONArray("focusSessions")?.let { arr ->
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        focusSessionDao.insert(
          FocusSessionEntity(
            dateKey = o.getString("dateKey"),
            sessionType = o.optString("sessionType", "STUDY"),
            timerMode = o.optString("timerMode", "POMODORO"),
            plannedMinutes = o.optInt("plannedMinutes", 25),
            elapsedSeconds = o.optLong("elapsedSeconds", 0L),
            completed = o.optBoolean("completed", true),
            note = o.optString("note", ""),
            startedAtEpochMs = o.optLong("startedAtEpochMs", System.currentTimeMillis()),
            endedAtEpochMs = o.optLong("endedAtEpochMs", System.currentTimeMillis()),
          )
        )
        restoredItems++
      }
    }
    return restoredItems
  }

  suspend fun buildPrintableReportHtml(startDateKey: String, endDateKey: String): String {
    val usage = appUsageDao.getUsageInRange(startDateKey, endDateKey)
    val sessions = focusSessionDao.getAll().filter { it.dateKey in startDateKey..endDateKey }
    val goal = focusGoalDao.getGoal() ?: FocusGoalEntity()
    val analytics = dailyAnalyticsDao.getAll().filter { it.dateKey in startDateKey..endDateKey }
    val activeHabits = habitDao?.getAllHabits()?.filter { !it.isArchived }.orEmpty()
    val habitLogs =
      habitDao?.getAllCompletions()?.filter { it.dateKey in startDateKey..endDateKey && it.completedCount > 0 }.orEmpty()

    val totalScreenMin = (usage.sumOf { it.foregroundTimeMs } / 60_000L).toInt()
    val totalFocusMin = (sessions.sumOf { it.elapsedSeconds } / 60L).toInt()
    val completedSessions = sessions.count { it.completed }
    val blockedAttempts = analytics.sumOf { it.blockedAttemptsCount }
    val mindfulUnlocks = analytics.sumOf { it.unlockCount }
    val goalHit = totalScreenMin <= goal.dailyScreenTimeGoalMinutes * 7
    val statusBadge = if (goalHit) "On Track with Wellbeing Goal" else "Above Weekly Screen Budget"

    val appRows =
      usage
        .groupBy { it.appName }
        .map { (name, items) -> name to (items.sumOf { it.foregroundTimeMs } / 60_000L) }
        .sortedByDescending { it.second }
        .take(12)
        .joinToString("") { (name, mins) ->
          "<tr><td>${escapeHtml(name)}</td><td>${FocusTimeUtils.formatDurationMinutes(mins.toInt())}</td></tr>"
        }
        .ifBlank { "<tr><td colspan='2'>No measured app usage in this range.</td></tr>" }

    val categoryRows =
      usage
        .groupBy { it.category.ifBlank { "Other" } }
        .map { (cat, items) -> cat to (items.sumOf { it.foregroundTimeMs } / 60_000L) }
        .sortedByDescending { it.second }
        .take(6)
        .joinToString("") { (cat, mins) ->
          "<tr><td>${escapeHtml(cat)}</td><td>${FocusTimeUtils.formatDurationMinutes(mins.toInt())}</td></tr>"
        }
        .ifBlank { "<tr><td colspan='2'>No category data recorded.</td></tr>" }

    val reflectionLine =
      when {
        totalFocusMin >= goal.weeklyFocusGoalMinutes ->
          "Strong week: you surpassed your weekly focus goal while logging $completedSessions deep work sessions."
        blockedAttempts > 0 ->
          "Your FocusNest Shield deflected $blockedAttempts distraction triggers. Consider tightening evening bedtime boundaries."
        else ->
          "Keep measuring daily usage and building one small screen-free routine at a time."
      }

    return """
      <!DOCTYPE html>
      <html>
      <head><meta charset="utf-8"><style>
      body{font-family:sans-serif;padding:28px;color:#0E2D27;background:#ffffff;line-height:1.45}
      .header{display:flex;justify-content:space-between;align-items:center;border-bottom:2px solid #0E2D27;padding-bottom:12px;margin-bottom:18px}
      .badge{display:inline-block;padding:4px 10px;border-radius:999px;background:#D7F5EA;color:#0E2D27;font-size:12px;font-weight:bold}
      .grid{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;margin:16px 0}
      .kpi{border:1px solid #d5e0dc;border-radius:10px;padding:12px;background:#F6F8F6}
      .kpi .label{font-size:11px;color:#46615A;text-transform:uppercase}
      .kpi .val{font-size:20px;font-weight:bold;margin-top:4px}
      h1{margin:0;font-size:22px}h2{font-size:15px;margin-top:20px;margin-bottom:8px;color:#155E50}
      table{width:100%;border-collapse:collapse;margin-top:6px}
      th,td{border-bottom:1px solid #d5e0dc;padding:8px;text-align:left;font-size:13px}
      th{background:#F0F5F3;font-weight:bold}
      .reflection{margin-top:18px;padding:12px 14px;border-left:4px solid #155E50;background:#F4F9F7;font-size:13px}
      </style></head>
      <body>
      <div class="header">
        <div>
          <h1>FocusNest Weekly Wellbeing Report Card</h1>
          <div style="color:#46615A;font-size:13px">Window: $startDateKey to $endDateKey • 100% On-Device Report</div>
        </div>
        <span class="badge">$statusBadge</span>
      </div>
      <div class="grid">
        <div class="kpi"><div class="label">Screen Time</div><div class="val">${FocusTimeUtils.formatDurationMinutes(totalScreenMin)}</div></div>
        <div class="kpi"><div class="label">Deep Focus</div><div class="val">${FocusTimeUtils.formatDurationMinutes(totalFocusMin)} ($completedSessions)</div></div>
        <div class="kpi"><div class="label">Shield Deflections</div><div class="val">$blockedAttempts / $mindfulUnlocks</div></div>
        <div class="kpi"><div class="label">Habit Check-ins</div><div class="val">${habitLogs.size} (${activeHabits.size} active)</div></div>
      </div>
      <h2>Category Time Distribution</h2>
      <table><tr><th>Category</th><th>Time Used</th></tr>$categoryRows</table>
      <h2>Top Applications</h2>
      <table><tr><th>Application</th><th>Measured Usage</th></tr>$appRows</table>
      <div class="reflection"><strong>Weekly Reflection Summary:</strong> ${escapeHtml(reflectionLine)}</div>
      </body></html>
    """.trimIndent()
  }

  private fun escapeHtml(input: String): String =
    input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

  suspend fun clearUsageHistory() {
    appUsageDao.deleteAll()
    dailyAnalyticsDao.deleteAll()
  }

  suspend fun clearNotificationHistory() {
    notificationRecordDao.deleteAll()
  }

  suspend fun clearHabits() {
    habitDao?.deleteAllCompletions()
    habitDao?.deleteAllHabits()
  }

  suspend fun deleteAllUserData() {
    appUsageDao.deleteAll()
    appLimitDao.deleteAll()
    categoryLimitDao?.deleteAll()
    appGroupDao.deleteAll()
    blockProfileDao.deleteAll()
    blockingScheduleDao.deleteAll()
    focusSessionDao.deleteAll()
    unlockDao.deleteAllHistory()
    websiteRuleDao.deleteAll()
    notificationRecordDao.deleteAll()
    dailyAnalyticsDao.deleteAll()
    habitDao?.deleteAllCompletions()
    habitDao?.deleteAllHabits()
    preferences.clearAll()
  }
}
