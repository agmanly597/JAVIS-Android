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

    fun stopFloatService() {
        context.stopService(Intent(context, FloatingWindowService::class.java))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JavisDeepBlue)
    ) {
        TopAppBar(
            title = { Text("Settings", color = JavisTextPrimary) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = JavisDarkSurface)
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile
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

            // AI Provider
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
                            value = groqKey,
                            onValueChange = { groqKey = it },
                            show = showGroqKey,
                            onToggleShow = { showGroqKey = !showGroqKey },
                            onSave = {
                                scope.launch {
                                    viewModel.settingsManager.updateGroqApiKey(groqKey)
                                    viewModel.aiProviderManager.configure()
                                }
                            }
                        )
                        ApiKeyField(
                            label = "DeepSeek API Key",
                            value = deepSeekKey,
                            onValueChange = { deepSeekKey = it },
                            show = showDSKey,
                            onToggleShow = { showDSKey = !showDSKey },
                            onSave = {
                                scope.launch {
                                    viewModel.settingsManager.updateDeepSeekApiKey(deepSeekKey)
                                    viewModel.aiProviderManager.configure()
                                }
                            }
                        )
                    }
                }
            }

            // Voice
            item {
                SettingsSection("Voice") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Speech Speed: ${String.format("%.1f", settings.voiceSpeed)}x",
                            style = MaterialTheme.typography.bodyMedium,
                            color = JavisTextSecondary
                        )
                        Slider(
                            value = settings.voiceSpeed,
                            onValueChange = { scope.launch { viewModel.settingsManager.updateVoiceSpeed(it) } },
                            valueRange = 0.5f..2.0f,
                            steps = 5,
                            colors = SliderDefaults.colors(thumbColor = JavisBlue, activeTrackColor = JavisBlue)
                        )
                        Text(
                            "Speech Pitch: ${String.format("%.1f", settings.voicePitch)}x",
                            style = MaterialTheme.typography.bodyMedium,
                            color = JavisTextSecondary
                        )
                        Slider(
                            value = settings.voicePitch,
                            onValueChange = { scope.launch { viewModel.settingsManager.updateVoicePitch(it) } },
                            valueRange = 0.5f..2.0f,
                            steps = 5,
                            colors = SliderDefaults.colors(thumbColor = JavisCyan, activeTrackColor = JavisCyan)
                        )
                    }
                }
            }

            // Floating Button — dedicated section with permission handling
            item {
                SettingsSection("Floating Button") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ToggleRow(
                            label = "Show Floating Button",
                            description = "Tap-anywhere J button stays on top of all apps",
                            checked = settings.floatingButtonEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    viewModel.settingsManager.updateFloatingButton(enabled)
                                    if (enabled) {
                                        if (hasOverlayPermission()) startFloatService()
                                        else {
                                            context.startActivity(
                                                Intent(context, FloatingOverlayActivity::class.java).apply {
                                                    action = FloatingOverlayActivity.ACTION_REQUEST_PERMISSION
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                            )
                                        }
                                    } else {
                                        stopFloatService()
                                    }
                                }
                            }
                        )
                        if (!hasOverlayPermission()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Permission required",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        context.startActivity(
                                            Intent(context, FloatingOverlayActivity::class.java).apply {
                                                action = FloatingOverlayActivity.ACTION_REQUEST_PERMISSION
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        )
                                    }
                                ) {
                                    Text("Grant", color = JavisBlue)
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { if (hasOverlayPermission()) startFloatService() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = JavisBlue)
                            ) {
                                Icon(Icons.Default.OpenWith, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Show Now", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { stopFloatService() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = JavisTextSecondary)
                            ) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Hide Now", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // System
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
                    }
                }
            }

            // About
            item {
                SettingsSection("About") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("JAVIS v1.0.0", style = MaterialTheme.typography.bodyMedium, color = JavisTextPrimary)
                        Text("Personal AI Assistant", style = MaterialTheme.typography.bodySmall, color = JavisTextSecondary)
                        Text("Optimized for low-end Android devices", style = MaterialTheme.typography.bodySmall, color = JavisTextSecondary)
                    }
                }
            }
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
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (show) androidx.compose.ui.text.input.VisualTransformation.None
        else androidx.compose.ui.text.input.PasswordVisualTransformation(),
        trailingIcon = {
            Row {
                IconButton(onClick = onToggleShow) {
                    Icon(if (show) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = JavisTextSecondary)
                }
                IconButton(onClick = onSave) {
                    Icon(Icons.Default.Save, null, tint = JavisBlue)
                }
            }
        },
        colors = outlinedTextFieldColors(),
        singleLine = true
    )
}

@Composable
fun ToggleRow(label: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = JavisTextPrimary)
            Text(description, style = MaterialTheme.typography.bodySmall, color = JavisTextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = JavisDeepBlue, checkedTrackColor = JavisBlue)
        )
    }
}

@Composable
fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = JavisTextPrimary,
    unfocusedTextColor = JavisTextPrimary,
    focusedBorderColor = JavisBlue,
    unfocusedBorderColor = JavisDivider,
    focusedLabelColor = JavisBlue,
    unfocusedLabelColor = JavisTextSecondary,
    cursorColor = JavisBlue
)
