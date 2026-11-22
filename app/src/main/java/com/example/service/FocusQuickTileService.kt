package com.example.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.example.FocusNestApplication
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android Quick Settings Tile to toggle FocusNest Quick Shield directly from the notification shade.
 */
class FocusQuickTileService : TileService() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onStartListening() {
    super.onStartListening()
    refreshTile()
  }

  override fun onClick() {
    super.onClick()
    val app = applicationContext as? FocusNestApplication ?: return
    scope.launch {
      val current = app.container.preferences.settingsFlow.first()
      val next = !current.quickBlockerEnabled
      if (!next && current.invincibleModeEnabled) {
        withContext(Dispatchers.Main) {
          Toast.makeText(
            applicationContext,
            "Invincible Mode protects Quick Shield until cooldown finishes.",
            Toast.LENGTH_SHORT,
          ).show()
        }
      } else {
        app.container.preferences.updateSettings { it.copy(quickBlockerEnabled = next) }
        WidgetUpdater.refreshAll(applicationContext)
      }
      refreshTile()
    }
  }

  private fun refreshTile() {
    val app = applicationContext as? FocusNestApplication ?: return
    scope.launch {
      val enabled = app.container.preferences.settingsFlow.first().quickBlockerEnabled
      withContext(Dispatchers.Main) {
        val tile = qsTile ?: return@withContext
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Focus Shield"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          tile.subtitle = if (enabled) "Blocking ON" else "Standby"
        }
        tile.updateTile()
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    scope.cancel()
  }
}
