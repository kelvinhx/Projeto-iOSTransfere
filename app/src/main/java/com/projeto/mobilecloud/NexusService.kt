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
        
        // Criar canal de notificação para Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Nexus Engine", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // Notificação que mantém o serviço vivo (Foreground)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Nexus Pro Ativo")
            .setContentText("Servidor rodando em segundo plano")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .build()

        startForeground(1, notification)

        // Inicia o servidor Ktor se ainda não estiver rodando
        if (server == null) {
            server = FileServer(this)
            server?.start()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}