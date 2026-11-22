package com.example.ui.blocker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.FocusNestApplication
import com.example.R
import com.example.core.common.KeystoreAndBackupCrypto
import com.example.core.design.FocusNestLogoMark
import com.example.core.design.SanctuaryCard
import com.example.domain.model.UnlockChallengeEngine
import com.example.domain.model.UnlockChallengeType
import com.example.feature.challenges.OfflineQrBarcodeScannerDialog
import com.example.service.AppBlockerAccessibilityService
import com.example.ui.theme.FocusNestTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BlockedOverlayActivity : ComponentActivity() {
  private var nfcAdapter: NfcAdapter? = null
  private val scannedNfcUidState = mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    nfcAdapter = runCatching { NfcAdapter.getDefaultAdapter(this) }.getOrNull()

    val pkg = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: "com.example.app"
    val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: pkg.substringAfterLast('.')
    val reason = intent.getStringExtra(EXTRA_REASON) ?: "Protected by FocusNest"
    val profileName = intent.getStringExtra(EXTRA_PROFILE_NAME) ?: "FocusNest Shield"
    val challengeName = intent.getStringExtra(EXTRA_CHALLENGE_TYPE) ?: UnlockChallengeType.REFLECTION_REMINDER.name
    val isReelsPause = intent.getBooleanExtra(EXTRA_IS_REELS_PAUSE, false)
    val isLaunchPause = intent.getBooleanExtra(EXTRA_IS_LAUNCH_PAUSE, false)
    val launchPauseSeconds = intent.getIntExtra(EXTRA_LAUNCH_PAUSE_SECONDS, 8).coerceIn(5, 15)
    val challengeType = runCatching { UnlockChallengeType.valueOf(challengeName) }.getOrDefault(UnlockChallengeType.REFLECTION_REMINDER)

    val app = application as FocusNestApplication

    setContent {
      val settings by app.container.preferences.settingsFlow.collectAsState(initial = com.example.core.datastore.FocusNestSettings())
      val scope = rememberCoroutineScope()
      var errorText by remember { mutableStateOf<String?>(null) }

      FocusNestTheme(
        themeMode = settings.themeMode,
        accentPalette = settings.accentPalette,
        dynamicColor = settings.dynamicColorEnabled,
      ) {
        BlockedScreenContent(
          appName = appName,
          profileName = profileName,
          reason = reason,
          challengeType = challengeType,
          isReelsPause = isReelsPause,
          isLaunchPause = isLaunchPause,
          launchPauseSeconds = launchPauseSeconds,
          scannedNfcUid = scannedNfcUidState.value,
          errorText = errorText,
          hasEmergencyPasscode = settings.emergencyPasscodeHash.isNotBlank(),
          emergencyPasscodeHash = settings.emergencyPasscodeHash,
          onVerifyQrOrBarcode = { code, onSuccess ->
            scope.launch {
              val (token, errMsg) = app.container.blockerRepository.verifyAndConsumeQrOrBarcodeToken(code)
              if (token != null) onSuccess()
              else errorText = errMsg ?: "Code not registered in FocusNest NFC & QR Vault."
            }
          },
          onVerifyNfcUid = { uid, onSuccess ->
            scope.launch {
              val tag = app.container.blockerRepository.findNfcTag(uid)
              if (tag != null) onSuccess()
              else errorText = "NFC tag ($uid) is not registered."
            }
          },
          onProceedMindfulLaunch = {
            AppBlockerAccessibilityService.markLaunchPauseProceed(pkg)
            scope.launch {
              app.container.blockerRepository.recordMindfulLaunchProceed(
                packageName = pkg,
                appName = appName,
                pauseSeconds = launchPauseSeconds,
              )
              finish()
            }
          },
          onGrantUnlock = { minutes, intention ->
            scope.launch {
              val (ok, msg) =
                app.container.blockerRepository.grantTemporaryUnlock(
                  packageName = pkg,
                  appName = appName,
                  challengeType = challengeType,
                  durationMinutes = minutes,
                  intentionText = intention,
                )
              if (ok) {
                finish()
              } else {
                errorText = msg
              }
            }
          },
          onReturnHome = {
            val home = Intent(Intent.ACTION_MAIN).apply {
              addCategory(Intent.CATEGORY_HOME)
              flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(home)
            finish()
          },
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    nfcAdapter?.let { adapter ->
      runCatching {
        val pendingIntent =
          PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE,
          )
        adapter.enableForegroundDispatch(this, pendingIntent, null, null)
      }
    }
  }

  override fun onPause() {
    super.onPause()
    nfcAdapter?.let { runCatching { it.disableForegroundDispatch(this) } }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    val tag: Tag? =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
      } else {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
      }
    tag?.id?.let { bytes ->
      scannedNfcUidState.value = bytes.joinToString("") { "%02X".format(it) }
    }
  }

  companion object {
    const val EXTRA_PACKAGE_NAME = "extra_pkg"
    const val EXTRA_APP_NAME = "extra_app_name"
    const val EXTRA_REASON = "extra_reason"
    const val EXTRA_PROFILE_NAME = "extra_profile_name"
    const val EXTRA_CHALLENGE_TYPE = "extra_challenge"
    const val EXTRA_IS_REELS_PAUSE = "extra_reels_pause"
    const val EXTRA_IS_LAUNCH_PAUSE = "extra_launch_pause"
    const val EXTRA_LAUNCH_PAUSE_SECONDS = "extra_launch_pause_seconds"

    fun launchPreview(
      context: Context,
      packageName: String,
      appName: String,
      challengeType: UnlockChallengeType,
      isLaunchPause: Boolean = false,
      launchPauseSeconds: Int = 8,
    ) {
      context.startActivity(
        Intent(context, BlockedOverlayActivity::class.java).apply {
          putExtra(EXTRA_PACKAGE_NAME, packageName)
          putExtra(EXTRA_APP_NAME, appName)
          putExtra(
            EXTRA_REASON,
            if (isLaunchPause) "Mindful ${launchPauseSeconds}s breath before opening $appName"
            else "Preview of ${challengeType.label}",
          )
          putExtra(EXTRA_PROFILE_NAME, if (isLaunchPause) "Mindful Launch Pause" else "Interactive Preview")
          putExtra(EXTRA_CHALLENGE_TYPE, challengeType.name)
          putExtra(EXTRA_IS_LAUNCH_PAUSE, isLaunchPause)
          putExtra(EXTRA_LAUNCH_PAUSE_SECONDS, launchPauseSeconds)
        }
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlockedScreenContent(
  appName: String,
  profileName: String,
  reason: String,
  challengeType: UnlockChallengeType,
  isReelsPause: Boolean,
  isLaunchPause: Boolean,
  launchPauseSeconds: Int,
  scannedNfcUid: String?,
  errorText: String?,
  hasEmergencyPasscode: Boolean,
  emergencyPasscodeHash: String,
  onVerifyQrOrBarcode: (String, () -> Unit) -> Unit,
  onVerifyNfcUid: (String, () -> Unit) -> Unit,
  onProceedMindfulLaunch: () -> Unit,
  onGrantUnlock: (Int, String) -> Unit,
  onReturnHome: () -> Unit,
) {
  var chosenUnlockMinutes by remember { mutableIntStateOf(5) }
  var mathAttemptCount by remember { mutableIntStateOf(2) }
  var mathChallenge by remember(mathAttemptCount) { mutableStateOf(UnlockChallengeEngine.generateMathChallenge(mathAttemptCount)) }
  var mathInput by remember { mutableStateOf("") }
  val expectedSentence = remember { UnlockChallengeEngine.pickSentence(appName.hashCode()) }
  var sentenceInput by remember { mutableStateOf("") }
  var intentionInput by remember { mutableStateOf("") }
  var cooldownRemaining by remember { mutableIntStateOf(15) }
  var launchPauseRemaining by remember(launchPauseSeconds) { mutableIntStateOf(launchPauseSeconds) }
  var breathingSecondsLeft by remember { mutableIntStateOf(20) }
  var breathingPhaseText by remember { mutableStateOf("Inhale slowly…") }
  var breathingScaleTarget by remember { mutableStateOf(0.55f) }
  var showScanner by remember { mutableStateOf(false) }
  var showEmergencyBox by remember { mutableStateOf(false) }
  var emergencyPin by remember { mutableStateOf("") }
  var localError by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(challengeType, isLaunchPause, launchPauseSeconds) {
    if (isLaunchPause) {
      launchPauseRemaining = launchPauseSeconds
      while (launchPauseRemaining > 0) {
        val elapsed = launchPauseSeconds - launchPauseRemaining
        val half = (launchPauseSeconds / 2).coerceAtLeast(2)
        if (elapsed < half) {
          breathingPhaseText = "Inhale slowly… Is $appName intentional?"
          breathingScaleTarget = 1.0f
        } else {
          breathingPhaseText = "Exhale gently… Choose mindfully"
          breathingScaleTarget = 0.6f
        }
        delay(1000L)
        launchPauseRemaining--
      }
      breathingPhaseText = "Ready to decide"
      return@LaunchedEffect
    }
    if (challengeType == UnlockChallengeType.FIXED_COOLDOWN || challengeType == UnlockChallengeType.REFLECTION_REMINDER) {
      cooldownRemaining = 10
      while (cooldownRemaining > 0) {
        delay(1000L)
        cooldownRemaining--
      }
    }
    if (challengeType == UnlockChallengeType.BREATHING) {
      breathingSecondsLeft = 20
      for (step in 20 downTo 1) {
        breathingSecondsLeft = step
        val elapsed = 20 - step
        val cycleSec = elapsed % 10
        when {
          cycleSec < 4 -> {
            breathingPhaseText = "Inhale slowly… (4s)"
            breathingScaleTarget = 1.0f
          }
          cycleSec < 6 -> {
            breathingPhaseText = "Hold gently… (2s)"
            breathingScaleTarget = 1.0f
          }
          else -> {
            breathingPhaseText = "Exhale slowly… (4s)"
            breathingScaleTarget = 0.55f
          }
        }
        delay(1000L)
      }
      breathingSecondsLeft = 0
      breathingPhaseText = "Breathing cycle complete"
    }
  }

  LaunchedEffect(scannedNfcUid) {
    if (!scannedNfcUid.isNullOrBlank() && challengeType == UnlockChallengeType.NFC_SCAN) {
      onVerifyNfcUid(scannedNfcUid) { onGrantUnlock(chosenUnlockMinutes, "NFC unlock") }
    }
  }

  Scaffold { padding ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(24.dp)
          .verticalScroll(rememberScrollState())
          .widthIn(max = 520.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      androidx.compose.foundation.Image(
        painter = painterResource(if (isReelsPause || isLaunchPause) R.drawable.ic_focus_meditate else R.drawable.ic_blocking_shield),
        contentDescription = null,
        modifier = Modifier.size(64.dp),
      )
      Spacer(modifier = Modifier.height(8.dp))
      FocusNestLogoMark(size = 44.dp)
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text =
          when {
            isLaunchPause -> "Pause Before Opening $appName"
            isReelsPause -> "Take a Mindful Breath"
            else -> "$appName Is Paused"
          },
        style = MaterialTheme.typography.displayMedium,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "$profileName • $reason",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(18.dp))

      Button(
        onClick = onReturnHome,
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("blocked_return_home_btn"),
      ) {
        Icon(Icons.Default.Shield, contentDescription = null)
        Spacer(modifier = Modifier.padding(4.dp))
        Text(if (isLaunchPause) "Return to Focus" else "Stay Focused & Leave App")
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (isLaunchPause) {
        val animScale by animateFloatAsState(
          targetValue = breathingScaleTarget,
          animationSpec = tween(durationMillis = 1400),
          label = "launchPauseOrb",
        )
        val orbColor = MaterialTheme.colorScheme.primary
        SanctuaryCard(highlight = true) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(modifier = Modifier.size(132.dp), contentAlignment = Alignment.Center) {
              Canvas(modifier = Modifier.size(132.dp)) {
                drawCircle(
                  color = orbColor.copy(alpha = 0.22f),
                  radius = (size.minDimension / 2f) * animScale,
                )
                drawCircle(
                  color = orbColor.copy(alpha = 0.62f),
                  radius = (size.minDimension / 3.1f) * animScale,
                )
              }
              Text(
                text = if (launchPauseRemaining > 0) "${launchPauseRemaining}s" else "Ready",
                style = MaterialTheme.typography.headlineMedium,
              )
            }
            Text(breathingPhaseText, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            OutlinedButton(
              enabled = launchPauseRemaining == 0,
              onClick = onProceedMindfulLaunch,
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("blocked_proceed_mindfully_btn"),
            ) {
              Text(
                if (launchPauseRemaining > 0) "Wait ${launchPauseRemaining}s to Proceed"
                else "Proceed Mindfully to $appName"
              )
            }
          }
        }
        return@Scaffold
      }

      SanctuaryCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Unlock Method: ${challengeType.label}", style = MaterialTheme.typography.titleMedium)

          if (challengeType != UnlockChallengeType.STRICT_MODE) {
            Text("Temporary unlock window:", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf(1, 5, 10, 15).forEach { mins ->
                FilterChip(
                  selected = chosenUnlockMinutes == mins,
                  onClick = { chosenUnlockMinutes = mins },
                  label = { Text("${mins}m") },
                )
              }
            }
          }

          when (challengeType) {
            UnlockChallengeType.STRICT_MODE -> {
              Text(
                "Strict Mode is active. This app remains paused until your schedule or focus session completes.",
                style = MaterialTheme.typography.bodyMedium,
              )
            }
            UnlockChallengeType.FIXED_COOLDOWN,
            UnlockChallengeType.REFLECTION_REMINDER -> {
              Text(
                if (cooldownRemaining > 0) "Pause and breathe… unlock available in ${cooldownRemaining}s"
                else "Pause complete. Do you still intend to open $appName?",
                style = MaterialTheme.typography.bodyMedium,
              )
              Button(
                enabled = cooldownRemaining == 0,
                onClick = { onGrantUnlock(chosenUnlockMinutes, "Mindful cooldown") },
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text("Unlock for ${chosenUnlockMinutes}m")
              }
            }
            UnlockChallengeType.TEMP_UNLOCK_DURATION,
            UnlockChallengeType.DAILY_UNLOCK_LIMIT -> {
              Button(
                onClick = { onGrantUnlock(chosenUnlockMinutes, "Temporary unlock") },
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text("Grant ${chosenUnlockMinutes}m Unlock")
              }
            }
            UnlockChallengeType.ADAPTIVE_MATH -> {
              Text(mathChallenge.prompt, style = MaterialTheme.typography.headlineMedium)
              OutlinedTextField(
                value = mathInput,
                onValueChange = { mathInput = it },
                label = { Text("Answer") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = {
                    if (mathInput.trim().toIntOrNull() == mathChallenge.expectedAnswer) {
                      onGrantUnlock(chosenUnlockMinutes, "Solved adaptive math")
                    } else {
                      localError = "Incorrect answer. Try again."
                    }
                  },
                  modifier = Modifier.weight(1f),
                ) {
                  Text("Submit Answer")
                }
                OutlinedButton(
                  onClick = {
                    mathAttemptCount += 2
                    mathInput = ""
                    localError = null
                  },
                ) {
                  Text("New")
                }
              }
            }
            UnlockChallengeType.SENTENCE_TYPING -> {
              Text("Type verbatim:", style = MaterialTheme.typography.labelLarge)
              Text(expectedSentence, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
              OutlinedTextField(
                value = sentenceInput,
                onValueChange = { sentenceInput = it },
                label = { Text("Commitment sentence") },
                modifier = Modifier.fillMaxWidth(),
              )
              Button(
                onClick = {
                  if (UnlockChallengeEngine.validateSentence(expectedSentence, sentenceInput)) {
                    onGrantUnlock(chosenUnlockMinutes, "Commitment verified")
                  } else {
                    localError = "Sentence must match exactly."
                  }
                },
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text("Verify Sentence")
              }
            }
            UnlockChallengeType.INTENTION_ENTRY -> {
              Text("Quick reasons (tap to autofill):", style = MaterialTheme.typography.labelLarge)
              FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                UnlockChallengeEngine.predefinedIntentionReasons.forEach { preset ->
                  FilterChip(
                    selected = intentionInput == preset,
                    onClick = { intentionInput = preset },
                    label = { Text(preset) },
                  )
                }
              }
              OutlinedTextField(
                value = intentionInput,
                onValueChange = { intentionInput = it },
                label = { Text("Why do you need $appName right now? (min 3 words)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
              )
              Button(
                onClick = {
                  if (UnlockChallengeEngine.validateIntention(intentionInput)) {
                    onGrantUnlock(chosenUnlockMinutes, intentionInput)
                  } else {
                    localError = "Write a brief, specific intention (at least 3 words / 12 chars)."
                  }
                },
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text("Confirm Intention")
              }
            }
            UnlockChallengeType.BREATHING -> {
              val animScale by animateFloatAsState(
                targetValue = breathingScaleTarget,
                animationSpec = tween(durationMillis = 1500),
                label = "breathOrb",
              )
              val orbColor = MaterialTheme.colorScheme.primary
              Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                  Canvas(modifier = Modifier.size(120.dp)) {
                    drawCircle(
                      color = orbColor.copy(alpha = 0.25f),
                      radius = (size.minDimension / 2f) * animScale,
                    )
                    drawCircle(
                      color = orbColor.copy(alpha = 0.65f),
                      radius = (size.minDimension / 3.2f) * animScale,
                    )
                  }
                  Text(
                    text = if (breathingSecondsLeft > 0) "${breathingSecondsLeft}s" else "Done",
                    style = MaterialTheme.typography.titleLarge,
                  )
                }
                Text(breathingPhaseText, style = MaterialTheme.typography.bodyLarge)
                Button(
                  enabled = breathingSecondsLeft == 0,
                  onClick = { onGrantUnlock(chosenUnlockMinutes, "Completed mindful breathing cycle") },
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Text("Continue (${chosenUnlockMinutes}m)")
                }
              }
            }
            UnlockChallengeType.QR_SCAN,
            UnlockChallengeType.BARCODE_SCAN -> {
              Button(
                onClick = { showScanner = true },
                modifier = Modifier.fillMaxWidth(),
              ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Scan Registered ${challengeType.label}")
              }
            }
            UnlockChallengeType.NFC_SCAN -> {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Nfc, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Hold your registered FocusNest NFC tag to the back of your phone.")
              }
            }
          }

          (localError ?: errorText)?.let { err ->
            Text(err, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      TextButton(onClick = { showEmergencyBox = !showEmergencyBox }) {
        Text("Owner Emergency Recovery")
      }
      if (showEmergencyBox) {
        SanctuaryCard {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              "Device owner emergency bypass. Enter your FocusNest emergency passcode (if set) or confirm.",
              style = MaterialTheme.typography.bodyMedium,
            )
            if (hasEmergencyPasscode) {
              OutlinedTextField(
                value = emergencyPin,
                onValueChange = { emergencyPin = it },
                label = { Text("Emergency Passcode") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
            }
            OutlinedButton(
              onClick = {
                val ok =
                  !hasEmergencyPasscode ||
                    KeystoreAndBackupCrypto.sha256Hex(emergencyPin.trim()) == emergencyPasscodeHash
                if (ok) onGrantUnlock(5, "Owner emergency recovery")
                else localError = "Incorrect emergency passcode."
              },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text("Grant 5m Emergency Access")
            }
          }
        }
      }
    }
  }

  if (showScanner) {
    OfflineQrBarcodeScannerDialog(
      title = "Scan Unlock Token",
      onCodeScanned = { code ->
        showScanner = false
        onVerifyQrOrBarcode(code) { onGrantUnlock(chosenUnlockMinutes, "Token verified") }
      },
      onDismiss = { showScanner = false },
    )
  }
}
