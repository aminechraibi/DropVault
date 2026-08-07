package com.example.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.server.ServerService
import com.example.ui.InboxViewModel
import com.example.ui.components.ServerStatusCard
import com.example.ui.components.StorageCard

@Composable
fun WebAccessScreen(
    viewModel: InboxViewModel
) {
    val context = LocalContext.current
    val totalInboxSize by viewModel.totalInboxSize.collectAsState()
    val storageUsage by viewModel.storageUsage.collectAsState()

    var isServerRunning by remember { mutableStateOf(false) }
    var serverPort by remember { mutableStateOf(8080) }
    var serverPin by remember { mutableStateOf("******") }
    var boundService by remember { mutableStateOf<ServerService?>(null) }

    val deviceInfo = remember { viewModel.deviceInfoProvider.getDeviceDetails() }

    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as ServerService.LocalBinder
                val srv = binder.getService()
                boundService = srv
                isServerRunning = srv.isRunning
                serverPort = srv.currentPort
                serverPin = srv.currentPin
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                boundService = null
                isServerRunning = false
            }
        }
    }

    DisposableEffect(Unit) {
        val intent = Intent(context, ServerService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        onDispose {
            try {
                context.unbindService(connection)
            } catch (e: Exception) { }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ServerStatusCard(
            isRunning = isServerRunning,
            ipAddress = deviceInfo.ipAddress,
            port = serverPort,
            pin = serverPin,
            onToggleServer = { enabled ->
                val intent = Intent(context, ServerService::class.java)
                if (enabled) {
                    context.startService(intent)
                    isServerRunning = true
                } else {
                    intent.action = ServerService.ACTION_STOP
                    context.startService(intent)
                    isServerRunning = false
                }
            },
            onRegeneratePin = {
                boundService?.regeneratePin { newPin ->
                    serverPin = newPin
                }
            }
        )

        StorageCard(
            inboxSizeBytes = totalInboxSize,
            totalCount = storageUsage.sumOf { it.count },
            storageUsage = storageUsage,
            onClearCache = {
                viewModel.clearCache()
            }
        )
    }
}
