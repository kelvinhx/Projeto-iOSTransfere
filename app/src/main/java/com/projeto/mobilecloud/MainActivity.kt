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
        
        val ip = getLocalIpAddress()
        val url = "http://$ip:8080"

        // Layout Dark Premium
        val root = RelativeLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        // Card Central
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#121212"))
                cornerRadius = 30f
                setStroke(2, Color.parseColor("#333333"))
            }
            val params = RelativeLayout.LayoutParams(1000, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.addRule(RelativeLayout.CENTER_IN_PARENT)
            layoutParams = params
        }

        val title = TextView(this).apply {
            text = "NEXUS PRO 🚀"
            setTextColor(Color.WHITE)
            textSize = 35f
            setTypeface(null, Typeface.BOLD)
        }

        val qrCode = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(400, 400).apply { setMargins(0, 40, 0, 40) }
            // Gerar QR Code via API de Imagem para evitar bloatware de biblioteca
            thread {
                try {
                    val bitmap = BitmapFactory.decodeStream(URL("https://chart.googleapis.com/chart?chs=400x400&cht=qr&chl=$url").openStream())
                    runOnUiThread { setImageBitmap(bitmap) }
                } catch(e: Exception) {}
            }
        }

        val instruction = TextView(this).apply {
            text = "Escanear com o iPhone para Gerenciar\n$url"
            setTextColor(Color.parseColor("#007AFF"))
            gravity = Gravity.CENTER
            textSize = 18f
        }

        val btnFile = Button(this).apply {
            text = "⚙️ CONFIGURAR ACESSO TOTAL"
            isFocusable = true
            requestFocus()
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#007AFF"))
                cornerRadius = 15f
            }
            setTextColor(Color.WHITE)
            setPadding(40, 20, 40, 20)
            layoutParams = LinearLayout.LayoutParams(600, 100).apply { setMargins(0, 50, 0, 0) }
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            }
        }

        card.addView(title)
        card.addView(qrCode)
        card.addView(instruction)
        card.addView(btnFile)
        root.addView(card)

        setContentView(root)

        // Inicializa servidor
        val storage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!storage.exists()) storage.mkdirs()
        FileServer(storage).start()
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) return addr.hostAddress ?: ""
                }
            }
        } catch (e: Exception) {}
        return "Conecte ao Wi-Fi"
    }
}