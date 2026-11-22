package com.example.feature.insights

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.database.AppUsageEntity
import com.example.core.database.DailyAnalyticsEntity
import com.example.core.database.FocusGoalEntity
import com.example.core.database.FocusSessionEntity
import com.example.core.database.UnlockHistoryEntity
import com.example.core.design.AdaptiveTwoPane
import com.example.core.design.BarChartCard
import com.example.core.design.FocusNestEmptyState
import com.example.core.design.InstalledAppIcon
import com.example.core.design.MeasuredBadge
import com.example.core.design.MetricTile
import com.example.core.design.SanctuaryCard
import com.example.core.design.SectionHeader
import com.example.domain.model.FocusTimeUtils
import com.example.domain.model.PermissionSnapshot
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
  permissions: PermissionSnapshot,
  todayUsage: List<AppUsageEntity>,
  weeklyUsage: List<AppUsageEntity>,
  monthlyUsage: List<AppUsageEntity>,
  focusSessions: List<FocusSessionEntity>,
  focusGoal: FocusGoalEntity,
  dailyAnalytics: List<DailyAnalyticsEntity>,
  unlockHistory: List<UnlockHistoryEntity>,
  streakDays: Int,
  onComputeBestPeriod: suspend (List<FocusSessionEntity>) -> String,
  onBuildCsv: suspend (String, String) -> String,
  onBuildJson: suspend () -> String,
  onBuildPrintableHtml: suspend (String, String) -> String,
  onShowMessage: (String) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var rangeTab by rememberSaveable { mutableIntStateOf(1) } // 0=Daily, 1=Weekly, 2=Monthly, 3=Custom
  var customDays by rememberSaveable { mutableIntStateOf(14) }
  var bestPeriodLabel by remember { mutableStateOf("Calculating…") }

  LaunchedEffect(focusSessions) {
    bestPeriodLabel = onComputeBestPeriod(focusSessions)
  }

  val startDateKey =
    when (rangeTab) {
      0 -> FocusTimeUtils.todayKey()
      1 -> FocusTimeUtils.dateKeyDaysAgo(6)
      2 -> FocusTimeUtils.dateKeyDaysAgo(29)
      else -> FocusTimeUtils.dateKeyDaysAgo(customDays - 1)
    }
  val endDateKey = FocusTimeUtils.todayKey()

  val activeUsage =
    when (rangeTab) {
      0 -> todayUsage
      1 -> weeklyUsage
      else -> monthlyUsage.filter { it.dateKey >= startDateKey }
    }

  val aggregatedByApp =
    remember(activeUsage) {
      activeUsage
        .groupBy { it.packageName }
        .map { (_, list) ->
          val first = list.first()
          first.copy(
            foregroundTimeMs = list.sumOf { it.foregroundTimeMs },
            launchCount = list.sumOf { it.launchCount },
          )
        }
        .sortedByDescending { it.foregroundTimeMs }
    }

  val categoryBreakdown =
    remember(activeUsage) {
      val total = activeUsage.sumOf { it.foregroundTimeMs }.coerceAtLeast(1L)
      activeUsage
        .groupBy { it.category }
        .map { (cat, list) ->
          val ms = list.sumOf { it.foregroundTimeMs }
          Triple(cat, ms, ms.toFloat() / total.toFloat())
        }
        .sortedByDescending { it.second }
    }

  val filteredSessions =
    remember(focusSessions, startDateKey) {
      focusSessions.filter { it.dateKey >= startDateKey }
    }
  val filteredAnalytics =
    remember(dailyAnalytics, startDateKey) {
      dailyAnalytics.filter { it.dateKey >= startDateKey }
    }

  val totalScreenMinutes = (activeUsage.sumOf { it.foregroundTimeMs } / 60_000L).toInt()
  val totalFocusMinutes = (filteredSessions.sumOf { it.elapsedSeconds } / 60L).toInt()
  val longestSessionMinutes =
    remember(filteredSessions) { ((filteredSessions.maxOfOrNull { it.elapsedSeconds } ?: 0L) / 60L).toInt() }
  val bestFocusDay =
    remember(filteredSessions) {
      filteredSessions
        .groupBy { it.dateKey }
        .maxByOrNull { (_, list) -> list.sumOf { it.elapsedSeconds } }
        ?.let { (day, list) -> "$day (${(list.sumOf { it.elapsedSeconds } / 60L)}m)" }
    }
  val blockedTotal = filteredAnalytics.sumOf { it.blockedAttemptsCount }
  val unlockTotal = unlockHistory.count { it.dateKey >= startDateKey && it.granted }

  val csvSaver =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
      if (uri != null) {
        scope.launch {
          val csv = onBuildCsv(startDateKey, endDateKey)
          context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
          onShowMessage("CSV report exported.")
        }
      }
    }

  val jsonSaver =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
      if (uri != null) {
        scope.launch {
          val json = onBuildJson()
          context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
          onShowMessage("JSON export saved.")
        }
      }
    }

  val chartBars =
    remember(activeUsage, rangeTab) {
      val byDay = activeUsage.groupBy { it.dateKey }
      val count = if (rangeTab == 0) 1 else 7
      (count - 1 downTo 0).map { ago ->
        val d = LocalDate.now().minusDays(ago.toLong())
        val mins = (byDay[d.toString()]?.sumOf { it.foregroundTimeMs } ?: 0L) / 60_000f
        d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) to mins
      }
    }

  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text("Insights & Reports", style = MaterialTheme.typography.headlineMedium)
        Text(
          "Actual device telemetry & focus records",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      MeasuredBadge(isMeasured = permissions.usageStatsGranted)
    }

    PrimaryTabRow(selectedTabIndex = rangeTab) {
      listOf("Daily", "Weekly", "Monthly", "Custom").forEachIndexed { idx, label ->
        Tab(
          selected = rangeTab == idx,
          onClick = { rangeTab = idx },
          text = { Text(label) },
          modifier = Modifier.testTag("insights_tab_$idx"),
        )
      }
    }

    val goalPct =
      if (focusGoal.dailyFocusGoalMinutes > 0) {
        ((totalFocusMinutes.toFloat() / focusGoal.dailyFocusGoalMinutes.toFloat()) * 100).toInt().coerceAtMost(100)
      } else 0

    AdaptiveTwoPane(
      modifier = Modifier.weight(1f),
      primaryPane = {
        if (rangeTab == 3) {
          item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf(3, 7, 14, 30).forEach { days ->
                FilterChip(
                  selected = customDays == days,
                  onClick = { customDays = days },
                  label = { Text("Last $days days") },
                )
              }
            }
          }
        }

        item {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
              label = "Screen Time",
              value = FocusTimeUtils.formatDurationMinutes(totalScreenMinutes),
              icon = Icons.Default.QueryStats,
              modifier = Modifier.weight(1f),
            )
            MetricTile(
              label = "Focus Time",
              value = FocusTimeUtils.formatDurationMinutes(totalFocusMinutes),
              icon = Icons.Default.Timer,
              modifier = Modifier.weight(1f),
            )
          }
        }

        item {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
              label = "Blocked",
              value = "$blockedTotal",
              icon = Icons.Default.Block,
              tint = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.weight(1f),
            )
            MetricTile(
              label = "Unlocks / Streak",
              value = "$unlockTotal • ${streakDays}d",
              icon = Icons.Default.LocalFireDepartment,
              modifier = Modifier.weight(1f),
            )
          }
        }

        item {
          SanctuaryCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text("Best Focus Period", style = MaterialTheme.typography.titleMedium)
              Text(
                bestPeriodLabel,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                "Focus Goal Attainment: $goalPct% of daily target (${focusGoal.dailyFocusGoalMinutes}m)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              if (longestSessionMinutes > 0) {
                Text(
                  "Longest Session: ${longestSessionMinutes}m" +
                    if (bestFocusDay != null) " • Best Day: $bestFocusDay" else "",
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }

        item {
          SanctuaryCard(highlight = true) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text("Wellbeing Report Card ($startDateKey – $endDateKey)", style = MaterialTheme.typography.titleMedium)
              Text(
                "Generate an offline printable or PDF summary of focus sessions, goal attainment ($goalPct%), categories, and mindful reflections—or export raw CSV / JSON.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Button(
                onClick = {
                  scope.launch {
                    val html = onBuildPrintableHtml(startDateKey, endDateKey)
                    printHtmlReport(context, html)
                  }
                },
                modifier = Modifier.fillMaxWidth().testTag("insights_print_btn"),
              ) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Print / Save PDF Report Card")
              }
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                  onClick = { csvSaver.launch("focusnest_report_$endDateKey.csv") },
                  modifier = Modifier.weight(1f).testTag("insights_export_csv_btn"),
                ) {
                  Icon(Icons.Default.Download, contentDescription = null)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("CSV")
                }
                OutlinedButton(
                  onClick = { jsonSaver.launch("focusnest_data_$endDateKey.json") },
                  modifier = Modifier.weight(1f).testTag("insights_export_json_btn"),
                ) {
                  Text("JSON")
                }
              }
            }
          }
        }
      },
      secondaryPane = {
        item {
          BarChartCard(
            title = "Screen-Time Distribution",
            subtitle = "$startDateKey to $endDateKey",
            entries = chartBars,
            valueFormatter = { "${it.toInt()}m" },
          )
        }

        item {
          SectionHeader("Category Breakdown", subtitle = "Share of measured foreground time")
        }

        if (categoryBreakdown.isEmpty()) {
          item {
            FocusNestEmptyState(
              icon = Icons.Default.QueryStats,
              title = "No Category Data Available",
              message = "Grant Usage Access and use your apps to see measured category breakdowns here.",
              illustrationRes = R.drawable.ic_empty_state,
            )
          }
        } else {
          items(categoryBreakdown, key = { it.first }) { (cat, ms, ratio) ->
            SanctuaryCard {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text(cat, style = MaterialTheme.typography.titleMedium)
                  Text("${FocusTimeUtils.formatDurationMillis(ms)} (${(ratio * 100).toInt()}%)")
                }
                LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth())
              }
            }
          }
        }

        if (aggregatedByApp.isNotEmpty()) {
          item {
            SectionHeader("Most-Used & Least-Used Apps", subtitle = "Sorted by measured duration")
          }
          items(aggregatedByApp.take(10), key = { it.packageName }) { app ->
            SanctuaryCard {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                InstalledAppIcon(packageName = app.packageName, appName = app.appName, size = 36.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(app.appName, style = MaterialTheme.typography.titleMedium)
                  Text(
                    "${app.category} • ${app.launchCount} launches",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Text(
                  FocusTimeUtils.formatDurationMillis(app.foregroundTimeMs),
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }
      },
    )
  }
}

private fun printHtmlReport(context: Context, html: String) {
  runCatching {
    val webView = WebView(context)
    webView.webViewClient =
      object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
          val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
          val adapter = webView.createPrintDocumentAdapter("FocusNest_Wellbeing_Report")
          printManager.print("FocusNest Report", adapter, PrintAttributes.Builder().build())
        }
      }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
  }
}
