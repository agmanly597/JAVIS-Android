package com.javis.ai.services

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.javis.ai.notifications.NotificationStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

data class NotificationInfo(
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val replyAction: Notification.Action? = null
)

@AndroidEntryPoint
class JavisNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var notificationStore: NotificationStore

    companion object {
        var instance: JavisNotificationListenerService? = null
        const val TAG = "JavisNotifListener"

        // Packages that have voice reply support (Notification.Action with RemoteInput)
        private val REPLY_SUPPORTED = setOf(
            "com.whatsapp", "com.whatsapp.w4b",
            "com.facebook.orca", "com.facebook.katana",
            "org.telegram.messenger",
            "com.instagram.android",
            "com.twitter.android", "com.twitter.android.lite",
            "com.google.android.gm",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms"
        )

        // Low-value apps to ignore
        private val IGNORED_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.javis.ai"
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        try {
            val pkg = sbn.packageName ?: return
            if (IGNORED_PACKAGES.any { pkg.startsWith(it) }) return

            val extras = sbn.notification?.extras ?: return
            val title = extras.getCharSequence("android.title")?.toString() ?: return
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val bigText = extras.getCharSequence("android.bigText")?.toString() ?: text

            if (title.isBlank()) return

            Log.d(TAG, "Notification from $pkg: $title — $text")

            notificationStore.addNotification(
                packageName = pkg,
                title = title,
                text = bigText.ifEmpty { text },
                timestamp = sbn.postTime
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Could track dismissals for memory/pattern learning
    }

    /**
     * Attempts to send a voice reply to the latest notification from a given app.
     * Uses the notification's RemoteInput reply action (same as Android quick reply).
     */
    fun replyToLatestNotification(packageName: String, replyText: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        return try {
            val activeNotifications = activeNotifications ?: return false
            val target = activeNotifications
                .filter { it.packageName == packageName }
                .maxByOrNull { it.postTime } ?: return false

            val actions = target.notification?.actions ?: return false
            val replyAction = actions.firstOrNull { action ->
                action.remoteInputs != null &&
                (action.title?.toString()?.lowercase()?.let {
                    it.contains("reply") || it.contains("respond") || it.contains("answer")
                } == true)
            } ?: return false

            val remoteInputs = replyAction.remoteInputs ?: return false

            val intent = Intent().apply {
                @Suppress("NewApi")
                android.app.RemoteInput.addResultsToIntent(remoteInputs, this,
                    android.os.Bundle().apply {
                        remoteInputs.forEach { ri -> putCharSequence(ri.resultKey, replyText) }
                    })
            }

            replyAction.actionIntent.send(this, 0, intent)
            Log.d(TAG, "Replied to $packageName: $replyText")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send reply", e)
            false
        }
    }

    /**
     * Get all active notifications as human-readable summary.
     */
    fun getNotificationSummary(): String {
        return try {
            val active = activeNotifications ?: return "No active notifications."
            val filtered = active.filter { !IGNORED_PACKAGES.any { p -> it.packageName.startsWith(p) } }
            if (filtered.isEmpty()) return "No new notifications."
            filtered.takeLast(5).joinToString(". ") { sbn ->
                val title = sbn.notification?.extras?.getCharSequence("android.title") ?: ""
                val text = sbn.notification?.extras?.getCharSequence("android.text") ?: ""
                "$title: $text"
            }
        } catch (e: Exception) {
            "Couldn't read notifications."
        }
    }

    fun canReply(packageName: String): Boolean = packageName in REPLY_SUPPORTED

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
