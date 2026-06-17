package com.javis.ai

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class JavisApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            manager.createNotificationChannels(listOf(
                NotificationChannel(CHANNEL_ASSISTANT, "JAVIS Assistant", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "JAVIS assistant running in background"
                    setShowBadge(false)
                },
                NotificationChannel(CHANNEL_WAKE, "Wake Word Listener", NotificationManager.IMPORTANCE_MIN).apply {
                    description = "Always-on wake word detection — Say Hey Javis"
                    setShowBadge(false)
                },
                NotificationChannel(CHANNEL_ALERTS, "JAVIS Alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Important notifications from JAVIS"
                },
                NotificationChannel(CHANNEL_NOTIFICATION_SUMMARY, "Notification Summaries", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Summarized notifications read by JAVIS"
                }
            ))
        }
    }

    companion object {
        const val CHANNEL_ASSISTANT = "javis_assistant"
        const val CHANNEL_WAKE = "javis_wake"
        const val CHANNEL_ALERTS = "javis_alerts"
        const val CHANNEL_NOTIFICATION_SUMMARY = "javis_notif_summary"
        const val NOTIFICATION_ID_ASSISTANT = 1001
        const val NOTIFICATION_ID_WAKE = 1002
    }
}
