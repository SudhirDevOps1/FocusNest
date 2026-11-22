package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.FocusNestApplication
import com.example.MainActivity
import com.example.R
import com.example.core.database.WebsiteRuleEntity
import com.example.domain.blocker.RuleEvaluator
import com.example.domain.model.VpnConnectionState
import com.example.domain.model.VpnDiagnostics
import com.example.domain.model.WebsiteRuleEvaluation
import com.example.domain.model.WebsiteRuleOutcome
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 100% on-device local DNS sinkhole VpnService.
 * - Started ONLY after explicit VpnService.prepare() consent from the user.
 * - Never sends browsing history or packet contents to any remote server.
 * - Enforces Android 14+ foregroundServiceType="specialUse" safely by promoting immediately
 *   inside onStartCommand() before asynchronous DataStore reads or tunnel establishment.
 */
class FocusNestVpnService : VpnService() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var vpnInterface: ParcelFileDescriptor? = null
  private var startJob: Job? = null
  private var packetLoopJob: Job? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP -> {
        stopVpn()
        return START_NOT_STICKY
      }
      ACTION_SYNC_RULES -> {
        serviceScope.launch { refreshRuleDiagnostics(applicationContext as? FocusNestApplication) }
        return if (isRunning) START_STICKY else START_NOT_STICKY
      }
      else -> {
        // Call startForeground immediately to satisfy Android 12-16 FGS timeout guarantees.
        promoteForeground("Starting local on-device DNS filter…")
        startVpnAsync()
        return START_STICKY
      }
    }
  }

  private fun promoteForeground(statusText: String) {
    val notification = buildVpnNotification(statusText)
    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startForeground(
          VPN_NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
      } else {
        startForeground(VPN_NOTIFICATION_ID, notification)
      }
    }
  }

  private fun startVpnAsync() {
    if (vpnInterface != null && currentState == VpnConnectionState.CONNECTED) return
    startJob?.cancel()
    updateDiagnostics(
      state = VpnConnectionState.CONNECTING,
      statusMessage = "Establishing local DNS sinkhole interface…",
      lastError = null,
    )

    startJob =
      serviceScope.launch {
        val app = applicationContext as? FocusNestApplication
        if (app == null) {
          failStart("Application container unavailable.")
          return@launch
        }

        if (prepare(applicationContext) != null) {
          failStart("Android VPN consent not granted. Tap Start Local Website Filter to authorize.")
          return@launch
        }

        val settings = runCatching { app.container.preferences.settingsFlow.first() }.getOrNull()
        val scopedPackages = settings?.blockedNetworkPackages.orEmpty()
        val rules = runCatching { app.container.database.websiteRuleDao().getAll() }.getOrDefault(emptyList())
        val enabledRules = rules.filter { it.isEnabled }

        val established =
          runCatching {
            val builder =
              Builder()
                .setSession("FocusNest Local Website Filter")
                .addAddress("10.88.0.2", 32)
                .addDnsServer("10.88.0.1")
                .addRoute("10.88.0.1", 32)
                .setMtu(1500)

            if (scopedPackages.isNotEmpty()) {
              scopedPackages.forEach { targetPkg ->
                if (targetPkg != packageName) {
                  runCatching { builder.addAllowedApplication(targetPkg) }
                }
              }
            } else {
              runCatching { builder.addDisallowedApplication(packageName) }
            }
            builder.establish()
          }

        val fdDescriptor = established.getOrNull()
        val err = established.exceptionOrNull()
        if (fdDescriptor == null) {
          val reason =
            err?.message?.takeIf { it.isNotBlank() }
              ?: "Could not establish local VPN interface (another VPN app may be active or revoked)."
          failStart(reason)
          return@launch
        }

        vpnInterface = fdDescriptor
        isRunning = true
        val now = System.currentTimeMillis()
        updateDiagnostics(
          state = VpnConnectionState.CONNECTED,
          statusMessage = "On-device DNS filter active • ${enabledRules.size} active rules",
          lastError = null,
          activeRuleCount = enabledRules.size,
          blocklistCount = enabledRules.count { !it.isAllowlist },
          allowlistCount = enabledRules.count { it.isAllowlist },
          lastRuleSyncEpochMs = now,
          isPerAppScoped = scopedPackages.isNotEmpty(),
          scopedPackageCount = scopedPackages.size,
        )
        promoteForeground("Local DNS filter active • ${enabledRules.size} rules on-device")
        runCatching { app.container.preferences.updateSettings { it.copy(vpnBlockingEnabled = true) } }

        packetLoopJob?.cancel()
        packetLoopJob =
          serviceScope.launch {
            runPacketLoop(app, fdDescriptor)
          }
      }
  }

  private suspend fun runPacketLoop(
    app: FocusNestApplication,
    fdDescriptor: ParcelFileDescriptor,
  ) {
    val fd = fdDescriptor.fileDescriptor
    val input = FileInputStream(fd)
    val output = FileOutputStream(fd)
    val buffer = ByteArray(4096)

    while (serviceScope.isActive && vpnInterface === fdDescriptor && isRunning) {
      val length = runCatching { input.read(buffer) }.getOrDefault(-1)
      if (length <= 0) {
        if (!isRunning) break
        continue
      }

      val domain = parseDnsQueryDomain(buffer, length) ?: continue
      val currentDiag = _diagnosticsFlow.value
      val rules = runCatching { app.container.database.websiteRuleDao().getAll() }.getOrDefault(emptyList())
      val eval = RuleEvaluator.evaluateDomain(domain, rules)

      var blockedIncrement = 0L
      if (eval.outcome == WebsiteRuleOutcome.BLOCKED) {
        blockedIncrement = 1L
        val hitTarget = eval.matchedRuleDomain ?: domain
        runCatching { app.container.database.websiteRuleDao().incrementHits(hitTarget) }
        buildDnsNxdomainResponse(buffer, length)?.let { response ->
          runCatching { output.write(response) }
        }
      }

      _diagnosticsFlow.value =
        currentDiag.copy(
          packetsInspected = currentDiag.packetsInspected + 1L,
          blockedQueriesCount = currentDiag.blockedQueriesCount + blockedIncrement,
          lastRuleSyncEpochMs = System.currentTimeMillis(),
          activeRuleCount = eval.evaluatedRuleCount,
        )
    }
  }

  private suspend fun refreshRuleDiagnostics(app: FocusNestApplication?) {
    if (app == null) return
    val rules = runCatching { app.container.database.websiteRuleDao().getAll() }.getOrDefault(emptyList())
    val enabled = rules.filter { it.isEnabled }
    val settings = runCatching { app.container.preferences.settingsFlow.first() }.getOrNull()
    val prev = _diagnosticsFlow.value
    _diagnosticsFlow.value =
      prev.copy(
        activeRuleCount = enabled.size,
        blocklistCount = enabled.count { !it.isAllowlist },
        allowlistCount = enabled.count { it.isAllowlist },
        lastRuleSyncEpochMs = System.currentTimeMillis(),
        isPerAppScoped = settings?.blockedNetworkPackages?.isNotEmpty() == true,
        scopedPackageCount = settings?.blockedNetworkPackages?.size ?: 0,
      )
  }

  private fun failStart(message: String) {
    isRunning = false
    runCatching { vpnInterface?.close() }
    vpnInterface = null
    updateDiagnostics(
      state = VpnConnectionState.ERROR,
      statusMessage = "Local website filter failed to start",
      lastError = message,
    )
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  private fun stopVpn() {
    updateDiagnostics(
      state = VpnConnectionState.DISCONNECTING,
      statusMessage = "Stopping local DNS filter…",
      lastError = null,
    )
    isRunning = false
    startJob?.cancel()
    packetLoopJob?.cancel()
    runCatching { vpnInterface?.close() }
    vpnInterface = null

    val app = applicationContext as? FocusNestApplication
    if (app != null) {
      CoroutineScope(Dispatchers.IO).launch {
        runCatching { app.container.preferences.updateSettings { it.copy(vpnBlockingEnabled = false) } }
      }
    }
    updateDiagnostics(
      state = VpnConnectionState.DISCONNECTED,
      statusMessage = "Local on-device DNS sinkhole inactive",
      lastError = null,
    )
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  private fun buildVpnNotification(contentText: String): Notification {
    val openIntent =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )
    return NotificationCompat.Builder(this, FocusNestApplication.CHANNEL_VPN)
      .setSmallIcon(R.drawable.ic_launcher_monochrome)
      .setContentTitle("FocusNest Local Website Filter")
      .setContentText(contentText)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setContentIntent(openIntent)
      .build()
  }

  override fun onRevoke() {
    updateDiagnostics(
      state = VpnConnectionState.ERROR,
      statusMessage = "VPN permission revoked by system or another VPN",
      lastError = "Re-authorize Local Website Filter when ready.",
    )
    isRunning = false
    startJob?.cancel()
    packetLoopJob?.cancel()
    runCatching { vpnInterface?.close() }
    vpnInterface = null
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
    super.onRevoke()
  }

  override fun onDestroy() {
    isRunning = false
    startJob?.cancel()
    packetLoopJob?.cancel()
    runCatching { vpnInterface?.close() }
    vpnInterface = null
    if (_diagnosticsFlow.value.state == VpnConnectionState.CONNECTED) {
      updateDiagnostics(
        state = VpnConnectionState.DISCONNECTED,
        statusMessage = "Local on-device DNS sinkhole stopped",
        lastError = null,
      )
    }
    serviceScope.cancel()
    super.onDestroy()
  }

  companion object {
    @Volatile var isRunning: Boolean = false

    private val _diagnosticsFlow = MutableStateFlow(VpnDiagnostics())
    val diagnosticsFlow: StateFlow<VpnDiagnostics> = _diagnosticsFlow.asStateFlow()

    val currentState: VpnConnectionState
      get() = _diagnosticsFlow.value.state

    const val VPN_NOTIFICATION_ID = 4002
    const val ACTION_START = "com.example.focusnest.VPN_START"
    const val ACTION_STOP = "com.example.focusnest.VPN_STOP"
    const val ACTION_SYNC_RULES = "com.example.focusnest.VPN_SYNC_RULES"

    internal fun updateDiagnostics(
      state: VpnConnectionState,
      statusMessage: String,
      lastError: String? = null,
      activeRuleCount: Int = _diagnosticsFlow.value.activeRuleCount,
      blocklistCount: Int = _diagnosticsFlow.value.blocklistCount,
      allowlistCount: Int = _diagnosticsFlow.value.allowlistCount,
      lastRuleSyncEpochMs: Long = _diagnosticsFlow.value.lastRuleSyncEpochMs,
      isPerAppScoped: Boolean = _diagnosticsFlow.value.isPerAppScoped,
      scopedPackageCount: Int = _diagnosticsFlow.value.scopedPackageCount,
    ) {
      _diagnosticsFlow.value =
        _diagnosticsFlow.value.copy(
          state = state,
          statusMessage = statusMessage,
          lastError = lastError,
          activeRuleCount = activeRuleCount,
          blocklistCount = blocklistCount,
          allowlistCount = allowlistCount,
          lastRuleSyncEpochMs = lastRuleSyncEpochMs,
          isPerAppScoped = isPerAppScoped,
          scopedPackageCount = scopedPackageCount,
        )
    }

    fun start(context: Context) {
      updateDiagnostics(
        state = VpnConnectionState.CONNECTING,
        statusMessage = "Requesting local DNS filter startup…",
        lastError = null,
      )
      val intent = Intent(context, FocusNestVpnService::class.java).apply { action = ACTION_START }
      context.startForegroundService(intent)
    }

    fun stop(context: Context) {
      val intent = Intent(context, FocusNestVpnService::class.java).apply { action = ACTION_STOP }
      context.startService(intent)
    }

    fun syncRules(context: Context) {
      if (!isRunning) return
      val intent = Intent(context, FocusNestVpnService::class.java).apply { action = ACTION_SYNC_RULES }
      runCatching { context.startService(intent) }
    }

    fun domainMatches(queried: String, ruleDomain: String): Boolean =
      RuleEvaluator.domainMatches(queried, ruleDomain)

    fun evaluateDomain(domain: String, rules: List<WebsiteRuleEntity>): WebsiteRuleEvaluation =
      RuleEvaluator.evaluateDomain(domain, rules)

    internal fun parseDnsQueryDomain(packet: ByteArray, length: Int): String? {
      if (length < 28 || length > packet.size) return null
      val version = (packet[0].toInt() ushr 4) and 0x0F
      if (version != 4) return null
      val ihl = (packet[0].toInt() and 0x0F) * 4
      if (ihl < 20) return null
      val protocol = packet[9].toInt() and 0xFF
      if (protocol != 17 || length < ihl + 8 + 12) return null // UDP only
      val dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
      if (dstPort != 53) return null

      var pos = ihl + 8 + 12 // IP + UDP + DNS header
      val labels = mutableListOf<String>()
      while (pos < length) {
        val labelLen = packet[pos].toInt() and 0xFF
        if (labelLen == 0) break
        if ((labelLen and 0xC0) != 0 || pos + 1 + labelLen > length) return null
        labels.add(String(packet, pos + 1, labelLen, Charsets.US_ASCII))
        pos += 1 + labelLen
      }
      return if (labels.isEmpty()) null else labels.joinToString(".")
    }

    internal fun buildDnsNxdomainResponse(queryPacket: ByteArray, length: Int): ByteArray? {
      if (length < 28 || length > queryPacket.size) return null
      val ihl = (queryPacket[0].toInt() and 0x0F) * 4
      if (ihl < 20 || length < ihl + 8 + 12) return null
      val resp = queryPacket.copyOf(length)
      // Swap IPv4 src/dst
      for (i in 0..3) {
        val tmp = resp[12 + i]
        resp[12 + i] = resp[16 + i]
        resp[16 + i] = tmp
      }
      // Swap UDP src/dst ports
      val s0 = resp[ihl]
      val s1 = resp[ihl + 1]
      resp[ihl] = resp[ihl + 2]
      resp[ihl + 1] = resp[ihl + 3]
      resp[ihl + 2] = s0
      resp[ihl + 3] = s1
      // Zero UDP checksum (valid for IPv4 UDP)
      resp[ihl + 6] = 0
      resp[ihl + 7] = 0
      // Set DNS QR=1, RCODE=3 (NXDOMAIN)
      val dnsStart = ihl + 8
      resp[dnsStart + 2] = (resp[dnsStart + 2].toInt() or 0x80).toByte()
      resp[dnsStart + 3] = ((resp[dnsStart + 3].toInt() and 0xF0) or 0x03).toByte()
      // Recompute IPv4 header checksum
      resp[10] = 0
      resp[11] = 0
      var sum = 0
      val buf = ByteBuffer.wrap(resp, 0, ihl)
      for (i in 0 until ihl step 2) {
        sum += buf.getShort(i).toInt() and 0xFFFF
      }
      while ((sum ushr 16) != 0) {
        sum = (sum and 0xFFFF) + (sum ushr 16)
      }
      val checksum = sum.inv() and 0xFFFF
      resp[10] = (checksum ushr 8).toByte()
      resp[11] = (checksum and 0xFF).toByte()
      return resp
    }
  }
}
