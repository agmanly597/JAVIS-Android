package com.javis.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.javis.ai.memory.entities.UserMemory
import com.javis.ai.ui.MainViewModel
import com.javis.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(viewModel: MainViewModel) {
    val allMemories by viewModel.memoryManager.getAllMemories().collectAsState(initial = emptyList())
    val topApps by viewModel.memoryManager.getTopApps(8).collectAsState(initial = emptyList())
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JavisDeepBlue)
    ) {
        TopAppBar(
            title = { Text("Memory", color = JavisTextPrimary) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = JavisDarkSurface),
            actions = {
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Clear", tint = JavisError)
                }
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User memories
            item {
                Text(
                    "What JAVIS Remembers",
                    style = MaterialTheme.typography.titleMedium,
                    color = JavisBlue
                )
            }

            if (allMemories.isEmpty()) {
                item {
                    Surface(shape = RoundedCornerShape(12.dp), color = JavisCard) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No memories yet. Tell JAVIS your name and preferences.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = JavisTextSecondary
                            )
                        }
                    }
                }
            } else {
                items(allMemories) { memory ->
                    MemoryItem(memory)
                }
            }

            // App usage
            if (topApps.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Most Used Apps",
                        style = MaterialTheme.typography.titleMedium,
                        color = JavisBlue
                    )
                }
                items(topApps) { app ->
                    Surface(shape = RoundedCornerShape(10.dp), color = JavisCard) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Apps, contentDescription = null, tint = JavisBlue)
                                Text(app.appName, style = MaterialTheme.typography.bodyMedium, color = JavisTextPrimary)
                            }
                            Text(
                                "${app.launchCount}x",
                                style = MaterialTheme.typography.labelMedium,
                                color = JavisTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Memory?") },
            text = { Text("This will erase everything JAVIS has learned about you.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.memoryManager.also {
                            kotlinx.coroutines.MainScope().launch {
                                it.clearAllMemory()
                                it.clearConversationHistory()
                            }
                        }
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JavisError)
                ) { Text("Clear") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
            containerColor = JavisCard
        )
    }
}

private fun kotlinx.coroutines.MainScope() = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
private fun kotlinx.coroutines.CoroutineScope.launch(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) =
    kotlinx.coroutines.launch(block = block)

@Composable
fun MemoryItem(memory: UserMemory) {
    Surface(shape = RoundedCornerShape(10.dp), color = JavisCard) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    memory.key.replace("_", " ").replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = JavisBlue
                )
                Text(
                    memory.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = JavisTextPrimary
                )
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = JavisPurple.copy(alpha = 0.2f)
            ) {
                Text(
                    memory.category,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = JavisPurple
                )
            }
        }
    }
}
