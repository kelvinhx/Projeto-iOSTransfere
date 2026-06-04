package com.projeto.mobilecloud

import android.content.Intent
import android.graphics.Color
import android.os.*
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.net.InetAddress
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {

    private var server: FileServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#050505"))
            setPadding(80, 80, 80, 80)
        }

        val ip = getLocalIpAddress()
        val url = "http://$ip:8080"

        val title = TextView(this).apply {
            text = "NEXUS TRANSFER"
            setTextColor(Color.WHITE)
            textSize = 40f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val subtitle = TextView(this).apply {
            text = "AGUARDANDO CONEXÃO DO IPHONE"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 18f
            setPadding(0, 10, 0, 50)
        }

        val linkCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(40, 40, 40, 40)
            gravity = Gravity.CENTER
        }

        val urlText = TextView(this).apply {
            text = url
            setTextColor(Color.YELLOW)
            textSize = 28f
        }
        
        linkCard.addView(urlText)

        val btnPerm = Button(this).apply {
            text = "CONFIGURAR ARMAZENAMENTO"
            isFocusable = true
            requestFocus() // Foco inicial para o D-Pad
            setBackgroundColor(Color.parseColor("#1C1C1E"))
            setTextColor(Color.WHITE)
            setPadding(30, 20, 30, 20)
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(linkCard)
        root.addView(Space(this).apply { layoutParams = LinearLayout.LayoutParams(1, 40) })
        root.addView(btnPerm)

        setContentView(root)

        // Inicia o servidor na pasta de Downloads
        val storage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!storage.exists()) storage.mkdirs()
        
        server = FileServer(storage)
        server?.start()
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "0.0.0.0"
                    }
                }
            }
        } catch (e: Exception) {}
        return "Sem Wi-Fi"
    }
}

// Helper para espaçamento
class Space(context: android.content.Context) : android.view.View(context)