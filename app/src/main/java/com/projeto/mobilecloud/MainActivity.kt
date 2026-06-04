package com.projeto.mobilecloud

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var fileListContainer: LinearLayout
    private var currentPath = File("/storage/emulated/0")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (!checkStoragePermission()) {
            showPermissionScreen()
        } else {
            showMainUI()
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else true
    }

    private fun showPermissionScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
        }
        val txt = TextView(this).apply {
            text = "ACESSO AOS ARQUIVOS NECESSÁRIO\nNexus Explorer precisa de permissão total."
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            textSize = 20f
            setPadding(0, 0, 0, 40)
        }
        val btn = Button(this).apply {
            text = "CONCEDER AGORA"
            isFocusable = true
            requestFocus()
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            }
        }
        root.addView(txt); root.addView(btn)
        setContentView(root)
    }

    private fun showMainUI() {
        val root = RelativeLayout(this).apply { setBackgroundColor(Color.BLACK) }

        // Header
        val header = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(40, 20, 40, 20)
            setBackgroundColor(Color.parseColor("#121212"))
        }
        
        val title = TextView(this).apply {
            text = "NEXUS PRO"
            setTextColor(Color.WHITE)
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
        }
        
        val qrImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(150, 150).apply { leftMargin = 40 }
            setBackgroundColor(Color.WHITE)
        }

        header.addView(title); header.addView(qrImage)

        // File Explorer List
        val scroll = ScrollView(this).apply {
            val params = RelativeLayout.LayoutParams(-1, -1)
            params.addRule(RelativeLayout.BELOW, header.id)
            layoutParams = params
        }
        
        fileListContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 20) }
        scroll.addView(fileListContainer)

        root.addView(header); root.addView(scroll)
        setContentView(root)

        // Load Files and Start Server
        val ip = getLocalIpAddress()
        val url = "http://$ip:8080"
        
        thread {
            try {
                val bitmap = BitmapFactory.decodeStream(URL("https://chart.googleapis.com/chart?chs=150x150&cht=qr&chl=$url").openStream())
                runOnUiThread { qrImage.setImageBitmap(bitmap) }
            } catch (e: Exception) {}
        }

        FileServer().start()
        refreshFileList()
    }

    private fun refreshFileList() {
        fileListContainer.removeAllViews()
        
        // Botão Voltar
        if (currentPath.absolutePath != "/storage/emulated/0") {
            fileListContainer.addView(createFileItem(".. [Voltar]", true) {
                currentPath = currentPath.parentFile ?: currentPath
                refreshFileList()
            })
        }

        val files = currentPath.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: listOf()
        files.forEach { file ->
            fileListContainer.addView(createFileItem(file.name, file.isDirectory) {
                if (file.isDirectory) {
                    currentPath = file
                    refreshFileList()
                }
            })
        }
    }

    private fun createFileItem(name: String, isDir: Boolean, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = (if (isDir) "📂 " else "📄 ") + name
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            isFocusable = true
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply { setColor(Color.parseColor("#1C1C1E")); cornerRadius = 10f }
            val params = LinearLayout.LayoutParams(-1, -2)
            params.setMargins(0, 5, 0, 5)
            layoutParams = params
            setOnClickListener { onClick() }
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