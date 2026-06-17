package com.javis.ai.services

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.javis.ai.JavisApplication
import com.javis.ai.R

/**
 * Always-on background service that listens for "Javis" / "Hey Javis" / "OK Javis".
 * Restarts the SpeechRecognizer every cycle so Android doesn't kill it.
 * Uses foreground service with minimal priority to stay alive on low-end devices.
 */
class WakeWordService : Service() {

    private var recognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var restartDelay = 600L

    companion object {
        const val TAG = "WakeWordService"
        private val WAKE_WORDS = listOf("javis", "hey javis", "ok javis", "okay javis", "jarvis")

        fun containsWakeWord(text: String): Boolean {
            val lower = text.lowercase()
            return WAKE_WORDS.any { lower.contains(it) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.w(TAG, "Speech recognition not available — wake word disabled")
            stopSelf()
            return
        }
        startForeground(JavisApplication.NOTIFICATION_ID_WAKE, buildNotification())
        isRunning = true
        startListeningCycle()
    }

    private fun startListeningCycle() {
        if (!isRunning) return
        handler.postDelayed({
            if (!isRunning) return@postDelayed
            destroyRecognizer()
            recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(wakeListener)
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            }
            recognizer?.startListening(intent)
        }, restartDelay)
    }

    private val wakeListener = object : RecognitionListener {
        override fun onPartialResults(partialResults: android.os.Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: return
            if (containsWakeWord(partial)) {
                Log.d(TAG, "Wake word detected (partial): $partial")
                onWakeWordDetected()
            }
        }

        override fun onResults(results: android.os.Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: ""
            if (containsWakeWord(text)) {
                Log.d(TAG, "Wake word detected: $text")
                onWakeWordDetected()
            } else {
                startListeningCycle()
            }
        }

        override fun onError(error: Int) {
            val delay = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> 1000L
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> 3000L
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 400L
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 2000L
                else -> 800L
            }
            restartDelay = delay
            startListeningCycle()
        }

        override fun onReadyForSpeech(params: android.os.Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
    }

    private fun onWakeWordDetected() {
        destroyRecognizer()
        val intent = Intent(this, JavisAssistantService::class.java).apply {
            action = JavisAssistantService.ACTION_ACTIVATE
            putExtra("source", "wake_word")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        // Resume listening after JAVIS is done (3s gap)
        restartDelay = 3000L
        startListeningCycle()
    }

    private fun destroyRecognizer() {
        runCatching {
            recognizer?.destroy()
            recognizer = null
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, JavisApplication.CHANNEL_WAKE)
            .setContentTitle("JAVIS listening")
            .setContentText("Say \"Hey Javis\" to activate")
            .setSmallIcon(R.drawable.ic_javis_tile)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        destroyRecognizer()
        super.onDestroy()
    }
}
