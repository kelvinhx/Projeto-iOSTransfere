package com.projeto.mobilecloud

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.net.Uri
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
            setBackgroundColor(Color.parseColor("#08080A"))
        }

        // Card Centralizado
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#121214"))
                cornerRadius = 30f
                setStroke(2, Color.DKGRAY)
            }
            layoutParams = LinearLayout.LayoutParams(900, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val qrImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(400, 400)
            setBackgroundColor(Color.WHITE)
        }

        val ip = getLocalIpAddress()
        val url = "http://$ip:8080"

        val infoText = TextView(this).apply {
            text = "NEXUS PRO EXPLORER\n$url"
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 30)
        }

        val btnAction = Button(this).apply {
            text = "LIBERAR ACESSO AOS ARQUIVOS"
            isFocusable = true
            requestFocus()
            setTextColor(Color.WHITE)
            setPadding(40, 20, 40, 20)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#007AFF"))
                cornerRadius = 15f
            }
            setOnClickListener { requestSmartPermission() }
        }

        card.addView(qrImage); card.addView(infoText); card.addView(btnAction)
        root.addView(card)
        setContentView(root)

        // Inicia Servidor e QR Code
        FileServer().start()
        thread {
            try {
                val bitmap = BitmapFactory.decodeStream(URL("https://chart.googleapis.com/chart?chs=400x400&cht=qr&chl=$url").openStream())
                runOnUiThread { qrImage.setImageBitmap(bitmap) }
            } catch (e: Exception) {}
        }
    }

    private fun requestSmartPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                // Tenta abrir direto na permissão do App
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    // Se falhar, abre a lista geral de todos os arquivos
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                } catch (e2: Exception) {
                    // Se tudo falhar, abre as configurações gerais
                    startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                    Toast.makeText(this, "Vá em Apps > Nexus > Permissões", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            // Android 10 ou inferior
            requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
        }
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