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

        Log.d("BootReceiver", "Boot received, checking auto-start setting")

        val autoStart = runBlocking {
            context.dataStore.data
                .map { it[booleanPreferencesKey("auto_start_on_boot")] ?: true }
                .first()
        }

        if (autoStart) {
            Log.d("BootReceiver", "Auto-starting JAVIS service")
            val serviceIntent = Intent(context, JavisAssistantService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
