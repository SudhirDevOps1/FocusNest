package com.example.feature.more

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.NotificationsPaused
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.BuildConfig
import com.example.R
import com.example.core.common.OemCompatibilityHelper
import com.example.core.common.QrCodeGenerator
import com.example.core.database.BlockProfileEntity
import com.example.core.database.FocusGoalEntity
import com.example.core.database.NfcTagEntity
import com.example.core.database.NotificationRecordEntity
import com.example.core.database.QrTokenEntity
import com.example.core.datastore.FocusNestSettings
import com.example.core.design.FocusNestEmptyState
import com.example.core.design.SanctuaryCard
import com.example.core.design.SectionHeader
import com.example.core.permissions.PermissionManager
import com.example.domain.model.AccentPalette
import com.example.domain.model.BedtimeChecklistDefaults
import com.example.domain.model.FocusTimeUtils
import com.example.domain.model.PermissionSnapshot
import com.example.domain.model.ThemeMode
import com.example.domain.model.UpdateReleaseConfig
import com.example.feature.challenges.OfflineQrBarcodeScannerDialog
import java.util.UUID
import kotlinx.coroutines.launch

enum class MoreSubPage {
  HUB,
  VAULT,
  INVINCIBLE,
  MINDFUL,
  NOTIFICATIONS,
  PERMISSIONS,
  SETTINGS,
  PRIVACY_AND_ABOUT,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoreScreen(
  settings: FocusNestSettings,
  permissions: PermissionSnapshot,
  focusGoal: FocusGoalEntity,
  profiles: List<BlockProfileEntity>,
  nfcTags: List<NfcTagEntity>,
  qrTokens: List<QrTokenEntity>,
  lastScannedNfcUid: String?,
  recentNotifications: List<NotificationRecordEntity>,
  onRefreshPermissions: () -> Unit,
  onSaveNfcTag: (String, String, String, Long?, Int) -> Unit,
  onDeleteNfcTag: (NfcTagEntity) -> Unit,
  onSaveQrToken: (String, String, String, Long?, Boolean, Int) -> Unit,
  onDeleteQrToken: (QrTokenEntity) -> Unit,
  onEnableInvincible: (Int, Boolean, String) -> Unit,
  onRequestInvincibleUnlock: () -> Unit,
  onCompleteInvincibleDisable: () -> Boolean,
  onEmergencyDisableInvincible: (String) -> Boolean,
  onUpdateMindful: (
    bedtimeEnabled: Boolean?,
    bedtimeStart: Int?,
    bedtimeEnd: Int?,
    bedtimeDnd: Boolean?,
    breakEnabled: Boolean?,
    breakInterval: Int?,
    notifFilterEnabled: Boolean?,
    cooldownSec: Int?,
  ) -> Unit,
  onUpdateAppearance: (ThemeMode?, AccentPalette?, Boolean?, String?) -> Unit,
  onUpdateGoals: (Int, Int, Int, Int) -> Unit,
  onBuildEncryptedBackup: suspend (String) -> String,
  onRestoreBackup: suspend (String, String?) -> Result<Int>,
  onDeleteAllData: () -> Unit,
  onShowMessage: (String) -> Unit,
  habitsCount: Int = 0,
  onNavigateToHabits: () -> Unit = {},
  onClearUsageHistory: () -> Unit = {},
  onClearNotificationHistory: () -> Unit = {},
  onClearHabits: () -> Unit = {},
  onToggleWindDownStep: (String) -> Unit = {},
  onOpenReleases: () -> Unit = {},
) {
  var subPage by rememberSaveable { mutableStateOf(MoreSubPage.HUB) }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isTwoPane = maxWidth >= 720.dp

    if (!isTwoPane && subPage != MoreSubPage.HUB) {
      BackHandler { subPage = MoreSubPage.HUB }
    }

    @Composable
    fun RenderDetailPane(target: MoreSubPage, showBack: Boolean) {
      val backAction: (() -> Unit)? = if (showBack) ({ subPage = MoreSubPage.HUB }) else null
      when (target) {
        MoreSubPage.HUB,
        MoreSubPage.VAULT ->
          VaultSubPage(
            permissions = permissions,
            profiles = profiles,
            nfcTags = nfcTags,
            qrTokens = qrTokens,
            lastScannedNfcUid = lastScannedNfcUid,
            onBack = backAction,
            onSaveNfcTag = onSaveNfcTag,
            onDeleteNfcTag = onDeleteNfcTag,
            onSaveQrToken = onSaveQrToken,
            onDeleteQrToken = onDeleteQrToken,
          )
        MoreSubPage.INVINCIBLE ->
          InvincibleSubPage(
            settings = settings,
            onBack = backAction,
            onEnableInvincible = onEnableInvincible,
            onRequestUnlock = onRequestInvincibleUnlock,
            onCompleteDisable = onCompleteInvincibleDisable,
            onEmergencyDisable = onEmergencyDisableInvincible,
            onShowMessage = onShowMessage,
          )
        MoreSubPage.MINDFUL ->
          MindfulSubPage(
            settings = settings,
            permissions = permissions,
            onBack = backAction,
            onUpdateMindful = onUpdateMindful,
            onToggleWindDownStep = onToggleWindDownStep,
            onRefreshPermissions = onRefreshPermissions,
          )
        MoreSubPage.NOTIFICATIONS ->
          NotificationsSubPage(
            settings = settings,
            permissions = permissions,
            records = recentNotifications,
            onBack = backAction,
            onToggleFilter = { enabled ->
              onUpdateMindful(null, null, null, null, null, null, enabled, null)
            },
          )
        MoreSubPage.PERMISSIONS ->
          PermissionsSubPage(
            permissions = permissions,
            onBack = backAction,
            onRefresh = onRefreshPermissions,
          )
        MoreSubPage.SETTINGS ->
          SettingsSubPage(
            settings = settings,
            focusGoal = focusGoal,
            onBack = backAction,
            onUpdateAppearance = onUpdateAppearance,
            onUpdateGoals = onUpdateGoals,
            onOpenReleases = onOpenReleases,
            onBuildEncryptedBackup = onBuildEncryptedBackup,
            onRestoreBackup = onRestoreBackup,
            onClearUsageHistory = onClearUsageHistory,
            onClearNotificationHistory = onClearNotificationHistory,
            onClearHabits = onClearHabits,
            onDeleteAllData = onDeleteAllData,
            onShowMessage = onShowMessage,
          )
        MoreSubPage.PRIVACY_AND_ABOUT ->
          PrivacyAndAboutSubPage(
            onBack = backAction,
            onOpenReleases = onOpenReleases,
          )
      }
    }

    if (isTwoPane) {
      val activeDetail = if (subPage == MoreSubPage.HUB) MoreSubPage.VAULT else subPage
      Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        MoreHubList(
          settings = settings,
          habitsCount = habitsCount,
          nfcCount = nfcTags.size,
          qrCount = qrTokens.size,
          selectedSubPage = activeDetail,
          onSelect = { subPage = it },
          onNavigateToHabits = onNavigateToHabits,
          modifier = Modifier.weight(0.38f),
        )
        Box(modifier = Modifier.weight(0.62f)) {
          RenderDetailPane(target = activeDetail, showBack = false)
        }
      }
    } else if (subPage == MoreSubPage.HUB) {
      MoreHubList(
        settings = settings,
        habitsCount = habitsCount,
        nfcCount = nfcTags.size,
        qrCount = qrTokens.size,
        selectedSubPage = MoreSubPage.HUB,
        onSelect = { subPage = it },
        onNavigateToHabits = onNavigateToHabits,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      RenderDetailPane(target = subPage, showBack = true)
    }
  }
}

@Composable
private fun MoreHubList(
  settings: FocusNestSettings,
  habitsCount: Int,
  nfcCount: Int,
  qrCount: Int,
  selectedSubPage: MoreSubPage,
  onSelect: (MoreSubPage) -> Unit,
  onNavigateToHabits: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      Text("More & Sanctuary Controls", style = MaterialTheme.typography.headlineMedium)
      Text(
        "NFC/QR tokens, Invincible Mode, Mindful Tools, Permissions & Backups",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    item {
      MoreHubTile(
        icon = Icons.Default.SelfImprovement,
        title = "Daily Habits & Offline Streaks",
        subtitle = if (habitsCount == 0) "Track screen-free routines & build daily streaks" else "$habitsCount active habits tracked offline",
        tag = "more_tile_habits",
        selected = false,
        onClick = onNavigateToHabits,
      )
    }
    item {
      MoreHubTile(
        icon = Icons.Default.Nfc,
        title = "NFC, QR & Barcode Vault",
        subtitle = "$nfcCount NFC tags • $qrCount QR/Barcode tokens",
        tag = "more_tile_vault",
        selected = selectedSubPage == MoreSubPage.VAULT,
        onClick = { onSelect(MoreSubPage.VAULT) },
      )
    }
    item {
      MoreHubTile(
        icon = Icons.Default.Lock,
        title = "Invincible Mode",
        subtitle = if (settings.invincibleModeEnabled) "Active (${settings.invincibleDelayMinutes}m lock delay)" else "Off • Optional delayed rule lock & biometric gate",
        tag = "more_tile_invincible",
        selected = selectedSubPage == MoreSubPage.INVINCIBLE,
        onClick = { onSelect(MoreSubPage.INVINCIBLE) },
      )
    }
    item {
      MoreHubTile(
        icon = Icons.Default.Bedtime,
        title = "Mindful Usage Tools",
        subtitle = "Bedtime Wind-Down, Scheduled DND, Break Reminders & Cooldowns",
        tag = "more_tile_mindful",
        selected = selectedSubPage == MoreSubPage.MINDFUL,
        onClick = { onSelect(MoreSubPage.MINDFUL) },
      )
    }
    item {
      MoreHubTile(
        icon = Icons.Default.NotificationsPaused,
        title = "Notification Management",
        subtitle = if (settings.notificationFilterEnabled) "Local focus filtering enabled" else "Optional local notification log & muting",
        tag = "more_tile_notifications",
        selected = selectedSubPage == MoreSubPage.NOTIFICATIONS,
        onClick = { onSelect(MoreSubPage.NOTIFICATIONS) },
      )
    }
    item {
      MoreHubTile(
        icon = Icons.Default.AdminPanelSettings,
        title = "Permissions & Device Compatibility",
        subtitle = "Transparent status for Android 11–16 & Realme/OEM optimization",
        tag = "more_tile_permissions",
        selected = selectedSubPage == MoreSubPage.PERMISSIONS,
        onClick = { onSelect(MoreSubPage.PERMISSIONS) },
      )
    }
    item {
      MoreHubTile(
        icon = Icons.Default.Settings,
        title = "Settings, Updates & Encrypted Backup",
        subtitle = "v${BuildConfig.VERSION_NAME}, theme, goals, AES-256-GCM backup & releases",
        tag = "more_tile_settings",
        selected = selectedSubPage == MoreSubPage.SETTINGS,
        onClick = { onSelect(MoreSubPage.SETTINGS) },
      )
    }
    item {
      MoreHubTile(
        icon = Icons.Default.Security,
        title = "Privacy Policy, Threat Model & Licenses",
        subtitle = "Offline-first guarantees & open-source credits",
        tag = "more_tile_privacy",
        selected = selectedSubPage == MoreSubPage.PRIVACY_AND_ABOUT,
        onClick = { onSelect(MoreSubPage.PRIVACY_AND_ABOUT) },
      )
    }
  }
}

@Composable
private fun MoreHubTile(
  icon: ImageVector,
  title: String,
  subtitle: String,
  tag: String,
  selected: Boolean = false,
  onClick: () -> Unit,
) {
  SanctuaryCard(
    highlight = selected,
    modifier = Modifier.testTag(tag).clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@Composable
private fun SubPageHeader(title: String, onBack: (() -> Unit)?) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = if (onBack != null) 8.dp else 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (onBack != null) {
      IconButton(onClick = onBack, modifier = Modifier.testTag("subpage_back_btn")) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
      }
    }
    Text(title, style = MaterialTheme.typography.headlineMedium)
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VaultSubPage(
  permissions: PermissionSnapshot,
  profiles: List<BlockProfileEntity>,
  nfcTags: List<NfcTagEntity>,
  qrTokens: List<QrTokenEntity>,
  lastScannedNfcUid: String?,
  onBack: (() -> Unit)?,
  onSaveNfcTag: (String, String, String, Long?, Int) -> Unit,
  onDeleteNfcTag: (NfcTagEntity) -> Unit,
  onSaveQrToken: (String, String, String, Long?, Boolean, Int) -> Unit,
  onDeleteQrToken: (QrTokenEntity) -> Unit,
) {
  val context = LocalContext.current
  var showScanner by remember { mutableStateOf(false) }
  var qrPreviewToken by remember { mutableStateOf<String?>(null) }
  var nfcUidInput by remember(lastScannedNfcUid) { mutableStateOf(lastScannedNfcUid ?: "") }
  var nfcLabel by remember { mutableStateOf("Desk Study Tag") }
  var nfcAction by remember { mutableStateOf("START_FOCUS") }
  var qrLabel by remember { mutableStateOf("Physical Study Card") }
  var qrIsOneTime by remember { mutableStateOf(false) }
  var qrExpiresHours by remember { mutableIntStateOf(0) }

  Column(modifier = Modifier.fillMaxSize()) {
    SubPageHeader("NFC, QR & Barcode Vault", onBack)
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        SanctuaryCard {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Nfc, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text =
                  when {
                    !permissions.nfcSupported -> "NFC Hardware Not Detected on This Device"
                    !permissions.nfcEnabled -> "NFC Supported • Currently Disabled in Android Settings"
                    else -> "NFC Ready • Tap any NFC tag against device back"
                  },
                style = MaterialTheme.typography.titleMedium,
              )
            }
            if (permissions.nfcSupported && !permissions.nfcEnabled) {
              OutlinedButton(
                onClick = { PermissionManager.safeStartSettings(context, PermissionManager.nfcSettingsIntent()) }
              ) { Text("Enable NFC in Settings") }
            }
            OutlinedTextField(
              value = nfcUidInput,
              onValueChange = { nfcUidInput = it },
              label = { Text("NFC Tag UID (tap tag or enter hex)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
              value = nfcLabel,
              onValueChange = { nfcLabel = it },
              label = { Text("Tag Label") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf("START_FOCUS" to "Start 25m Focus", "UNLOCK_PROFILE" to "Unlock Profile", "TOGGLE_BLOCKER" to "Toggle Shield").forEach { (act, lbl) ->
                FilterChip(selected = nfcAction == act, onClick = { nfcAction = act }, label = { Text(lbl) })
              }
            }
            Button(
              enabled = nfcUidInput.isNotBlank(),
              onClick = {
                onSaveNfcTag(nfcUidInput, nfcLabel, nfcAction, profiles.firstOrNull()?.id, 25)
                nfcUidInput = ""
              },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text("Register NFC Tag")
            }
          }
        }
      }

      items(nfcTags, key = { it.tagUidHex }) { tag ->
        SanctuaryCard {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(tag.label, style = MaterialTheme.typography.titleMedium)
              Text("${tag.tagUidHex} • ${tag.actionType}", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = { onDeleteNfcTag(tag) }) {
              Icon(Icons.Default.Delete, contentDescription = "Delete NFC tag")
            }
          }
        }
      }

      item {
        SanctuaryCard {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Personal QR Codes & Barcodes (100% Offline)", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
              value = qrLabel,
              onValueChange = { qrLabel = it },
              label = { Text("Token Label") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text("One-Time Single-Use Token")
              Switch(checked = qrIsOneTime, onCheckedChange = { qrIsOneTime = it })
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf(0 to "Never expires", 1 to "Expires 1h", 24 to "Expires 24h").forEach { (hrs, lbl) ->
                FilterChip(
                  selected = qrExpiresHours == hrs,
                  onClick = { qrExpiresHours = hrs },
                  label = { Text(lbl) },
                )
              }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = {
                  val token = "FOCUSNEST:${UUID.randomUUID().toString().take(8).uppercase()}"
                  onSaveQrToken(token, qrLabel, "QR_CODE", profiles.firstOrNull()?.id, qrIsOneTime, qrExpiresHours)
                  qrPreviewToken = token
                },
                modifier = Modifier.weight(1f),
              ) {
                Icon(Icons.Default.QrCode2, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generate QR")
              }
              OutlinedButton(
                onClick = { showScanner = true },
                modifier = Modifier.weight(1f),
              ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan Token")
              }
            }
            qrPreviewToken?.let { code ->
              val bmp = remember(code) { QrCodeGenerator.generateQrBitmap(code, 360) }
              if (bmp != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                  Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Generated FocusNest unlock QR code",
                    modifier = Modifier.size(180.dp),
                  )
                  Text(code, style = MaterialTheme.typography.labelMedium)
                }
              }
            }
          }
        }
      }

      items(qrTokens, key = { it.id }) { token ->
        val statusTag =
          buildString {
            if (token.isOneTime) append(if (token.isConsumed) " • [Used]" else " • [One-Time]")
            if (token.expiresAtEpochMs > 0L) {
              append(if (System.currentTimeMillis() > token.expiresAtEpochMs) " • [Expired]" else " • [Timed]")
            }
          }
        SanctuaryCard {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("${token.label}$statusTag", style = MaterialTheme.typography.titleMedium)
              Text("${token.tokenFormat} • ${token.tokenValue}", style = MaterialTheme.typography.bodyMedium)
            }
            Row {
              if (token.tokenFormat == "QR_CODE") {
                TextButton(onClick = { qrPreviewToken = token.tokenValue }) { Text("View") }
              }
              IconButton(onClick = { onDeleteQrToken(token) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete token")
              }
            }
          }
        }
      }
    }
  }

  if (showScanner) {
    OfflineQrBarcodeScannerDialog(
      title = "Register QR or Barcode",
      onCodeScanned = { code ->
        val fmt = if (code.all { it.isDigit() }) "BARCODE" else "QR_CODE"
        onSaveQrToken(code, qrLabel, fmt, profiles.firstOrNull()?.id, qrIsOneTime, qrExpiresHours)
        showScanner = false
      },
      onDismiss = { showScanner = false },
    )
  }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? =
  when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
  }

@Composable
private fun InvincibleSubPage(
  settings: FocusNestSettings,
  onBack: (() -> Unit)?,
  onEnableInvincible: (Int, Boolean, String) -> Unit,
  onRequestUnlock: () -> Unit,
  onCompleteDisable: () -> Boolean,
  onEmergencyDisable: (String) -> Boolean,
  onShowMessage: (String) -> Unit,
) {
  val context = LocalContext.current
  var delayMinutes by remember { mutableIntStateOf(settings.invincibleDelayMinutes) }
  var requireBiometric by remember { mutableStateOf(settings.invincibleBiometricRequired) }
  var emergencyPin by remember { mutableStateOf("") }
  var recoveryPinInput by remember { mutableStateOf("") }

  fun launchBiometricThen(onAuthed: () -> Unit) {
    val activity = context.findFragmentActivity()
    val canAuth =
      BiometricManager.from(context).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
      ) == BiometricManager.BIOMETRIC_SUCCESS
    if (activity != null && canAuth) {
      val prompt =
        BiometricPrompt(
          activity,
          ContextCompat.getMainExecutor(context),
          object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
              onAuthed()
            }
          },
        )
      val info =
        BiometricPrompt.PromptInfo.Builder()
          .setTitle("Verify Owner Identity")
          .setSubtitle("Confirm before changing Invincible Mode protection")
          .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
          )
          .build()
      prompt.authenticate(info)
    } else {
      onAuthed()
    }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    SubPageHeader("Invincible Mode", onBack)
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        SanctuaryCard(highlight = settings.invincibleModeEnabled) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              if (settings.invincibleModeEnabled) "Invincible Mode Active" else "Invincible Mode Standby",
              style = MaterialTheme.typography.titleLarge,
            )
            Text(
              "Locks active restriction rules behind a configurable delay and optional Biometric confirmation. FocusNest never abuses Device Admin or stealth-locks your phone—you always keep owner recovery.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      if (!settings.invincibleModeEnabled) {
        item {
          SanctuaryCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text("Rule-Change Delay: $delayMinutes minutes", style = MaterialTheme.typography.titleMedium)
              Slider(
                value = delayMinutes.toFloat(),
                onValueChange = { delayMinutes = (it.toInt() / 5) * 5 },
                valueRange = 5f..60f,
              )
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Require Biometric / Device Credential")
                Switch(checked = requireBiometric, onCheckedChange = { requireBiometric = it })
              }
              OutlinedTextField(
                value = emergencyPin,
                onValueChange = { emergencyPin = it },
                label = { Text("Owner Emergency Recovery Passcode") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              Button(
                onClick = {
                  if (requireBiometric) {
                    launchBiometricThen { onEnableInvincible(delayMinutes, true, emergencyPin) }
                  } else {
                    onEnableInvincible(delayMinutes, false, emergencyPin)
                  }
                },
                modifier = Modifier.fillMaxWidth().testTag("invincible_enable_btn"),
              ) {
                Text("Activate Invincible Mode")
              }
            }
          }
        }
      } else {
        item {
          SanctuaryCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              val pendingAt = settings.invinciblePendingUnlockAtEpochMs
              val now = System.currentTimeMillis()
              if (pendingAt == 0L) {
                Button(
                  onClick = {
                    if (settings.invincibleBiometricRequired) launchBiometricThen { onRequestUnlock() }
                    else onRequestUnlock()
                  },
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Text("Request Disable (${settings.invincibleDelayMinutes}m Cooldown)")
                }
              } else if (now < pendingAt) {
                val remMin = ((pendingAt - now) / 60_000L) + 1
                Text("Cooldown active: $remMin min remaining before rules unlock.")
              } else {
                Button(
                  onClick = { onCompleteDisable() },
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Text("Complete Disable Now")
                }
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text("Owner Emergency Recovery", style = MaterialTheme.typography.titleMedium)
              OutlinedTextField(
                value = recoveryPinInput,
                onValueChange = { recoveryPinInput = it },
                label = { Text("Emergency Recovery Passcode") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedButton(
                onClick = { onEmergencyDisable(recoveryPinInput) },
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text("Recover Immediately")
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MindfulSubPage(
  settings: FocusNestSettings,
  permissions: PermissionSnapshot,
  onBack: (() -> Unit)?,
  onUpdateMindful: (Boolean?, Int?, Int?, Boolean?, Boolean?, Int?, Boolean?, Int?) -> Unit,
  onToggleWindDownStep: (String) -> Unit,
  onRefreshPermissions: () -> Unit,
) {
  val context = LocalContext.current
  val todayKey = FocusTimeUtils.todayKey()
  val completedTonight =
    remember(settings.windDownDateKey, settings.windDownCompletedIds, todayKey) {
      if (settings.windDownDateKey == todayKey) settings.windDownCompletedIds else emptySet()
    }
  val totalSteps = BedtimeChecklistDefaults.steps.size.coerceAtLeast(1)

  Column(modifier = Modifier.fillMaxSize()) {
    SubPageHeader("Mindful Usage Tools", onBack)
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        SanctuaryCard {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Column(Modifier.weight(1f)) {
                Text("Bedtime Wind-Down", style = MaterialTheme.typography.titleMedium)
                Text(
                  "${FocusTimeUtils.formatMinuteOfDay(settings.bedtimeStartMinute)} – ${FocusTimeUtils.formatMinuteOfDay(settings.bedtimeEndMinute)}",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = settings.bedtimeEnabled,
                onCheckedChange = { onUpdateMindful(it, null, null, null, null, null, null, null) },
              )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text("Scheduled Do Not Disturb")
              Switch(
                checked = settings.bedtimeDndEnabled,
                onCheckedChange = { enabled ->
                  if (enabled && !permissions.dndPolicyAccessGranted) {
                    PermissionManager.safeStartSettings(context, PermissionManager.dndPolicySettingsIntent())
                    onRefreshPermissions()
                  } else {
                    onUpdateMindful(null, null, null, enabled, null, null, null, null)
                  }
                },
              )
            }
            Text(
              "Android Grayscale Note: System-wide or per-app grayscale requires Android Digital Wellbeing Bedtime Mode or Display Color settings.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = { PermissionManager.safeStartSettings(context, PermissionManager.displaySettingsIntent()) },
                modifier = Modifier.weight(1f).testTag("mindful_display_settings_btn"),
              ) {
                Text("Display & Dim")
              }
              OutlinedButton(
                onClick = { PermissionManager.safeStartSettings(context, PermissionManager.nightDisplaySettingsIntent()) },
                modifier = Modifier.weight(1f).testTag("mindful_night_light_btn"),
              ) {
                Text("Night Light")
              }
            }
          }
        }
      }

      item {
        SanctuaryCard(highlight = completedTonight.size == BedtimeChecklistDefaults.steps.size) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Evening Wind-Down Checklist", style = MaterialTheme.typography.titleMedium)
              Text(
                "${completedTonight.size} / $totalSteps tonight",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            LinearProgressIndicator(
              progress = { completedTonight.size.toFloat() / totalSteps.toFloat() },
              modifier = Modifier.fillMaxWidth(),
            )
            BedtimeChecklistDefaults.steps.forEach { step ->
              val checked = step.id in completedTonight
              Row(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .clickable { onToggleWindDownStep(step.id) }
                    .padding(vertical = 4.dp)
                    .testTag("wind_down_step_${step.id}"),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Checkbox(
                  checked = checked,
                  onCheckedChange = { onToggleWindDownStep(step.id) },
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(step.title, style = MaterialTheme.typography.titleSmall)
                  Text(
                    step.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }

      item {
        SanctuaryCard {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Column(Modifier.weight(1f)) {
                Text("Mindful Break Reminders", style = MaterialTheme.typography.titleMedium)
                Text("Every ${settings.breakReminderIntervalMinutes} minutes", style = MaterialTheme.typography.bodyMedium)
              }
              Switch(
                checked = settings.breakReminderEnabled,
                onCheckedChange = { onUpdateMindful(null, null, null, null, it, null, null, null) },
              )
            }
            Slider(
              value = settings.breakReminderIntervalMinutes.toFloat(),
              onValueChange = { onUpdateMindful(null, null, null, null, null, (it.toInt() / 15) * 15, null, null) },
              valueRange = 15f..120f,
            )
            Text("App Opening Cooldown: ${settings.appOpeningCooldownSeconds}s", style = MaterialTheme.typography.titleMedium)
            Slider(
              value = settings.appOpeningCooldownSeconds.toFloat(),
              onValueChange = { onUpdateMindful(null, null, null, null, null, null, null, it.toInt()) },
              valueRange = 3f..60f,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun NotificationsSubPage(
  settings: FocusNestSettings,
  permissions: PermissionSnapshot,
  records: List<NotificationRecordEntity>,
  onBack: (() -> Unit)?,
  onToggleFilter: (Boolean) -> Unit,
) {
  val context = LocalContext.current
  Column(modifier = Modifier.fillMaxSize()) {
    SubPageHeader("Notification Management", onBack)
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        SanctuaryCard(highlight = settings.notificationFilterEnabled) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Column(Modifier.weight(1f)) {
                Text("Local Focus Notification Filter", style = MaterialTheme.typography.titleMedium)
                Text(
                  "Mutes distracting notifications during active Focus sessions and logs summaries strictly on-device.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = settings.notificationFilterEnabled,
                onCheckedChange = onToggleFilter,
              )
            }
            if (!permissions.notificationListenerEnabled) {
              Button(
                onClick = {
                  PermissionManager.safeStartSettings(
                    context,
                    PermissionManager.notificationListenerSettingsIntent(),
                  )
                },
              ) { Text("Grant Notification Access") }
            }
          }
        }
      }
      item { SectionHeader("Local Notification History", subtitle = "Never transmitted off-device") }
      if (records.isEmpty()) {
        item {
          FocusNestEmptyState(
            icon = Icons.Default.NotificationsPaused,
            title = "No Filtered Notifications Logged",
            message = "Enable Notification Access and Focus Notification Filter to review muted notifications here.",
          )
        }
      } else {
        items(records.take(30), key = { it.id }) { rec ->
          SanctuaryCard {
            Column(modifier = Modifier.padding(14.dp)) {
              Text("${rec.appName} ${if (rec.wasMuted) "• Muted" else ""}", style = MaterialTheme.typography.titleMedium)
              if (rec.titleSummary.isNotBlank()) {
                Text(rec.titleSummary, style = MaterialTheme.typography.bodyMedium)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PermissionsSubPage(
  permissions: PermissionSnapshot,
  onBack: (() -> Unit)?,
  onRefresh: () -> Unit,
) {
  val context = LocalContext.current
  val oem = remember { OemCompatibilityHelper.getGuidance() }
  val notifLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onRefresh() }
  val cameraLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onRefresh() }

  Column(modifier = Modifier.fillMaxSize()) {
    SubPageHeader("Permissions & OEM Guide", onBack)
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      item {
        OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
          Text("Refresh All Permission States")
        }
      }
      item {
        PermissionRow("Usage Access (UsageStatsManager)", permissions.usageStatsGranted) {
          PermissionManager.safeStartSettings(context, PermissionManager.usageAccessIntent(context))
        }
      }
      item {
        PermissionRow("Accessibility (App & Reels Blocking)", permissions.accessibilityEnabled) {
          PermissionManager.safeStartSettings(context, PermissionManager.accessibilitySettingsIntent())
        }
      }
      item {
        PermissionRow("Local VPN Website Filter", permissions.vpnPrepared && permissions.vpnActive) {
          onRefresh()
        }
      }
      item {
        PermissionRow("Notification Listener", permissions.notificationListenerEnabled) {
          PermissionManager.safeStartSettings(context, PermissionManager.notificationListenerSettingsIntent())
        }
      }
      item {
        PermissionRow("Post Notifications", permissions.postNotificationsGranted) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          }
        }
      }
      item {
        PermissionRow("Camera (Offline QR/Barcode Scan)", permissions.cameraGranted) {
          cameraLauncher.launch(Manifest.permission.CAMERA)
        }
      }
      item {
        PermissionRow("Unrestricted Battery (Recommended)", permissions.ignoringBatteryOptimizations) {
          PermissionManager.safeStartSettings(context, PermissionManager.batteryOptimizationIntent(context))
        }
      }
      item {
        SanctuaryCard(highlight = oem.hasAggressiveBackgroundKilling) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${oem.manufacturer} • ${oem.androidVersionLabel}", style = MaterialTheme.typography.titleMedium)
            oem.steps.forEach { step ->
              Text("• $step", style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PermissionRow(title: String, granted: Boolean, onConfigure: () -> Unit) {
  SanctuaryCard(highlight = granted) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
          if (granted) "Active" else "Not enabled",
          style = MaterialTheme.typography.bodyMedium,
          color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      OutlinedButton(onClick = onConfigure) { Text("Configure") }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsSubPage(
  settings: FocusNestSettings,
  focusGoal: FocusGoalEntity,
  onBack: (() -> Unit)?,
  onUpdateAppearance: (ThemeMode?, AccentPalette?, Boolean?, String?) -> Unit,
  onUpdateGoals: (Int, Int, Int, Int) -> Unit,
  onOpenReleases: () -> Unit,
  onBuildEncryptedBackup: suspend (String) -> String,
  onRestoreBackup: suspend (String, String?) -> Result<Int>,
  onClearUsageHistory: () -> Unit,
  onClearNotificationHistory: () -> Unit,
  onClearHabits: () -> Unit,
  onDeleteAllData: () -> Unit,
  onShowMessage: (String) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var passphrase by remember { mutableStateOf("") }
  var pendingRestoreText by remember { mutableStateOf<String?>(null) }
  var showDeleteDialog by remember { mutableStateOf(false) }

  var screenGoal by remember(focusGoal) { mutableIntStateOf(focusGoal.dailyScreenTimeGoalMinutes) }
  var focusGoalMin by remember(focusGoal) { mutableIntStateOf(focusGoal.dailyFocusGoalMinutes) }

  val backupCreator =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
      if (uri != null) {
        scope.launch {
          runCatching {
            val payload = onBuildEncryptedBackup(passphrase)
            context.contentResolver.openOutputStream(uri)?.use { it.write(payload.toByteArray()) }
          }.onSuccess { onShowMessage("Encrypted AES-256-GCM backup saved.") }
            .onFailure { onShowMessage(it.message ?: "Backup failed.") }
        }
      }
    }

  val backupRestorer =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        if (text != null) {
          pendingRestoreText = text
        }
      }
    }

  Column(modifier = Modifier.fillMaxSize()) {
    SubPageHeader("Settings & Encrypted Backup", onBack)
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        SanctuaryCard {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Theme & Accent Color", style = MaterialTheme.typography.titleMedium)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              ThemeMode.entries.forEach { mode ->
                FilterChip(
                  selected = settings.themeMode == mode,
                  onClick = { onUpdateAppearance(mode, null, null, null) },
                  label = { Text(mode.label) },
                )
              }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              AccentPalette.entries.forEach { palette ->
                FilterChip(
                  selected = settings.accentPalette == palette,
                  onClick = { onUpdateAppearance(null, palette, null, null) },
                  label = { Text(palette.label) },
                )
              }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Material You Dynamic Color")
                Switch(
                  checked = settings.dynamicColorEnabled,
                  onCheckedChange = { onUpdateAppearance(null, null, it, null) },
                )
              }
            }
          }
        }
      }

      item {
        SanctuaryCard {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Text("App Language", style = MaterialTheme.typography.titleMedium)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf("en" to "English", "hi" to "हिन्दी (Hindi)").forEach { (code, label) ->
                FilterChip(
                  selected = settings.languageCode == code,
                  onClick = { onUpdateAppearance(null, null, null, code) },
                  label = { Text(label) },
                  modifier = Modifier.testTag("lang_chip_$code"),
                )
              }
            }
          }
        }
      }

      item {
        SanctuaryCard {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Daily & Weekly Goals", style = MaterialTheme.typography.titleMedium)
            }
            Text("Daily Screen Goal: ${FocusTimeUtils.formatDurationMinutes(screenGoal)}")
            Slider(value = screenGoal.toFloat(), onValueChange = { screenGoal = (it.toInt() / 15) * 15 }, valueRange = 60f..480f)
            Text("Daily Focus Goal: ${FocusTimeUtils.formatDurationMinutes(focusGoalMin)}")
            Slider(value = focusGoalMin.toFloat(), onValueChange = { focusGoalMin = (it.toInt() / 5) * 5 }, valueRange = 15f..240f)
            Button(
              onClick = {
                onUpdateGoals(screenGoal, focusGoalMin, (focusGoalMin / 25).coerceAtLeast(1), focusGoalMin * 7)
              },
              modifier = Modifier.fillMaxWidth(),
            ) { Text("Update Goals") }
          }
        }
      }

      item {
        SanctuaryCard {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Encrypted Local Backup & Restore", style = MaterialTheme.typography.titleMedium)
            }
            OutlinedTextField(
              value = passphrase,
              onValueChange = { passphrase = it },
              label = { Text("Backup Passphrase (min 6 chars)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                enabled = passphrase.length >= 6,
                onClick = { backupCreator.launch("focusnest_backup_${FocusTimeUtils.todayKey()}.json") },
                modifier = Modifier.weight(1f),
              ) { Text("Export Encrypted") }
              OutlinedButton(
                onClick = { backupRestorer.launch(arrayOf("application/json", "*/*")) },
                modifier = Modifier.weight(1f),
              ) { Text("Restore File") }
            }
          }
        }
      }

      item {
        SanctuaryCard {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Granular Privacy Cleanup", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedButton(onClick = onClearUsageHistory) { Text("Clear Usage History") }
              OutlinedButton(onClick = onClearNotificationHistory) { Text("Clear Notification Log") }
              OutlinedButton(onClick = onClearHabits) { Text("Clear Habits") }
            }
          }
        }
      }

      item {
        SanctuaryCard(highlight = true) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(8.dp))
              Text("App Version & Offline Updates", style = MaterialTheme.typography.titleMedium)
            }
            Text(
              "Installed: v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE}) • Repo: ${UpdateReleaseConfig.GITHUB_OWNER_REPO}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              "FocusNest declares zero INTERNET permission and never polls servers silently in the background. Checking for updates opens the public GitHub Releases page in your browser.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            UpdateReleaseConfig.latestHighlights.forEach { bullet ->
              Text("• $bullet", style = MaterialTheme.typography.bodySmall)
            }
            Button(
              onClick = onOpenReleases,
              modifier = Modifier.fillMaxWidth().testTag("more_check_updates_btn"),
            ) {
              Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Check Releases on GitHub")
            }
          }
        }
      }

      item {
        Button(
          onClick = { showDeleteDialog = true },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.fillMaxWidth().testTag("settings_delete_all_btn"),
        ) {
          Icon(Icons.Default.DeleteForever, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Delete All Local FocusNest Data")
        }
      }
    }
  }

  pendingRestoreText?.let { payload ->
    AlertDialog(
      onDismissRequest = { pendingRestoreText = null },
      title = { Text("Restore Backup") },
      text = {
        OutlinedTextField(
          value = passphrase,
          onValueChange = { passphrase = it },
          label = { Text("Passphrase (if encrypted)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            scope.launch {
              val res = onRestoreBackup(payload, passphrase.ifBlank { null })
              pendingRestoreText = null
              res.onSuccess { count -> onShowMessage("Restored $count items from backup.") }
                .onFailure { onShowMessage(it.message ?: "Restore failed.") }
            }
          }
        ) { Text("Restore") }
      },
      dismissButton = { TextButton(onClick = { pendingRestoreText = null }) { Text("Cancel") } },
    )
  }

  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = { Text("Delete All Local Data?") },
      text = { Text("This removes all saved profiles, focus sessions, usage snapshots, and tokens from this device.") },
      confirmButton = {
        Button(
          onClick = {
            onDeleteAllData()
            showDeleteDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) { Text("Delete Everything") }
      },
      dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
    )
  }
}

@Composable
private fun PrivacyAndAboutSubPage(
  onBack: (() -> Unit)?,
  onOpenReleases: () -> Unit,
) {
  val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
  val logoRes = if (isDarkSurface) R.drawable.ic_logo_dark else R.drawable.ic_logo_light

  Column(modifier = Modifier.fillMaxSize()) {
    SubPageHeader("Privacy, Security & Credits", onBack)
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        SanctuaryCard(highlight = true) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Image(
                painter = painterResource(logoRes),
                contentDescription = "FocusNest brand artwork",
                modifier = Modifier.size(44.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text("FocusNest v${BuildConfig.VERSION_NAME} • MIT Open Source", style = MaterialTheme.typography.titleMedium)
            }
            Text(
              "100% Free • No Ads • No Trackers • No Cloud Accounts • No Internet Permission.",
              style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedButton(
              onClick = onOpenReleases,
              modifier = Modifier.fillMaxWidth().testTag("privacy_check_updates_btn"),
            ) {
              Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("View Official GitHub Releases")
            }
          }
        }
      }
      item {
        SanctuaryCard {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Privacy & Threat Model Summary", style = MaterialTheme.typography.titleMedium)
            Text("• Room SQLite & DataStore Preferences remain local to private app storage.", style = MaterialTheme.typography.bodyMedium)
            Text("• Optional Accessibility & Notification Listener modules never transmit or log screen contents.", style = MaterialTheme.typography.bodyMedium)
            Text("• Optional Local VpnService runs strictly on-device DNS sinkholing with zero remote relays.", style = MaterialTheme.typography.bodyMedium)
            Text("• Portable backups use standard AES-256-GCM + PBKDF2WithHmacSHA256.", style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
      item {
        SanctuaryCard {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Open-Source Credits", style = MaterialTheme.typography.titleMedium)
            Text("• Jetpack Compose, Material 3, Room, DataStore, WorkManager, CameraX, Biometric (Apache 2.0)", style = MaterialTheme.typography.bodyMedium)
            Text("• ZXing Core offline QR & Barcode engine (Apache 2.0)", style = MaterialTheme.typography.bodyMedium)
            Text("• Outfit, Plus Jakarta Sans & JetBrains Mono fonts (SIL Open Font License)", style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    }
  }
}
