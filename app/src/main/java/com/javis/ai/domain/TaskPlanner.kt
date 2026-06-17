package com.javis.ai.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.util.Log
import com.javis.ai.apps.AppLauncher
import com.javis.ai.calls.CallManager
import com.javis.ai.memory.MemoryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class TaskResult(
    val success: Boolean,
    val spokenResponse: String,
    val requiresAI: Boolean = false,
    val pendingConfirmation: PendingAction? = null
)

data class PendingAction(
    val type: String,
    val description: String,
    val data: Map<String, String> = emptyMap()
)

@Singleton
class TaskPlanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appLauncher: AppLauncher,
    private val callManager: CallManager,
    private val memoryManager: MemoryManager
) {
    suspend fun execute(intent: ParsedIntent): TaskResult {
        return when (intent.type) {
            IntentType.OPEN_APP -> handleOpenApp(intent)
            IntentType.SEARCH_APP -> handleSearchApp(intent)
            IntentType.SEARCH_WEB -> handleSearchWeb(intent)
            IntentType.CALL_CONTACT -> handleCall(intent)
            IntentType.SEND_MESSAGE -> handleSendMessage(intent)
            IntentType.SET_ALARM -> handleSetAlarm(intent)
            IntentType.SET_TIMER -> handleSetTimer(intent)
            IntentType.SET_REMINDER -> handleSetReminder(intent)
            IntentType.READ_NOTIFICATIONS -> TaskResult(true, "Checking your notifications.", requiresAI = false)
            IntentType.OPEN_SETTINGS -> handleOpenSettings()
            IntentType.OPEN_URL -> handleOpenUrl(intent)
            IntentType.REMEMBER_INFO -> handleRemember(intent)
            IntentType.RECALL_INFO -> handleRecall(intent)
            IntentType.CHAT_AI -> TaskResult(false, "", requiresAI = true)
            IntentType.PRAYER_TIME -> TaskResult(false, "", requiresAI = true)
            else -> TaskResult(false, "", requiresAI = true)
        }
    }

    private fun handleOpenApp(intent: ParsedIntent): TaskResult {
        val appName = intent.appTarget ?: return TaskResult(false, "Which app would you like to open?")
        val pkg = appLauncher.resolveApp(appName)
        return if (pkg != null && appLauncher.launchApp(pkg)) {
            TaskResult(true, "Opening $appName.")
        } else {
            TaskResult(false, "I couldn't find $appName on your device. Is it installed?")
        }
    }

    private fun handleSearchApp(intent: ParsedIntent): TaskResult {
        val appName = intent.appTarget ?: return TaskResult(false, "", requiresAI = true)
        val query = intent.searchQuery ?: ""
        return when {
            appName.contains("youtube") -> {
                appLauncher.launchYouTubeSearch(query)
                TaskResult(true, "Searching YouTube for $query.")
            }
            appName.contains("google") || appName.contains("chrome") -> {
                appLauncher.launchGoogleSearch(query)
                TaskResult(true, "Searching Google for $query.")
            }
            else -> {
                val pkg = appLauncher.resolveApp(appName)
                if (pkg != null) {
                    appLauncher.launchApp(pkg)
                    TaskResult(true, "Opened $appName. You can search for $query there.")
                } else {
                    TaskResult(false, "I couldn't find $appName on your device.")
                }
            }
        }
    }

    private fun handleSearchWeb(intent: ParsedIntent): TaskResult {
        val query = intent.searchQuery ?: intent.rawText
        appLauncher.launchGoogleSearch(query)
        return TaskResult(true, "Searching Google for $query.")
    }

    private fun handleCall(intent: ParsedIntent): TaskResult {
        val contactName = intent.contactName ?: return TaskResult(false, "Who would you like to call?")
        val number = callManager.parsePhoneNumber(contactName)
        if (number != null) {
            return TaskResult(
                success = true,
                spokenResponse = "Ready to call $number. Should I proceed?",
                pendingConfirmation = PendingAction("call", "Call $number", mapOf("number" to number))
            )
        }
        val contact = callManager.findContact(contactName)
        return if (contact != null) {
            TaskResult(
                success = true,
                spokenResponse = "I found ${contact.name}. Should I call them now?",
                pendingConfirmation = PendingAction("call", "Call ${contact.name}", mapOf("number" to contact.number, "name" to contact.name))
            )
        } else {
            TaskResult(false, "I couldn't find $contactName in your contacts.")
        }
    }

    private fun handleSendMessage(intent: ParsedIntent): TaskResult {
        val contact = intent.contactName ?: return TaskResult(false, "Who should I message?")
        val message = intent.message
        val app = intent.appTarget ?: "whatsapp"
        return if (message != null) {
            TaskResult(
                success = true,
                spokenResponse = "I'll message $contact: \"$message\". Should I open WhatsApp?",
                pendingConfirmation = PendingAction(
                    "send_message",
                    "Message $contact via $app",
                    mapOf("contact" to contact, "message" to message, "app" to app)
                )
            )
        } else {
            val pkg = appLauncher.resolveApp(app)
            if (pkg != null) appLauncher.launchApp(pkg)
            TaskResult(true, "Opening $app to message $contact.")
        }
    }

    private fun handleSetAlarm(intent: ParsedIntent): TaskResult {
        return try {
            val timeStr = intent.timeString ?: return TaskResult(false, "What time should I set the alarm for?")
            val (hour, minute) = parseTime(timeStr) ?: return TaskResult(false, "I didn't catch the time. Please say it again.")
            val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, "JAVIS Alarm")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(alarmIntent)
            val amPm = if (hour < 12) "AM" else "PM"
            val displayHour = if (hour % 12 == 0) 12 else hour % 12
            TaskResult(true, "Alarm set for $displayHour:${minute.toString().padStart(2, '0')} $amPm.")
        } catch (e: Exception) {
            Log.e("TaskPlanner", "Alarm error", e)
            TaskResult(false, "I had trouble setting the alarm. Please try using the Clock app directly.")
        }
    }

    private fun handleSetTimer(intent: ParsedIntent): TaskResult {
        return try {
            val timeStr = intent.timeString ?: return TaskResult(false, "How long should the timer be?")
            val seconds = parseTimerSeconds(timeStr)
            if (seconds <= 0) return TaskResult(false, "I didn't catch the duration. Please try again.")
            val timerIntent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, "JAVIS Timer")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(timerIntent)
            TaskResult(true, "Timer set for $timeStr.")
        } catch (e: Exception) {
            TaskResult(false, "I couldn't set the timer. Please try the Clock app.")
        }
    }

    private fun handleSetReminder(intent: ParsedIntent): TaskResult {
        return TaskResult(false, "I'll set a reminder for ${intent.timeString ?: "that time"}. For now, let me set an alarm.", requiresAI = false).also {
            handleSetAlarm(intent)
        }
    }

    private fun handleOpenSettings(): TaskResult {
        val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return TaskResult(true, "Opening Settings.")
    }

    private fun handleOpenUrl(intent: ParsedIntent): TaskResult {
        val url = intent.url ?: return TaskResult(false, "", requiresAI = true)
        appLauncher.launchBrowser(url)
        return TaskResult(true, "Opening $url in your browser.")
    }

    private suspend fun handleRemember(intent: ParsedIntent): TaskResult {
        val text = intent.rawText
        val namePattern = Regex("my name is (.+)", RegexOption.IGNORE_CASE)
        val match = namePattern.find(text)
        return if (match != null) {
            val name = match.groupValues[1].trim()
            memoryManager.remember("user_name", name, "personal")
            TaskResult(true, "Got it. I'll remember your name is $name.")
        } else {
            TaskResult(false, "", requiresAI = true)
        }
    }

    private suspend fun handleRecall(intent: ParsedIntent): TaskResult {
        val text = intent.rawText.lowercase()
        return when {
            text.contains("name") -> {
                val name = memoryManager.recall("user_name")
                if (name != null) TaskResult(true, "Your name is $name.")
                else TaskResult(true, "I don't have your name saved yet. What's your name?")
            }
            else -> TaskResult(false, "", requiresAI = true)
        }
    }

    fun confirmAction(action: PendingAction) {
        when (action.type) {
            "call" -> callManager.initiateCall(action.data["number"] ?: return)
            "send_message" -> {
                val app = action.data["app"] ?: "whatsapp"
                val pkg = appLauncher.resolveApp(app)
                if (pkg != null) appLauncher.launchApp(pkg)
            }
        }
    }

    private fun parseTime(timeStr: String): Pair<Int, Int>? {
        return try {
            val normalized = timeStr.lowercase().trim()
            val isPM = normalized.contains("pm")
            val isAM = normalized.contains("am")
            val cleanTime = normalized.replace(Regex("[apm\\s]"), "")
            val parts = cleanTime.split(":")
            var hour = parts[0].toInt()
            val minute = if (parts.size > 1) parts[1].toInt() else 0
            if (isPM && hour != 12) hour += 12
            if (isAM && hour == 12) hour = 0
            Pair(hour, minute)
        } catch (e: Exception) { null }
    }

    private fun parseTimerSeconds(timeStr: String): Int {
        val lower = timeStr.lowercase()
        val numMatch = Regex("(\\d+)").find(lower)?.groupValues?.get(1)?.toIntOrNull() ?: return 0
        return when {
            lower.contains("hour") || lower.contains("hr") -> numMatch * 3600
            lower.contains("minute") || lower.contains("min") -> numMatch * 60
            lower.contains("second") || lower.contains("sec") -> numMatch
            else -> numMatch * 60
        }
    }
}
