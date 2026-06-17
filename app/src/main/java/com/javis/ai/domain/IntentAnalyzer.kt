package com.javis.ai.domain

import javax.inject.Inject
import javax.inject.Singleton

enum class IntentType {
    // App control
    OPEN_APP,
    SEARCH_APP,
    // Web
    SEARCH_WEB,
    OPEN_URL,
    // Communication
    CALL_CONTACT,
    SEND_MESSAGE,
    REPLY_NOTIFICATION,
    // Scheduling
    SET_ALARM,
    SET_TIMER,
    SET_REMINDER,
    // System
    VOLUME_UP,
    VOLUME_DOWN,
    VOLUME_MUTE,
    BRIGHTNESS_UP,
    BRIGHTNESS_DOWN,
    TOGGLE_WIFI,
    TOGGLE_BLUETOOTH,
    TOGGLE_FLASHLIGHT,
    TAKE_PHOTO,
    TAKE_SCREENSHOT,
    // Navigation
    SCROLL_UP,
    SCROLL_DOWN,
    GO_BACK,
    GO_HOME,
    NAVIGATE_MAPS,
    // Reading
    READ_NOTIFICATIONS,
    READ_SCREEN,
    // Settings
    OPEN_SETTINGS,
    // Memory
    REMEMBER_INFO,
    RECALL_INFO,
    // Utility
    CALCULATE,
    TRANSLATE,
    // Domain
    PRAYER_TIME,
    // AI chat
    CHAT_AI,
    UNKNOWN
}

data class ParsedIntent(
    val type: IntentType,
    val appTarget: String? = null,
    val contactName: String? = null,
    val searchQuery: String? = null,
    val message: String? = null,
    val timeString: String? = null,
    val url: String? = null,
    val rawText: String = "",
    val confidence: Float = 1.0f
)

@Singleton
class IntentAnalyzer @Inject constructor() {

    fun analyze(input: String): ParsedIntent {
        val text = input.lowercase().trim()
        return when {
            // System controls — check before open/search to avoid conflicts
            matchesVolumeUp(text)       -> ParsedIntent(IntentType.VOLUME_UP, rawText = input)
            matchesVolumeDown(text)     -> ParsedIntent(IntentType.VOLUME_DOWN, rawText = input)
            matchesVolumeMute(text)     -> ParsedIntent(IntentType.VOLUME_MUTE, rawText = input)
            matchesBrightnessUp(text)   -> ParsedIntent(IntentType.BRIGHTNESS_UP, rawText = input)
            matchesBrightnessDown(text) -> ParsedIntent(IntentType.BRIGHTNESS_DOWN, rawText = input)
            matchesToggleWifi(text)     -> ParsedIntent(IntentType.TOGGLE_WIFI, rawText = input)
            matchesToggleBluetooth(text)-> ParsedIntent(IntentType.TOGGLE_BLUETOOTH, rawText = input)
            matchesFlashlight(text)     -> ParsedIntent(IntentType.TOGGLE_FLASHLIGHT, rawText = input)
            matchesTakePhoto(text)      -> ParsedIntent(IntentType.TAKE_PHOTO, rawText = input)
            matchesScreenshot(text)     -> ParsedIntent(IntentType.TAKE_SCREENSHOT, rawText = input)
            // Navigation
            matchesScrollUp(text)       -> ParsedIntent(IntentType.SCROLL_UP, rawText = input)
            matchesScrollDown(text)     -> ParsedIntent(IntentType.SCROLL_DOWN, rawText = input)
            matchesGoBack(text)         -> ParsedIntent(IntentType.GO_BACK, rawText = input)
            matchesGoHome(text)         -> ParsedIntent(IntentType.GO_HOME, rawText = input)
            matchesNavigateMaps(text)   -> ParsedIntent(IntentType.NAVIGATE_MAPS, rawText = input, searchQuery = extractDestination(text))
            // Communication
            matchesCall(text)           -> parseCall(text, input)
            matchesSendMessage(text)    -> parseSendMessage(text, input)
            matchesReplyNotif(text)     -> ParsedIntent(IntentType.REPLY_NOTIFICATION, rawText = input)
            // Scheduling
            matchesSetAlarm(text)       -> parseAlarm(text, input)
            matchesSetTimer(text)       -> parseTimer(text, input)
            matchesSetReminder(text)    -> parseReminder(text, input)
            // App
            matchesOpenApp(text)        -> parseOpenApp(text, input)
            matchesSearch(text)         -> parseSearch(text, input)
            matchesOpenUrl(text)        -> parseUrl(text, input)
            // Reading
            matchesReadNotifications(text) -> ParsedIntent(IntentType.READ_NOTIFICATIONS, rawText = input)
            matchesReadScreen(text)     -> ParsedIntent(IntentType.READ_SCREEN, rawText = input)
            // Settings
            matchesOpenSettings(text)   -> ParsedIntent(IntentType.OPEN_SETTINGS, rawText = input)
            // Utility
            matchesCalculate(text)      -> ParsedIntent(IntentType.CALCULATE, rawText = input)
            matchesTranslate(text)      -> ParsedIntent(IntentType.TRANSLATE, rawText = input)
            // Memory
            matchesRemember(text)       -> ParsedIntent(IntentType.REMEMBER_INFO, rawText = input)
            matchesRecall(text)         -> ParsedIntent(IntentType.RECALL_INFO, rawText = input)
            // Domain
            matchesPrayerTime(text)     -> ParsedIntent(IntentType.PRAYER_TIME, rawText = input)
            else                        -> ParsedIntent(IntentType.CHAT_AI, rawText = input)
        }
    }

    // ─── Matchers ────────────────────────────────────────────────────────────

    private fun matchesOpenApp(t: String) =
        t.startsWith("open ") || t.startsWith("launch ") || t.startsWith("start ") ||
        t.startsWith("go to ") || t.contains("show me ")

    private fun matchesSearch(t: String) =
        t.contains("search for") || t.contains("search ") || t.contains("look up") ||
        t.contains("find ") || t.contains("google ") || t.startsWith("yt ")

    private fun matchesCall(t: String) =
        t.startsWith("call ") || t.startsWith("dial ") || t.startsWith("phone ") ||
        t.contains("ring ") || t.contains("call my ")

    private fun matchesSendMessage(t: String) =
        t.contains("message ") || t.contains("text ") || t.contains("whatsapp ") ||
        t.contains("send ") || t.contains("tell ") || t.contains("msg ")

    private fun matchesReplyNotif(t: String) =
        (t.contains("reply") || t.contains("respond")) &&
        (t.contains("notification") || t.contains("message") || t.contains("to"))

    private fun matchesSetAlarm(t: String) =
        t.contains("alarm") || (t.contains("wake") && t.contains("up")) || t.contains("set alarm")

    private fun matchesSetTimer(t: String) =
        t.contains("timer") || t.contains("countdown") ||
        (t.contains("set") && t.contains("minute"))

    private fun matchesSetReminder(t: String) =
        t.contains("remind") || t.contains("reminder")

    private fun matchesReadNotifications(t: String) =
        t.contains("notification") || (t.contains("messages") && t.contains("read")) ||
        t.contains("what did i miss") || t.contains("any new messages") ||
        t.contains("read my messages")

    private fun matchesReadScreen(t: String) =
        t.contains("read screen") || t.contains("what's on screen") ||
        t.contains("what does it say") || t.contains("read this page") ||
        t.contains("read the page")

    private fun matchesOpenSettings(t: String) =
        t.contains("setting") || (t.contains("wifi") && !matchesToggleWifi(t)) ||
        (t.contains("bluetooth") && !matchesToggleBluetooth(t))

    private fun matchesOpenUrl(t: String) =
        t.startsWith("open http") || t.startsWith("go to http") ||
        t.contains(".com") || t.contains(".org") || t.contains(".net")

    private fun matchesRemember(t: String) =
        t.startsWith("remember ") || t.contains("my name is") || t.contains("i am ") ||
        t.contains("note that") || t.contains("don't forget")

    private fun matchesRecall(t: String) =
        t.contains("what is my") || t.contains("what's my") || t.contains("do you know my") ||
        t.contains("who am i") || t.contains("what do you know about me")

    private fun matchesPrayerTime(t: String) =
        t.contains("fajr") || t.contains("prayer") || t.contains("salah") ||
        t.contains("namaz") || t.contains("asr") || t.contains("maghrib") || t.contains("isha")

    private fun matchesVolumeUp(t: String) =
        (t.contains("volume") && (t.contains("up") || t.contains("higher") || t.contains("louder") || t.contains("increase"))) ||
        t.contains("turn up") || t.contains("louder")

    private fun matchesVolumeDown(t: String) =
        (t.contains("volume") && (t.contains("down") || t.contains("lower") || t.contains("quieter") || t.contains("decrease"))) ||
        t.contains("quieter") || t.contains("turn down")

    private fun matchesVolumeMute(t: String) =
        t.contains("mute") || t.contains("silence") || (t.contains("volume") && t.contains("off"))

    private fun matchesBrightnessUp(t: String) =
        (t.contains("brightness") || t.contains("screen")) &&
        (t.contains("up") || t.contains("higher") || t.contains("brighter") || t.contains("increase"))

    private fun matchesBrightnessDown(t: String) =
        (t.contains("brightness") || t.contains("screen")) &&
        (t.contains("down") || t.contains("lower") || t.contains("dimmer") || t.contains("dim"))

    private fun matchesToggleWifi(t: String) =
        (t.contains("turn") || t.contains("toggle") || t.contains("switch") || t.contains("enable") || t.contains("disable")) &&
        t.contains("wifi")

    private fun matchesToggleBluetooth(t: String) =
        (t.contains("turn") || t.contains("toggle") || t.contains("switch") || t.contains("enable") || t.contains("disable")) &&
        t.contains("bluetooth")

    private fun matchesFlashlight(t: String) =
        t.contains("flashlight") || t.contains("torch") || t.contains("flash light")

    private fun matchesTakePhoto(t: String) =
        t.contains("take a photo") || t.contains("take photo") || t.contains("take a picture") ||
        t.contains("open camera") || t.contains("selfie")

    private fun matchesScreenshot(t: String) =
        t.contains("screenshot") || t.contains("screen shot") || t.contains("capture screen")

    private fun matchesScrollUp(t: String) =
        t.contains("scroll up") || t.contains("scroll to top") || t.contains("go up")

    private fun matchesScrollDown(t: String) =
        t.contains("scroll down") || t.contains("scroll to bottom") || t.contains("go down")

    private fun matchesGoBack(t: String) =
        t == "go back" || t == "back" || t.contains("press back") || t.contains("hit back")

    private fun matchesGoHome(t: String) =
        t == "go home" || t == "home" || t.contains("press home") || t.contains("home screen")

    private fun matchesNavigateMaps(t: String) =
        (t.contains("navigate") || t.contains("take me to") || t.contains("directions to") ||
         t.contains("how do i get to") || t.contains("go to") && t.contains("map")) &&
        !t.contains("app") && !t.contains("whatsapp")

    private fun matchesCalculate(t: String) =
        t.startsWith("calculate ") || t.startsWith("what is ") && t.any { it.isDigit() } ||
        t.contains("plus") || t.contains("minus") || t.contains("times") ||
        t.contains("divided by") || t.contains("percent of") || Regex("\\d+\\s*[+\\-*/]\\s*\\d+").containsMatchIn(t)

    private fun matchesTranslate(t: String) =
        t.contains("translate") || t.contains("in english") || t.contains("in french") ||
        t.contains("in arabic") || t.contains("in hausa") || t.contains("in spanish") ||
        t.contains("what does") && t.contains("mean")

    // ─── Parsers ──────────────────────────────────────────────────────────────

    private fun parseOpenApp(text: String, raw: String): ParsedIntent {
        val appName = text.replace(Regex("^(open|launch|start|go to|show me)\\s+"), "").trim()
        val (finalApp, query) = if (appName.contains(" and search ")) {
            val parts = appName.split(" and search ", limit = 2)
            Pair(parts[0].trim(), parts[1].trim())
        } else Pair(appName, null)
        return if (query != null) {
            ParsedIntent(IntentType.SEARCH_APP, appTarget = finalApp, searchQuery = query, rawText = raw)
        } else {
            ParsedIntent(IntentType.OPEN_APP, appTarget = finalApp, rawText = raw)
        }
    }

    private fun parseSearch(text: String, raw: String): ParsedIntent {
        val query = text
            .replace(Regex("(search for|search|look up|google|find)\\s+"), "")
            .replace(Regex("(on |in |using )?(youtube|google|chrome|browser).*"), "")
            .trim()
        val isYouTube = text.contains("youtube") || text.startsWith("yt ")
        return ParsedIntent(
            type = if (isYouTube) IntentType.SEARCH_APP else IntentType.SEARCH_WEB,
            appTarget = if (isYouTube) "youtube" else null,
            searchQuery = query.ifEmpty { raw },
            rawText = raw
        )
    }

    private fun parseCall(text: String, raw: String): ParsedIntent {
        val contact = text
            .replace(Regex("^(call|dial|phone|ring)\\s+"), "")
            .replace(Regex("\\s*(for me|please|now)$"), "")
            .trim()
        return ParsedIntent(IntentType.CALL_CONTACT, contactName = contact, rawText = raw)
    }

    private fun parseSendMessage(text: String, raw: String): ParsedIntent {
        val contactPattern = Regex("(?:message|text|tell|whatsapp|reply to|send to|msg)\\s+(\\w+)")
        val msgPattern = Regex("(?:saying|say|that|:)\\s+(.+)$")
        val contact = contactPattern.find(text)?.groupValues?.getOrNull(1)
        val msg = msgPattern.find(text)?.groupValues?.getOrNull(1)
            ?: text.replace(Regex("^.+(?:saying|say|that|:)\\s*"), "").trim()
        return ParsedIntent(
            IntentType.SEND_MESSAGE,
            contactName = contact,
            message = msg.ifEmpty { null },
            appTarget = if (text.contains("whatsapp")) "whatsapp" else null,
            rawText = raw
        )
    }

    private fun parseAlarm(text: String, raw: String): ParsedIntent {
        val timeRegex = Regex("(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?|\\d{1,2}\\s*(?:o'clock)?)")
        val time = timeRegex.find(text)?.value?.trim()
        return ParsedIntent(IntentType.SET_ALARM, timeString = time, rawText = raw)
    }

    private fun parseTimer(text: String, raw: String): ParsedIntent {
        val numRegex = Regex("(\\d+)\\s*(second|minute|hour|sec|min|hr)")
        val match = numRegex.find(text)
        val timeStr = if (match != null) "${match.groupValues[1]} ${match.groupValues[2]}" else null
        return ParsedIntent(IntentType.SET_TIMER, timeString = timeStr, rawText = raw)
    }

    private fun parseReminder(text: String, raw: String): ParsedIntent {
        val timeRegex = Regex("(tomorrow|tonight|today|at\\s+\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)")
        val time = timeRegex.find(text)?.value?.trim()
        return ParsedIntent(IntentType.SET_REMINDER, timeString = time, rawText = raw)
    }

    private fun parseUrl(text: String, raw: String): ParsedIntent {
        val urlRegex = Regex("https?://[^\\s]+|[\\w-]+\\.(?:com|org|net|io|gov|edu)[^\\s]*")
        val url = urlRegex.find(text)?.value
        return ParsedIntent(IntentType.OPEN_URL, url = url, rawText = raw)
    }

    private fun extractDestination(text: String): String {
        return text
            .replace(Regex("(navigate|take me to|directions to|how do i get to|go to|map)"), "")
            .trim()
    }
}
