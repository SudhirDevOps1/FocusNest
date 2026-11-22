package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.json.JSONArray

class FocusNestConverters {
  @TypeConverter
  fun fromStringList(value: List<String>?): String {
    val array = JSONArray()
    value?.forEach { array.put(it) }
    return array.toString()
  }

  @TypeConverter
  fun toStringList(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return try {
      val array = JSONArray(raw)
      List(array.length()) { index -> array.optString(index, "") }.filter { it.isNotEmpty() }
    } catch (_: Exception) {
      emptyList()
    }
  }

  @TypeConverter
  fun fromIntList(value: List<Int>?): String {
    val array = JSONArray()
    value?.forEach { array.put(it) }
    return array.toString()
  }

  @TypeConverter
  fun toIntList(raw: String?): List<Int> {
    if (raw.isNullOrBlank()) return emptyList()
    return try {
      val array = JSONArray(raw)
      List(array.length()) { index -> array.optInt(index, 0) }
    } catch (_: Exception) {
      emptyList()
    }
  }
}

@Database(
  entities =
    [
      InstalledAppEntity::class,
      AppUsageEntity::class,
      AppLimitEntity::class,
      AppGroupEntity::class,
      BlockProfileEntity::class,
      BlockingScheduleEntity::class,
      FocusSessionEntity::class,
      FocusGoalEntity::class,
      UnlockRuleEntity::class,
      UnlockHistoryEntity::class,
      WebsiteRuleEntity::class,
      ReminderEntity::class,
      NotificationRecordEntity::class,
      DailyAnalyticsEntity::class,
      UserPreferenceEntity::class,
      NfcTagEntity::class,
      QrTokenEntity::class,
      ShortFormRuleEntity::class,
      HabitEntity::class,
      HabitCompletionEntity::class,
      CategoryLimitEntity::class,
    ],
  version = 2,
  exportSchema = false,
)
@TypeConverters(FocusNestConverters::class)
abstract class FocusNestDatabase : RoomDatabase() {
  abstract fun installedAppDao(): InstalledAppDao
  abstract fun appUsageDao(): AppUsageDao
  abstract fun appLimitDao(): AppLimitDao
  abstract fun appGroupDao(): AppGroupDao
  abstract fun blockProfileDao(): BlockProfileDao
  abstract fun blockingScheduleDao(): BlockingScheduleDao
  abstract fun focusSessionDao(): FocusSessionDao
  abstract fun focusGoalDao(): FocusGoalDao
  abstract fun unlockDao(): UnlockDao
  abstract fun websiteRuleDao(): WebsiteRuleDao
  abstract fun reminderDao(): ReminderDao
  abstract fun notificationRecordDao(): NotificationRecordDao
  abstract fun dailyAnalyticsDao(): DailyAnalyticsDao
  abstract fun userPreferenceDao(): UserPreferenceDao
  abstract fun tokenVaultDao(): TokenVaultDao
  abstract fun shortFormRuleDao(): ShortFormRuleDao
  abstract fun habitDao(): HabitDao
  abstract fun categoryLimitDao(): CategoryLimitDao

  companion object {
    const val DATABASE_NAME = "focusnest_local.db"

    val MIGRATION_1_2 =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE app_limits ADD COLUMN categoryName TEXT NOT NULL DEFAULT ''")
          db.execSQL("ALTER TABLE app_limits ADD COLUMN gracePeriodMinutes INTEGER NOT NULL DEFAULT 0")
          db.execSQL("ALTER TABLE app_limits ADD COLUMN warned80DateKey TEXT NOT NULL DEFAULT ''")
          db.execSQL("ALTER TABLE app_limits ADD COLUMN warned90DateKey TEXT NOT NULL DEFAULT ''")
          db.execSQL("ALTER TABLE app_limits ADD COLUMN warned100DateKey TEXT NOT NULL DEFAULT ''")

          db.execSQL("ALTER TABLE qr_tokens ADD COLUMN isOneTime INTEGER NOT NULL DEFAULT 0")
          db.execSQL("ALTER TABLE qr_tokens ADD COLUMN expiresAtEpochMs INTEGER NOT NULL DEFAULT 0")
          db.execSQL("ALTER TABLE qr_tokens ADD COLUMN isConsumed INTEGER NOT NULL DEFAULT 0")
          db.execSQL("ALTER TABLE qr_tokens ADD COLUMN signatureHex TEXT NOT NULL DEFAULT ''")

          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `habits` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `title` TEXT NOT NULL,
              `description` TEXT NOT NULL,
              `frequencyType` TEXT NOT NULL,
              `targetCount` INTEGER NOT NULL,
              `unitLabel` TEXT NOT NULL,
              `reminderMinuteOfDay` INTEGER NOT NULL,
              `reminderEnabled` INTEGER NOT NULL,
              `colorHexTag` TEXT NOT NULL,
              `createdAtEpochMs` INTEGER NOT NULL,
              `isArchived` INTEGER NOT NULL
            )
            """.trimIndent()
          )

          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `habit_completions` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `habitId` INTEGER NOT NULL,
              `dateKey` TEXT NOT NULL,
              `completedCount` INTEGER NOT NULL,
              `completedAtEpochMs` INTEGER NOT NULL,
              FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
          )
          db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_completions_habitId` ON `habit_completions` (`habitId`)")
          db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_completions_dateKey` ON `habit_completions` (`dateKey`)")

          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `category_limits` (
              `categoryName` TEXT NOT NULL,
              `dailyLimitMinutes` INTEGER NOT NULL,
              `isEnabled` INTEGER NOT NULL,
              `challengeType` TEXT NOT NULL,
              `updatedAtEpochMs` INTEGER NOT NULL,
              PRIMARY KEY(`categoryName`)
            )
            """.trimIndent()
          )
        }
      }

    @Volatile private var INSTANCE: FocusNestDatabase? = null
    @Volatile private var instanceDbPath: String? = null

    fun getInstance(context: Context): FocusNestDatabase {
      val appContext = context.applicationContext ?: context
      val dbPath = appContext.getDatabasePath(DATABASE_NAME).absolutePath
      val existing = INSTANCE
      if (existing != null && existing.isOpen && instanceDbPath == dbPath) {
        return existing
      }
      return synchronized(this) {
        val current = INSTANCE
        if (current != null && current.isOpen && instanceDbPath == dbPath) {
          current
        } else {
          runCatching { current?.close() }
          Room.databaseBuilder(
              appContext,
              FocusNestDatabase::class.java,
              DATABASE_NAME,
            )
            .addMigrations(MIGRATION_1_2)
            .build()
            .also {
              INSTANCE = it
              instanceDbPath = dbPath
            }
        }
      }
    }
  }
}
