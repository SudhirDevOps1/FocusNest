package com.example.service

import android.app.Notification
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.FocusNestApplication
import com.example.core.database.NotificationRecordEntity
import com.example.domain.model.FocusTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Optional local-only NotificationListenerService.
 * Stores notification metadata strictly on-device when enabled by the user.
 */
class FocusNotificationService : NotificationListenerService() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    val item = sbn ?: return
    if (item.packageName == packageName || item.isOngoing) return
    val app = applicationContext as? FocusNestApplication ?: return

    serviceScope.launch {
      val settings = app.container.preferences.settingsFlow.first()
      if (!settings.notificationFilterEnabled) return@launch

      val timer = app.container.preferences.timerStateFlow.first()
      val activeProfiles = app.container.database.blockProfileDao().getAll().filter { it.isActive }
      val shouldMute =
        (timer.isRunning && timer.phase.name == "FOCUS") ||
          activeProfiles.any { it.muteNotifications && item.packageName in it.blockedPackages }

      val title = item.notification.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
      val label = resolveLabel(item.packageName)

      app.container.database.notificationRecordDao().insert(
        NotificationRecordEntity(
          dateKey = FocusTimeUtils.todayKey(),
          packageName = item.packageName,
          appName = label,
          titleSummary = title.take(64),
          wasMuted = shouldMute,
          postedAtEpochMs = item.postTime,
        )
      )

      if (shouldMute) {
        runCatching { cancelNotification(item.key) }
      }
    }
  }

  private fun resolveLabel(pkg: String): String {
    return try {
      val info =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          packageManager.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
        } else {
          @Suppress("DEPRECATION")
          packageManager.getApplicationInfo(pkg, 0)
        }
      packageManager.getApplicationLabel(info).toString()
    } catch (_: Exception) {
      pkg.substringAfterLast('.')
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    serviceScope.cancel()
  }
}
