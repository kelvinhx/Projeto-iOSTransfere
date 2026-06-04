package com.projeto.mobilecloud

import java.util.*

object Logger {
    private val logs = LinkedList<String>()

    fun log(message: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = "[$time] $message"
        logs.addFirst(entry)
        if (logs.size > 100) logs.removeLast() // Mantém apenas os últimos 100
        println(entry)
    }

    fun getLogs(): String = logs.joinToString("\n")
}