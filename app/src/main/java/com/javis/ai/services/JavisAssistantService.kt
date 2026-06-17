package com.javis.ai.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.javis.ai.JavisApplication
import com.javis.ai.MainActivity
import com.javis.ai.R
import com.javis.ai.ai.AIProviderManager
import com.javis.ai.domain.AgentRouter
import com.javis.ai.domain.PendingAction
import com.javis.ai.voice.SpeechRecognitionManager
import com.javis.ai.voice.SpeechState
import com.javis.ai.voice.TTSManager
import com.javis.ai.settings.SettingsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class JavisAssistantService : Service() {

    @Inject lateinit var speechManager: SpeechRecognitionManager
    @Inject lateinit var ttsManager: TTSManager
    @Inject lateinit var agentRouter: AgentRouter
    @Inject lateinit var aiProviderManager: AIProviderManager
    @Inject lateinit var settingsManager: SettingsManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var pendingAction: PendingAction? = null
    private var isListening = false
    private var awaitingConfirmation = false

    companion object {
        const val ACTION_ACTIVATE = "com.javis.ai.ACTION_ACTIVATE"
        const val ACTION_LISTEN = "com.javis.ai.ACTION_LISTEN"
        const val ACTION_STOP = "com.javis.ai.ACTION_STOP"
        const val ACTION_SETTINGS = "com.javis.ai.ACTION_SETTINGS"
        const val ACTION_CONFIRM = "com.javis.ai.ACTION_CONFIRM"
        const val ACTION_CANCEL = "com.javis.ai.ACTION_CANCEL"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("JavisService", "Service created")
        ttsManager.initialize {
            Log.d("JavisService", "TTS ready")
        }
        speechManager.initialize()
        scope.launch { aiProviderManager.configure() }
        observeSpeechResults()
        scope.launch { maybeStartFloatingButton() }
    }

    private suspend fun maybeStartFloatingButton() {
        val settings = settingsManager.settings.first()
        if (!settings.floatingButtonEnabled) return
        val canDraw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(this)
        } else true
        if (!canDraw) return
        val floatIntent = Intent(this, FloatingWindowService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(floatIntent)
        } else {
            startService(floatIntent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(JavisApplication.NOTIFICATION_ID_ASSISTANT, buildNotification("JAVIS is ready"))

        when (intent?.action) {
            ACTION_ACTIVATE, null -> activate()
            ACTION_LISTEN -> startListening()
            ACTION_STOP -> stopSelf()
            ACTION_SETTINGS -> openSettings()
            ACTION_CONFIRM -> confirmPendingAction()
            ACTION_CANCEL -> {
                pendingAction = null
                awaitingConfirmation = false
                ttsManager.speak("Cancelled.")
            }
        }

        return START_STICKY
    }

    private fun activate() {
        scope.launch {
            ttsManager.speakAndWait("Hello. Javis online. How can I help you?")
            delay(200)
            startListening()
        }
    }

    private fun startListening() {
        if (isListening) return
        isListening = true
        updateNotification("Listening...")
        speechManager.startListening { text ->
            isListening = false
            updateNotification("Processing...")
            scope.launch { processInput(text) }
        }
    }

    private fun observeSpeechResults() {
        scope.launch {
            speechManager.state.collectLatest { state ->
                when (state) {
                    is SpeechState.Error -> {
                        isListening = false
                        updateNotification("JAVIS is ready")
                        if (state.message.contains("permission", ignoreCase = true)) {
                            ttsManager.speak("Microphone permission is required for voice input.")
                        }
                    }
                    is SpeechState.Idle -> {
                        isListening = false
                    }
                    else -> {}
                }
            }
        }
    }

    private suspend fun processInput(input: String) {
        Log.d("JavisService", "Processing: $input")

        if (awaitingConfirmation) {
            val lower = input.lowercase()
            when {
                lower.contains("yes") || lower.contains("send") ||
                lower.contains("ok") || lower.contains("confirm") ||
                lower.contains("do it") || lower.contains("go ahead") -> {
                    confirmPendingAction()
                    return
                }
                lower.contains("no") || lower.contains("cancel") ||
                lower.contains("stop") || lower.contains("never mind") -> {
                    pendingAction = null
                    awaitingConfirmation = false
                    ttsManager.speakAndWait("Alright, cancelled.")
                    updateNotification("JAVIS is ready")
                    return
                }
            }
        }

        val response = agentRouter.route(input)

        if (response.pendingAction != null) {
            pendingAction = response.pendingAction
            awaitingConfirmation = true
            updateNotification("Awaiting confirmation...")
            ttsManager.speakAndWait(response.spokenText)
        } else {
            awaitingConfirmation = false
            pendingAction = null
            updateNotification("JAVIS is ready")
            ttsManager.speakAndWait(response.spokenText)
        }
    }

    private fun confirmPendingAction() {
        val action = pendingAction ?: return
        pendingAction = null
        awaitingConfirmation = false
        agentRouter.confirmPendingAction(action)
        ttsManager.speak("Done.")
        updateNotification("JAVIS is ready")
    }

    private fun openSettings() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("navigate_to", "settings")
        }
        startActivity(intent)
    }

    private fun buildNotification(status: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val listenIntent = PendingIntent.getService(
            this, 1,
            Intent(this, JavisAssistantService::class.java).apply { action = ACTION_LISTEN },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 2,
            Intent(this, JavisAssistantService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val settingsIntent = PendingIntent.getService(
            this, 3,
            Intent(this, JavisAssistantService::class.java).apply { action = ACTION_SETTINGS },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, JavisApplication.CHANNEL_ASSISTANT)
            .setContentTitle("JAVIS Assistant")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_javis_tile)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .addAction(0, "Listen", listenIntent)
            .addAction(0, "Stop", stopIntent)
            .addAction(0, "Settings", settingsIntent)
            .build()
    }

    private fun updateNotification(status: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(JavisApplication.NOTIFICATION_ID_ASSISTANT, buildNotification(status))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        speechManager.destroy()
        ttsManager.destroy()
        Log.d("JavisService", "Service destroyed")
    }
}
