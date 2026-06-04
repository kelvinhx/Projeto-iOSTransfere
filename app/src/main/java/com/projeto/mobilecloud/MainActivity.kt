package com.projeto.mobilecloud

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.*
import android.net.Uri
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private var currentPath = File(AppConfig.ROOT_PATH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Gatilho Automático de Permissão
        checkPermissionsAndStart()
        
        setupUI()
        FileServer().start()
        refreshList()
    }

    private fun checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestPerm()
            }
        }
    }

    private fun setupUI() {
        val root = RelativeLayout(this).apply { setBackgroundColor(Color.BLACK) }

        // Sidebar Fixa
        val sidebar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            background = ColorDrawable(Color.parseColor("#121212"))
            layoutParams = RelativeLayout.LayoutParams(400, -1)
        }

        val qrImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(300, 300)
            setBackgroundColor(Color.WHITE)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        val url = "http://${getLocalIpAddress()}:${AppConfig.SERVER_PORT}"
        val info = TextView(this).apply {
            text = "NEXUS PRO\n$url"
            setTextColor(Color.WHITE); textSize = 14f; gravity = Gravity.CENTER; setPadding(0,20,0,20)
        }

        sidebar.addView(qrImage); sidebar.addView(info)

        // Explorer
        val scroll = ScrollView(this).apply {
            val params = RelativeLayout.LayoutParams(-1, -1)
            params.addRule(RelativeLayout.RIGHT_OF, sidebar.id)
            layoutParams = params
        }
        container = LinearLayout(this).apply { 
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30) 
        }
        scroll.addView(container)

        root.addView(sidebar); root.addView(scroll)
        setContentView(root)

        // QR Code Loader
        thread {
            try {
                val bitmap = BitmapFactory.decodeStream(URL("https://chart.googleapis.com/chart?chs=300x300&cht=qr&chl=$url").openStream())
                runOnUiThread { qrImage.setImageBitmap(bitmap) }
            } catch (e: Exception) { 
                Logger.log("Erro QR: Tente abrir $url manualmente")
            }
        }
    }

    private fun refreshList() {
        container.removeAllViews()
        val files = currentPath.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: listOf()
        
        if (currentPath.absolutePath != AppConfig.ROOT_PATH) {
            container.addView(createFileButton("⬅️ .. [VOLTAR PARA ANTERIOR]") {
                currentPath = currentPath.parentFile ?: currentPath
                refreshList()
            })
        }

        files.forEach { file ->
            val icon = if (file.isDirectory) "📂" else "📄"
            container.addView(createFileButton("$icon ${file.name.uppercase()}") {
                if (file.isDirectory) {
                    currentPath = file
                    refreshList()
                }
            })
        }
    }

    private fun createFileButton(txt: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = txt
            isFocusable = true
            setTextColor(Color.LTGRAY)
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            setPadding(40, 30, 40, 30)
            textSize = 16f
            
            // EFEITO DE FOCO (D-PAD)
            val normal = GradientDrawable().apply { setColor(Color.parseColor("#1C1C1E")); cornerRadius = 12f }
            val focused = GradientDrawable().apply { 
                setColor(Color.parseColor(AppConfig.THEME_ACCENT)) 
                cornerRadius = 12f
                setStroke(4, Color.WHITE)
            }
            
            val states = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_focused), focused)
                addState(intArrayOf(), normal)
            }
            
            background = states
            
            setOnFocusChangeListener { _, hasFocus ->
                setTextColor(if (hasFocus) Color.WHITE else Color.LTGRAY)
                scaleX = if (hasFocus) 1.02f else 1.0f
                scaleY = if (hasFocus) 1.02f else 1.0f
            }

            setOnClickListener { onClick() }
            
            val p = LinearLayout.LayoutParams(-1, -2)
            p.setMargins(0, 8, 0, 8)
            layoutParams = p
        }
    }

    private fun requestPerm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val i = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                i.data = Uri.parse("package:$packageName")
                startActivity(i)
            } catch (e: Exception) {
                startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
    }

    private fun getLocalIpAddress(): String {
        val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
        for (i in interfaces) {
            for (a in i.inetAddresses) {
                if (!a.isLoopbackAddress && a is java.net.Inet4Address) return a.hostAddress ?: ""
            }
        }
        return "0.0.0.0"
    }
}