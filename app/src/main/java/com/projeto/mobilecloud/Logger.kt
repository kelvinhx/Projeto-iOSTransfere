package com.projeto.mobilecloud

import android.app.ActivityManager
import android.content.Context
import java.util.*

object Logger {
    private val logs = LinkedList<String>()

    fun log(message: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logs.addFirst("[$time] $message")
        if (logs.size > 100) logs.removeLast()
    }

    fun getSystemStatus(context: Context): String {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemory(memInfo)
        val ramLivre = memInfo.availMem / 1024 / 1024
        return "RAM Livre: ${ramLivre}MB | Low RAM: ${memInfo.lowMemory}"
    }

    fun getLogs(): String = logs.joinToString("\n")
}