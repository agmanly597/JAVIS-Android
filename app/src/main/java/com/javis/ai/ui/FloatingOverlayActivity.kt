package com.javis.ai.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import com.javis.ai.services.FloatingWindowService

class FloatingOverlayActivity : Activity() {

    companion object {
        const val ACTION_REQUEST_PERMISSION = "com.javis.ai.REQUEST_OVERLAY_PERMISSION"
        const val ACTION_START_FLOAT = "com.javis.ai.START_FLOAT"
        const val ACTION_STOP_FLOAT = "com.javis.ai.STOP_FLOAT"
        const val REQUEST_OVERLAY = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (intent?.action) {
            ACTION_REQUEST_PERMISSION -> requestOverlayPermission()
            ACTION_STOP_FLOAT -> {
                stopService(Intent(this, FloatingWindowService::class.java))
                finish()
            }
            else -> startFloatIfAllowed()
        }
    }

    private fun startFloatIfAllowed() {
        if (canDrawOverlays()) {
            startFloatingService()
            finish()
        } else {
            requestOverlayPermission()
        }
    }

    private fun requestOverlayPermission() {
        if (canDrawOverlays()) {
            startFloatingService()
            finish()
            return
        }
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, REQUEST_OVERLAY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY) {
            if (canDrawOverlays()) {
                startFloatingService()
            }
            finish()
        }
    }

    private fun startFloatingService() {
        val serviceIntent = Intent(this, FloatingWindowService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun canDrawOverlays(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true
}
