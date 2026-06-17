package com.javis.ai.domain

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds a live context string injected into AI calls so JAVIS
 * gives time-aware, situation-aware answers without asking.
 *
 * Example output:
 * "Current context: Tuesday evening, 7:43 PM. Battery: 34%, charging.
 *  Network: WiFi. User's recent app: com.whatsapp."
 */
@Singleton
class ContextEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun buildContext(): String {
        val sb = StringBuilder("Current context: ")
        sb.append(getTimeContext())
        sb.append(". Battery: ${getBatteryInfo()}")
        sb.append(". Network: ${getNetworkInfo()}")
        val activeApp = JavisAccessibilityService.instance?.getCurrentApp()
        if (!activeApp.isNullOrBlank() && activeApp != "com.javis.ai") {
            sb.append(". Active app: ${friendlyAppName(activeApp)}")
        }
        return sb.toString()
    }

    private fun getTimeContext(): String {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dayName = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")[cal.get(Calendar.DAY_OF_WEEK) - 1]
        val period = when {
            hour < 5  -> "late night"
            hour < 12 -> "morning"
            hour < 17 -> "afternoon"
            hour < 20 -> "evening"
            else      -> "night"
        }
        val h12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val min = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
        val amPm = if (hour < 12) "AM" else "PM"
        return "$dayName $period, $h12:$min $amPm"
    }

    private fun getBatteryInfo(): String {
        val intent = context.registerReceiver(null, IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (scale > 0) (level * 100 / scale) else -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return if (pct >= 0) "$pct%${if (charging) ", charging" else ""}" else "unknown"
    }

    private fun getNetworkInfo(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return "unknown"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nc = cm.getNetworkCapabilities(cm.activeNetwork)
            when {
                nc == null -> "offline"
                nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
                else -> "connected"
            }
        } else {
            @Suppress("DEPRECATION")
            when (cm.activeNetworkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> "WiFi"
                ConnectivityManager.TYPE_MOBILE -> "Mobile data"
                else -> "offline"
            }
        }
    }

    private fun friendlyAppName(pkg: String) = when {
        pkg.contains("whatsapp") -> "WhatsApp"
        pkg.contains("youtube") -> "YouTube"
        pkg.contains("chrome") || pkg.contains("browser") -> "browser"
        pkg.contains("instagram") -> "Instagram"
        pkg.contains("facebook") -> "Facebook"
        pkg.contains("twitter") || pkg.contains("twitterx") -> "Twitter/X"
        pkg.contains("tiktok") -> "TikTok"
        pkg.contains("telegram") -> "Telegram"
        pkg.contains("maps") -> "Maps"
        pkg.contains("gmail") -> "Gmail"
        pkg.contains("camera") -> "Camera"
        pkg.contains("gallery") || pkg.contains("photos") -> "Gallery"
        pkg.contains("settings") -> "Settings"
        else -> pkg.substringAfterLast('.')
    }
}
