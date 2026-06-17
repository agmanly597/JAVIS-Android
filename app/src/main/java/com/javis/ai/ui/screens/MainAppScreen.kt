package com.javis.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.javis.ai.ui.MainViewModel
import com.javis.ai.ui.theme.*

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Chat : Screen("chat", "Chat", Icons.Default.Chat)
    object Voice : Screen("voice", "Voice", Icons.Default.Mic)
    object Notifications : Screen("notifications", "Alerts", Icons.Default.Notifications)
    object Memory : Screen("memory", "Memory", Icons.Default.Psychology)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination

    val tabs = listOf(
        Screen.Chat, Screen.Voice, Screen.Notifications, Screen.Memory, Screen.Settings
    )

    Scaffold(
        containerColor = JavisDeepBlue,
        bottomBar = {
            NavigationBar(
                containerColor = JavisDarkSurface,
                tonalElevation = 0.dp
            ) {
                tabs.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label, style = MaterialTheme.typography.labelSmall) },
                        selected = currentDest?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = JavisBlue,
                            selectedTextColor = JavisBlue,
                            unselectedIconColor = JavisTextSecondary,
                            unselectedTextColor = JavisTextSecondary,
                            indicatorColor = JavisCard
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Chat.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Chat.route) { ChatScreen(viewModel) }
            composable(Screen.Voice.route) { VoiceScreen(viewModel) }
            composable(Screen.Notifications.route) { NotificationsScreen(viewModel) }
            composable(Screen.Memory.route) { MemoryScreen(viewModel) }
            composable(Screen.Settings.route) { SettingsScreen(viewModel) }
        }
    }
}
