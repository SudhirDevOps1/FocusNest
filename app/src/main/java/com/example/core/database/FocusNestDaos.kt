package com.example.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InstalledAppDao {
  @Query("SELECT * FROM installed_apps ORDER BY appName ASC")
  fun observeAll(): Flow<List<InstalledAppEntity>>

  @Query("SELECT * FROM installed_apps ORDER BY appName ASC")
  suspend fun getAll(): List<InstalledAppEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(apps: List<InstalledAppEntity>)

  @Query("UPDATE installed_apps SET isSelectedDistracting = :distracting WHERE packageName = :pkg")
  suspend fun setDistracting(pkg: String, distracting: Boolean)

  @Query("UPDATE installed_apps SET isEmergencyWhitelisted = :whitelisted WHERE packageName = :pkg")
  suspend fun setEmergencyWhitelisted(pkg: String, whitelisted: Boolean)
}

@Dao
interface AppUsageDao {
  @Query("SELECT * FROM app_usage WHERE dateKey = :dateKey ORDER BY foregroundTimeMs DESC")
  fun observeUsageForDate(dateKey: String): Flow<List<AppUsageEntity>>

  @Query("SELECT * FROM app_usage WHERE dateKey BETWEEN :startDateKey AND :endDateKey ORDER BY foregroundTimeMs DESC")
  fun observeUsageInRange(startDateKey: String, endDateKey: String): Flow<List<AppUsageEntity>>

  @Query("SELECT * FROM app_usage WHERE dateKey = :dateKey ORDER BY foregroundTimeMs DESC")
  suspend fun getUsageForDate(dateKey: String): List<AppUsageEntity>

  @Query("SELECT * FROM app_usage WHERE dateKey BETWEEN :startDateKey AND :endDateKey")
  suspend fun getUsageInRange(startDateKey: String, endDateKey: String): List<AppUsageEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(entries: List<AppUsageEntity>)

  @Query("DELETE FROM app_usage")
  suspend fun deleteAll()
}

@Dao
interface AppLimitDao {
  @Query("SELECT * FROM app_limits ORDER BY appName ASC")
  fun observeAll(): Flow<List<AppLimitEntity>>

  @Query("SELECT * FROM app_limits")
  suspend fun getAll(): List<AppLimitEntity>

  @Query("SELECT * FROM app_limits WHERE packageName = :packageName LIMIT 1")
  suspend fun getByPackage(packageName: String): AppLimitEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(limit: AppLimitEntity)

  @Query("DELETE FROM app_limits WHERE packageName = :packageName")
  suspend fun deleteByPackage(packageName: String)

  @Query("DELETE FROM app_limits")
  suspend fun deleteAll()
}

@Dao
interface AppGroupDao {
  @Query("SELECT * FROM app_groups ORDER BY name ASC")
  fun observeAll(): Flow<List<AppGroupEntity>>

  @Query("SELECT * FROM app_groups")
  suspend fun getAll(): List<AppGroupEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(group: AppGroupEntity): Long

  @Delete
  suspend fun delete(group: AppGroupEntity)

  @Query("DELETE FROM app_groups")
  suspend fun deleteAll()
}

@Dao
interface BlockProfileDao {
  @Query("SELECT * FROM block_profiles ORDER BY priority DESC, name ASC")
  fun observeAll(): Flow<List<BlockProfileEntity>>

  @Query("SELECT * FROM block_profiles ORDER BY priority DESC, name ASC")
  suspend fun getAll(): List<BlockProfileEntity>

  @Query("SELECT * FROM block_profiles WHERE id = :id LIMIT 1")
  suspend fun getById(id: Long): BlockProfileEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(profile: BlockProfileEntity): Long

  @Query("UPDATE block_profiles SET isActive = :active WHERE id = :id")
  suspend fun setActive(id: Long, active: Boolean)

  @Delete
  suspend fun delete(profile: BlockProfileEntity)

  @Query("DELETE FROM block_profiles")
  suspend fun deleteAll()
}

@Dao
interface BlockingScheduleDao {
  @Query("SELECT * FROM blocking_schedules ORDER BY startMinuteOfDay ASC")
  fun observeAll(): Flow<List<BlockingScheduleEntity>>

  @Query("SELECT * FROM blocking_schedules")
  suspend fun getAll(): List<BlockingScheduleEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(schedule: BlockingScheduleEntity): Long

  @Delete
  suspend fun delete(schedule: BlockingScheduleEntity)

  @Query("DELETE FROM blocking_schedules")
  suspend fun deleteAll()
}

@Dao
interface FocusSessionDao {
  @Query("SELECT * FROM focus_sessions ORDER BY startedAtEpochMs DESC")
  fun observeAll(): Flow<List<FocusSessionEntity>>

  @Query("SELECT * FROM focus_sessions WHERE dateKey = :dateKey ORDER BY startedAtEpochMs DESC")
  fun observeByDate(dateKey: String): Flow<List<FocusSessionEntity>>

  @Query("SELECT * FROM focus_sessions ORDER BY startedAtEpochMs DESC")
  suspend fun getAll(): List<FocusSessionEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(session: FocusSessionEntity): Long

  @Query("DELETE FROM focus_sessions")
  suspend fun deleteAll()
}

@Dao
interface FocusGoalDao {
  @Query("SELECT * FROM focus_goals WHERE id = 1")
  fun observeGoal(): Flow<FocusGoalEntity?>

  @Query("SELECT * FROM focus_goals WHERE id = 1")
  suspend fun getGoal(): FocusGoalEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(goal: FocusGoalEntity)
}

@Dao
interface UnlockDao {
  @Query("SELECT * FROM unlock_rules ORDER BY id ASC")
  fun observeRules(): Flow<List<UnlockRuleEntity>>

  @Query("SELECT * FROM unlock_rules ORDER BY id ASC")
  suspend fun getRules(): List<UnlockRuleEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRule(rule: UnlockRuleEntity): Long

  @Query("SELECT * FROM unlock_history ORDER BY timestampEpochMs DESC LIMIT 200")
  fun observeHistory(): Flow<List<UnlockHistoryEntity>>

  @Query("SELECT * FROM unlock_history WHERE dateKey = :dateKey AND packageName = :pkg")
  suspend fun getHistoryForAppToday(dateKey: String, pkg: String): List<UnlockHistoryEntity>

  @Query("SELECT * FROM unlock_history ORDER BY timestampEpochMs DESC")
  suspend fun getAllHistory(): List<UnlockHistoryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertHistory(entry: UnlockHistoryEntity): Long

  @Query("DELETE FROM unlock_history")
  suspend fun deleteAllHistory()
}

@Dao
interface WebsiteRuleDao {
  @Query("SELECT * FROM website_rules ORDER BY isAllowlist ASC, domain ASC")
  fun observeAll(): Flow<List<WebsiteRuleEntity>>

  @Query("SELECT * FROM website_rules")
  suspend fun getAll(): List<WebsiteRuleEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(rule: WebsiteRuleEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(rules: List<WebsiteRuleEntity>)

  @Query("UPDATE website_rules SET blockedHits = blockedHits + 1 WHERE domain = :domain")
  suspend fun incrementHits(domain: String)

  @Delete
  suspend fun delete(rule: WebsiteRuleEntity)

  @Query("DELETE FROM website_rules")
  suspend fun deleteAll()
}

@Dao
interface ReminderDao {
  @Query("SELECT * FROM reminders ORDER BY id ASC")
  fun observeAll(): Flow<List<ReminderEntity>>

  @Query("SELECT * FROM reminders")
  suspend fun getAll(): List<ReminderEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(reminder: ReminderEntity): Long

  @Delete
  suspend fun delete(reminder: ReminderEntity)
}

@Dao
interface NotificationRecordDao {
  @Query("SELECT * FROM notification_records ORDER BY postedAtEpochMs DESC LIMIT 200")
  fun observeRecent(): Flow<List<NotificationRecordEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(record: NotificationRecordEntity): Long

  @Query("DELETE FROM notification_records")
  suspend fun deleteAll()
}

@Dao
interface DailyAnalyticsDao {
  @Query("SELECT * FROM daily_analytics ORDER BY dateKey DESC LIMIT 90")
  fun observeRecent(): Flow<List<DailyAnalyticsEntity>>

  @Query("SELECT * FROM daily_analytics WHERE dateKey = :dateKey LIMIT 1")
  fun observeByDate(dateKey: String): Flow<DailyAnalyticsEntity?>

  @Query("SELECT * FROM daily_analytics WHERE dateKey = :dateKey LIMIT 1")
  suspend fun getByDate(dateKey: String): DailyAnalyticsEntity?

  @Query("SELECT * FROM daily_analytics ORDER BY dateKey DESC")
  suspend fun getAll(): List<DailyAnalyticsEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(entry: DailyAnalyticsEntity)

  @Query("DELETE FROM daily_analytics")
  suspend fun deleteAll()
}

@Dao
interface UserPreferenceDao {
  @Query("SELECT * FROM user_preferences")
  fun observeAll(): Flow<List<UserPreferenceEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(pref: UserPreferenceEntity)

  @Query("DELETE FROM user_preferences")
  suspend fun deleteAll()
}

@Dao
interface TokenVaultDao {
  @Query("SELECT * FROM nfc_tags ORDER BY createdAtEpochMs DESC")
  fun observeNfcTags(): Flow<List<NfcTagEntity>>

  @Query("SELECT * FROM nfc_tags WHERE tagUidHex = :uid LIMIT 1")
  suspend fun getNfcTag(uid: String): NfcTagEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertNfcTag(tag: NfcTagEntity)

  @Delete
  suspend fun deleteNfcTag(tag: NfcTagEntity)

  @Query("SELECT * FROM qr_tokens ORDER BY createdAtEpochMs DESC")
  fun observeQrTokens(): Flow<List<QrTokenEntity>>

  @Query("SELECT * FROM qr_tokens WHERE tokenValue = :token LIMIT 1")
  suspend fun findQrToken(token: String): QrTokenEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertQrToken(token: QrTokenEntity): Long

  @Query("UPDATE qr_tokens SET isConsumed = 1 WHERE id = :id")
  suspend fun markQrTokenConsumed(id: Long)

  @Delete
  suspend fun deleteQrToken(token: QrTokenEntity)
}

@Dao
interface ShortFormRuleDao {
  @Query("SELECT * FROM short_form_rules ORDER BY appName ASC")
  fun observeAll(): Flow<List<ShortFormRuleEntity>>

  @Query("SELECT * FROM short_form_rules")
  suspend fun getAll(): List<ShortFormRuleEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(rule: ShortFormRuleEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(rules: List<ShortFormRuleEntity>)
}

@Dao
interface HabitDao {
  @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAtEpochMs DESC")
  fun observeActiveHabits(): Flow<List<HabitEntity>>

  @Query("SELECT * FROM habits ORDER BY createdAtEpochMs DESC")
  suspend fun getAllHabits(): List<HabitEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertHabit(habit: HabitEntity): Long

  @Delete
  suspend fun deleteHabit(habit: HabitEntity)

  @Query("SELECT * FROM habit_completions ORDER BY dateKey DESC")
  fun observeAllCompletions(): Flow<List<HabitCompletionEntity>>

  @Query("SELECT * FROM habit_completions WHERE dateKey BETWEEN :startDateKey AND :endDateKey")
  suspend fun getCompletionsInRange(startDateKey: String, endDateKey: String): List<HabitCompletionEntity>

  @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND dateKey = :dateKey LIMIT 1")
  suspend fun getCompletionForHabitDate(habitId: Long, dateKey: String): HabitCompletionEntity?

  @Query("SELECT * FROM habit_completions ORDER BY dateKey DESC")
  suspend fun getAllCompletions(): List<HabitCompletionEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCompletion(completion: HabitCompletionEntity): Long

  @Query("DELETE FROM habit_completions")
  suspend fun deleteAllCompletions()

  @Query("DELETE FROM habits")
  suspend fun deleteAllHabits()
}

@Dao
interface CategoryLimitDao {
  @Query("SELECT * FROM category_limits ORDER BY categoryName ASC")
  fun observeAll(): Flow<List<CategoryLimitEntity>>

  @Query("SELECT * FROM category_limits ORDER BY categoryName ASC")
  suspend fun getAll(): List<CategoryLimitEntity>

  @Query("SELECT * FROM category_limits WHERE categoryName = :categoryName LIMIT 1")
  suspend fun getByCategory(categoryName: String): CategoryLimitEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(limit: CategoryLimitEntity)

  @Query("DELETE FROM category_limits WHERE categoryName = :categoryName")
  suspend fun deleteByCategory(categoryName: String)

  @Query("DELETE FROM category_limits")
  suspend fun deleteAll()
}
