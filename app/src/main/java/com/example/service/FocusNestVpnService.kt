package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.FocusNestApplication
import com.example.MainActivity
import com.example.R
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 100% on-device local DNS sinkhole VpnService.
 * - Started ONLY after explicit VpnService.prepare() consent from the user.
 * - Never sends browsing history or packet contents to any remote server.
 * - Routes only a local virtual DNS address (10.88.0.1) so blocked domains receive local NXDOMAIN
 *   while non-DNS traffic stays unaffected.
 */
class FocusNestVpnService : VpnService() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var vpnInterface: ParcelFileDescriptor? = null
  private var packetLoopJob: Job? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP) {
      stopVpn()
      return START_NOT_STICKY
    }
    startVpn()
    return START_STICKY
  }

  private fun startVpn() {
    if (vpnInterface != null) return
    val app = applicationContext as FocusNestApplication
    val settings = kotlinx.coroutines.runBlocking { app.container.preferences.settingsFlow.first() }

    val builder =
      Builder()
        .setSession("FocusNest Local Website Filter")
        .addAddress("10.88.0.2", 32)
        .addDnsServer("10.88.0.1")
        .addRoute("10.88.0.1", 32)
        .setMtu(1500)
    if (settings.blockedNetworkPackages.isNotEmpty()) {
      settings.blockedNetworkPackages.forEach { targetPkg ->
        if (targetPkg != packageName) {
          runCatching { builder.addAllowedApplication(targetPkg) }
        }
      }
    } else {
      runCatching { builder.addDisallowedApplication(packageName) }
    }

    vpnInterface = runCatching { builder.establish() }.getOrNull()
    if (vpnInterface == null) {
      isRunning = false
      stopSelf()
      return
    }

    isRunning = true
    startForeground(VPN_NOTIFICATION_ID, buildVpnNotification())

    packetLoopJob =
      serviceScope.launch {
        app.container.preferences.updateSettings { it.copy(vpnBlockingEnabled = true) }
        val fd = vpnInterface?.fileDescriptor ?: return@launch
        val input = FileInputStream(fd)
        val output = FileOutputStream(fd)
        val buffer = ByteArray(4096)

        while (isActive && vpnInterface != null) {
          val length = runCatching { input.read(buffer) }.getOrDefault(-1)
          if (length <= 0) continue

          val domain = parseDnsQueryDomain(buffer, length) ?: continue
          val rules = app.container.database.websiteRuleDao().getAll().filter { it.isEnabled }
          val allowlisted = rules.any { it.isAllowlist && domainMatches(domain, it.domain) }
          val blocklisted =
            !allowlisted && rules.any { !it.isAllowlist && domainMatches(domain, it.domain) }

          if (blocklisted) {
            app.container.database.websiteRuleDao().incrementHits(domain)
            buildDnsNxdomainResponse(buffer, length)?.let { response ->
              runCatching { output.write(response) }
            }
          }
        }
      }
  }

  private fun stopVpn() {
    isRunning = false
    packetLoopJob?.cancel()
    runCatching { vpnInterface?.close() }
    vpnInterface = null
    val app = applicationContext as? FocusNestApplication
    if (app != null) {
      CoroutineScope(Dispatchers.IO).launch {
        app.container.preferences.updateSettings { it.copy(vpnBlockingEnabled = false) }
      }
    }
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  private fun buildVpnNotification(): Notification {
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
      .setContentText("On-device DNS domain filter active • Zero external servers")
      .setOngoing(true)
      .setContentIntent(openIntent)
      .build()
  }

  override fun onRevoke() {
    stopVpn()
    super.onRevoke()
  }

  override fun onDestroy() {
    isRunning = false
    packetLoopJob?.cancel()
    runCatching { vpnInterface?.close() }
    serviceScope.cancel()
    super.onDestroy()
  }

  companion object {
    @Volatile var isRunning: Boolean = false
    const val VPN_NOTIFICATION_ID = 4002
    const val ACTION_START = "com.example.focusnest.VPN_START"
    const val ACTION_STOP = "com.example.focusnest.VPN_STOP"

    fun start(context: Context) {
      val intent = Intent(context, FocusNestVpnService::class.java).apply { action = ACTION_START }
      context.startForegroundService(intent)
    }

    fun stop(context: Context) {
      val intent = Intent(context, FocusNestVpnService::class.java).apply { action = ACTION_STOP }
      context.startService(intent)
    }

    fun domainMatches(queried: String, ruleDomain: String): Boolean {
      val q = queried.trim().lowercase().trimEnd('.')
      val r = ruleDomain.trim().lowercase().removePrefix("*.").trimEnd('.')
      if (r.isEmpty()) return false
      return q == r || q.endsWith(".$r")
    }

    internal fun parseDnsQueryDomain(packet: ByteArray, length: Int): String? {
      if (length < 28) return null
      val version = (packet[0].toInt() ushr 4) and 0x0F
      if (version != 4) return null
      val ihl = (packet[0].toInt() and 0x0F) * 4
      val protocol = packet[9].toInt() and 0xFF
      if (protocol != 17 || length < ihl + 8 + 12) return null // UDP only
      val dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
      if (dstPort != 53) return null

      var pos = ihl + 8 + 12 // IP + UDP + DNS header
      val labels = mutableListOf<String>()
      while (pos < length) {
        val labelLen = packet[pos].toInt() and 0xFF
        if (labelLen == 0) break
        if (pos + 1 + labelLen > length) return null
        labels.add(String(packet, pos + 1, labelLen, Charsets.US_ASCII))
        pos += 1 + labelLen
      }
      return if (labels.isEmpty()) null else labels.joinToString(".")
    }

    internal fun buildDnsNxdomainResponse(queryPacket: ByteArray, length: Int): ByteArray? {
      if (length < 28) return null
      val ihl = (queryPacket[0].toInt() and 0x0F) * 4
      if (length < ihl + 8 + 12) return null
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
