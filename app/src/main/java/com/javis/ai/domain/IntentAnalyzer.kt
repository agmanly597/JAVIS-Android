package com.javis.ai.domain

import javax.inject.Inject
import javax.inject.Singleton

enum class IntentType {
    OPEN_APP,
    SEARCH_APP,
    SEARCH_WEB,
    CALL_CONTACT,
    SEND_MESSAGE,
    SET_ALARM,
    SET_TIMER,
    SET_REMINDER,
    READ_NOTIFICATIONS,
    OPEN_SETTINGS,
    OPEN_URL,
    FILE_BROWSE,
    CHAT_AI,
    REMEMBER_INFO,
    RECALL_INFO,
    TOGGLE_SERVICE,
    PRAYER_TIME,
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
            matchesOpenApp(text) -> parseOpenApp(text, input)
            matchesSearch(text) -> parseSearch(text, input)
            matchesCall(text) -> parseCall(text, input)
            matchesSendMessage(text) -> parseSendMessage(text, input)
            matchesSetAlarm(text) -> parseAlarm(text, input)
            matchesSetTimer(text) -> parseTimer(text, input)
            matchesSetReminder(text) -> parseReminder(text, input)
            matchesReadNotifications(text) -> ParsedIntent(IntentType.READ_NOTIFICATIONS, rawText = input)
            matchesOpenSettings(text) -> ParsedIntent(IntentType.OPEN_SETTINGS, rawText = input)
            matchesOpenUrl(text) -> parseUrl(text, input)
            matchesRemember(text) -> ParsedIntent(IntentType.REMEMBER_INFO, rawText = input)
            matchesRecall(text) -> ParsedIntent(IntentType.RECALL_INFO, rawText = input)
            matchesPrayerTime(text) -> ParsedIntent(IntentType.PRAYER_TIME, rawText = input)
            else -> ParsedIntent(IntentType.CHAT_AI, rawText = input)
        }
    }

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
        t.contains("send ") || t.contains("reply to ") || t.contains("tell ")

    private fun matchesSetAlarm(t: String) =
        t.contains("alarm") || (t.contains("wake") && t.contains("up")) ||
        t.contains("set alarm")

    private fun matchesSetTimer(t: String) =
        t.contains("timer") || t.contains("countdown") ||
        (t.contains("set") && t.contains("minute"))

    private fun matchesSetReminder(t: String) =
        t.contains("remind") || t.contains("reminder")

    private fun matchesReadNotifications(t: String) =
        t.contains("notification") || t.contains("messages") && t.contains("read") ||
        t.contains("what did i miss") || t.contains("any new messages")

    private fun matchesOpenSettings(t: String) =
        t.contains("setting") || t.contains("wifi") || t.contains("bluetooth") ||
        t.contains("brightness") || t.contains("volume")

    private fun matchesOpenUrl(t: String) =
        t.startsWith("open http") || t.startsWith("go to http") ||
        t.contains(".com") || t.contains(".org") || t.contains(".net")

    private fun matchesRemember(t: String) =
        t.startsWith("remember ") || t.contains("my name is") || t.contains("i am ")

    private fun matchesRecall(t: String) =
        t.contains("what is my") || t.contains("what's my") || t.contains("do you know my")

    private fun matchesPrayerTime(t: String) =
        t.contains("fajr") || t.contains("prayer") || t.contains("salah") ||
        t.contains("namaz") || t.contains("asr") || t.contains("maghrib") || t.contains("isha")

    private fun parseOpenApp(text: String, raw: String): ParsedIntent {
        val appName = text
            .replace(Regex("^(open|launch|start|go to|show me)\\s+"), "")
            .trim()
        val (finalApp, query) = if (appName.contains(" and search ")) {
            val parts = appName.split(" and search ", limit = 2)
            Pair(parts[0].trim(), parts[1].trim())
        } else {
            Pair(appName, null)
        }
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
        val contactPattern = Regex("(?:message|text|tell|whatsapp|reply to|send to)\\s+(\\w+)")
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
}
