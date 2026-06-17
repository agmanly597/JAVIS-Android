package com.javis.ai.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class JavisNotification(
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val isRead: Boolean = false
)

@Singleton
class NotificationStore @Inject constructor() {
    private val _notifications = MutableStateFlow<List<JavisNotification>>(emptyList())
    val notifications: StateFlow<List<JavisNotification>> = _notifications

    private val appNameCache = mutableMapOf<String, String>()

    fun addNotification(packageName: String, title: String, text: String, timestamp: Long) {
        val appName = appNameCache.getOrDefault(packageName, friendlyAppName(packageName))
        val notif = JavisNotification(
            packageName = packageName,
            appName = appName,
            title = title,
            text = text,
            timestamp = timestamp
        )
        val current = _notifications.value.toMutableList()
        current.add(0, notif)
        if (current.size > 200) current.removeAt(current.lastIndex)
        _notifications.value = current
    }

    fun markAllRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }

    fun clearAll() {
        _notifications.value = emptyList()
    }

    fun getUnreadCount(): Int = _notifications.value.count { !it.isRead }

    fun getSummary(): String {
        val unread = _notifications.value.filter { !it.isRead }
        if (unread.isEmpty()) return "No new notifications."
        val byApp = unread.groupBy { it.appName }
        return buildString {
            append("You have ${unread.size} unread notification${if (unread.size > 1) "s" else ""}. ")
            byApp.entries.take(4).forEach { (app, notifs) ->
                append("$app: ${notifs.size}. ")
            }
        }
    }

    private fun friendlyAppName(pkg: String): String {
        return when {
            pkg.contains("whatsapp") -> "WhatsApp"
            pkg.contains("telegram") -> "Telegram"
            pkg.contains("gmail") -> "Gmail"
            pkg.contains("youtube") -> "YouTube"
            pkg.contains("instagram") -> "Instagram"
            pkg.contains("facebook") -> "Facebook"
            pkg.contains("twitter") -> "X (Twitter)"
            pkg.contains("sms") || pkg.contains("mms") -> "Messages"
            else -> pkg.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: pkg
        }
    }
}
