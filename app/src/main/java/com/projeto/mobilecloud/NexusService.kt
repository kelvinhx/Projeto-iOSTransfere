package com.projeto.mobilecloud

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.os.Build
import androidx.core.app.NotificationCompat

class NexusService : Service() {
    private var server: FileServer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "NexusServer"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Nexus Engine", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Nexus Pro Ativo")
            .setContentText("Servidor rodando")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)

        if (server == null) {
            server = FileServer(this)
            server?.start()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}