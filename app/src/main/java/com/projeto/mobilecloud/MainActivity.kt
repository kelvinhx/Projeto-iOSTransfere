package com.projeto.mobilecloud

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Impedir que a tela durma na TV
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.parseColor("#050505"))
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
        }

        val ip = getLocalIpAddress()
        val url = "http://$ip:8080"

        // Card UI
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 50, 50, 50)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#111111"))
                cornerRadius = 40f
                setStroke(3, Color.parseColor("#222222"))
            }
        }

        val qrCode = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(450, 450)
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.WHITE) // Fundo branco para o QR ser legível
            thread {
                try {
                    val bitmap = BitmapFactory.decodeStream(URL("https://chart.googleapis.com/chart?chs=450x450&cht=qr&chl=$url").openStream())
                    runOnUiThread { setImageBitmap(bitmap) }
                } catch(e: Exception) {}
            }
        }

        val statusText = TextView(this).apply {
            text = "NEXUS EXPLORER ATIVO\n$url"
            setTextColor(Color.WHITE)
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 30)
        }

        val btnSetup = Button(this).apply {
            text = "CONCEDER ACESSO TOTAL AOS ARQUIVOS"
            isFocusable = true
            requestFocus()
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0A84FF"))
                cornerRadius = 20f
            }
            setPadding(40, 25, 40, 25)
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        }

        card.addView(qrCode)
        card.addView(statusText)
        card.addView(btnSetup)
        root.addView(card)
        scroll.addView(root)
        setContentView(scroll)

        // Inicia o servidor apenas se tivermos o IP
        if (ip != "0.0.0.0") {
            FileServer().start()
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "0.0.0.0"
                    }
                }
            }
        } catch (e: Exception) {}
        return "0.0.0.0"
    }
}