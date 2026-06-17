package com.javis.ai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

sealed class TTSState {
    object Idle : TTSState()
    object Speaking : TTSState()
    data class Done(val utteranceId: String) : TTSState()
}

@Singleton
class TTSManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    private val _state = MutableStateFlow<TTSState>(TTSState.Idle)
    val state: StateFlow<TTSState> = _state

    var speed: Float = 1.0f
    var pitch: Float = 1.0f
    var locale: Locale = Locale.US

    fun initialize(onReady: (() -> Unit)? = null) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
                applyVoiceSettings()
                onReady?.invoke()
                Log.d("TTSManager", "TTS initialized successfully")
            } else {
                Log.e("TTSManager", "TTS initialization failed: $status")
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _state.value = TTSState.Speaking
            }
            override fun onDone(utteranceId: String?) {
                _state.value = TTSState.Done(utteranceId ?: "")
            }
            override fun onError(utteranceId: String?) {
                _state.value = TTSState.Idle
            }
        })
    }

    fun speak(text: String, utteranceId: String = "javis_${System.currentTimeMillis()}") {
        if (!isReady) {
            Log.w("TTSManager", "TTS not ready, cannot speak")
            return
        }
        val cleanText = text
            .replace(Regex("[*_~`#]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun speakQueued(text: String, utteranceId: String = "javis_${System.currentTimeMillis()}") {
        if (!isReady) return
        val cleanText = text.replace(Regex("[*_~`#]"), "").trim()
        tts?.speak(cleanText, TextToSpeech.QUEUE_ADD, null, utteranceId)
    }

    suspend fun speakAndWait(text: String): Boolean = suspendCancellableCoroutine { cont ->
        val id = "wait_${System.currentTimeMillis()}"
        val listener = object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                if (utteranceId == id && cont.isActive) cont.resume(true)
            }
            override fun onError(utteranceId: String?) {
                if (utteranceId == id && cont.isActive) cont.resume(false)
            }
        }
        tts?.setOnUtteranceProgressListener(listener)
        speak(text, id)
        cont.invokeOnCancellation { stop() }
    }

    fun stop() {
        tts?.stop()
        _state.value = TTSState.Idle
    }

    fun isSpeaking() = tts?.isSpeaking == true

    fun applyVoiceSettings() {
        if (!isReady) return
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.setLanguage(Locale.US)
        }
        tts?.setSpeechRate(speed)
        tts?.setPitch(pitch)
    }

    fun getAvailableVoices(): List<android.speech.tts.Voice> {
        return tts?.voices?.filter { it.locale.language == locale.language }?.toList() ?: emptyList()
    }

    fun setVoice(voice: android.speech.tts.Voice) {
        if (isReady) tts?.voice = voice
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
