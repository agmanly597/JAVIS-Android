package com.javis.ai.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.javis.ai.ai.ProviderType
import com.javis.ai.services.FloatingWindowService
import com.javis.ai.services.WakeWordService
import com.javis.ai.ui.FloatingOverlayActivity
import com.javis.ai.ui.MainViewModel
import com.javis.ai.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var groqKey by remember(settings.groqApiKey) { mutableStateOf(settings.groqApiKey) }
    var deepSeekKey by remember(settings.deepSeekApiKey) { mutableStateOf(settings.deepSeekApiKey) }
    var userName by remember(settings.userName) { mutableStateOf(settings.userName) }
    var showGroqKey by remember { mutableStateOf(false) }
    var showDSKey by remember { mutableStateOf(false) }

    fun hasOverlayPermission() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else true

    fun startFloatService() {
        val i = Intent(context, FloatingWindowService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i)
        else context.startService(i)
    }

    fun stopFloatService() = context.stopService(Intent(context, FloatingWindowService::class.java))

    fun startWakeWord() {
        val i = Intent(context, WakeWordService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i)
        else context.startService(i)
    }

    fun stopWakeWord() = context.stopService(Intent(context, WakeWordService::class.java))

    Column(modifier = Modifier.fillMaxSize().background(JavisDeepBlue)) {
        TopAppBar(
            title = { Text("Settings", color = JavisTextPrimary) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = JavisDarkSurface)
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Profile ──────────────────────────────────────────────────────
            item {
                SettingsSection("Profile") {
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text("Your Name") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                scope.launch {
                                    viewModel.settingsManager.updateUserName(userName)
                                    viewModel.memoryManager.remember("user_name", userName, "personal")
                                }
                            }) {
                                Icon(Icons.Default.Save, contentDescription = "Save", tint = JavisBlue)
                            }
                        },
                        colors = outlinedTextFieldColors()
                    )
                }
            }

            // ── AI Provider ──────────────────────────────────────────────────
            item {
                SettingsSection("AI Provider") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ProviderType.entries.forEach { provider ->
                                FilterChip(
                                    selected = settings.preferredProvider == provider,
                                    onClick = { scope.launch { viewModel.settingsManager.updatePreferredProvider(provider) } },
                                    label = { Text(provider.name, style = MaterialTheme.typography.labelMedium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = JavisBlue,
                                        selectedLabelColor = JavisDeepBlue
                                    )
                                )
                            }
                        }
                        ApiKeyField(
                            label = "Groq API Key",
                            value = groqKey, onValueChange = { groqKey = it },
                            show = showGroqKey, onToggleShow = { showGroqKey = !showGroqKey },
                            onSave = { scope.launch { viewModel.settingsManager.updateGroqApiKey(groqKey); viewModel.aiProviderManager.configure() } }
                        )
                        ApiKeyField(
                            label = "DeepSeek API Key",
                            value = deepSeekKey, onValueChange = { deepSeekKey = it },
                            show = showDSKey, onToggleShow = { showDSKey = !showDSKey },
                            onSave = { scope.launch { viewModel.settingsManager.updateDeepSeekApiKey(deepSeekKey); viewModel.aiProviderManager.configure() } }
                        )
                    }
                }
            }

            // ── Voice ────────────────────────────────────────────────────────
            item {
                SettingsSection("Voice") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Speed: ${String.format("%.1f", settings.voiceSpeed)}x",
                            style = MaterialTheme.typography.bodyMedium, color = JavisTextSecondary)
                        Slider(
                            value = settings.voiceSpeed,
                            onValueChange = { scope.launch { viewModel.settingsManager.updateVoiceSpeed(it) } },
                            valueRange = 0.5f..2.0f, steps = 5,
                            colors = SliderDefaults.colors(thumbColor = JavisBlue, activeTrackColor = JavisBlue)
                        )
                        Text("Pitch: ${String.format("%.1f", settings.voicePitch)}x",
                            style = MaterialTheme.typography.bodyMedium, color = JavisTextSecondary)
                        Slider(
                            value = settings.voicePitch,
                            onValueChange = { scope.launch { viewModel.settingsManager.updateVoicePitch(it) } },
                            valueRange = 0.5f..2.0f, steps = 5,
                            colors = SliderDefaults.colors(thumbColor = JavisCyan, activeTrackColor = JavisCyan)
                        )
                    }
                }
            }

            // ── Wake Word ────────────────────────────────────────────────────
            item {
                SettingsSection("Wake Word — \"Hey Javis\"") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ToggleRow(
                            label = "Always listen for \"Hey Javis\"",
                            description = "Activates JAVIS when you say the wake word from any app",
                            checked = settings.wakeWordEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    viewModel.settingsManager.updateWakeWord(enabled)
                                    if (enabled) startWakeWord() else stopWakeWord()
                                }
                            }
                        )
                        if (settings.wakeWordEnabled) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = JavisBlue.copy(alpha = 0.12f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Wake phrases:", style = MaterialTheme.typography.labelSmall, color = JavisBlue)
                                    listOf("\"Hey Javis\"", "\"Javis\"", "\"OK Javis\"").forEach {
                                        Text("  $it", style = MaterialTheme.typography.bodySmall, color = JavisTextSecondary)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Note: Uses microphone continuously. May increase battery usage slightly.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = JavisTextSecondary.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Floating Button / Widget ──────────────────────────────────────
            item {
                SettingsSection("Quick Activation") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                        // Widget info (overlay alternative)
                        Surface(shape = RoundedCornerShape(8.dp), color = JavisCard) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Widgets, null, tint = JavisCyan, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Home Screen Widget", style = MaterialTheme.typography.bodyMedium, color = JavisTextPrimary)
                                    Text("Long-press your home screen → Widgets → JAVIS to add a 1×1 tap-to-activate button anywhere.",
                                        style = MaterialTheme.typography.bodySmall, color = JavisTextSecondary)
                                }
                            }
                        }

                        Divider(color = JavisDivider)

                        ToggleRow(
                            label = "Floating J Button",
                            description = if (hasOverlayPermission()) "Button floats over all apps — tap to activate"
                                          else "⚠ Requires 'Draw over other apps' permission",
                            checked = settings.floatingButtonEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    viewModel.settingsManager.updateFloatingButton(enabled)
                                    if (enabled) {
                                        if (hasOverlayPermission()) startFloatService()
                                        else context.startActivity(
                                            Intent(context, FloatingOverlayActivity::class.java).apply {
                                                action = FloatingOverlayActivity.ACTION_REQUEST_PERMISSION
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        )
                                    } else stopFloatService()
                                }
                            }
                        )

                        if (!hasOverlayPermission()) {
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(context, FloatingOverlayActivity::class.java).apply {
                                            action = FloatingOverlayActivity.ACTION_REQUEST_PERMISSION
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = JavisBlue)
                            ) {
                                Icon(Icons.Default.Security, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Grant 'Draw over apps' Permission")
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { startFloatService() }, modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = JavisBlue)) {
                                    Text("Show Now", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(onClick = { stopFloatService() }, modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = JavisTextSecondary)) {
                                    Text("Hide Now", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // ── System ───────────────────────────────────────────────────────
            item {
                SettingsSection("System") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToggleRow(
                            label = "Read Notifications Aloud",
                            description = "JAVIS reads new notifications to you",
                            checked = settings.readNotificationsAloud,
                            onCheckedChange = { scope.launch { viewModel.settingsManager.updateReadNotifications(it) } }
                        )
                        ToggleRow(
                            label = "Auto-start on Boot",
                            description = "JAVIS starts when your device boots",
                            checked = settings.autoStartOnBoot,
                            onCheckedChange = { scope.launch { viewModel.settingsManager.updateAutoStart(it) } }
                        )
                        ToggleRow(
                            label = "Conversation Mode",
                            description = "JAVIS re-listens after each response for follow-up",
                            checked = settings.conversationMode,
                            onCheckedChange = { scope.launch { viewModel.settingsManager.updateConversationMode(it) } }
                        )
                    }
                }
            }

            // ── Permissions Guide ────────────────────────────────────────────
            item {
                SettingsSection("Permissions Guide") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PermissionRow("Microphone", "Voice input — required", required = true)
                        PermissionRow("Contacts", "Call/message by name", required = false)
                        PermissionRow("Notification Access", "Read/reply to notifications", required = false, onClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                        })
                        PermissionRow("Accessibility", "Navigate apps, scroll, read screen", required = false, onClick = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                        })
                    }
                }
            }

            // ── About ────────────────────────────────────────────────────────
            item {
                SettingsSection("About") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("JAVIS v1.1.0", style = MaterialTheme.typography.bodyMedium, color = JavisTextPrimary)
                        Text("Personal AI Assistant • Kotlin + Compose", style = MaterialTheme.typography.bodySmall, color = JavisTextSecondary)
                        Text("Optimized for low-end Android (Redmi A1)", style = MaterialTheme.typography.bodySmall, color = JavisTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRow(label: String, description: String, required: Boolean, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = JavisTextPrimary)
            Text(description, style = MaterialTheme.typography.bodySmall, color = JavisTextSecondary)
        }
        if (onClick != null) {
            TextButton(onClick = onClick) { Text("Open", color = JavisBlue, style = MaterialTheme.typography.labelSmall) }
        } else if (required) {
            Text("Required", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = JavisCard) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = JavisBlue)
            content()
        }
    }
}

@Composable
fun ApiKeyField(label: String, value: String, onValueChange: (String) -> Unit, show: Boolean, onToggleShow: () -> Unit, onSave: () -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (show) androidx.compose.ui.text.input.VisualTransformation.None
        else androidx.compose.ui.text.input.PasswordVisualTransformation(),
        trailingIcon = {
            Row {
                IconButton(onClick = onToggleShow) {
                    Icon(if (show) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = JavisTextSecondary)
                }
                IconButton(onClick = onSave) { Icon(Icons.Default.Save, null, tint = JavisBlue) }
            }
        },
        colors = outlinedTextFieldColors(), singleLine = true
    )
}

@Composable
fun ToggleRow(label: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = JavisTextPrimary)
            Text(description, style = MaterialTheme.typography.bodySmall, color = JavisTextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = JavisDeepBlue, checkedTrackColor = JavisBlue))
    }
}

@Composable
fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = JavisTextPrimary, unfocusedTextColor = JavisTextPrimary,
    focusedBorderColor = JavisBlue, unfocusedBorderColor = JavisDivider,
    focusedLabelColor = JavisBlue, unfocusedLabelColor = JavisTextSecondary,
    cursorColor = JavisBlue
)
