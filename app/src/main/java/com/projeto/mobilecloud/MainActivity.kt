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
        
        setupUI()
        
        // Inicia Serviços
        FileServer().start()
        NetworkDiscovery(this).registerService(AppConfig.SERVER_PORT)
        
        // Verifica permissão automaticamente
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Logger.log("Permissão pendente")
        }

        refreshList()
    }

    private fun setupUI() {
        val root = RelativeLayout(this).apply { setBackgroundColor(Color.BLACK) }

        // Sidebar
        val sidebar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            background = ColorDrawable(Color.parseColor("#111113"))
            layoutParams = RelativeLayout.LayoutParams(420, -1)
        }

        val qrImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(320, 320)
            setBackgroundColor(Color.WHITE)
            setPadding(10, 10, 10, 10)
        }

        val url = "http://${getLocalIpAddress()}:${AppConfig.SERVER_PORT}"
        val info = TextView(this).apply {
            text = "NEXUS PRO\n$url"
            setTextColor(Color.CYAN); textSize = 16f; gravity = Gravity.CENTER; setPadding(0, 30, 0, 30)
        }

        val btnPerm = createStyledButton("LIBERAR ACESSO") { requestPerm() }

        sidebar.addView(qrImage); sidebar.addView(info); sidebar.addView(btnPerm)

        // Explorer List
        val scroll = ScrollView(this).apply {
            val params = RelativeLayout.LayoutParams(-1, -1)
            params.addRule(RelativeLayout.RIGHT_OF, sidebar.id)
            layoutParams = params
        }
        container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(30, 30, 30, 30) }
        scroll.addView(container)

        root.addView(sidebar); root.addView(scroll)
        setContentView(root)

        // Carrega QR Code
        thread {
            try {
                val bitmap = BitmapFactory.decodeStream(URL("https://chart.googleapis.com/chart?chs=320x320&cht=qr&chl=$url").openStream())
                runOnUiThread { qrImage.setImageBitmap(bitmap) }
            } catch (e: Exception) { Logger.log("Falha QR: Sem Internet") }
        }
    }

    private fun refreshList() {
        container.removeAllViews()
        val files = currentPath.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: listOf()
        
        if (currentPath.absolutePath != AppConfig.ROOT_PATH) {
            container.addView(createStyledButton("⬅️ VOLTAR") {
                currentPath = currentPath.parentFile ?: currentPath
                refreshList()
            })
        }

        files.forEach { file ->
            val icon = if (file.isDirectory) "📂" else "📄"
            val label = "$icon ${file.name.uppercase()}\n${FileUtils.formatSize(file.length())}"
            container.addView(createStyledButton(label) {
                if (file.isDirectory) { currentPath = file; refreshList() }
            })
        }
    }

    private fun createStyledButton(txt: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = txt; isFocusable = true; setTextColor(Color.LTGRAY)
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START; setPadding(40, 30, 40, 30)
            
            // Selector de Foco para TV
            val normal = GradientDrawable().apply { setColor(Color.parseColor("#1C1C1E")); cornerRadius = 12f }
            val focused = GradientDrawable().apply { 
                setColor(Color.parseColor(AppConfig.THEME_ACCENT))
                cornerRadius = 12f
                setStroke(4, Color.WHITE)
            }
            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_focused), focused)
                addState(intArrayOf(), normal)
            }

            setOnFocusChangeListener { _, hasFocus ->
                setTextColor(if (hasFocus) Color.WHITE else Color.LTGRAY)
            }
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 10, 0, 10) }
        }
    }

    private fun requestPerm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val i = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                i.data = Uri.parse("package:$packageName")
                startActivity(i)
            } catch (e: Exception) { startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
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