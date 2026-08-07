package com.example.server

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.domain.InboxRepository
import com.example.data.preferences.SettingsRepository
import com.example.data.storage.FileStorageManager
import com.example.device.DeviceInfoProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ServerService : Service() {

    private val binder = LocalBinder()
    private var httpServer: LocalHttpServer? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    var isRunning = false
        private set

    var currentPort = 8080
        private set

    var currentPin = ""
        private set

    inner class LocalBinder : Binder() {
        fun getService(): ServerService = this@ServerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopServer()
            stopSelf()
            return START_NOT_STICKY
        }

        startServer()
        return START_STICKY
    }

    fun startServer() {
        if (isRunning) return

        val repository = InboxRepository(
            applicationContext,
            AppDatabase.getDatabase(applicationContext).inboxDao(),
            FileStorageManager(applicationContext),
            SettingsRepository(applicationContext)
        )
        val settings = SettingsRepository(applicationContext)
        val deviceInfo = DeviceInfoProvider(applicationContext)

        scope.launch {
            val port = settings.serverPort.first()
            val pin = settings.serverPin.first()

            currentPort = port
            currentPin = pin

            try {
                httpServer = LocalHttpServer(port, applicationContext, repository, pin).apply {
                    start()
                }
                isRunning = true

                val ip = deviceInfo.getLocalIpAddress()
                showNotification(ip, port, pin)
            } catch (e: Exception) {
                e.printStackTrace()
                stopSelf()
            }
        }
    }

    fun stopServer() {
        httpServer?.stop()
        httpServer = null
        isRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    fun regeneratePin(onNewPin: (String) -> Unit) {
        val settings = SettingsRepository(applicationContext)
        scope.launch {
            val newPin = settings.regeneratePin()
            currentPin = newPin
            httpServer?.updatePin(newPin)

            val deviceInfo = DeviceInfoProvider(applicationContext)
            val ip = deviceInfo.getLocalIpAddress()
            showNotification(ip, currentPort, newPin)

            onNewPin(newPin)
        }
    }

    private fun showNotification(ip: String, port: Int, pin: String) {
        val channelId = "local_inbox_server_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Local Web Server Status",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, ServerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Local Inbox Web Access Active")
            .setContentText("http://$ip:$port | PIN: $pin")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openIntent)
            .addAction(R.mipmap.ic_launcher, "Stop Server", stopIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.example.server.ACTION_STOP"
    }
}
