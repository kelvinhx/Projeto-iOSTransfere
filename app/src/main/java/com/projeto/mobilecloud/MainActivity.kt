package com.projeto.mobilecloud

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.graphics.Color
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.net.InetAddress
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {

    private var server: FileServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Layout Principal Programático
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F111A")) // Dark Navy
            setPadding(60, 60, 60, 60)
        }

        val title = TextView(this).apply {
            text = "NEXUS TRANSFER"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 34f
            setPadding(0, 0, 0, 20)
        }

        val ipDisplay = TextView(this).apply {
            val ip = getLocalIpAddress()
            text = "IP DA TV: $ip\nAcesse pelo iPhone na porta :8080"
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
        }

        val statusBtn = Button(this).apply {
            text = "CONCEDER PERMISSÃO DE ARQUIVOS"
            isFocusable = true
            requestFocus()
            setPadding(40, 20, 40, 20)
            setOnClickListener {
                checkAndRequestPermissions()
            }
        }

        root.addView(title)
        root.addView(ipDisplay)
        root.addView(statusBtn)
        setContentView(root)

        // Inicia o motor do servidor
        startNexusServer()
    }

    private fun startNexusServer() {
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        server = FileServer(path)
        server?.start()
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Permissão já concedida!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is InetAddress) {
                        val s = addr.hostAddress
                        if (s.contains(".")) return s
                    }
                }
            }
        } catch (e: Exception) {}
        return "Conecte ao Wi-Fi"
    }
}