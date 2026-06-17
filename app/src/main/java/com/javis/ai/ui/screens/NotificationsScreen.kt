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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.javis.ai.notifications.JavisNotification
import com.javis.ai.ui.MainViewModel
import com.javis.ai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(viewModel: MainViewModel) {
    val notifications by viewModel.notificationStore.notifications.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JavisDeepBlue)
    ) {
        TopAppBar(
            title = { Text("Notifications", color = JavisTextPrimary) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = JavisDarkSurface),
            actions = {
                IconButton(onClick = {
                    viewModel.notificationStore.markAllRead()
                }) {
                    Icon(Icons.Default.DoneAll, contentDescription = "Mark all read", tint = JavisBlue)
                }
                IconButton(onClick = {
                    viewModel.notificationStore.clearAll()
                }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = JavisTextSecondary)
                }
            }
        )

        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.NotificationsNone,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = JavisTextSecondary.copy(alpha = 0.5f)
                    )
                    Text(
                        "No notifications yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = JavisTextSecondary
                    )
                    Text(
                        "Enable Notification Access in Settings\nfor JAVIS to read your alerts",
                        style = MaterialTheme.typography.bodySmall,
                        color = JavisTextSecondary.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Text(
                "${notifications.count { !it.isRead }} unread",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = JavisTextSecondary
            )

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications) { notif ->
                    NotificationCard(notif) {
                        viewModel.sendTextMessage("Summarize: ${notif.title}: ${notif.text}")
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(notif: JavisNotification, onAskJavis: () -> Unit) {
    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (!notif.isRead) JavisCard else JavisCard.copy(alpha = 0.6f),
        tonalElevation = if (!notif.isRead) 2.dp else 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!notif.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(JavisBlue, shape = androidx.compose.foundation.shape.CircleShape)
                        )
                    }
                    Text(
                        notif.appName,
                        style = MaterialTheme.typography.labelMedium,
                        color = JavisBlue
                    )
                }
                Text(
                    sdf.format(Date(notif.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = JavisTextSecondary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                notif.title,
                style = MaterialTheme.typography.titleSmall,
                color = JavisTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (notif.text.isNotEmpty()) {
                Text(
                    notif.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = JavisTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onAskJavis,
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ask JAVIS", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
