package com.javis.ai.tiles

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.javis.ai.services.JavisAssistantService

class JavisTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile(isServiceRunning())
    }

    override fun onClick() {
        super.onClick()
        if (isServiceRunning()) {
            stopService(Intent(this, JavisAssistantService::class.java))
            updateTile(false)
        } else {
            val intent = Intent(this, JavisAssistantService::class.java).apply {
                action = JavisAssistantService.ACTION_ACTIVATE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            updateTile(true)
        }
    }

    private fun updateTile(active: Boolean) {
        qsTile?.apply {
            state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "JAVIS"
            subtitle = if (active) "Listening..." else "Tap to activate"
            updateTile()
        }
    }

    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(android.app.ActivityManager::class.java)
        return manager.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == JavisAssistantService::class.java.name
        }
    }
}
