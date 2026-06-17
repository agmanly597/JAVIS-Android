package com.javis.ai.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.javis.ai.services.JavisAssistantService

class FloatingOverlayActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, JavisAssistantService::class.java).apply {
            action = JavisAssistantService.ACTION_ACTIVATE
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        finish()
    }
}
