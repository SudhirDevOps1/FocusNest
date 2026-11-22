package com.example.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "installed_apps")
data class InstalledAppEntity(
  @PrimaryKey val packageName: String,
  val appName: String,
  val category: String,
  val isSystemApp: Boolean = false,
  val isSelectedDistracting: Boolean = false,
  val isEmergencyWhitelisted: Boolean = false,
  val lastSeenEpochMs: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "app_usage",
  primaryKeys = ["dateKey", "packageName"],
  indices = [Index(value = ["dateKey"]), Index(value = ["packageName"])],
)
data class AppUsageEntity(
  val dateKey: String,
  val packageName: String,
  val appName: String,
  val category: String,
  val foregroundTimeMs: Long,
  val launchCount: Int = 0,
  val firstUsedEpochMs: Long? = null,
  val lastUsedEpochMs: Long? = null,
  val isMeasured: Boolean = true,
)

@Entity(tableName = "app_limits")
data class AppLimitEntity(
  @PrimaryKey val packageName: String,
  val appName: String,
  val dailyLimitMinutes: Int,
  val weeklyLimitMinutes: Int = 0,
  val isBlockedManually: Boolean = false,
  val cooldownSeconds: Int = 15,
  val challengeType: String = "REFLECTION_REMINDER",
  val tempUnlockUntilEpochMs: Long? = null,
  val updatedAtEpochMs: Long = System.currentTimeMillis(),
  val categoryName: String = "",
  val gracePeriodMinutes: Int = 0,
  val warned80DateKey: String = "",
  val warned90DateKey: String = "",
  val warned100DateKey: String = "",
)

@Entity(tableName = "app_groups")
data class AppGroupEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val packageNames: List<String> = emptyList(),
  val sharedDailyLimitMinutes: Int = 60,
  val isEnabled: Boolean = true,
)

@Entity(tableName = "block_profiles")
data class BlockProfileEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val presetType: String,
  val blockedPackages: List<String> = emptyList(),
  val allowedPackages: List<String> = emptyList(),
  val blockedDomains: List<String> = emptyList(),
  val dailyLimitMinutes: Int = 0,
  val focusDurationMinutes: Int = 25,
  val unlockPolicy: String = "REFLECTION_REMINDER",
  val muteNotifications: Boolean = true,
  val strictMode: Boolean = false,
  val isActive: Boolean = false,
  val priority: Int = 5,
)

@Entity(
  tableName = "blocking_schedules",
  indices = [Index(value = ["profileId"])],
)
data class BlockingScheduleEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val profileId: Long,
  val label: String,
  val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5), // 1=Mon .. 7=Sun
  val startMinuteOfDay: Int = 9 * 60,
  val endMinuteOfDay: Int = 17 * 60,
  val isEnabled: Boolean = true,
)

@Entity(
  tableName = "focus_sessions",
  indices = [Index(value = ["dateKey"]), Index(value = ["startedAtEpochMs"])],
)
data class FocusSessionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val dateKey: String,
  val sessionType: String,
  val timerMode: String,
  val plannedMinutes: Int,
  val elapsedSeconds: Long,
  val completed: Boolean,
  val interrupted: Boolean = false,
  val note: String = "",
  val profileId: Long? = null,
  val startedAtEpochMs: Long,
  val endedAtEpochMs: Long,
)

@Entity(tableName = "focus_goals")
data class FocusGoalEntity(
  @PrimaryKey val id: Int = 1,
  val dailyScreenTimeGoalMinutes: Int = 180,
  val dailyFocusGoalMinutes: Int = 60,
  val dailySessionsGoal: Int = 2,
  val weeklyFocusGoalMinutes: Int = 420,
  val updatedAtEpochMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "unlock_rules")
data class UnlockRuleEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val challengeType: String,
  val cooldownSeconds: Int = 30,
  val tempUnlockMinutes: Int = 5,
  val dailyUnlockCap: Int = 5,
  val customSentence: String = "",
  val requireReflection: Boolean = true,
  val isEnabled: Boolean = true,
)

@Entity(
  tableName = "unlock_history",
  indices = [Index(value = ["dateKey"]), Index(value = ["packageName"])],
)
data class UnlockHistoryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val dateKey: String,
  val packageName: String,
  val appName: String,
  val challengeType: String,
  val granted: Boolean,
  val grantedMinutes: Int = 0,
  val intentionText: String = "",
  val timestampEpochMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "website_rules")
data class WebsiteRuleEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val domain: String,
  val category: String = "Distraction",
  val isAllowlist: Boolean = false,
  val isEnabled: Boolean = true,
  val blockedHits: Int = 0,
)

@Entity(tableName = "reminders")
data class ReminderEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val message: String,
  val reminderType: String, // BREAK, BEDTIME, FOCUS, INTENTION
  val intervalMinutes: Int = 45,
  val minuteOfDay: Int = 22 * 60,
  val isEnabled: Boolean = true,
)

@Entity(
  tableName = "notification_records",
  indices = [Index(value = ["dateKey"]), Index(value = ["packageName"])],
)
data class NotificationRecordEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val dateKey: String,
  val packageName: String,
  val appName: String,
  val titleSummary: String,
  val wasMuted: Boolean = false,
  val postedAtEpochMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "daily_analytics")
data class DailyAnalyticsEntity(
  @PrimaryKey val dateKey: String,
  val totalScreenTimeMs: Long = 0L,
  val totalFocusMinutes: Int = 0,
  val completedFocusSessions: Int = 0,
  val blockedAttemptsCount: Int = 0,
  val unlockCount: Int = 0,
  val shortFormInterventionsCount: Int = 0,
  val isMeasuredFromUsageStats: Boolean = false,
  val updatedAtEpochMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
  @PrimaryKey val key: String,
  val value: String,
  val updatedAtEpochMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "nfc_tags")
data class NfcTagEntity(
  @PrimaryKey val tagUidHex: String,
  val label: String,
  val actionType: String, // START_FOCUS, UNLOCK_PROFILE, TOGGLE_BLOCKER
  val targetProfileId: Long? = null,
  val focusMinutes: Int = 25,
  val createdAtEpochMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "qr_tokens")
data class QrTokenEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val tokenValue: String,
  val label: String,
  val tokenFormat: String, // QR_CODE, BARCODE
  val targetProfileId: Long? = null,
  val createdAtEpochMs: Long = System.currentTimeMillis(),
  val isOneTime: Boolean = false,
  val expiresAtEpochMs: Long = 0L,
  val isConsumed: Boolean = false,
  val signatureHex: String = "",
)

@Entity(tableName = "short_form_rules")
data class ShortFormRuleEntity(
  @PrimaryKey val packageName: String,
  val appName: String,
  val featureLabel: String,
  val isEnabled: Boolean = false,
  val dailyInteractionLimit: Int = 10,
  val interactionsToday: Int = 0,
  val lastResetDateKey: String = "",
  val pauseSeconds: Int = 8,
  val requireReflection: Boolean = true,
)

@Entity(tableName = "habits")
data class HabitEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val description: String = "",
  val frequencyType: String = "DAILY", // DAILY, WEEKLY
  val targetCount: Int = 1,
  val unitLabel: String = "times",
  val reminderMinuteOfDay: Int = 9 * 60,
  val reminderEnabled: Boolean = false,
  val colorHexTag: String = "#155E50",
  val createdAtEpochMs: Long = System.currentTimeMillis(),
  val isArchived: Boolean = false,
)

@Entity(
  tableName = "habit_completions",
  foreignKeys =
    [
      ForeignKey(
        entity = HabitEntity::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index(value = ["habitId"]), Index(value = ["dateKey"])],
)
data class HabitCompletionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val habitId: Long,
  val dateKey: String,
  val completedCount: Int = 1,
  val completedAtEpochMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "category_limits")
data class CategoryLimitEntity(
  @PrimaryKey val categoryName: String,
  val dailyLimitMinutes: Int,
  val isEnabled: Boolean = true,
  val challengeType: String = "REFLECTION_REMINDER",
  val updatedAtEpochMs: Long = System.currentTimeMillis(),
)
