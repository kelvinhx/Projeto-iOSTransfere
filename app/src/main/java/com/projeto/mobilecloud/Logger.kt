package com.projeto.mobilecloud

import android.app.ActivityManager
import android.content.Context
import java.util.*

object Logger {
    private val logs = LinkedList<String>()

    fun log(message: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = "[$time] $message"
        logs.addFirst(entry)
        if (logs.size > 100) logs.removeLast()
    }

    fun getSystemStatus(androidContext: Context): String {
        return try {
            val actManager = androidContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            val ramLivre = memInfo.availMem / 1024 / 1024
            "RAM Livre: ${ramLivre}MB | Low RAM: ${memInfo.lowMemory}"
        } catch (e: Exception) {
            "Status: Indisponível"
        }
    }

    fun getLogs(): String = logs.joinToString("\n")
}