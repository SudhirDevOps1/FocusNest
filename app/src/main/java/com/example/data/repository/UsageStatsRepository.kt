package com.example.data.repository

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.core.database.AppUsageDao
import com.example.core.database.AppUsageEntity
import com.example.core.database.DailyAnalyticsDao
import com.example.core.database.DailyAnalyticsEntity
import com.example.core.database.InstalledAppDao
import com.example.core.database.InstalledAppEntity
import com.example.core.permissions.PermissionManager
import com.example.domain.model.AppCategory
import com.example.domain.model.FocusTimeUtils
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class UsageStatsRepository(
  private val context: Context,
  private val installedAppDao: InstalledAppDao,
  private val appUsageDao: AppUsageDao,
  private val dailyAnalyticsDao: DailyAnalyticsDao,
) {
  val installedApps: Flow<List<InstalledAppEntity>> = installedAppDao.observeAll()

  fun observeUsageForDate(dateKey: String): Flow<List<AppUsageEntity>> =
    appUsageDao.observeUsageForDate(dateKey)

  fun observeUsageInRange(startDateKey: String, endDateKey: String): Flow<List<AppUsageEntity>> =
    appUsageDao.observeUsageInRange(startDateKey, endDateKey)

  suspend fun setAppDistracting(packageName: String, distracting: Boolean) {
    installedAppDao.setDistracting(packageName, distracting)
  }

  suspend fun setAppEmergencyWhitelisted(packageName: String, whitelisted: Boolean) {
    installedAppDao.setEmergencyWhitelisted(packageName, whitelisted)
  }

  suspend fun syncInstalledApps(): List<InstalledAppEntity> =
    withContext(Dispatchers.IO) {
      val pm = context.packageManager
      val launcherIntent =
        Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
      val resolveInfos =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          pm.queryIntentActivities(
            launcherIntent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
          )
        } else {
          @Suppress("DEPRECATION")
          pm.queryIntentActivities(launcherIntent, 0)
        }

      val existingMap = installedAppDao.getAll().associateBy { it.packageName }
      val selfPackage = context.packageName
      val now = System.currentTimeMillis()
      val discovered =
        resolveInfos
          .mapNotNull { info ->
            val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
            if (pkg == selfPackage) return@mapNotNull null
            val appInfo = info.activityInfo.applicationInfo
            val label = info.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: pkg
            val existing = existingMap[pkg]
            val category = inferCategory(pkg, appInfo)
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            InstalledAppEntity(
              packageName = pkg,
              appName = label,
              category = category.name,
              isSystemApp = isSystem,
              isSelectedDistracting = existing?.isSelectedDistracting ?: false,
              isEmergencyWhitelisted =
                existing?.isEmergencyWhitelisted ?: isDefaultSafeApp(pkg),
              lastSeenEpochMs = now,
            )
          }
          .distinctBy { it.packageName }
          .sortedBy { it.appName.lowercase() }

      if (discovered.isNotEmpty()) {
        installedAppDao.upsertAll(discovered)
      }
      discovered
    }

  /**
   * Synchronizes real UsageStatsManager records for today (and recent days if requested).
   * Never fabricates usage data when PACKAGE_USAGE_STATS is not granted or unavailable.
   */
  suspend fun syncTodayUsageFromSystem(): Boolean =
    withContext(Dispatchers.IO) {
      if (!PermissionManager.hasUsageStatsPermission(context)) {
        return@withContext false
      }
      val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
          ?: return@withContext false

      val zone = ZoneId.systemDefault()
      val today = LocalDate.now(zone)
      val startOfDayMs = today.atStartOfDay(zone).toInstant().toEpochMilli()
      val nowMs = System.currentTimeMillis()
      val dateKey = FocusTimeUtils.todayKey(nowMs)

      val installedByPkg =
        (installedAppDao.getAll().ifEmpty { syncInstalledApps() }).associateBy { it.packageName }

      // Compute foreground durations & launch counts directly from UsageEvents when available,
      // falling back to queryUsageStats for OEMs with sparse event buffers.
      val eventsMap = mutableMapOf<String, MutableAppStats>()
      val usageEvents = runCatching { usageStatsManager.queryEvents(startOfDayMs, nowMs) }.getOrNull()
      if (usageEvents != null) {
        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
          usageEvents.getNextEvent(event)
          val pkg = event.packageName ?: continue
          if (pkg == context.packageName) continue
          val stats = eventsMap.getOrPut(pkg) { MutableAppStats() }
          when (event.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> {
              stats.launchCount += 1
              stats.activeStartMs = event.timeStamp
              if (stats.firstUsedMs == null) stats.firstUsedMs = event.timeStamp
              stats.lastUsedMs = event.timeStamp
            }
            UsageEvents.Event.ACTIVITY_PAUSED,
            UsageEvents.Event.ACTIVITY_STOPPED -> {
              val started = stats.activeStartMs
              if (started != null && event.timeStamp >= started) {
                stats.foregroundMs += (event.timeStamp - started)
                stats.activeStartMs = null
              }
              stats.lastUsedMs = maxOf(stats.lastUsedMs ?: 0L, event.timeStamp)
            }
          }
        }
        // Close any currently open foreground app up to nowMs
        eventsMap.values.forEach { stats ->
          val open = stats.activeStartMs
          if (open != null && nowMs >= open) {
            stats.foregroundMs += (nowMs - open)
          }
        }
      }

      val aggregatedStats =
        runCatching {
            usageStatsManager.queryUsageStats(
              UsageStatsManager.INTERVAL_DAILY,
              startOfDayMs,
              nowMs,
            )
          }
          .getOrNull()
          .orEmpty()

      val mergedPackages = (eventsMap.keys + aggregatedStats.map { it.packageName }).toSet()
      val entries =
        mergedPackages
          .filter { it != context.packageName }
          .mapNotNull { pkg ->
            val eventStat = eventsMap[pkg]
            val sysStat = aggregatedStats.firstOrNull { it.packageName == pkg }
            val fgMs =
              maxOf(
                eventStat?.foregroundMs ?: 0L,
                sysStat?.totalTimeInForeground ?: 0L,
              )
            if (fgMs < 1_000L && (eventStat?.launchCount ?: 0) == 0) return@mapNotNull null

            val installed = installedByPkg[pkg]
            val appName = installed?.appName ?: resolveAppLabel(pkg)
            val category = installed?.category ?: inferCategory(pkg, null).name
            AppUsageEntity(
              dateKey = dateKey,
              packageName = pkg,
              appName = appName,
              category = category,
              foregroundTimeMs = fgMs,
              launchCount = eventStat?.launchCount ?: 0,
              firstUsedEpochMs = eventStat?.firstUsedMs ?: sysStat?.firstTimeStamp,
              lastUsedEpochMs = eventStat?.lastUsedMs ?: sysStat?.lastTimeUsed,
              isMeasured = true,
            )
          }
          .sortedByDescending { it.foregroundTimeMs }

      if (entries.isNotEmpty()) {
        appUsageDao.upsertAll(entries)
      }

      val existingAnalytics = dailyAnalyticsDao.getByDate(dateKey) ?: DailyAnalyticsEntity(dateKey)
      dailyAnalyticsDao.upsert(
        existingAnalytics.copy(
          totalScreenTimeMs = entries.sumOf { it.foregroundTimeMs },
          isMeasuredFromUsageStats = true,
          updatedAtEpochMs = nowMs,
        )
      )
      true
    }

  private fun resolveAppLabel(packageName: String): String {
    return try {
      val pm = context.packageManager
      val info = pm.getApplicationInfo(packageName, 0)
      pm.getApplicationLabel(info).toString()
    } catch (_: Exception) {
      packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }
  }

  private fun isDefaultSafeApp(pkg: String): Boolean {
    val lower = pkg.lowercase()
    return lower.contains("dialer") ||
      lower.contains("phone") ||
      lower.contains("contacts") ||
      lower.contains("emergency") ||
      lower.contains("settings")
  }

  private fun inferCategory(pkg: String, appInfo: ApplicationInfo?): AppCategory {
    if (appInfo != null) {
      when (appInfo.category) {
        ApplicationInfo.CATEGORY_SOCIAL -> return AppCategory.SOCIAL
        ApplicationInfo.CATEGORY_VIDEO -> return AppCategory.VIDEO
        ApplicationInfo.CATEGORY_GAME -> return AppCategory.GAMING
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> return AppCategory.PRODUCTIVITY
        ApplicationInfo.CATEGORY_AUDIO,
        ApplicationInfo.CATEGORY_IMAGE,
        ApplicationInfo.CATEGORY_MAPS,
        ApplicationInfo.CATEGORY_NEWS,
        ApplicationInfo.CATEGORY_ACCESSIBILITY,
        ApplicationInfo.CATEGORY_UNDEFINED -> Unit
        else -> Unit
      }
    }
    val lower = pkg.lowercase()
    return when {
      lower.contains("instagram") ||
        lower.contains("tiktok") ||
        lower.contains("musically") ||
        lower.contains("facebook") ||
        lower.contains("twitter") ||
        lower.contains("reddit") ||
        lower.contains("snapchat") -> AppCategory.SOCIAL
      lower.contains("youtube") || lower.contains("netflix") || lower.contains("twitch") ->
        AppCategory.VIDEO
      lower.contains("whatsapp") ||
        lower.contains("telegram") ||
        lower.contains("discord") ||
        lower.contains("messaging") -> AppCategory.COMMUNICATION
      lower.contains("chrome") || lower.contains("firefox") || lower.contains("browser") ->
        AppCategory.BROWSER
      lower.contains("docs") || lower.contains("notion") || lower.contains("calendar") ->
        AppCategory.PRODUCTIVITY
      lower.contains("duolingo") || lower.contains("anki") || lower.contains("khan") ->
        AppCategory.EDUCATION
      else -> AppCategory.OTHER
    }
  }

  private data class MutableAppStats(
    var foregroundMs: Long = 0L,
    var launchCount: Int = 0,
    var firstUsedMs: Long? = null,
    var lastUsedMs: Long? = null,
    var activeStartMs: Long? = null,
  )
}
