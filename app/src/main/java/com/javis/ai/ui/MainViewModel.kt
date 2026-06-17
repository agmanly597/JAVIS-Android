package com.javis.ai.ui

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.javis.ai.ai.AIProviderManager
import com.javis.ai.domain.AgentRouter
import com.javis.ai.domain.PendingAction
import com.javis.ai.memory.MemoryManager
import com.javis.ai.notifications.NotificationStore
import com.javis.ai.services.FloatingWindowService
import com.javis.ai.services.JavisAssistantService
import com.javis.ai.settings.JavisSettings
import com.javis.ai.settings.SettingsManager
import com.javis.ai.voice.SpeechRecognitionManager
import com.javis.ai.voice.SpeechState
import com.javis.ai.voice.TTSManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatEntry(
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class UiState(
    val messages: List<ChatEntry> = emptyList(),
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val isProcessing: Boolean = false,
    val pendingAction: PendingAction? = null,
    val serviceRunning: Boolean = false,
    val speechError: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    val speechManager: SpeechRecognitionManager,
    val ttsManager: TTSManager,
    val agentRouter: AgentRouter,
    val memoryManager: MemoryManager,
    val notificationStore: NotificationStore,
    val settingsManager: SettingsManager,
    val aiProviderManager: AIProviderManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val settings: StateFlow<JavisSettings> = settingsManager.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JavisSettings())

    init {
        viewModelScope.launch { aiProviderManager.configure() }
        speechManager.initialize()

        viewModelScope.launch {
            speechManager.state.collect { state ->
                when (state) {
                    is SpeechState.Listening -> _uiState.update { it.copy(isListening = true, speechError = null) }
                    is SpeechState.Result -> {
                        _uiState.update { it.copy(isListening = false) }
                        processUserInput(state.text)
                    }
                    is SpeechState.Error -> _uiState.update {
                        it.copy(isListening = false, speechError = state.message)
                    }
                    else -> _uiState.update { it.copy(isListening = false) }
                }
            }
        }
    }

    fun startListening() {
        if (_uiState.value.isListening) return
        _uiState.update { it.copy(speechError = null) }
        speechManager.startListening { text ->
            viewModelScope.launch { processUserInput(text) }
        }
    }

    fun stopListening() {
        speechManager.stopListening()
        _uiState.update { it.copy(isListening = false) }
    }

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { processUserInput(text) }
    }

    private suspend fun processUserInput(input: String) {
        addMessage("user", input)
        _uiState.update { it.copy(isProcessing = true) }

        val response = agentRouter.route(input)

        addMessage("assistant", response.spokenText)
        _uiState.update {
            it.copy(
                isProcessing = false,
                pendingAction = response.pendingAction
            )
        }

        ttsManager.speak(response.spokenText)
        memoryManager.saveMessage("user", input)
        memoryManager.saveMessage("assistant", response.spokenText)
    }

    fun confirmPendingAction() {
        val action = _uiState.value.pendingAction ?: return
        agentRouter.confirmPendingAction(action)
        _uiState.update { it.copy(pendingAction = null) }
        addMessage("assistant", "Done.")
        ttsManager.speak("Done.")
    }

    fun cancelPendingAction() {
        _uiState.update { it.copy(pendingAction = null) }
        ttsManager.speak("Cancelled.")
    }

    fun startService() {
        val context = getApplication<Application>()
        val intent = Intent(context, JavisAssistantService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        _uiState.update { it.copy(serviceRunning = true) }
    }

    fun stopService() {
        val context = getApplication<Application>()
        context.stopService(Intent(context, JavisAssistantService::class.java))
        _uiState.update { it.copy(serviceRunning = false) }
    }

    fun clearConversation() {
        _uiState.update { it.copy(messages = emptyList(), pendingAction = null) }
        aiProviderManager.clearHistory()
        viewModelScope.launch { memoryManager.clearConversationHistory() }
    }

    private fun addMessage(role: String, content: String) {
        _uiState.update { state ->
            state.copy(messages = state.messages + ChatEntry(role, content))
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
    }
}
