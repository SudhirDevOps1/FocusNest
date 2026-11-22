package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.core.database.FocusNestDatabase
import com.example.core.datastore.FocusNestPreferences
import com.example.data.repository.BlockerRepository
import com.example.data.repository.FocusRepository
import com.example.data.repository.HabitRepository
import com.example.data.repository.InsightsAndBackupRepository
import com.example.data.repository.UsageStatsRepository
import com.example.service.FocusUsageMonitorWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
  val database: FocusNestDatabase = FocusNestDatabase.getInstance(context)
  val preferences: FocusNestPreferences = FocusNestPreferences(context)

  val usageStatsRepository: UsageStatsRepository =
    UsageStatsRepository(
      context = context,
      installedAppDao = database.installedAppDao(),
      appUsageDao = database.appUsageDao(),
      dailyAnalyticsDao = database.dailyAnalyticsDao(),
    )

  val blockerRepository: BlockerRepository =
    BlockerRepository(
      appLimitDao = database.appLimitDao(),
      appGroupDao = database.appGroupDao(),
      blockProfileDao = database.blockProfileDao(),
      blockingScheduleDao = database.blockingScheduleDao(),
      unlockDao = database.unlockDao(),
      websiteRuleDao = database.websiteRuleDao(),
      tokenVaultDao = database.tokenVaultDao(),
      shortFormRuleDao = database.shortFormRuleDao(),
      appUsageDao = database.appUsageDao(),
      dailyAnalyticsDao = database.dailyAnalyticsDao(),
      installedAppDao = database.installedAppDao(),
      preferences = preferences,
      categoryLimitDao = database.categoryLimitDao(),
    )

  val focusRepository: FocusRepository =
    FocusRepository(
      focusSessionDao = database.focusSessionDao(),
      focusGoalDao = database.focusGoalDao(),
      dailyAnalyticsDao = database.dailyAnalyticsDao(),
    )

  val habitRepository: HabitRepository =
    HabitRepository(
      habitDao = database.habitDao(),
    )

  val insightsRepository: InsightsAndBackupRepository =
    InsightsAndBackupRepository(
      appUsageDao = database.appUsageDao(),
      dailyAnalyticsDao = database.dailyAnalyticsDao(),
      focusSessionDao = database.focusSessionDao(),
      focusGoalDao = database.focusGoalDao(),
      appLimitDao = database.appLimitDao(),
      appGroupDao = database.appGroupDao(),
      blockProfileDao = database.blockProfileDao(),
      blockingScheduleDao = database.blockingScheduleDao(),
      websiteRuleDao = database.websiteRuleDao(),
      unlockDao = database.unlockDao(),
      reminderDao = database.reminderDao(),
      notificationRecordDao = database.notificationRecordDao(),
      preferences = preferences,
      habitDao = database.habitDao(),
      categoryLimitDao = database.categoryLimitDao(),
    )
}

class FocusNestApplication : Application() {
  lateinit var container: AppContainer
    private set

  private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
    createNotificationChannels()
    appScope.launch {
      container.focusRepository.ensureDefaultGoal()
      container.blockerRepository.ensureStarterConfigurations()
    }
    scheduleBackgroundMonitor()
  }

  private fun createNotificationChannels() {
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val timerChannel =
      NotificationChannel(
          CHANNEL_TIMER,
          "Focus Timer",
          NotificationManager.IMPORTANCE_LOW,
        )
        .apply { description = "Ongoing focus timer and break phase alerts." }
    val reminderChannel =
      NotificationChannel(
          CHANNEL_REMINDERS,
          "Mindful Reminders",
          NotificationManager.IMPORTANCE_DEFAULT,
        )
        .apply { description = "Break, bedtime, and 80%/90%/100% app usage limit warnings." }
    val vpnChannel =
      NotificationChannel(
          CHANNEL_VPN,
          "Local Website Filter",
          NotificationManager.IMPORTANCE_LOW,
        )
        .apply { description = "Status of the on-device local DNS website filter." }
    nm.createNotificationChannels(listOf(timerChannel, reminderChannel, vpnChannel))
  }

  private fun scheduleBackgroundMonitor() {
    runCatching {
      val request =
        PeriodicWorkRequestBuilder<FocusUsageMonitorWorker>(15, TimeUnit.MINUTES).build()
      WorkManager.getInstance(this)
        .enqueueUniquePeriodicWork(
          "focusnest_usage_monitor",
          ExistingPeriodicWorkPolicy.KEEP,
          request,
        )
    }
  }

  companion object {
    const val CHANNEL_TIMER = "focus_timer_channel"
    const val CHANNEL_REMINDERS = "mindful_reminders_channel"
    const val CHANNEL_VPN = "local_vpn_channel"
  }
}
