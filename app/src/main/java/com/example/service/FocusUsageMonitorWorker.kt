package com.example.service

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.FocusNestApplication
import com.example.MainActivity
import com.example.R
import com.example.domain.model.LimitThresholdLevel
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FocusUsageMonitorWorker(
  appContext: Context,
  params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
  override suspend fun doWork(): Result {
    val app = applicationContext as? FocusNestApplication ?: return Result.success()
    app.container.usageStatsRepository.syncTodayUsageFromSystem()
    WidgetUpdater.refreshAll(applicationContext)

    val warnings = app.container.blockerRepository.evaluateAndMarkAppLimitWarnings()
    warnings.forEachIndexed { idx, (limit, level) ->
      val pct =
        when (level) {
          LimitThresholdLevel.WARN_80 -> "80%"
          LimitThresholdLevel.WARN_90 -> "90%"
          LimitThresholdLevel.WARN_100 -> "100%"
          LimitThresholdLevel.NONE -> ""
        }
      if (pct.isNotEmpty()) {
        postGentleReminder(
          context = applicationContext,
          notificationId = 4020 + idx,
          title = "${limit.appName} reached $pct of daily limit",
          body = "Configured daily cap: ${limit.dailyLimitMinutes}m. Consider winding down.",
        )
      }
    }

    val settings = app.container.preferences.settingsFlow.first()
    if (settings.breakReminderEnabled) {
      postGentleReminder(
        context = applicationContext,
        notificationId = 4010,
        title = "Mindful Break Check-In",
        body = "Step back for 60 seconds, stretch your shoulders, and rest your eyes.",
      )
    }
    return Result.success()
  }

  private fun postGentleReminder(
    context: Context,
    notificationId: Int = 4010,
    title: String,
    body: String,
  ) {
    runCatching {
      val intent =
        PendingIntent.getActivity(
          context,
          notificationId,
          Intent(context, MainActivity::class.java),
          PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
      val notif =
        NotificationCompat.Builder(context, FocusNestApplication.CHANNEL_REMINDERS)
          .setSmallIcon(R.drawable.ic_launcher_monochrome)
          .setContentTitle(title)
          .setContentText(body)
          .setAutoCancel(true)
          .setContentIntent(intent)
          .build()
      if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
          ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
      ) {
        return
      }
      NotificationManagerCompat.from(context).notify(notificationId, notif)
    }
  }
}

class BootCompletedReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    val action = intent?.action ?: return
    if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
      val pendingResult: PendingResult? = goAsync()
      CoroutineScope(Dispatchers.IO).launch {
        try {
          val app = context.applicationContext as? FocusNestApplication
          if (app != null) {
            val timer = app.container.preferences.timerStateFlow.first()
            if (timer.isRunning && timer.phase.name != "IDLE") {
              FocusTimerService.sendAction(context, FocusTimerService.ACTION_RESTORE)
            }
            WidgetUpdater.refreshAll(context)
          }
        } finally {
          pendingResult?.finish()
        }
      }
    }
  }
}
