package com.javis.ai.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.javis.ai.notifications.NotificationStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class JavisNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var notificationStore: NotificationStore

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        try {
            val pkg = sbn.packageName ?: return
            val extras = sbn.notification?.extras ?: return
            val title = extras.getCharSequence("android.title")?.toString() ?: return
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val bigText = extras.getCharSequence("android.bigText")?.toString() ?: text

            if (title.isBlank()) return

            Log.d("JavisNotifListener", "Notification from $pkg: $title")
            notificationStore.addNotification(
                packageName = pkg,
                title = title,
                text = bigText.ifEmpty { text },
                timestamp = sbn.postTime
            )
        } catch (e: Exception) {
            Log.e("JavisNotifListener", "Error processing notification", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Optional: track dismissed notifications
    }
}
