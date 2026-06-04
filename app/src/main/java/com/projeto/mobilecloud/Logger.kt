package com.projeto.mobilecloud

import java.util.*

object Logger {
    private val logs = LinkedList<String>()
    fun log(message: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logs.addFirst("[$time] $message")
        if (logs.size > 50) logs.removeLast()
    }
    fun getLogs(): String = logs.joinToString("\n")
}