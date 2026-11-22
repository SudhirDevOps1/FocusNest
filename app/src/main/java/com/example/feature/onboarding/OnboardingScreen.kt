package com.example.feature.onboarding

import android.Manifest
import android.net.VpnService
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.database.InstalledAppEntity
import com.example.core.design.FocusNestLogoMark
import com.example.core.design.SanctuaryCard
import com.example.core.permissions.PermissionManager
import com.example.domain.model.FocusTimeUtils
import com.example.domain.model.PermissionSnapshot
import com.example.service.FocusNestVpnService

@Composable
fun OnboardingScreen(
  permissions: PermissionSnapshot,
  installedApps: List<InstalledAppEntity>,
  onRefreshPermissions: () -> Unit,
  onComplete: (
    dailyScreenGoalMinutes: Int,
    dailyFocusGoalMinutes: Int,
    distractingPackages: Set<String>,
    firstProfileName: String,
    firstFocusMinutes: Int,
  ) -> Unit,
) {
  val context = LocalContext.current
  var step by rememberSaveable { mutableIntStateOf(1) }
  var screenGoalMin by rememberSaveable { mutableIntStateOf(180) }
  var focusGoalMin by rememberSaveable { mutableIntStateOf(60) }
  var selectedApps by rememberSaveable { mutableStateOf(setOf<String>()) }
  var appSearch by rememberSaveable { mutableStateOf("") }
  var profileName by rememberSaveable { mutableStateOf("Daily Deep Work") }
  var profileFocusMin by rememberSaveable { mutableIntStateOf(25) }

  val notifPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
      onRefreshPermissions()
    }
  val vpnConsentLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == android.app.Activity.RESULT_OK) {
        FocusNestVpnService.start(context)
      }
      onRefreshPermissions()
    }

  Scaffold { innerPadding ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(horizontal = 20.dp, vertical = 14.dp)
          .widthIn(max = 600.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        if (step > 1) {
          IconButton(
            onClick = { step-- },
            modifier = Modifier.testTag("onboarding_back_btn"),
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous step")
          }
        } else {
          FocusNestLogoMark(size = 34.dp)
        }
        Text(
          text = "Step $step of 10",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
          onClick = {
            onComplete(screenGoalMin, focusGoalMin, selectedApps, profileName, profileFocusMin)
          },
          modifier = Modifier.testTag("onboarding_skip_all_btn"),
        ) {
          Text("Skip")
        }
      }

      LinearProgressIndicator(
        progress = { step / 10f },
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
      )

      Spacer(modifier = Modifier.height(8.dp))

      AnimatedContent(
        targetState = step,
        modifier = Modifier.weight(1f).fillMaxWidth(),
        label = "onboardingStep",
      ) { currentStep ->
        when (currentStep) {
          1 ->
            StepScrollContainer {
              Image(
                painter = painterResource(id = R.drawable.img_hero_sanctuary_1795316424810),
                contentDescription = "FocusNest sanctuary landscape",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(190.dp).clip(MaterialTheme.shapes.large),
              )
              Spacer(modifier = Modifier.height(20.dp))
              Text("Welcome to FocusNest", style = MaterialTheme.typography.displayMedium)
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                "Own Your Time. Protect Your Focus.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                "FocusNest is a 100% free, open-source, privacy-first digital wellbeing companion. Everything stays locally on your device—no cloud accounts, no trackers, and no ads.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

          2 ->
            StepScrollContainer {
              StepHeader(
                icon = Icons.Default.SelfImprovement,
                title = "Intentional Digital Wellbeing",
                description =
                  "Instead of locking you out with dark patterns, FocusNest combines real screen-time visibility, structured Pomodoro sessions, and thoughtful friction when you open distracting apps.",
              )
              Spacer(modifier = Modifier.height(16.dp))
              InfoBullet("100% Offline-First", "All usage history, sessions, and block rules live in local Room SQLite storage.")
              InfoBullet("Transparent Permissions", "Every Android permission is optional and requested only when you turn on that feature.")
              InfoBullet("Owner-Controlled", "You always retain emergency recovery and backup control.")
            }

          3 ->
            StepScrollContainer {
              StepHeader(
                icon = Icons.Default.QueryStats,
                title = "Usage Access (Optional)",
                description =
                  "Android's UsageStatsManager lets FocusNest show accurate daily app usage, launch counts, and daily limits. FocusNest never fabricates usage data if this access is off.",
              )
              Spacer(modifier = Modifier.height(16.dp))
              PermissionActionCard(
                title = "Usage Access Permission",
                granted = permissions.usageStatsGranted,
                buttonLabel = "Open Usage Access Settings",
                onAction = {
                  PermissionManager.safeStartSettings(
                    context,
                    PermissionManager.usageAccessIntent(context),
                  )
                },
                onRefresh = onRefreshPermissions,
              )
            }

          4 ->
            StepScrollContainer {
              StepHeader(
                icon = Icons.Default.Shield,
                title = "App Blocking & Reels Assistant",
                description =
                  "To detect distracting apps or Reels/Shorts feeds in real time, Android uses an optional Accessibility Service. FocusNest never logs screen contents, keystrokes, or personal messages.",
              )
              Spacer(modifier = Modifier.height(16.dp))
              PermissionActionCard(
                title = "Accessibility Service",
                granted = permissions.accessibilityEnabled,
                buttonLabel = "Configure Accessibility (Optional)",
                onAction = {
                  PermissionManager.safeStartSettings(
                    context,
                    PermissionManager.accessibilitySettingsIntent(),
                  )
                },
                onRefresh = onRefreshPermissions,
              )
            }

          5 ->
            StepScrollContainer {
              StepHeader(
                icon = Icons.Default.VpnLock,
                title = "Local On-Device Website Filter",
                description =
                  "FocusNest can run a local-only Android VpnService DNS sinkhole to block distracting websites offline. No browsing data ever leaves your device, and you can skip this anytime.",
              )
              Spacer(modifier = Modifier.height(16.dp))
              PermissionActionCard(
                title = "Local VPN Consent",
                granted = permissions.vpnPrepared && permissions.vpnActive,
                buttonLabel = "Authorize Local Filter (Optional)",
                onAction = {
                  val prepareIntent = VpnService.prepare(context)
                  if (prepareIntent != null) {
                    vpnConsentLauncher.launch(prepareIntent)
                  } else {
                    FocusNestVpnService.start(context)
                    onRefreshPermissions()
                  }
                },
                onRefresh = onRefreshPermissions,
              )
            }

          6 ->
            StepScrollContainer {
              StepHeader(
                icon = Icons.Default.Notifications,
                title = "Notifications & Focus Muting",
                description =
                  "Allow timer completion alerts and optionally filter distracting notifications locally during Focus or Bedtime profiles.",
              )
              Spacer(modifier = Modifier.height(16.dp))
              PermissionActionCard(
                title = "Post Notifications",
                granted = permissions.postNotificationsGranted,
                buttonLabel = "Allow Timer Alerts",
                onAction = {
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                  }
                },
                onRefresh = onRefreshPermissions,
              )
              Spacer(modifier = Modifier.height(12.dp))
              PermissionActionCard(
                title = "Notification Listener (Optional)",
                granted = permissions.notificationListenerEnabled,
                buttonLabel = "Open Notification Access",
                onAction = {
                  PermissionManager.safeStartSettings(
                    context,
                    PermissionManager.notificationListenerSettingsIntent(),
                  )
                },
                onRefresh = onRefreshPermissions,
              )
            }

          7 ->
            StepScrollContainer {
              StepHeader(
                icon = Icons.Default.Timer,
                title = "Set Your Daily Goals",
                description = "Choose realistic targets for total daily screen time and intentional focus minutes.",
              )
              Spacer(modifier = Modifier.height(16.dp))
              SanctuaryCard {
                Column(modifier = Modifier.padding(18.dp)) {
                  Text(
                    "Daily Screen-Time Limit: ${FocusTimeUtils.formatDurationMinutes(screenGoalMin)}",
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Slider(
                    value = screenGoalMin.toFloat(),
                    onValueChange = { screenGoalMin = (it.toInt() / 15) * 15 },
                    valueRange = 60f..480f,
                  )
                  Spacer(modifier = Modifier.height(14.dp))
                  Text(
                    "Daily Focus Goal: ${FocusTimeUtils.formatDurationMinutes(focusGoalMin)}",
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Slider(
                    value = focusGoalMin.toFloat(),
                    onValueChange = { focusGoalMin = (it.toInt() / 5) * 5 },
                    valueRange = 15f..240f,
                  )
                }
              }
            }

          8 -> {
            val filtered =
              remember(installedApps, appSearch) {
                installedApps.filter {
                  appSearch.isBlank() ||
                    it.appName.contains(appSearch, ignoreCase = true) ||
                    it.packageName.contains(appSearch, ignoreCase = true)
                }
              }
            Column(modifier = Modifier.fillMaxSize()) {
              StepHeader(
                icon = Icons.Default.Lock,
                title = "Select Distracting Apps",
                description = "Pick apps you want included in your Quick Shield and focus sessions (you can edit anytime).",
              )
              Spacer(modifier = Modifier.height(10.dp))
              OutlinedTextField(
                value = appSearch,
                onValueChange = { appSearch = it },
                label = { Text("Search installed apps") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("onboarding_app_search"),
              )
              Spacer(modifier = Modifier.height(8.dp))
              LazyColumn(modifier = Modifier.weight(1f)) {
                items(filtered, key = { it.packageName }) { app ->
                  val checked = app.packageName in selectedApps
                  Row(
                    modifier =
                      Modifier
                        .fillMaxWidth()
                        .clickable {
                          selectedApps =
                            if (checked) selectedApps - app.packageName
                            else selectedApps + app.packageName
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Checkbox(
                      checked = checked,
                      onCheckedChange = {
                        selectedApps =
                          if (checked) selectedApps - app.packageName
                          else selectedApps + app.packageName
                      },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text(app.appName, style = MaterialTheme.typography.titleMedium)
                      Text(
                        app.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                }
              }
            }
          }

          9 ->
            StepScrollContainer {
              StepHeader(
                icon = Icons.Default.Security,
                title = "Configure Your First Profile",
                description = "Create a custom focus profile paired with your selected apps.",
              )
              Spacer(modifier = Modifier.height(16.dp))
              SanctuaryCard {
                Column(modifier = Modifier.padding(18.dp)) {
                  OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profile Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_profile_name"),
                  )
                  Spacer(modifier = Modifier.height(14.dp))
                  Text(
                    "Default Focus Duration: $profileFocusMin min",
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Slider(
                    value = profileFocusMin.toFloat(),
                    onValueChange = { profileFocusMin = (it.toInt() / 5) * 5 },
                    valueRange = 15f..90f,
                  )
                  Text(
                    "Selected apps in shield: ${selectedApps.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }

          10 ->
            StepScrollContainer {
              StepHeader(
                icon = Icons.Default.CheckCircle,
                title = "Your Focus Sanctuary Is Ready",
                description =
                  "Everything is configured locally on your device. Review your starting summary and enter FocusNest.",
              )
              Spacer(modifier = Modifier.height(16.dp))
              SanctuaryCard(highlight = true) {
                Column(
                  modifier = Modifier.padding(18.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Text("Daily Screen Goal: ${FocusTimeUtils.formatDurationMinutes(screenGoalMin)}", style = MaterialTheme.typography.titleMedium)
                  Text("Daily Focus Goal: ${FocusTimeUtils.formatDurationMinutes(focusGoalMin)}", style = MaterialTheme.typography.titleMedium)
                  Text("First Profile: $profileName ($profileFocusMin min)", style = MaterialTheme.typography.titleMedium)
                  Text("Distracting Apps Flagged: ${selectedApps.size}", style = MaterialTheme.typography.titleMedium)
                }
              }
            }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (step in 3..6) {
          OutlinedButton(
            onClick = { step++ },
            modifier = Modifier.weight(1f).height(50.dp).testTag("onboarding_skip_step_btn"),
          ) {
            Text("Skip for Now")
          }
        }
        Button(
          onClick = {
            if (step < 10) {
              step++
            } else {
              onComplete(screenGoalMin, focusGoalMin, selectedApps, profileName, profileFocusMin)
            }
          },
          modifier = Modifier.weight(1f).height(50.dp).testTag("onboarding_next_btn"),
        ) {
          Text(if (step < 10) "Continue" else "Open Dashboard")
          Spacer(modifier = Modifier.width(6.dp))
          Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
      }
    }
  }
}

@Composable
private fun StepScrollContainer(content: @Composable () -> Unit) {
  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
  ) {
    content()
  }
}

@Composable
private fun StepHeader(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  description: String,
) {
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.primaryContainer,
    modifier = Modifier.size(52.dp),
  ) {
    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
      Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
    }
  }
  Spacer(modifier = Modifier.height(14.dp))
  Text(title, style = MaterialTheme.typography.headlineMedium)
  Spacer(modifier = Modifier.height(8.dp))
  Text(
    description,
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
private fun InfoBullet(title: String, body: String) {
  SanctuaryCard(modifier = Modifier.padding(vertical = 5.dp)) {
    Column(modifier = Modifier.padding(14.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
      Spacer(modifier = Modifier.height(4.dp))
      Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
private fun PermissionActionCard(
  title: String,
  granted: Boolean,
  buttonLabel: String,
  onAction: () -> Unit,
  onRefresh: () -> Unit,
) {
  SanctuaryCard(highlight = granted) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
          text = if (granted) "Granted" else "Not enabled",
          style = MaterialTheme.typography.labelLarge,
          color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onAction, modifier = Modifier.weight(1f)) {
          Text(buttonLabel)
        }
        OutlinedButton(onClick = onRefresh) {
          Text("Check")
        }
      }
    }
  }
}
