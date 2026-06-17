package com.javis.ai.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.javis.ai.settings.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        Log.d("BootReceiver", "Boot received")

        val prefs = runBlocking {
            context.dataStore.data.first()
        }

        val autoStart = prefs[booleanPreferencesKey("auto_start_on_boot")] ?: true
        val wakeWordEnabled = prefs[booleanPreferencesKey("wake_word_enabled")] ?: false

        if (autoStart) {
            Log.d("BootReceiver", "Auto-starting JAVIS assistant service")
            startSvc(context, JavisAssistantService::class.java)
        }

        if (wakeWordEnabled) {
            Log.d("BootReceiver", "Starting wake word service")
            startSvc(context, WakeWordService::class.java)
        }
    }

    private fun <T : android.app.Service> startSvc(context: Context, cls: Class<T>) {
        val svcIntent = Intent(context, cls)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(svcIntent)
        } else {
            context.startService(svcIntent)
        }
    }
}
