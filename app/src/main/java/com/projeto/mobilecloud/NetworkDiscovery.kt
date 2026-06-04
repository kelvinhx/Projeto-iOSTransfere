package com.projeto.mobilecloud

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo

class NetworkDiscovery(context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    fun registerService(port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "NexusExplorer"
            serviceType = "_http._tcp."
            setPort(port)
        }
        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) = Logger.log("Serviço NSD Registrado")
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Logger.log("Falha no NSD: $errorCode")
                override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            })
        } catch (e: Exception) { Logger.log("Erro NSD: ${e.message}") }
    }
}