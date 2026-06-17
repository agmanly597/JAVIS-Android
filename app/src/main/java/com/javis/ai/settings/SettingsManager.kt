package com.javis.ai.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.javis.ai.ai.ProviderType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "javis_settings")

data class JavisSettings(
    val groqApiKey: String = "",
    val deepSeekApiKey: String = "",
    val preferredProvider: ProviderType = ProviderType.GROQ,
    val userName: String = "",
    val voiceSpeed: Float = 1.0f,
    val voicePitch: Float = 1.0f,
    val voiceLocale: String = "en-US",
    val readNotificationsAloud: Boolean = false,
    val floatingButtonEnabled: Boolean = true,
    val autoStartOnBoot: Boolean = true,
    val wakeWordEnabled: Boolean = false,
    val conversationMode: Boolean = false
)

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val GROQ_API_KEY = stringPreferencesKey("groq_api_key")
        val DEEPSEEK_API_KEY = stringPreferencesKey("deepseek_api_key")
        val PREFERRED_PROVIDER = stringPreferencesKey("preferred_provider")
        val USER_NAME = stringPreferencesKey("user_name")
        val VOICE_SPEED = floatPreferencesKey("voice_speed")
        val VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val VOICE_LOCALE = stringPreferencesKey("voice_locale")
        val READ_NOTIFS = booleanPreferencesKey("read_notifications_aloud")
        val FLOATING_BTN = booleanPreferencesKey("floating_button_enabled")
        val AUTO_START = booleanPreferencesKey("auto_start_on_boot")
        val WAKE_WORD = booleanPreferencesKey("wake_word_enabled")
        val CONVERSATION_MODE = booleanPreferencesKey("conversation_mode")
    }

    val settings: Flow<JavisSettings> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            JavisSettings(
                groqApiKey = prefs[Keys.GROQ_API_KEY] ?: "",
                deepSeekApiKey = prefs[Keys.DEEPSEEK_API_KEY] ?: "",
                preferredProvider = runCatching {
                    ProviderType.valueOf(prefs[Keys.PREFERRED_PROVIDER] ?: "GROQ")
                }.getOrDefault(ProviderType.GROQ),
                userName = prefs[Keys.USER_NAME] ?: "",
                voiceSpeed = prefs[Keys.VOICE_SPEED] ?: 1.0f,
                voicePitch = prefs[Keys.VOICE_PITCH] ?: 1.0f,
                voiceLocale = prefs[Keys.VOICE_LOCALE] ?: "en-US",
                readNotificationsAloud = prefs[Keys.READ_NOTIFS] ?: false,
                floatingButtonEnabled = prefs[Keys.FLOATING_BTN] ?: true,
                autoStartOnBoot = prefs[Keys.AUTO_START] ?: true,
                wakeWordEnabled = prefs[Keys.WAKE_WORD] ?: false,
                conversationMode = prefs[Keys.CONVERSATION_MODE] ?: false
            )
        }

    suspend fun updateGroqApiKey(key: String) = update { it[Keys.GROQ_API_KEY] = key }
    suspend fun updateDeepSeekApiKey(key: String) = update { it[Keys.DEEPSEEK_API_KEY] = key }
    suspend fun updatePreferredProvider(p: ProviderType) = update { it[Keys.PREFERRED_PROVIDER] = p.name }
    suspend fun updateUserName(name: String) = update { it[Keys.USER_NAME] = name }
    suspend fun updateVoiceSpeed(speed: Float) = update { it[Keys.VOICE_SPEED] = speed }
    suspend fun updateVoicePitch(pitch: Float) = update { it[Keys.VOICE_PITCH] = pitch }
    suspend fun updateVoiceLocale(locale: String) = update { it[Keys.VOICE_LOCALE] = locale }
    suspend fun updateReadNotifications(enabled: Boolean) = update { it[Keys.READ_NOTIFS] = enabled }
    suspend fun updateFloatingButton(enabled: Boolean) = update { it[Keys.FLOATING_BTN] = enabled }
    suspend fun updateAutoStart(enabled: Boolean) = update { it[Keys.AUTO_START] = enabled }
    suspend fun updateWakeWord(enabled: Boolean) = update { it[Keys.WAKE_WORD] = enabled }
    suspend fun updateConversationMode(enabled: Boolean) = update { it[Keys.CONVERSATION_MODE] = enabled }

    private suspend fun update(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit { prefs -> block(prefs) }
    }
}
