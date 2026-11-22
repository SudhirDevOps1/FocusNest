package com.example.core.permissions

import android.Manifest
import android.annotation.SuppressLint
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
import com.example.domain.model.VpnConnectionState
import com.example.service.AppBlockerAccessibilityService
import com.example.service.FocusNestVpnService
import com.example.service.FocusNotificationService

enum class PermissionKind {
  RUNTIME,
  SPECIAL_ACCESS,
  SYSTEM_CONSENT,
}

data class PermissionDiagnosticItem(
  val id: String,
  val title: String,
  val summary: String,
  val kind: PermissionKind,
  val granted: Boolean,
  val wasRevoked: Boolean = false,
  val androidNote: String? = null,
)

/**
 * Centralized PermissionStatusManager + backward-compatible PermissionManager facade.
 * Separates RUNTIME permissions (Camera, Post Notifications) from SPECIAL_ACCESS system settings
 * (UsageStats, Accessibility, NotificationListener, DND, Battery Optimization, Local VPN consent),
 * detects revocations across resumes, and provides reliable OEM-safe settings & fallback intents.
 */
object PermissionStatusManager {
  fun inspect(context: Context, previous: PermissionSnapshot? = null): PermissionSnapshot {
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

    val usageGranted = PermissionManager.hasUsageStatsPermission(context)
    val accessibilityGranted = PermissionManager.isAccessibilityServiceEnabled(context)
    val notificationListenerGranted = PermissionManager.isNotificationListenerEnabled(context)
    val vpnPrepared = PermissionManager.isVpnPrepared(context)
    val vpnState = FocusNestVpnService.currentState
    val vpnActive = FocusNestVpnService.isRunning || vpnState == VpnConnectionState.CONNECTED
    val dndGranted = PermissionManager.isDndPolicyAccessGranted(context)
    val batteryIgnored = PermissionManager.isIgnoringBatteryOptimizations(context)

    val current =
      PermissionSnapshot(
        usageStatsGranted = usageGranted,
        accessibilityEnabled = accessibilityGranted,
        notificationListenerEnabled = notificationListenerGranted,
        vpnPrepared = vpnPrepared,
        vpnActive = vpnActive,
        postNotificationsGranted = postNotifications,
        cameraGranted = cameraGranted,
        dndPolicyAccessGranted = dndGranted,
        ignoringBatteryOptimizations = batteryIgnored,
        nfcSupported = nfcAdapter != null,
        nfcEnabled = nfcAdapter?.isEnabled == true,
        vpnState = vpnState,
      )

    val revoked = detectRevocations(previous, current)
    return current.copy(revokedPermissions = revoked)
  }

  fun detectRevocations(
    previous: PermissionSnapshot?,
    current: PermissionSnapshot,
  ): Set<String> {
    if (previous == null) return emptySet()
    return buildSet {
      if (previous.usageStatsGranted && !current.usageStatsGranted) add("Usage Access")
      if (previous.accessibilityEnabled && !current.accessibilityEnabled) add("Accessibility Service")
      if (previous.notificationListenerEnabled && !current.notificationListenerEnabled) add("Notification Listener")
      if (previous.postNotificationsGranted && !current.postNotificationsGranted) add("Post Notifications")
      if (previous.cameraGranted && !current.cameraGranted) add("Camera")
      if (previous.dndPolicyAccessGranted && !current.dndPolicyAccessGranted) add("Do Not Disturb Policy")
      if (previous.ignoringBatteryOptimizations && !current.ignoringBatteryOptimizations) add("Unrestricted Battery")
      if (previous.vpnActive && !current.vpnActive) add("Local VPN Website Filter")
    }
  }

  fun buildDiagnostics(snapshot: PermissionSnapshot): List<PermissionDiagnosticItem> {
    val android13Note =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        "On sideloaded APKs (Android 13+), open App Info -> 3-dot menu -> Allow restricted settings if the toggle is grayed out."
      } else {
        null
      }
    return listOf(
      PermissionDiagnosticItem(
        id = "usage_access",
        title = "Usage Access (UsageStatsManager)",
        summary = "Measures real foreground app time and daily/weekly limits.",
        kind = PermissionKind.SPECIAL_ACCESS,
        granted = snapshot.usageStatsGranted,
        wasRevoked = "Usage Access" in snapshot.revokedPermissions,
      ),
      PermissionDiagnosticItem(
        id = "accessibility",
        title = "Accessibility Service (App & Reels Shield)",
        summary = "Optional real-time foreground app and short-form video blocker.",
        kind = PermissionKind.SPECIAL_ACCESS,
        granted = snapshot.accessibilityEnabled,
        wasRevoked = "Accessibility Service" in snapshot.revokedPermissions,
        androidNote = android13Note,
      ),
      PermissionDiagnosticItem(
        id = "vpn_filter",
        title = "Local VPN Website Filter",
        summary = "On-device DNS sinkhole (${snapshot.vpnState.label}); zero remote servers.",
        kind = PermissionKind.SYSTEM_CONSENT,
        granted = snapshot.vpnPrepared && snapshot.vpnActive,
        wasRevoked = "Local VPN Website Filter" in snapshot.revokedPermissions,
      ),
      PermissionDiagnosticItem(
        id = "notifications_listener",
        title = "Notification Listener (Focus Muting)",
        summary = "Optional muting and on-device digest during Focus or Bedtime.",
        kind = PermissionKind.SPECIAL_ACCESS,
        granted = snapshot.notificationListenerEnabled,
        wasRevoked = "Notification Listener" in snapshot.revokedPermissions,
        androidNote = android13Note,
      ),
      PermissionDiagnosticItem(
        id = "dnd_policy",
        title = "Do Not Disturb Policy Access",
        summary = "Toggles scheduled priority DND during Bedtime Wind-Down.",
        kind = PermissionKind.SPECIAL_ACCESS,
        granted = snapshot.dndPolicyAccessGranted,
        wasRevoked = "Do Not Disturb Policy" in snapshot.revokedPermissions,
      ),
      PermissionDiagnosticItem(
        id = "post_notifications",
        title = "Post Notifications",
        summary = "Displays Focus timer notifications and usage threshold alerts.",
        kind = PermissionKind.RUNTIME,
        granted = snapshot.postNotificationsGranted,
        wasRevoked = "Post Notifications" in snapshot.revokedPermissions,
      ),
      PermissionDiagnosticItem(
        id = "camera",
        title = "Camera (QR & Barcode Unlock Scanner)",
        summary = "Scans physical unlock tokens offline via CameraX + ZXing.",
        kind = PermissionKind.RUNTIME,
        granted = snapshot.cameraGranted,
        wasRevoked = "Camera" in snapshot.revokedPermissions,
      ),
      PermissionDiagnosticItem(
        id = "battery",
        title = "Unrestricted Battery Optimization",
        summary = "Prevents OEM background killers (Realme/ColorOS/MIUI/OneUI) from stopping timers.",
        kind = PermissionKind.SPECIAL_ACCESS,
        granted = snapshot.ignoringBatteryOptimizations,
        wasRevoked = "Unrestricted Battery" in snapshot.revokedPermissions,
      ),
    )
  }
}

object PermissionManager {
  fun getSnapshot(context: Context, previous: PermissionSnapshot? = null): PermissionSnapshot =
    PermissionStatusManager.inspect(context, previous)

  fun hasUsageStatsPermission(context: Context): Boolean {
    return try {
      val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
      val mode =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          @Suppress("DEPRECATION")
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

  @SuppressLint("BatteryLife")
  fun batteryOptimizationIntent(context: Context): Intent =
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

  fun safeStartSettings(
    context: Context,
    primaryIntent: Intent,
    fallbackIntent: Intent? = null,
  ): Boolean {
    return try {
      context.startActivity(primaryIntent)
      true
    } catch (_: Exception) {
      try {
        context.startActivity(fallbackIntent ?: appDetailsIntent(context))
        true
      } catch (_: Exception) {
        false
      }
    }
  }
}
