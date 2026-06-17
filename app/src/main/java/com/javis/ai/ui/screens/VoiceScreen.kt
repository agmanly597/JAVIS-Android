package com.javis.ai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.javis.ai.ui.MainViewModel
import com.javis.ai.ui.theme.*

@Composable
fun VoiceScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val isListening = uiState.isListening
    val isProcessing = uiState.isProcessing

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val scale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val outerScale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outer_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JavisDeepBlue),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Text(
                "JAVIS",
                style = MaterialTheme.typography.displaySmall,
                color = JavisBlue
            )
            Text(
                "Voice Mode",
                style = MaterialTheme.typography.bodyLarge,
                color = JavisTextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Pulsing mic button
            Box(contentAlignment = Alignment.Center) {
                // Outer glow ring
                if (isListening) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .scale(outerScale)
                            .background(JavisGlow, CircleShape)
                    )
                }
                // Middle ring
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(if (isListening) scale else 1f)
                        .border(
                            2.dp,
                            Brush.sweepGradient(listOf(JavisBlue, JavisPurple, JavisCyan, JavisBlue)),
                            CircleShape
                        )
                        .background(
                            if (isListening) JavisBlue.copy(alpha = 0.1f) else Color.Transparent,
                            CircleShape
                        )
                )
                // Main button
                FilledIconButton(
                    onClick = {
                        if (isListening) viewModel.stopListening()
                        else viewModel.startListening()
                    },
                    modifier = Modifier.size(120.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = when {
                            isListening -> JavisBlue
                            isProcessing -> JavisPurple
                            else -> JavisCard
                        }
                    )
                ) {
                    Icon(
                        when {
                            isListening -> Icons.Default.MicOff
                            isProcessing -> Icons.Default.HourglassTop
                            else -> Icons.Default.Mic
                        },
                        contentDescription = "Microphone",
                        modifier = Modifier.size(48.dp),
                        tint = when {
                            isListening -> JavisDeepBlue
                            isProcessing -> Color.White
                            else -> JavisBlue
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                when {
                    isListening -> "Listening... speak now"
                    isProcessing -> "Processing your request..."
                    else -> "Tap to speak to JAVIS"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = if (isListening) JavisBlue else JavisTextSecondary,
                textAlign = TextAlign.Center
            )

            uiState.speechError?.let { error ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = JavisError.copy(alpha = 0.15f)
                ) {
                    Text(
                        error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = JavisError
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Quick action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickActionChip("Open App", Icons.Default.Apps) {
                    viewModel.sendTextMessage("Open YouTube")
                }
                QuickActionChip("Notifications", Icons.Default.Notifications) {
                    viewModel.sendTextMessage("Read my notifications")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickActionChip("Set Alarm", Icons.Default.Alarm) {
                    viewModel.sendTextMessage("Set alarm for 7 AM")
                }
                QuickActionChip("Web Search", Icons.Default.Search) {
                    viewModel.sendTextMessage("Search Google for today's news")
                }
            }
        }
    }
}

@Composable
fun QuickActionChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = JavisCard,
            labelColor = JavisTextPrimary,
            leadingIconContentColor = JavisBlue
        ),
        border = AssistChipDefaults.assistChipBorder(
            enabled = true,
            borderColor = JavisDivider,
            borderWidth = 1.dp
        )
    )
}
