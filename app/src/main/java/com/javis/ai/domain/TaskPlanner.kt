package com.javis.ai.domain

import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.util.Log
import com.javis.ai.apps.AppLauncher
import com.javis.ai.calls.CallManager
import com.javis.ai.memory.MemoryManager
import com.javis.ai.services.JavisAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import android.media.audiofx.AudioEffect
import android.os.Build
import android.provider.MediaStore
import android.view.KeyEvent
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
    suspend fun execute(intent: ParsedIntent): TaskResult = when (intent.type) {
        IntentType.OPEN_APP          -> handleOpenApp(intent)
        IntentType.SEARCH_APP        -> handleSearchApp(intent)
        IntentType.SEARCH_WEB        -> handleSearchWeb(intent)
        IntentType.CALL_CONTACT      -> handleCall(intent)
        IntentType.SEND_MESSAGE      -> handleSendMessage(intent)
        IntentType.SET_ALARM         -> handleSetAlarm(intent)
        IntentType.SET_TIMER         -> handleSetTimer(intent)
        IntentType.SET_REMINDER      -> handleSetReminder(intent)
        IntentType.READ_NOTIFICATIONS -> TaskResult(true, "Checking your notifications.", requiresAI = false)
        IntentType.READ_SCREEN       -> handleReadScreen()
        IntentType.OPEN_SETTINGS     -> handleOpenSettings()
        IntentType.OPEN_URL          -> handleOpenUrl(intent)
        IntentType.NAVIGATE_MAPS     -> handleNavigate(intent)
        IntentType.REMEMBER_INFO     -> handleRemember(intent)
        IntentType.RECALL_INFO       -> handleRecall(intent)
        IntentType.VOLUME_UP         -> handleVolumeUp()
        IntentType.VOLUME_DOWN       -> handleVolumeDown()
        IntentType.VOLUME_MUTE       -> handleVolumeMute()
        IntentType.BRIGHTNESS_UP     -> handleBrightness(up = true)
        IntentType.BRIGHTNESS_DOWN   -> handleBrightness(up = false)
        IntentType.TOGGLE_WIFI       -> handleToggleWifi()
        IntentType.TOGGLE_BLUETOOTH  -> handleToggleBluetooth()
        IntentType.TOGGLE_FLASHLIGHT -> handleToggleFlashlight()
        IntentType.TAKE_PHOTO        -> handleTakePhoto()
        IntentType.TAKE_SCREENSHOT   -> handleScreenshot()
        IntentType.SCROLL_UP         -> handleScroll(up = true)
        IntentType.SCROLL_DOWN       -> handleScroll(up = false)
        IntentType.GO_BACK           -> handleGoBack()
        IntentType.GO_HOME           -> handleGoHome()
        IntentType.REPLY_NOTIFICATION -> TaskResult(false, "", requiresAI = true)
        IntentType.CALCULATE         -> TaskResult(false, "", requiresAI = true)
        IntentType.TRANSLATE         -> TaskResult(false, "", requiresAI = true)
        IntentType.CHAT_AI           -> TaskResult(false, "", requiresAI = true)
        IntentType.PRAYER_TIME       -> TaskResult(false, "", requiresAI = true)
        else                          -> TaskResult(false, "", requiresAI = true)
    }

    // ─── App Control ─────────────────────────────────────────────────────────

    private fun handleOpenApp(intent: ParsedIntent): TaskResult {
        val appName = intent.appTarget ?: return TaskResult(false, "Which app would you like to open?")
        val pkg = appLauncher.resolveApp(appName)
        return if (pkg != null && appLauncher.launchApp(pkg)) {
            TaskResult(true, "Opening $appName.")
        } else {
            TaskResult(false, "I couldn't find $appName. Is it installed?")
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
                    TaskResult(false, "I couldn't find $appName.")
                }
            }
        }
    }

    private fun handleSearchWeb(intent: ParsedIntent): TaskResult {
        val query = intent.searchQuery ?: intent.rawText
        appLauncher.launchGoogleSearch(query)
        return TaskResult(true, "Searching Google for $query.")
    }

    private fun handleOpenUrl(intent: ParsedIntent): TaskResult {
        val url = intent.url ?: return TaskResult(false, "", requiresAI = true)
        appLauncher.launchBrowser(url)
        return TaskResult(true, "Opening $url in your browser.")
    }

    // ─── Communication ────────────────────────────────────────────────────────

    private fun handleCall(intent: ParsedIntent): TaskResult {
        val contactName = intent.contactName ?: return TaskResult(false, "Who would you like to call?")
        val number = callManager.parsePhoneNumber(contactName)
        if (number != null) {
            return TaskResult(true, "Ready to call $number. Should I proceed?",
                pendingConfirmation = PendingAction("call", "Call $number", mapOf("number" to number)))
        }
        val contact = callManager.findContact(contactName)
        return if (contact != null) {
            TaskResult(true, "I found ${contact.name}. Should I call them now?",
                pendingConfirmation = PendingAction("call", "Call ${contact.name}", mapOf("number" to contact.number, "name" to contact.name)))
        } else {
            TaskResult(false, "I couldn't find $contactName in your contacts.")
        }
    }

    private fun handleSendMessage(intent: ParsedIntent): TaskResult {
        val contact = intent.contactName ?: return TaskResult(false, "Who should I message?")
        val message = intent.message
        val app = intent.appTarget ?: "whatsapp"
        return if (message != null) {
            TaskResult(true, "I'll message $contact: \"$message\". Should I send it?",
                pendingConfirmation = PendingAction("send_message", "Message $contact via $app",
                    mapOf("contact" to contact, "message" to message, "app" to app)))
        } else {
            val pkg = appLauncher.resolveApp(app)
            if (pkg != null) appLauncher.launchApp(pkg)
            TaskResult(true, "Opening $app to message $contact.")
        }
    }

    // ─── Scheduling ───────────────────────────────────────────────────────────

    private fun handleSetAlarm(intent: ParsedIntent): TaskResult {
        return try {
            val timeStr = intent.timeString ?: return TaskResult(false, "What time should I set the alarm for?")
            val (hour, minute) = parseTime(timeStr) ?: return TaskResult(false, "I didn't catch the time. Please say it again.")
            context.startActivity(Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, "JAVIS Alarm")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            val amPm = if (hour < 12) "AM" else "PM"
            val displayHour = if (hour % 12 == 0) 12 else hour % 12
            TaskResult(true, "Alarm set for $displayHour:${minute.toString().padStart(2, '0')} $amPm.")
        } catch (e: Exception) {
            Log.e("TaskPlanner", "Alarm error", e)
            TaskResult(false, "I had trouble setting the alarm.")
        }
    }

    private fun handleSetTimer(intent: ParsedIntent): TaskResult {
        return try {
            val timeStr = intent.timeString ?: return TaskResult(false, "How long should the timer be?")
            val seconds = parseTimerSeconds(timeStr)
            if (seconds <= 0) return TaskResult(false, "I didn't catch the duration.")
            context.startActivity(Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, "JAVIS Timer")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            TaskResult(true, "Timer set for $timeStr.")
        } catch (e: Exception) {
            TaskResult(false, "I couldn't set the timer.")
        }
    }

    private fun handleSetReminder(intent: ParsedIntent): TaskResult {
        return TaskResult(true, "Setting a reminder for ${intent.timeString ?: "that time"}.").also {
            handleSetAlarm(intent)
        }
    }

    // ─── System Controls ─────────────────────────────────────────────────────

    private fun handleVolumeUp(): TaskResult {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
        return TaskResult(true, "Volume up.")
    }

    private fun handleVolumeDown(): TaskResult {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        return TaskResult(true, "Volume down.")
    }

    private fun handleVolumeMute(): TaskResult {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
        } else {
            @Suppress("DEPRECATION")
            am.setStreamMute(AudioManager.STREAM_MUSIC, true)
        }
        return TaskResult(true, "Muted.")
    }

    private fun handleBrightness(up: Boolean): TaskResult {
        // Brightness requires WRITE_SETTINGS — open settings as fallback
        return try {
            val intent = Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            TaskResult(true, if (up) "Opening display settings to adjust brightness." else "Opening display settings to dim the screen.")
        } catch (e: Exception) {
            TaskResult(false, "I need display settings access to change brightness.")
        }
    }

    private fun handleToggleWifi(): TaskResult {
        return try {
            context.startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            TaskResult(true, "Opening Wi-Fi settings.")
        } catch (e: Exception) {
            TaskResult(false, "I couldn't open Wi-Fi settings.")
        }
    }

    private fun handleToggleBluetooth(): TaskResult {
        return try {
            context.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            TaskResult(true, "Opening Bluetooth settings.")
        } catch (e: Exception) {
            TaskResult(false, "I couldn't open Bluetooth settings.")
        }
    }

    private fun handleToggleFlashlight(): TaskResult {
        return try {
            val pkg = appLauncher.resolveApp("flashlight")
            if (pkg != null) {
                appLauncher.launchApp(pkg)
                TaskResult(true, "Opening flashlight.")
            } else {
                TaskResult(false, "I need an accessibility shortcut to toggle the flashlight directly. Enable it in settings.", requiresAI = false)
            }
        } catch (e: Exception) {
            TaskResult(false, "I couldn't toggle the flashlight.")
        }
    }

    private fun handleTakePhoto(): TaskResult {
        return try {
            context.startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            TaskResult(true, "Opening camera.")
        } catch (e: Exception) {
            TaskResult(false, "I couldn't open the camera.")
        }
    }

    private fun handleScreenshot(): TaskResult {
        val acc = JavisAccessibilityService.instance
        return if (acc != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            acc.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            TaskResult(true, "Screenshot taken.")
        } else {
            TaskResult(false, "I need accessibility service enabled to take screenshots. Please enable it in settings.")
        }
    }

    // ─── Accessibility Navigation ─────────────────────────────────────────────

    private fun handleScroll(up: Boolean): TaskResult {
        val acc = JavisAccessibilityService.instance
        return if (acc != null) {
            acc.performScroll(up)
            TaskResult(true, if (up) "Scrolled up." else "Scrolled down.")
        } else {
            TaskResult(false, "Accessibility service not enabled. Go to Settings → Accessibility → JAVIS Assistant to enable it.")
        }
    }

    private fun handleGoBack(): TaskResult {
        val acc = JavisAccessibilityService.instance
        return if (acc != null) {
            acc.performGlobalBack()
            TaskResult(true, "Going back.")
        } else {
            TaskResult(false, "I need accessibility service to navigate.")
        }
    }

    private fun handleGoHome(): TaskResult {
        val acc = JavisAccessibilityService.instance
        return if (acc != null) {
            acc.performGlobalHome()
            TaskResult(true, "Going home.")
        } else {
            TaskResult(false, "I need accessibility service to navigate.")
        }
    }

    private fun handleReadScreen(): TaskResult {
        val acc = JavisAccessibilityService.instance
        return if (acc != null) {
            val text = acc.readVisibleText()
            if (text.isNotBlank()) {
                TaskResult(true, "The screen says: $text")
            } else {
                TaskResult(true, "I can't read any text on the current screen.")
            }
        } else {
            TaskResult(false, "Enable accessibility service to let me read the screen.")
        }
    }

    private fun handleNavigate(intent: ParsedIntent): TaskResult {
        val destination = intent.searchQuery ?: return TaskResult(false, "Where would you like to go?")
        return try {
            val uri = Uri.parse("geo:0,0?q=${Uri.encode(destination)}")
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            TaskResult(true, "Opening navigation to $destination.")
        } catch (e: Exception) {
            TaskResult(false, "I couldn't open maps.")
        }
    }

    private fun handleOpenSettings(): TaskResult {
        context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        return TaskResult(true, "Opening Settings.")
    }

    // ─── Memory ───────────────────────────────────────────────────────────────

    private suspend fun handleRemember(intent: ParsedIntent): TaskResult {
        val text = intent.rawText
        val namePattern = Regex("my name is (.+)", RegexOption.IGNORE_CASE)
        val match = namePattern.find(text)
        return if (match != null) {
            val name = match.groupValues[1].trim()
            memoryManager.remember("user_name", name, "personal")
            TaskResult(true, "Got it. I'll remember your name is $name.")
        } else {
            memoryManager.remember("note_${System.currentTimeMillis()}", text, "general")
            TaskResult(true, "Noted. I'll remember that.")
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

    // ─── Confirm actions ──────────────────────────────────────────────────────

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

    // ─── Time parsing helpers ─────────────────────────────────────────────────

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
