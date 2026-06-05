package com.projeto.mobilecloud

import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkManager {
    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (i in interfaces) {
                val addrs = i.inetAddresses
                for (a in addrs) {
                    if (!a.isLoopbackAddress && a is Inet4Address) {
                        return a.hostAddress ?: "0.0.0.0"
                    }
                }
            }
        } catch (e: Exception) {}
        return "0.0.0.0"
    }
}