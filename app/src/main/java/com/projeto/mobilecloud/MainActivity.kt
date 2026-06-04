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

    private lateinit var progressCard: LinearLayout
    private lateinit var mainCard: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var fileText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }

        // 1. Card Principal (QR Code e Link)
        mainCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#121214"))
                cornerRadius = 40f
            }
            val params = FrameLayout.LayoutParams(900, -2, Gravity.CENTER)
            layoutParams = params
        }

        val qrImage = ImageView(this).apply { layoutParams = LinearLayout.LayoutParams(400, 400); setBackgroundColor(Color.WHITE) }
        val ipText = TextView(this).apply { 
            text = "Acesse: http://${getLocalIpAddress()}:8080"
            setTextColor(Color.WHITE); textSize = 20f; setPadding(0, 30, 0, 30) 
        }
        val btnPerm = Button(this).apply {
            text = "CONFIGURAR ACESSO TOTAL"
            setOnClickListener { requestSmartPermission() }
        }
        mainCard.addView(qrImage); mainCard.addView(ipText); mainCard.addView(btnPerm)

        // 2. Card de Progresso (Invisível no Início)
        progressCard = LinearLayout(this).apply {
            visibility = View.GONE
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(80, 80, 80, 80)
            background = GradientDrawable().apply { setColor(Color.parseColor("#1A1A1C")); cornerRadius = 40f }
            val params = FrameLayout.LayoutParams(1000, -2, Gravity.CENTER)
            layoutParams = params
        }

        fileText = TextView(this).apply { text = "Recebendo..."; setTextColor(Color.WHITE); textSize = 24f }
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 40).apply { setMargins(0, 40, 0, 20) }
            progressDrawable = GradientDrawable().apply { setColor(Color.parseColor("#007AFF")); cornerRadius = 20f }
        }
        progressText = TextView(this).apply { text = "0%"; setTextColor(Color.CYAN); textSize = 30f; setTypeface(null, Typeface.BOLD) }
        
        progressCard.addView(fileText); progressCard.addView(progressBar); progressCard.addView(progressText)

        root.addView(mainCard); root.addView(progressCard)
        setContentView(root)

        FileServer().start()
        startStatusMonitor()
        
        // Carrega QR
        thread {
            try {
                val url = "http://${getLocalIpAddress()}:8080"
                val bitmap = BitmapFactory.decodeStream(URL("https://chart.googleapis.com/chart?chs=400x400&cht=qr&chl=$url").openStream())
                runOnUiThread { qrImage.setImageBitmap(bitmap) }
            } catch (e: Exception) {}
        }
    }

    private fun startStatusMonitor() {
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                if (TransferState.isUploading) {
                    mainCard.visibility = View.GONE
                    progressCard.visibility = View.VISIBLE
                    fileText.text = "Recebendo: ${TransferState.fileName}"
                    progressBar.progress = TransferState.progress
                    progressText.text = "${TransferState.progress}%"
                } else {
                    if (progressCard.visibility == View.VISIBLE) {
                        Toast.makeText(this@MainActivity, "✅ ${TransferState.fileName} recebido!", Toast.LENGTH_SHORT).show()
                    }
                    mainCard.visibility = View.VISIBLE
                    progressCard.visibility = View.GONE
                }
                handler.postDelayed(this, 500) // Monitora a cada meio segundo
            }
        })
    }

    private fun requestSmartPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            try { startActivity(intent) } catch (e: Exception) { startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
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