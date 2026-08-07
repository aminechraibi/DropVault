package com.example.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

enum class NavigationScreen(val title: String) {
    INBOX("Inbox"),
    FOLDERS("Folders"),
    WEB_ACCESS("Web Access"),
    SETTINGS("Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    currentScreen: NavigationScreen,
    onNavigate: (NavigationScreen) -> Unit,
    title: String = currentScreen.title,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (paddingValues: androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == NavigationScreen.INBOX,
                    onClick = { onNavigate(NavigationScreen.INBOX) },
                    icon = { Icon(if (currentScreen == NavigationScreen.INBOX) Icons.Filled.Inbox else Icons.Outlined.Inbox, contentDescription = "Inbox") },
                    label = { Text("Inbox") }
                )
                NavigationBarItem(
                    selected = currentScreen == NavigationScreen.FOLDERS,
                    onClick = { onNavigate(NavigationScreen.FOLDERS) },
                    icon = { Icon(if (currentScreen == NavigationScreen.FOLDERS) Icons.Filled.Folder else Icons.Outlined.Folder, contentDescription = "Folders") },
                    label = { Text("Folders") }
                )
                NavigationBarItem(
                    selected = currentScreen == NavigationScreen.WEB_ACCESS,
                    onClick = { onNavigate(NavigationScreen.WEB_ACCESS) },
                    icon = { Icon(if (currentScreen == NavigationScreen.WEB_ACCESS) Icons.Filled.Lan else Icons.Outlined.Lan, contentDescription = "Web Access") },
                    label = { Text("Web Access") }
                )
                NavigationBarItem(
                    selected = currentScreen == NavigationScreen.SETTINGS,
                    onClick = { onNavigate(NavigationScreen.SETTINGS) },
                    icon = { Icon(if (currentScreen == NavigationScreen.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        },
        floatingActionButton = floatingActionButton,
        content = content
    )
}
