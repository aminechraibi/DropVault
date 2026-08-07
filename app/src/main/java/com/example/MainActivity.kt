package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.InboxViewModel
import com.example.ui.components.AppScaffold
import com.example.ui.components.NavigationScreen
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.InboxScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WebAccessScreen
import com.example.ui.theme.Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Theme {
                val viewModel: InboxViewModel = viewModel()
                var currentScreen by remember { mutableStateOf(NavigationScreen.INBOX) }

                AppScaffold(
                    currentScreen = currentScreen,
                    onNavigate = { currentScreen = it }
                ) { paddingValues ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.padding(paddingValues)
                    ) {
                        when (currentScreen) {
                            NavigationScreen.INBOX -> InboxScreen(
                                viewModel = viewModel,
                                onOpenWebAccess = { currentScreen = NavigationScreen.WEB_ACCESS }
                            )
                            NavigationScreen.FOLDERS -> FoldersScreen(
                                viewModel = viewModel,
                                onNavigateToInbox = { currentScreen = NavigationScreen.INBOX }
                            )
                            NavigationScreen.WEB_ACCESS -> WebAccessScreen(
                                viewModel = viewModel
                            )
                            NavigationScreen.SETTINGS -> SettingsScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
