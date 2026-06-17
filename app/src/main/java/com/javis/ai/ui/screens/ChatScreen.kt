package com.javis.ai.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.javis.ai.ui.ChatEntry
import com.javis.ai.ui.MainViewModel
import com.javis.ai.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JavisDeepBlue)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(JavisDarkSurface, JavisCard))
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "JAVIS",
                            style = MaterialTheme.typography.headlineMedium,
                            color = JavisBlue
                        )
                        Text(
                            if (uiState.isProcessing) "Thinking..." else if (uiState.isListening) "Listening..." else "Ready",
                            style = MaterialTheme.typography.bodySmall,
                            color = JavisTextSecondary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.startService() },
                        ) {
                            Icon(
                                if (uiState.serviceRunning) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Service",
                                tint = if (uiState.serviceRunning) JavisGreen else JavisTextSecondary
                            )
                        }
                        IconButton(onClick = { viewModel.clearConversation() }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = JavisTextSecondary)
                        }
                    }
                }
            }

            // Messages
            if (uiState.messages.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = JavisBlue.copy(alpha = 0.4f)
                        )
                        Text(
                            "Hello. Javis online.\nHow can I help you?",
                            style = MaterialTheme.typography.titleMedium,
                            color = JavisTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.messages) { message ->
                        ChatBubble(message)
                    }
                    if (uiState.isProcessing) {
                        item { ThinkingIndicator() }
                    }
                }
            }

            // Pending action confirmation
            AnimatedVisibility(visible = uiState.pendingAction != null) {
                uiState.pendingAction?.let { action ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = JavisCard
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                action.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = JavisTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = { viewModel.cancelPendingAction() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = JavisError)
                            ) { Text("Cancel") }
                            Button(
                                onClick = { viewModel.confirmPendingAction() },
                                colors = ButtonDefaults.buttonColors(containerColor = JavisGreen, contentColor = JavisDeepBlue)
                            ) { Text("Confirm") }
                        }
                    }
                }
            }

            // Input bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = JavisDarkSurface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...", color = JavisTextSecondary) },
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendTextMessage(inputText)
                                inputText = ""
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = JavisTextPrimary,
                            unfocusedTextColor = JavisTextPrimary,
                            focusedBorderColor = JavisBlue,
                            unfocusedBorderColor = JavisDivider,
                            cursorColor = JavisBlue
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )

                    // Mic button
                    FilledIconButton(
                        onClick = {
                            if (uiState.isListening) viewModel.stopListening()
                            else viewModel.startListening()
                        },
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (uiState.isListening) JavisError else JavisBlue
                        )
                    ) {
                        Icon(
                            if (uiState.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Microphone",
                            tint = if (uiState.isListening) Color.White else JavisDeepBlue
                        )
                    }

                    // Send text button
                    AnimatedVisibility(visible = inputText.isNotBlank()) {
                        FilledIconButton(
                            onClick = {
                                viewModel.sendTextMessage(inputText)
                                inputText = ""
                            },
                            modifier = Modifier.size(56.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = JavisCyan)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = JavisDeepBlue)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatEntry) {
    val isUser = message.role == "user"
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(JavisBlue, JavisPurple)))
                    .align(Alignment.Bottom),
                contentAlignment = Alignment.Center
            ) {
                Text("J", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isUser) 16.dp else 4.dp,
                    topEnd = if (isUser) 4.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                color = if (isUser) JavisAccent else JavisCard
            ) {
                Text(
                    message.content,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = JavisTextPrimary
                )
            }
            Text(
                sdf.format(Date(message.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = JavisTextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun ThinkingIndicator() {
    var dotCount by remember { mutableStateOf(1) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            dotCount = (dotCount % 3) + 1
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(JavisBlue, JavisPurple))),
            contentAlignment = Alignment.Center
        ) {
            Text("J", style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            color = JavisCard
        ) {
            Text(
                ".".repeat(dotCount),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = JavisBlue
            )
        }
    }
}
