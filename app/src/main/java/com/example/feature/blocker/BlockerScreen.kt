package com.example.feature.blocker

import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.database.AppGroupEntity
import com.example.core.database.AppLimitEntity
import com.example.core.database.AppUsageEntity
import com.example.core.database.BlockProfileEntity
import com.example.core.database.BlockingScheduleEntity
import com.example.core.database.CategoryLimitEntity
import com.example.core.database.InstalledAppEntity
import com.example.core.database.ShortFormRuleEntity
import com.example.core.database.UnlockHistoryEntity
import com.example.core.database.WebsiteRuleEntity
import com.example.core.datastore.FocusNestSettings
import com.example.core.design.AdaptiveTwoPane
import com.example.core.design.FocusNestEmptyState
import com.example.core.design.InstalledAppIcon
import com.example.core.design.SanctuaryCard
import com.example.core.design.SectionHeader
import com.example.core.permissions.PermissionManager
import com.example.domain.model.AppCategory
import com.example.domain.model.FocusTimeUtils
import com.example.domain.model.PermissionSnapshot
import com.example.domain.model.UnlockChallengeType
import com.example.ui.blocker.BlockedOverlayActivity
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BlockerScreen(
  settings: FocusNestSettings,
  permissions: PermissionSnapshot,
  installedApps: List<InstalledAppEntity>,
  todayUsage: List<AppUsageEntity>,
  appLimits: List<AppLimitEntity>,
  appGroups: List<AppGroupEntity>,
  profiles: List<BlockProfileEntity>,
  schedules: List<BlockingScheduleEntity>,
  shortFormRules: List<ShortFormRuleEntity>,
  websiteRules: List<WebsiteRuleEntity>,
  unlockHistory: List<UnlockHistoryEntity>,
  onRefreshPermissions: () -> Unit,
  onToggleEmergencyWhitelist: (String, Boolean) -> Unit,
  onSaveAppLimit: (String, String, Int, Int, Boolean, UnlockChallengeType, Int, String) -> Unit,
  onDeleteAppLimit: (String) -> Unit,
  onSaveAppGroup: (String, List<String>, Int) -> Unit,
  onDeleteAppGroup: (AppGroupEntity) -> Unit,
  onToggleProfileActive: (BlockProfileEntity, Boolean) -> Unit,
  onSaveProfile: (BlockProfileEntity) -> Unit,
  onDeleteProfile: (BlockProfileEntity) -> Unit,
  onSaveSchedule: (BlockingScheduleEntity) -> Unit,
  onDeleteSchedule: (BlockingScheduleEntity) -> Unit,
  onUpdateShortFormRule: (ShortFormRuleEntity) -> Unit,
  onToggleReelsGlobal: (Boolean) -> Unit,
  onAddWebsiteRule: (String, String, Boolean) -> Unit,
  onDeleteWebsiteRule: (WebsiteRuleEntity) -> Unit,
  onInstallWebsitePack: (String) -> Unit,
  onStartVpn: () -> Unit,
  onStopVpn: () -> Unit,
  categoryLimits: List<CategoryLimitEntity> = emptyList(),
  onSaveCategoryLimit: (String, Int, Boolean, UnlockChallengeType) -> Unit = { _, _, _, _ -> },
  onDeleteCategoryLimit: (String) -> Unit = {},
  onToggleBlockedNetworkPackage: (String, Boolean) -> Unit = { _, _ -> },
  onUpdateLaunchPauseConfig: (Boolean?, Int?) -> Unit = { _, _ -> },
  onToggleLaunchPausePackage: (String, Boolean) -> Unit = { _, _ -> },
) {
  val context = LocalContext.current
  var selectedTab by rememberSaveable { mutableIntStateOf(0) }
  val tabs = listOf("Apps & Limits", "Profiles", "Reels & Shorts", "Websites (VPN)", "Unlocks")

  var appSearch by rememberSaveable { mutableStateOf("") }
  var selectedCategory by rememberSaveable { mutableStateOf<AppCategory?>(null) }
  var editingLimitForApp by remember { mutableStateOf<InstalledAppEntity?>(null) }
  var showCreateGroupDialog by remember { mutableStateOf(false) }
  var showCategoryLimitDialog by remember { mutableStateOf(false) }
  var showCreateProfileDialog by remember { mutableStateOf(false) }
  var showCreateScheduleDialog by remember { mutableStateOf(false) }
  var newDomainInput by rememberSaveable { mutableStateOf("") }
  var newDomainAllowlist by rememberSaveable { mutableStateOf(false) }

  val vpnLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == android.app.Activity.RESULT_OK) {
        onStartVpn()
      }
      onRefreshPermissions()
    }

  val limitsMap = remember(appLimits) { appLimits.associateBy { it.packageName } }
  val usageMap = remember(todayUsage) { todayUsage.associateBy { it.packageName } }

  Column(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
      Text("Shield & Blocker", style = MaterialTheme.typography.headlineMedium)
      Text(
        "App limits, scheduled profiles, Reels pause & local VPN filter",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    PrimaryScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
      tabs.forEachIndexed { index, title ->
        Tab(
          selected = selectedTab == index,
          onClick = { selectedTab = index },
          text = { Text(title) },
          modifier = Modifier.testTag("blocker_tab_$index"),
        )
      }
    }

    when (selectedTab) {
      0 -> {
        val filteredApps =
          remember(installedApps, appSearch, selectedCategory) {
            installedApps.filter { app ->
              val matchesQuery =
                appSearch.isBlank() ||
                  app.appName.contains(appSearch, ignoreCase = true) ||
                  app.packageName.contains(appSearch, ignoreCase = true)
              val matchesCat = selectedCategory == null || app.category == selectedCategory?.name
              matchesQuery && matchesCat
            }
          }

        AdaptiveTwoPane(
          modifier = Modifier.weight(1f),
          primaryPane = {
            if (!permissions.accessibilityEnabled) {
              item {
                SanctuaryCard(highlight = true) {
                  Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Accessibility Service Needed for Live Blocking", style = MaterialTheme.typography.titleMedium)
                    Text(
                      "UsageStatsManager measures usage, while Android's optional Accessibility Service detects foreground app launches in real time so FocusNest can present the block screen.",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                      onClick = {
                        PermissionManager.safeStartSettings(
                          context,
                          PermissionManager.accessibilitySettingsIntent(),
                        )
                      },
                      modifier = Modifier.testTag("blocker_enable_accessibility_btn"),
                    ) {
                      Text("Open Accessibility Settings")
                    }
                  }
                }
              }
            }

            item {
              SanctuaryCard(
                highlight = settings.launchPauseEnabled,
                modifier = Modifier.testTag("blocker_launch_pause_card"),
              ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                      Icon(Icons.Default.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                      Spacer(modifier = Modifier.width(8.dp))
                      Column {
                        Text("Mindful Launch Pause Curtain", style = MaterialTheme.typography.titleMedium)
                        Text(
                          "Show a ${settings.launchPauseSeconds}s breathing pause before selected apps open",
                          style = MaterialTheme.typography.bodyMedium,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                    }
                    Switch(
                      checked = settings.launchPauseEnabled,
                      onCheckedChange = { onUpdateLaunchPauseConfig(it, null) },
                      modifier = Modifier.testTag("blocker_launch_pause_switch"),
                    )
                  }
                  Text(
                    "Pause delay: ${settings.launchPauseSeconds}s • ${settings.launchPausePackages.size} app(s) armed",
                    style = MaterialTheme.typography.labelLarge,
                  )
                  Slider(
                    value = settings.launchPauseSeconds.toFloat(),
                    onValueChange = { onUpdateLaunchPauseConfig(null, it.toInt()) },
                    valueRange = 5f..15f,
                    steps = 9,
                    modifier = Modifier.testTag("blocker_launch_pause_slider"),
                  )
                  OutlinedButton(
                    onClick = {
                      val samplePkg = settings.launchPausePackages.firstOrNull()
                        ?: installedApps.firstOrNull()?.packageName
                        ?: "com.example.demo"
                      val sampleName =
                        installedApps.firstOrNull { it.packageName == samplePkg }?.appName ?: "Mindful App"
                      BlockedOverlayActivity.launchPreview(
                        context = context,
                        packageName = samplePkg,
                        appName = sampleName,
                        challengeType = UnlockChallengeType.BREATHING,
                        isLaunchPause = true,
                        launchPauseSeconds = settings.launchPauseSeconds,
                      )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("blocker_preview_launch_pause_btn"),
                  ) {
                    Text("Preview Breathing Pause Curtain")
                  }
                }
              }
            }

            item {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                  onClick = { showCreateGroupDialog = true },
                  modifier = Modifier.weight(1f).testTag("blocker_new_group_btn"),
                ) {
                  Icon(Icons.Default.GroupWork, contentDescription = null)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("App Group")
                }
                OutlinedButton(
                  onClick = { showCategoryLimitDialog = true },
                  modifier = Modifier.weight(1f).testTag("blocker_new_category_limit_btn"),
                ) {
                  Text("Category Cap")
                }
                OutlinedButton(
                  onClick = {
                    BlockedOverlayActivity.launchPreview(
                      context = context,
                      packageName = installedApps.firstOrNull()?.packageName ?: "com.example.demo",
                      appName = installedApps.firstOrNull()?.appName ?: "Sample App",
                      challengeType = UnlockChallengeType.BREATHING,
                    )
                  },
                  modifier = Modifier.weight(1f).testTag("blocker_preview_overlay_btn"),
                ) {
                  Text("Preview")
                }
              }
            }

            if (categoryLimits.isNotEmpty()) {
              item {
                SectionHeader("Category Daily Caps", subtitle = "Aggregate limits per app category")
              }
              items(categoryLimits, key = { "cat_${it.categoryName}" }) { cat ->
                SanctuaryCard {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(cat.categoryName, style = MaterialTheme.typography.titleMedium)
                      Text(
                        "${cat.dailyLimitMinutes}m daily cap • ${cat.challengeType}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    IconButton(onClick = { onDeleteCategoryLimit(cat.categoryName) }) {
                      Icon(Icons.Default.Delete, contentDescription = "Delete category limit")
                    }
                  }
                }
              }
            }

            if (appGroups.isNotEmpty()) {
              item {
                SectionHeader("Shared App Groups", subtitle = "Combined daily time caps")
              }
              items(appGroups, key = { it.id }) { group ->
                SanctuaryCard {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(group.name, style = MaterialTheme.typography.titleMedium)
                      Text(
                        "${group.packageNames.size} apps • ${group.sharedDailyLimitMinutes}m shared daily limit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    IconButton(onClick = { onDeleteAppGroup(group) }) {
                      Icon(Icons.Default.Delete, contentDescription = "Delete group")
                    }
                  }
                }
              }
            }
          },
          secondaryPane = {
            item {
              OutlinedTextField(
                value = appSearch,
                onValueChange = { appSearch = it },
                label = { Text("Search installed applications") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("blocker_app_search"),
              )
              Spacer(modifier = Modifier.height(8.dp))
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                FilterChip(
                  selected = selectedCategory == null,
                  onClick = { selectedCategory = null },
                  label = { Text("All") },
                )
                AppCategory.entries.forEach { cat ->
                  FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                    label = { Text(cat.label) },
                  )
                }
              }
            }

            if (filteredApps.isEmpty()) {
              item {
                FocusNestEmptyState(
                  icon = Icons.Default.Block,
                  title = "No Matching Apps Found",
                  message = "Try clearing your search filter or refreshing installed applications.",
                )
              }
            } else {
              items(filteredApps, key = { it.packageName }) { app ->
                val limit = limitsMap[app.packageName]
                val usageMs = usageMap[app.packageName]?.foregroundTimeMs ?: 0L
                val isLaunchPaused = app.packageName in settings.launchPausePackages
                SanctuaryCard(highlight = limit?.isBlockedManually == true || isLaunchPaused) {
                  Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      InstalledAppIcon(packageName = app.packageName, appName = app.appName, size = 36.dp)
                      Spacer(modifier = Modifier.width(10.dp))
                      Column(modifier = Modifier.weight(1f)) {
                        Text(app.appName, style = MaterialTheme.typography.titleMedium)
                        Text(
                          buildString {
                            append(app.category)
                            if (usageMs > 0L) append(" • ${FocusTimeUtils.formatDurationMillis(usageMs)} today")
                            if (app.isEmergencyWhitelisted) append(" • Emergency Safe")
                          },
                          style = MaterialTheme.typography.bodyMedium,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (limit != null) {
                          Text(
                            buildString {
                              if (limit.isBlockedManually) append("Blocked • ")
                              if (limit.dailyLimitMinutes > 0) append("${limit.dailyLimitMinutes}m/day ")
                              if (limit.gracePeriodMinutes > 0) append("+${limit.gracePeriodMinutes}m grace ")
                              if (limit.weeklyLimitMinutes > 0) append("${limit.weeklyLimitMinutes}m/wk ")
                              append("(${limit.challengeType})")
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                          )
                        }
                      }
                      Button(onClick = { editingLimitForApp = app }) {
                        Text(if (limit == null) "Rule" else "Edit")
                      }
                    }
                    FlowRow(
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      FilterChip(
                        selected = isLaunchPaused,
                        onClick = { onToggleLaunchPausePackage(app.packageName, !isLaunchPaused) },
                        label = { Text(if (isLaunchPaused) "Launch Pause On" else "Add Launch Pause") },
                        modifier = Modifier.testTag("blocker_pause_toggle_${app.packageName}"),
                      )
                      FilterChip(
                        selected = app.isEmergencyWhitelisted,
                        onClick = { onToggleEmergencyWhitelist(app.packageName, !app.isEmergencyWhitelisted) },
                        label = { Text(if (app.isEmergencyWhitelisted) "Emergency Safe" else "Allow Safe") },
                      )
                    }
                  }
                }
              }
            }
          },
        )
      }

      1 -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = { showCreateProfileDialog = true },
                modifier = Modifier.weight(1f).testTag("blocker_new_profile_btn"),
              ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Profile")
              }
              OutlinedButton(
                onClick = { showCreateScheduleDialog = true },
                modifier = Modifier.weight(1f).testTag("blocker_new_schedule_btn"),
              ) {
                Icon(Icons.Default.Schedule, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Schedule")
              }
            }
          }

          item {
            SectionHeader(
              "Restriction Profiles",
              subtitle = "Priority-ordered profiles prevent schedule conflicts",
            )
          }

          items(profiles, key = { it.id }) { profile ->
            SanctuaryCard(highlight = profile.isActive) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("${profile.name} (${profile.presetType})", style = MaterialTheme.typography.titleMedium)
                  Text(
                    "${profile.blockedPackages.size} blocked apps • Priority ${profile.priority} • ${profile.unlockPolicy}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(onClick = { onDeleteProfile(profile) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete profile")
                  }
                  Switch(
                    checked = profile.isActive,
                    onCheckedChange = { onToggleProfileActive(profile, it) },
                  )
                }
              }
            }
          }

          item {
            SectionHeader("Blocking Schedules", subtitle = "Automated recurring windows")
          }

          if (schedules.isEmpty()) {
            item {
              FocusNestEmptyState(
                icon = Icons.Default.Schedule,
                title = "No Schedules Configured",
                message = "Add a schedule to automatically activate Study, Work, or Sleep rules on chosen weekdays.",
                actionLabel = "Create Schedule",
                onAction = { showCreateScheduleDialog = true },
              )
            }
          } else {
            items(schedules, key = { it.id }) { sched ->
              val profileName = profiles.firstOrNull { it.id == sched.profileId }?.name ?: "Profile"
              SanctuaryCard {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(sched.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                      "$profileName • ${FocusTimeUtils.formatMinuteOfDay(sched.startMinuteOfDay)}–${FocusTimeUtils.formatMinuteOfDay(sched.endMinuteOfDay)}",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  IconButton(onClick = { onDeleteSchedule(sched) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete schedule")
                  }
                }
              }
            }
          }
        }
      }

      2 -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            SanctuaryCard(highlight = settings.reelsReductionEnabled) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text("Short-Form Video Reduction", style = MaterialTheme.typography.titleLarge)
                    Text(
                      "Transparent, user-authorized pause prompts for Reels & Shorts",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Switch(
                    checked = settings.reelsReductionEnabled,
                    onCheckedChange = onToggleReelsGlobal,
                    modifier = Modifier.testTag("reels_global_switch"),
                  )
                }
                Text(
                  "Note: Uses narrow view-ID checks via Accessibility Service only while enabled. Never collects or transmits screen text. If an app updates its layout, FocusNest fails safely without blocking core app access.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          items(shortFormRules, key = { it.packageName }) { rule ->
            SanctuaryCard {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text("${rule.appName} ${rule.featureLabel}", style = MaterialTheme.typography.titleMedium)
                      Text(
                        "Today: ${rule.interactionsToday} / ${rule.dailyInteractionLimit} pauses",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = { onUpdateShortFormRule(rule.copy(isEnabled = it)) },
                  )
                }
                Text("Daily interaction limit: ${rule.dailyInteractionLimit}")
                Slider(
                  value = rule.dailyInteractionLimit.toFloat(),
                  onValueChange = { onUpdateShortFormRule(rule.copy(dailyInteractionLimit = it.toInt())) },
                  valueRange = 1f..30f,
                )
                Text("Pause breath duration: ${rule.pauseSeconds}s")
                Slider(
                  value = rule.pauseSeconds.toFloat(),
                  onValueChange = { onUpdateShortFormRule(rule.copy(pauseSeconds = it.toInt())) },
                  valueRange = 3f..30f,
                )
              }
            }
          }
        }
      }

      3 -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            SanctuaryCard(highlight = permissions.vpnActive) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text("Local On-Device Website Filter", style = MaterialTheme.typography.titleLarge)
                    Text(
                      if (permissions.vpnActive) "Local DNS sinkhole active"
                      else "Inactive • Requires explicit Android VPN consent",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Text(
                  "FocusNest runs an on-device local DNS filter using VpnService. No browsing history leaves your phone, no remote VPN servers are used, and enabling this replaces any other active Android VPN.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                  onClick = {
                    if (permissions.vpnActive) {
                      onStopVpn()
                    } else {
                      val prepareIntent = VpnService.prepare(context)
                      if (prepareIntent != null) vpnLauncher.launch(prepareIntent)
                      else onStartVpn()
                    }
                  },
                  modifier = Modifier.fillMaxWidth().testTag("vpn_toggle_btn"),
                ) {
                  Text(if (permissions.vpnActive) "Stop Local Website Filter" else "Start Local Website Filter")
                }
              }
            }
          }

          item {
            SanctuaryCard {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add Domain Rule", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                  value = newDomainInput,
                  onValueChange = { newDomainInput = it },
                  label = { Text("Domain (e.g. youtube.com)") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().testTag("website_domain_input"),
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(if (newDomainAllowlist) "Allowlist (override block)" else "Blocklist")
                  Switch(checked = newDomainAllowlist, onCheckedChange = { newDomainAllowlist = it })
                }
                Button(
                  onClick = {
                    onAddWebsiteRule(newDomainInput, "Custom", newDomainAllowlist)
                    newDomainInput = ""
                  },
                  modifier = Modifier.fillMaxWidth().testTag("website_add_btn"),
                ) {
                  Text("Add Domain")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Offline Domain Packs", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  listOf("Short-Form & Social", "Streaming & Video", "Gaming & Streams").forEach { pack ->
                    OutlinedButton(onClick = { onInstallWebsitePack(pack) }) {
                      Text("+ $pack")
                    }
                  }
                }
              }
            }
          }

          item {
            SanctuaryCard {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Per-App VPN Network Scope", style = MaterialTheme.typography.titleMedium)
                Text(
                  if (settings.blockedNetworkPackages.isEmpty())
                    "All non-FocusNest apps route DNS queries through the local filter. Toggle specific browsers or distracting apps below to restrict scope strictly to them."
                  else
                    "${settings.blockedNetworkPackages.size} app(s) selected for per-app DNS filtering. Encrypted Private DNS / DoH in system settings may bypass local DNS rules.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  installedApps.take(12).forEach { app ->
                    val active = app.packageName in settings.blockedNetworkPackages
                    FilterChip(
                      selected = active,
                      onClick = { onToggleBlockedNetworkPackage(app.packageName, !active) },
                      label = { Text(app.appName) },
                    )
                  }
                }
              }
            }
          }

          items(websiteRules, key = { it.id }) { rule ->
            SanctuaryCard {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(rule.domain, style = MaterialTheme.typography.titleMedium)
                  Text(
                    "${if (rule.isAllowlist) "Allowlist" else "Blocklist"} • ${rule.category} • ${rule.blockedHits} hits",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                IconButton(onClick = { onDeleteWebsiteRule(rule) }) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete domain rule")
                }
              }
            }
          }
        }
      }

      4 -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          item {
            SectionHeader(
              "12 Configurable Unlock Mechanisms",
              subtitle = "Choose per-app or per-profile friction when unlocking",
            )
          }
          items(UnlockChallengeType.entries, key = { it.name }) { challenge ->
            SanctuaryCard {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(Icons.Default.LockOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(challenge.label, style = MaterialTheme.typography.titleMedium)
                  Text(
                    challenge.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                TextButton(
                  onClick = {
                    BlockedOverlayActivity.launchPreview(
                      context = context,
                      packageName = "com.example.preview",
                      appName = "Demo Challenge",
                      challengeType = challenge,
                    )
                  }
                ) {
                  Text("Try")
                }
              }
            }
          }

          item {
            SectionHeader("Unlock Audit History", subtitle = "Recent unlock grants and denials")
          }
          if (unlockHistory.isEmpty()) {
            item {
              FocusNestEmptyState(
                icon = Icons.Default.Shield,
                title = "No Unlock Events Yet",
                message = "Unlock challenges and intention entries will appear here locally.",
              )
            }
          } else {
            items(unlockHistory.take(25), key = { it.id }) { entry ->
              SanctuaryCard {
                Column(modifier = Modifier.padding(14.dp)) {
                  Text(
                    "${entry.appName} • ${if (entry.granted) "Granted (${entry.grantedMinutes}m)" else "Denied"}",
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Text(
                    "${entry.dateKey} • ${entry.challengeType}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  if (entry.intentionText.isNotBlank()) {
                    Text("\"${entry.intentionText}\"", style = MaterialTheme.typography.bodyMedium)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  editingLimitForApp?.let { app ->
    val existing = limitsMap[app.packageName]
    var dailyMin by remember { mutableIntStateOf(existing?.dailyLimitMinutes ?: 30) }
    var weeklyMin by remember { mutableIntStateOf(existing?.weeklyLimitMinutes ?: 0) }
    var graceMin by remember { mutableIntStateOf(existing?.gracePeriodMinutes ?: 0) }
    var manualBlock by remember { mutableStateOf(existing?.isBlockedManually ?: false) }
    var challenge by remember {
      mutableStateOf(
        runCatching { UnlockChallengeType.valueOf(existing?.challengeType ?: "REFLECTION_REMINDER") }
          .getOrDefault(UnlockChallengeType.REFLECTION_REMINDER)
      )
    }

    AlertDialog(
      onDismissRequest = { editingLimitForApp = null },
      title = { Text("Rule for ${app.appName}") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Block Manually")
            Switch(checked = manualBlock, onCheckedChange = { manualBlock = it })
          }
          Text("Daily Limit: ${if (dailyMin == 0) "Off" else "${dailyMin}m"}")
          Slider(value = dailyMin.toFloat(), onValueChange = { dailyMin = (it.toInt() / 5) * 5 }, valueRange = 0f..240f)
          Text("Grace Period after limit: ${if (graceMin == 0) "Off" else "+${graceMin}m"}")
          Slider(value = graceMin.toFloat(), onValueChange = { graceMin = it.toInt() }, valueRange = 0f..15f)
          Text("Weekly Limit: ${if (weeklyMin == 0) "Off" else "${weeklyMin}m"}")
          Slider(value = weeklyMin.toFloat(), onValueChange = { weeklyMin = (it.toInt() / 15) * 15 }, valueRange = 0f..900f)
          Text("Unlock Challenge", style = MaterialTheme.typography.labelLarge)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
              UnlockChallengeType.REFLECTION_REMINDER,
              UnlockChallengeType.BREATHING,
              UnlockChallengeType.ADAPTIVE_MATH,
              UnlockChallengeType.SENTENCE_TYPING,
              UnlockChallengeType.INTENTION_ENTRY,
              UnlockChallengeType.FIXED_COOLDOWN,
              UnlockChallengeType.QR_SCAN,
              UnlockChallengeType.NFC_SCAN,
              UnlockChallengeType.STRICT_MODE,
            ).forEach { type ->
              FilterChip(
                selected = challenge == type,
                onClick = { challenge = type },
                label = { Text(type.label) },
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onSaveAppLimit(app.packageName, app.appName, dailyMin, weeklyMin, manualBlock, challenge, graceMin, app.category)
            editingLimitForApp = null
          }
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        Row {
          if (existing != null) {
            TextButton(
              onClick = {
                onDeleteAppLimit(app.packageName)
                editingLimitForApp = null
              }
            ) {
              Text("Remove")
            }
          }
          TextButton(onClick = { editingLimitForApp = null }) { Text("Cancel") }
        }
      },
    )
  }

  if (showCategoryLimitDialog) {
    var chosenCategory by remember { mutableStateOf(AppCategory.SOCIAL) }
    var catMinutes by remember { mutableIntStateOf(60) }
    var catChallenge by remember { mutableStateOf(UnlockChallengeType.BREATHING) }
    AlertDialog(
      onDismissRequest = { showCategoryLimitDialog = false },
      title = { Text("Category Daily Cap") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AppCategory.entries.forEach { cat ->
              FilterChip(
                selected = chosenCategory == cat,
                onClick = { chosenCategory = cat },
                label = { Text(cat.label) },
              )
            }
          }
          Text("Shared Category Limit: ${catMinutes}m / day")
          Slider(value = catMinutes.toFloat(), onValueChange = { catMinutes = (it.toInt() / 5) * 5 }, valueRange = 10f..240f)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
              UnlockChallengeType.REFLECTION_REMINDER,
              UnlockChallengeType.BREATHING,
              UnlockChallengeType.ADAPTIVE_MATH,
              UnlockChallengeType.STRICT_MODE,
            ).forEach { ch ->
              FilterChip(selected = catChallenge == ch, onClick = { catChallenge = ch }, label = { Text(ch.label) })
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onSaveCategoryLimit(chosenCategory.name, catMinutes, true, catChallenge)
            showCategoryLimitDialog = false
          }
        ) { Text("Save Cap") }
      },
      dismissButton = { TextButton(onClick = { showCategoryLimitDialog = false }) { Text("Cancel") } },
    )
  }

  if (showCreateGroupDialog) {
    var groupName by remember { mutableStateOf("") }
    var sharedMin by remember { mutableIntStateOf(60) }
    var picked by remember { mutableStateOf(setOf<String>()) }
    AlertDialog(
      onDismissRequest = { showCreateGroupDialog = false },
      title = { Text("Create App Group") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it },
            label = { Text("Group Name (e.g. Social Media)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          Text("Shared Daily Limit: ${sharedMin}m")
          Slider(value = sharedMin.toFloat(), onValueChange = { sharedMin = (it.toInt() / 5) * 5 }, valueRange = 10f..240f)
          LazyColumn(modifier = Modifier.height(180.dp)) {
            items(installedApps.take(40), key = { it.packageName }) { app ->
              val isSel = app.packageName in picked
              Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                  checked = isSel,
                  onCheckedChange = {
                    picked = if (isSel) picked - app.packageName else picked + app.packageName
                  },
                )
                Text(app.appName)
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onSaveAppGroup(groupName, picked.toList(), sharedMin)
            showCreateGroupDialog = false
          }
        ) { Text("Save Group") }
      },
      dismissButton = { TextButton(onClick = { showCreateGroupDialog = false }) { Text("Cancel") } },
    )
  }

  if (showCreateProfileDialog) {
    var name by remember { mutableStateOf("") }
    var preset by remember { mutableStateOf("Custom") }
    var priority by remember { mutableIntStateOf(7) }
    var strict by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf(setOf<String>()) }
    AlertDialog(
      onDismissRequest = { showCreateProfileDialog = false },
      title = { Text("Create Block Profile") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Profile Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          Text("Priority: $priority")
          Slider(value = priority.toFloat(), onValueChange = { priority = it.toInt() }, valueRange = 1f..10f)
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Strict Mode")
            Switch(checked = strict, onCheckedChange = { strict = it })
          }
          LazyColumn(modifier = Modifier.height(150.dp)) {
            items(installedApps.take(40), key = { it.packageName }) { app ->
              val isSel = app.packageName in picked
              Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                  checked = isSel,
                  onCheckedChange = { picked = if (isSel) picked - app.packageName else picked + app.packageName },
                )
                Text(app.appName)
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (name.isNotBlank()) {
              onSaveProfile(
                BlockProfileEntity(
                  name = name.trim(),
                  presetType = preset,
                  blockedPackages = picked.toList(),
                  strictMode = strict,
                  priority = priority,
                )
              )
              showCreateProfileDialog = false
            }
          }
        ) { Text("Create") }
      },
      dismissButton = { TextButton(onClick = { showCreateProfileDialog = false }) { Text("Cancel") } },
    )
  }

  if (showCreateScheduleDialog) {
    var label by remember { mutableStateOf("Morning Study") }
    var startHour by remember { mutableIntStateOf(9) }
    var endHour by remember { mutableIntStateOf(12) }
    var chosenProfileId by remember { mutableStateOf(profiles.firstOrNull()?.id ?: 0L) }
    AlertDialog(
      onDismissRequest = { showCreateScheduleDialog = false },
      title = { Text("Create Blocking Schedule") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("Schedule Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            profiles.take(5).forEach { p ->
              FilterChip(
                selected = chosenProfileId == p.id,
                onClick = { chosenProfileId = p.id },
                label = { Text(p.name) },
              )
            }
          }
          Text("Start Hour: ${String.format(Locale.US, "%02d:00", startHour)}")
          Slider(value = startHour.toFloat(), onValueChange = { startHour = it.toInt() }, valueRange = 0f..23f)
          Text("End Hour: ${String.format(Locale.US, "%02d:00", endHour)}")
          Slider(value = endHour.toFloat(), onValueChange = { endHour = it.toInt() }, valueRange = 0f..23f)
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onSaveSchedule(
              BlockingScheduleEntity(
                profileId = chosenProfileId,
                label = label.trim().ifEmpty { "Scheduled Rule" },
                daysOfWeek = listOf(1, 2, 3, 4, 5),
                startMinuteOfDay = startHour * 60,
                endMinuteOfDay = endHour * 60,
                isEnabled = true,
              )
            )
            showCreateScheduleDialog = false
          }
        ) { Text("Save") }
      },
      dismissButton = { TextButton(onClick = { showCreateScheduleDialog = false }) { Text("Cancel") } },
    )
  }
}
