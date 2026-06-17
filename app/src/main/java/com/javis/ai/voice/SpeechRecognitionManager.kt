package com.javis.ai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class SpeechState {
    object Idle : SpeechState()
    object Listening : SpeechState()
    data class Result(val text: String) : SpeechState()
    data class Error(val message: String) : SpeechState()
    object PartialResult : SpeechState()
}

@Singleton
class SpeechRecognitionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var recognizer: SpeechRecognizer? = null
    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state: StateFlow<SpeechState> = _state

    private var onResultCallback: ((String) -> Unit)? = null

    fun isAvailable() = SpeechRecognizer.isRecognitionAvailable(context)

    fun initialize() {
        if (!isAvailable()) {
            Log.w("SpeechRecognitionMgr", "Speech recognition not available on this device")
            return
        }
        destroyRecognizer()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createListener())
        }
    }

    fun startListening(onResult: (String) -> Unit) {
        onResultCallback = onResult
        if (recognizer == null) initialize()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }

        try {
            _state.value = SpeechState.Listening
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("SpeechRecognitionMgr", "Failed to start listening", e)
            _state.value = SpeechState.Error("Failed to start: ${e.message}")
            initialize()
        }
    }

    fun stopListening() {
        recognizer?.stopListening()
        _state.value = SpeechState.Idle
    }

    fun cancel() {
        recognizer?.cancel()
        _state.value = SpeechState.Idle
    }

    fun destroy() {
        destroyRecognizer()
    }

    private fun destroyRecognizer() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = SpeechState.Listening
        }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            val msg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "Couldn't understand. Please try again."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                else -> "Unknown error ($error)"
            }
            Log.w("SpeechRecognitionMgr", "Error: $msg")
            _state.value = SpeechState.Error(msg)
            // Reinitialize for next use
            if (error != SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                initialize()
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.trim() ?: ""
            if (text.isNotEmpty()) {
                _state.value = SpeechState.Result(text)
                onResultCallback?.invoke(text)
            } else {
                _state.value = SpeechState.Error("Couldn't understand. Please try again.")
            }
            initialize()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            _state.value = SpeechState.PartialResult
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
