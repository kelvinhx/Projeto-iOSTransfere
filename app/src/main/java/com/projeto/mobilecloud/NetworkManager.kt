package com.projeto.mobilecloud

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import java.net.Inet4Address

object NetworkManager {
    private var currentIp: String = "0.0.0.0"

    fun getLocalIpAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            for (i in interfaces) {
                for (a in i.inetAddresses) {
                    if (!a.isLoopbackAddress && a is java.net.Inet4Address) {
                        currentIp = a.hostAddress ?: "0.0.0.0"
                        return currentIp
                    }
                }
            }
        } catch (e: Exception) { }
        return currentIp
    }

    // Verifica se o IP mudou para disparar uma atualização na UI
    fun hasIpChanged(): Boolean {
        val oldIp = currentIp
        return getLocalIpAddress() != oldIp
    }
}