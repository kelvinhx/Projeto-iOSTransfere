package com.projeto.mobilecloud

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Layout Principal
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
            layoutParams = LinearLayout.LayoutParams(900, -2)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#121214"))
                cornerRadius = 40f
                setStroke(3, Color.parseColor("#333333"))
            }
        }

        val title = TextView(this).apply {
            text = "NEXUS FILE EXPLORER"
            setTextColor(Color.WHITE)
            textSize = 30f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 40)
        }

        val qrImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(450, 450)
            setBackgroundColor(Color.WHITE) // Fundo branco para o QR ser escaneável
            setPadding(10, 10, 10, 10)
        }

        val ip = getLocalIpAddress()
        val url = "http://$ip:8080"

        val statusText = TextView(this).apply {
            text = "Escaneie para gerenciar arquivos\n$url"
            setTextColor(Color.parseColor("#007AFF"))
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 40)
        }

        val btnSetup = Button(this).apply {
            text = "CONCEDER ACESSO TOTAL"
            isFocusable = true
            requestFocus()
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1C1C1E"))
                cornerRadius = 20f
                setStroke(2, Color.DKGRAY)
            }
            setPadding(40, 30, 40, 30)
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            }
        }

        card.addView(title)
        card.addView(qrImage)
        card.addView(statusText)
        card.addView(btnSetup)
        root.addView(card)
        setContentView(root)

        // Thread para carregar o QR Code
        thread {
            try {
                val bitmap = BitmapFactory.decodeStream(URL("https://chart.googleapis.com/chart?chs=450x450&cht=qr&chl=$url").openStream())
                runOnUiThread { qrImage.setImageBitmap(bitmap) }
            } catch (e: Exception) {}
        }

        FileServer().start()
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) return addr.hostAddress ?: "0.0.0.0"
                }
            }
        } catch (e: Exception) {}
        return "0.0.0.0"
    }
}