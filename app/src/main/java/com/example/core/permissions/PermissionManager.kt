package com.example.core.permissions

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.nfc.NfcAdapter
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.domain.model.PermissionSnapshot
import com.example.service.AppBlockerAccessibilityService
import com.example.service.FocusNestVpnService
import com.example.service.FocusNotificationService

object PermissionManager {
  fun getSnapshot(context: Context): PermissionSnapshot {
    val nfcAdapter = runCatching { NfcAdapter.getDefaultAdapter(context) }.getOrNull()
    val postNotifications =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      } else {
        true
      }
    val cameraGranted =
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

    return PermissionSnapshot(
      usageStatsGranted = hasUsageStatsPermission(context),
      accessibilityEnabled = isAccessibilityServiceEnabled(context),
      notificationListenerEnabled = isNotificationListenerEnabled(context),
      vpnPrepared = isVpnPrepared(context),
      vpnActive = FocusNestVpnService.isRunning,
      postNotificationsGranted = postNotifications,
      cameraGranted = cameraGranted,
      dndPolicyAccessGranted = isDndPolicyAccessGranted(context),
      ignoringBatteryOptimizations = isIgnoringBatteryOptimizations(context),
      nfcSupported = nfcAdapter != null,
      nfcEnabled = nfcAdapter?.isEnabled == true,
    )
  }

  fun hasUsageStatsPermission(context: Context): Boolean {
    return try {
      val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
      val mode =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
          )
        } else {
          @Suppress("DEPRECATION")
          appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
          )
        }
      mode == AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) {
      false
    }
  }

  fun isAccessibilityServiceEnabled(context: Context): Boolean {
    return try {
      val expected = ComponentName(context, AppBlockerAccessibilityService::class.java)
      val enabled =
        Settings.Secure.getString(
          context.contentResolver,
          Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
      enabled
        .split(':')
        .mapNotNull { ComponentName.unflattenFromString(it) }
        .any { it == expected }
    } catch (_: Exception) {
      false
    }
  }

  fun isNotificationListenerEnabled(context: Context): Boolean {
    return try {
      val expected = ComponentName(context, FocusNotificationService::class.java)
      val enabled =
        Settings.Secure.getString(
          context.contentResolver,
          "enabled_notification_listeners",
        ) ?: return false
      enabled
        .split(':')
        .mapNotNull { ComponentName.unflattenFromString(it) }
        .any { it == expected }
    } catch (_: Exception) {
      false
    }
  }

  fun isVpnPrepared(context: Context): Boolean {
    return try {
      VpnService.prepare(context) == null
    } catch (_: Exception) {
      false
    }
  }

  fun isDndPolicyAccessGranted(context: Context): Boolean {
    return try {
      val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      nm?.isNotificationPolicyAccessGranted == true
    } catch (_: Exception) {
      false
    }
  }

  fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    return try {
      val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
      pm?.isIgnoringBatteryOptimizations(context.packageName) == true
    } catch (_: Exception) {
      false
    }
  }

  fun usageAccessIntent(context: Context): Intent =
    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
      data = Uri.fromParts("package", context.packageName, null)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

  fun accessibilitySettingsIntent(): Intent =
    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

  fun notificationListenerSettingsIntent(): Intent =
    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

  fun dndPolicySettingsIntent(): Intent =
    Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

  fun batteryOptimizationIntent(
    @Suppress("UNUSED_PARAMETER") context: Context,
  ): Intent =
    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

  fun nfcSettingsIntent(): Intent =
    Intent(Settings.ACTION_NFC_SETTINGS).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

  fun displaySettingsIntent(): Intent =
    Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

  fun nightDisplaySettingsIntent(): Intent =
    Intent(Settings.ACTION_NIGHT_DISPLAY_SETTINGS).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

  fun appDetailsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      data = Uri.fromParts("package", context.packageName, null)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

  fun openExternalUrl(context: Context, url: String): Boolean {
    return try {
      val intent =
        Intent(Intent.ACTION_VIEW, url.toUri()).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      context.startActivity(intent)
      true
    } catch (_: Exception) {
      false
    }
  }

  fun safeStartSettings(context: Context, primaryIntent: Intent, fallbackIntent: Intent? = null) {
    try {
      context.startActivity(primaryIntent)
    } catch (_: Exception) {
      try {
        context.startActivity(fallbackIntent ?: appDetailsIntent(context))
      } catch (_: Exception) {
        // Ignore on headless/restricted test environments
      }
    }
  }
}
