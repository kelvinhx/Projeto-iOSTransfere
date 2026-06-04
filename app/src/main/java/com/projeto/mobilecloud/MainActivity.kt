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

        // Layout Principal: Fundo Preto
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(-1, -1)
        }

        // Card Central: Cinza escuro com bordas
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            layoutParams = LinearLayout.LayoutParams(800, -2) // Largura fixa de 800px
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#151517"))
                cornerRadius = 30f
                setStroke(2, Color.parseColor("#333333"))
            }
        }

        val title = TextView(this).apply {
            text = "NEXUS FILE EXPLORER"
            setTextColor(Color.WHITE)
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 30)
        }

        // Placeholder do QR Code com tamanho fixo
        val qrImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(400, 400)
            setBackgroundColor(Color.DKGRAY)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        val ip = getLocalIpAddress()
        val url = "http://$ip:8080"

        val statusText = TextView(this).apply {
            text = "Acesse no iPhone:\n$url"
            setTextColor(Color.parseColor("#007AFF"))
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }

        val btnSetup = Button(this).apply {
            text = "CONCEDER PERMISSÃO DE ARQUIVOS"
            isFocusable = true
            requestFocus() // Importante para o controle remoto
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1C1C1E"))
                cornerRadius = 15f
                setStroke(2, Color.GRAY)
            }
            setPadding(30, 20, 30, 20)
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

        // Carregar QR Code na Thread de Rede
        thread {
            try {
                val bitmap = BitmapFactory.decodeStream(URL("https://chart.googleapis.com/chart?chs=400x400&cht=qr&chl=$url").openStream())
                runOnUiThread { qrImage.setImageBitmap(bitmap) }
            } catch (e: Exception) {
                runOnUiThread { statusText.text = "Erro ao gerar QR Code.\nUse o IP: $url" }
            }
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