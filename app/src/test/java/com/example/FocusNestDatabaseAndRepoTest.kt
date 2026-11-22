package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.AppGroupEntity
import com.example.core.database.AppLimitEntity
import com.example.core.database.AppUsageEntity
import com.example.core.database.BlockProfileEntity
import com.example.core.database.CategoryLimitEntity
import com.example.core.database.FocusNestDatabase
import com.example.core.database.FocusSessionEntity
import com.example.core.database.HabitEntity
import com.example.core.database.InstalledAppEntity
import com.example.core.database.QrTokenEntity
import com.example.core.database.WebsiteRuleEntity
import com.example.core.datastore.FocusNestPreferences
import com.example.core.datastore.PersistedTimerState
import com.example.data.repository.BlockerRepository
import com.example.data.repository.FocusRepository
import com.example.data.repository.HabitRepository
import com.example.data.repository.InsightsAndBackupRepository
import com.example.domain.model.FocusTimeUtils
import com.example.domain.model.LimitThresholdLevel
import com.example.domain.model.SessionType
import com.example.domain.model.TimerMode
import com.example.domain.model.TimerPhase
import com.example.domain.model.UnlockChallengeType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FocusNestDatabaseAndRepoTest {

  private lateinit var context: Context
  private lateinit var db: FocusNestDatabase
  private lateinit var prefs: FocusNestPreferences
  private lateinit var blockerRepo: BlockerRepository
  private lateinit var focusRepo: FocusRepository
  private lateinit var habitRepo: HabitRepository
  private lateinit var insightsRepo: InsightsAndBackupRepository

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    db = Room.inMemoryDatabaseBuilder(context, FocusNestDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    prefs = FocusNestPreferences(context)
    blockerRepo =
      BlockerRepository(
        appLimitDao = db.appLimitDao(),
        appGroupDao = db.appGroupDao(),
        blockProfileDao = db.blockProfileDao(),
        blockingScheduleDao = db.blockingScheduleDao(),
        unlockDao = db.unlockDao(),
        websiteRuleDao = db.websiteRuleDao(),
        tokenVaultDao = db.tokenVaultDao(),
        shortFormRuleDao = db.shortFormRuleDao(),
        appUsageDao = db.appUsageDao(),
        dailyAnalyticsDao = db.dailyAnalyticsDao(),
        installedAppDao = db.installedAppDao(),
        preferences = prefs,
        categoryLimitDao = db.categoryLimitDao(),
      )
    focusRepo = FocusRepository(db.focusSessionDao(), db.focusGoalDao(), db.dailyAnalyticsDao())
    habitRepo = HabitRepository(db.habitDao())
    insightsRepo =
      InsightsAndBackupRepository(
        appUsageDao = db.appUsageDao(),
        dailyAnalyticsDao = db.dailyAnalyticsDao(),
        focusSessionDao = db.focusSessionDao(),
        focusGoalDao = db.focusGoalDao(),
        appLimitDao = db.appLimitDao(),
        appGroupDao = db.appGroupDao(),
        blockProfileDao = db.blockProfileDao(),
        blockingScheduleDao = db.blockingScheduleDao(),
        websiteRuleDao = db.websiteRuleDao(),
        unlockDao = db.unlockDao(),
        reminderDao = db.reminderDao(),
        notificationRecordDao = db.notificationRecordDao(),
        preferences = prefs,
        habitDao = db.habitDao(),
        categoryLimitDao = db.categoryLimitDao(),
      )
  }

  @After
  fun teardown() {
    db.close()
  }

  @Test
  fun blockerRepository_evaluatesLimitsGroupsAndTemporaryUnlocks() = runTest {
    val pkg = "com.social.demo"
    val today = FocusTimeUtils.todayKey()
    db.installedAppDao().upsertAll(
      listOf(InstalledAppEntity(pkg, "Demo Social", "SOCIAL", isEmergencyWhitelisted = false))
    )
    db.appUsageDao().upsertAll(
      listOf(AppUsageEntity(today, pkg, "Demo Social", "SOCIAL", foregroundTimeMs = 35 * 60_000L, launchCount = 5))
    )
    blockerRepo.saveAppGroup(
      AppGroupEntity(name = "Social", packageNames = listOf(pkg), sharedDailyLimitMinutes = 30)
    )

    val blockedDecision = blockerRepo.evaluatePackageDecision(pkg)
    assertTrue(blockedDecision.isBlocked)

    val (granted, _) =
      blockerRepo.grantTemporaryUnlock(
        packageName = pkg,
        appName = "Demo Social",
        challengeType = UnlockChallengeType.ADAPTIVE_MATH,
        durationMinutes = 5,
      )
    assertTrue(granted)

    val afterUnlock = blockerRepo.evaluatePackageDecision(pkg)
    assertFalse(afterUnlock.isBlocked)
  }

  @Test
  fun focusRepository_calculatesStreakAcrossConsecutiveDays() = runTest {
    val today = FocusTimeUtils.todayKey()
    val yesterday = FocusTimeUtils.dateKeyDaysAgo(1)
    val now = System.currentTimeMillis()
    focusRepo.recordFocusSession(
      FocusSessionEntity(
        dateKey = today,
        sessionType = SessionType.STUDY.name,
        timerMode = TimerMode.POMODORO.name,
        plannedMinutes = 25,
        elapsedSeconds = 1500,
        completed = true,
        startedAtEpochMs = now - 1500_000,
        endedAtEpochMs = now,
      )
    )
    focusRepo.recordFocusSession(
      FocusSessionEntity(
        dateKey = yesterday,
        sessionType = SessionType.CODING.name,
        timerMode = TimerMode.COUNTDOWN.name,
        plannedMinutes = 40,
        elapsedSeconds = 2400,
        completed = true,
        startedAtEpochMs = now - 86_400_000,
        endedAtEpochMs = now - 84_000_000,
      )
    )

    val sessions = db.focusSessionDao().getAll()
    assertEquals(2, focusRepo.computeCurrentStreakDays(sessions))
  }

  @Test
  fun encryptedBackupAndRestore_roundTripsDatabaseRecords() = runTest {
    blockerRepo.saveProfile(
      BlockProfileEntity(name = "Night Study", presetType = "Study", blockedPackages = listOf("com.video.app"))
    )
    blockerRepo.saveAppLimit(
      AppLimitEntity("com.video.app", "Video App", dailyLimitMinutes = 20)
    )
    blockerRepo.saveWebsiteRule(WebsiteRuleEntity(domain = "example.com", category = "Test"))

    val encrypted = insightsRepo.exportEncryptedBackup("secret-pass-2026")
    assertTrue(encrypted.contains("focusnest-encrypted-backup"))

    insightsRepo.deleteAllUserData()
    assertTrue(db.blockProfileDao().getAll().isEmpty())

    val restoredCount = insightsRepo.restoreFromBackup(encrypted, "secret-pass-2026")
    assertTrue(restoredCount >= 3)
    assertEquals("Night Study", db.blockProfileDao().getAll().first().name)
    assertEquals("example.com", db.websiteRuleDao().getAll().first().domain)
  }

  @Test
  fun timerStatePersistence_survivesReload() = runTest {
    val saved =
      PersistedTimerState(
        isRunning = true,
        phase = TimerPhase.FOCUS,
        mode = TimerMode.POMODORO,
        sessionType = SessionType.CODING,
        plannedMinutes = 50,
        targetEndEpochMs = 1_900_000_000_000L,
        startedAtEpochMs = 1_899_997_000_000L,
        completedCycles = 1,
        sessionNote = "Kernel module",
      )
    prefs.saveTimerState(saved)
    val loaded = prefs.timerStateFlow.first()
    assertTrue(loaded.isRunning)
    assertEquals(SessionType.CODING, loaded.sessionType)
    assertEquals("Kernel module", loaded.sessionNote)
    assertEquals(1_900_000_000_000L, loaded.targetEndEpochMs)
  }

  @Test
  fun habitRepository_tracksStreaksAndWeeklyRatesLocally() = runTest {
    val id =
      habitRepo.saveHabit(
        HabitEntity(title = "Read 20 pages", frequencyType = "DAILY", targetCount = 20, unitLabel = "pages")
      )
    val habit = db.habitDao().getAllHabits().first { it.id == id }
    val today = FocusTimeUtils.todayKey()
    val yesterday = FocusTimeUtils.dateKeyDaysAgo(1)

    habitRepo.recordHabitProgress(habit, dateKey = today, delta = 20)
    habitRepo.recordHabitProgress(habit, dateKey = yesterday, delta = 20)

    val completions = db.habitDao().getAllCompletions()
    assertEquals(2, habitRepo.computeHabitStreakDays(habit, completions))
    val rates = habitRepo.weeklyCompletionRates(listOf(habit), completions)
    assertEquals(7, rates.size)
    assertEquals(100f, rates.last().second, 0.01f)

    insightsRepo.clearHabits()
    assertTrue(db.habitDao().getAllHabits().isEmpty())
  }

  @Test
  fun qrTokenSecurity_enforcesOneTimeConsumptionExpiryAndSignature() = runTest {
    val now = 1_750_000_000_000L
    blockerRepo.registerQrOrBarcodeToken(
      QrTokenEntity(
        tokenValue = "FOCUSNEST:ONE1",
        label = "OneTime",
        tokenFormat = "QR_CODE",
        createdAtEpochMs = now,
        isOneTime = true,
      )
    )
    val (firstTok, firstErr) = blockerRepo.verifyAndConsumeQrOrBarcodeToken("FOCUSNEST:ONE1", now + 1000L)
    assertNotNull(firstTok)
    assertNull(firstErr)

    val (replayTok, replayErr) = blockerRepo.verifyAndConsumeQrOrBarcodeToken("FOCUSNEST:ONE1", now + 2000L)
    assertNull(replayTok)
    assertTrue(replayErr?.contains("already been used") == true)

    blockerRepo.registerQrOrBarcodeToken(
      QrTokenEntity(
        tokenValue = "FOCUSNEST:EXP1",
        label = "Expiring",
        tokenFormat = "QR_CODE",
        createdAtEpochMs = now,
        expiresAtEpochMs = now + 10_000L,
      )
    )
    val (expiredTok, expiredErr) = blockerRepo.verifyAndConsumeQrOrBarcodeToken("FOCUSNEST:EXP1", now + 20_000L)
    assertNull(expiredTok)
    assertTrue(expiredErr?.contains("expired") == true)
  }

  @Test
  fun categoryLimitAndWarningThresholds_evaluateAccurately() = runTest {
    val pkg = "com.video.tube"
    val today = FocusTimeUtils.todayKey()
    db.installedAppDao().upsertAll(listOf(InstalledAppEntity(pkg, "Tube", "VIDEO")))
    db.appUsageDao().upsertAll(listOf(AppUsageEntity(today, pkg, "Tube", "VIDEO", foregroundTimeMs = 26 * 60_000L)))
    blockerRepo.saveAppLimit(AppLimitEntity(packageName = pkg, appName = "Tube", dailyLimitMinutes = 30))

    val firstWarnings = blockerRepo.evaluateAndMarkAppLimitWarnings()
    assertEquals(1, firstWarnings.size)
    assertEquals(LimitThresholdLevel.WARN_80, firstWarnings.first().second)

    // Repeat evaluation on same day must not re-fire WARN_80
    val duplicateCheck = blockerRepo.evaluateAndMarkAppLimitWarnings()
    assertTrue(duplicateCheck.isEmpty())

    // Category cap triggers block
    blockerRepo.saveCategoryLimit(CategoryLimitEntity(categoryName = "VIDEO", dailyLimitMinutes = 20))
    val decision = blockerRepo.evaluatePackageDecision(pkg)
    assertTrue(decision.isBlocked)
  }

  @Test
  fun roomMigration1To2_preservesExistingV1TablesAndAddsV2Schema() {
    val dbName = "migration_v1_v2_test.db"
    context.deleteDatabase(dbName)
    val config =
      androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
        .name(dbName)
        .callback(
          object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS `app_limits` (`packageName` TEXT NOT NULL, `appName` TEXT NOT NULL, `dailyLimitMinutes` INTEGER NOT NULL, `weeklyLimitMinutes` INTEGER NOT NULL, `isBlockedManually` INTEGER NOT NULL, `cooldownSeconds` INTEGER NOT NULL, `challengeType` TEXT NOT NULL, `tempUnlockUntilEpochMs` INTEGER, `updatedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`packageName`))"
              )
              db.execSQL(
                "INSERT INTO `app_limits` VALUES ('com.v1.app', 'Legacy App', 30, 0, 0, 15, 'REFLECTION_REMINDER', NULL, 1000)"
              )
              db.execSQL(
                "CREATE TABLE IF NOT EXISTS `qr_tokens` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tokenValue` TEXT NOT NULL, `label` TEXT NOT NULL, `tokenFormat` TEXT NOT NULL, `targetProfileId` INTEGER, `createdAtEpochMs` INTEGER NOT NULL)"
              )
            }

            override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
          }
        )
        .build()

    val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(config)
    val sqliteDb = helper.writableDatabase
    FocusNestDatabase.MIGRATION_1_2.migrate(sqliteDb)

    sqliteDb.query("SELECT appName, categoryName, gracePeriodMinutes FROM app_limits WHERE packageName='com.v1.app'").use { c ->
      assertTrue(c.moveToFirst())
      assertEquals("Legacy App", c.getString(0))
      assertEquals("", c.getString(1))
      assertEquals(0, c.getInt(2))
    }
    sqliteDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='habits'").use { c ->
      assertTrue(c.moveToFirst())
    }
    sqliteDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='category_limits'").use { c ->
      assertTrue(c.moveToFirst())
    }
    helper.close()
    context.deleteDatabase(dbName)
  }

  @Test
  fun v12PreferencesAndReportCard_persistAndRenderWellbeingSummary() = runTest {
    val today = FocusTimeUtils.todayKey()
    prefs.updateSettings {
      it.copy(
        ambientSoundType = com.example.domain.model.AmbientSoundType.SOFT_RAIN,
        ambientSoundVolume = 0.72f,
        launchPauseEnabled = true,
        launchPauseSeconds = 11,
        launchPausePackages = setOf("com.video.app"),
        windDownDateKey = today,
        windDownCompletedIds = setOf("charger_away", "grayscale_night"),
      )
    }
    val reloaded = prefs.settingsFlow.first()
    assertEquals(com.example.domain.model.AmbientSoundType.SOFT_RAIN, reloaded.ambientSoundType)
    assertEquals(0.72f, reloaded.ambientSoundVolume, 0.01f)
    assertTrue(reloaded.launchPauseEnabled)
    assertEquals(11, reloaded.launchPauseSeconds)
    assertTrue("com.video.app" in reloaded.launchPausePackages)
    assertEquals(setOf("charger_away", "grayscale_night"), reloaded.windDownCompletedIds)

    blockerRepo.recordMindfulLaunchProceed("com.video.app", "Video App", 11)
    val history = db.unlockDao().getRecentHistory()
    assertTrue(history.any { it.packageName == "com.video.app" && it.granted && it.intentionText.contains("11s") })

    focusRepo.recordFocusSession(
      FocusSessionEntity(
        dateKey = today,
        sessionType = SessionType.STUDY.name,
        timerMode = TimerMode.POMODORO.name,
        plannedMinutes = 25,
        elapsedSeconds = 1500,
        completed = true,
        note = "Architecture chapter",
        startedAtEpochMs = 1_000_000L,
        endedAtEpochMs = 2_500_000L,
      )
    )
    db.appUsageDao().upsertAll(
      listOf(AppUsageEntity(today, "com.video.app", "Video App", "VIDEO", foregroundTimeMs = 20 * 60_000L, launchCount = 4))
    )

    val html = insightsRepo.buildPrintableReportHtml(today, today)
    assertTrue(html.contains("Wellbeing Report Card"))
    assertTrue(html.contains("Category Distribution"))
    assertTrue(html.contains("Video App"))
    assertTrue(html.contains("Architecture chapter"))
  }
}
