package com.projeto.mobilecloud

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
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
        Logger.log("App Iniciado")

        setupUI()
        
        // Inicia Serviços de Fundo
        FileServer().start()
        NetworkDiscovery(this).registerService(AppConfig.SERVER_PORT)
        
        loadQRCode()
        refreshList()
    }

    private fun setupUI() {
        val root = RelativeLayout(this).apply { setBackgroundColor(Color.parseColor(AppConfig.THEME_BG)) }

        // Sidebar de Conexão
        val sidebar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(30, 30, 30, 30)
            background = GradientDrawable().apply { setColor(Color.parseColor("#111113")) }
            val params = RelativeLayout.LayoutParams(450, -1)
            layoutParams = params
        }

        val qrImage = ImageView(this).apply { 
            layoutParams = LinearLayout.LayoutParams(350, 350)
            setBackgroundColor(Color.WHITE)
            setPadding(10, 10, 10, 10)
        }

        val info = TextView(this).apply {
            text = "IP: ${getLocalIpAddress()}\nPorta: ${AppConfig.SERVER_PORT}"
            setTextColor(Color.CYAN); textSize = 16f; setPadding(0, 20, 0, 20); gravity = Gravity.CENTER
        }

        val btnPerm = Button(this).apply {
            text = "LIBERAR ACESSO"
            isFocusable = true
            setOnClickListener { requestPerm() }
            background = GradientDrawable().apply { setColor(Color.parseColor(AppConfig.THEME_ACCENT)); cornerRadius = 10f }
        }

        sidebar.addView(qrImage); sidebar.addView(info); sidebar.addView(btnPerm)

        // Explorer
        val scroll = ScrollView(this).apply {
            val params = RelativeLayout.LayoutParams(-1, -1)
            params.addRule(RelativeLayout.RIGHT_OF, sidebar.id)
            layoutParams = params
        }
        container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 20, 20, 20) }
        scroll.addView(container)

        root.addView(sidebar); root.addView(scroll)
        setContentView(root)
        
        // Atribui a imagem do QR ao sidebar futuramente
        this.findViewById<ImageView>(qrImage.id)?.let { /* placeholder */ }
        
        thread {
            try {
                val url = "http://${getLocalIpAddress()}:${AppConfig.SERVER_PORT}"
                val bitmap = BitmapFactory.decodeStream(URL("https://chart.googleapis.com/chart?chs=350x350&cht=qr&chl=$url").openStream())
                runOnUiThread { qrImage.setImageBitmap(bitmap) }
            } catch (e: Exception) { Logger.log("Erro QR: ${e.message}") }
        }
    }

    private fun refreshList() {
        container.removeAllViews()
        val files = currentPath.listFiles()?.sortedBy { !it.isDirectory } ?: listOf()
        
        if (currentPath.absolutePath != AppConfig.ROOT_PATH) {
            container.addView(createFileButton(".. [VOLTAR]") {
                currentPath = currentPath.parentFile ?: currentPath
                refreshList()
            })
        }

        files.forEach { file ->
            val label = (if (file.isDirectory) "📂 " else "📄 ") + file.name + " (" + FileUtils.formatSize(file.length()) + ")"
            container.addView(createFileButton(label) {
                if (file.isDirectory) { currentPath = file; refreshList() }
            })
        }
    }

    private fun createFileButton(txt: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = txt; isFocusable = true; setTextColor(Color.WHITE); textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            background = GradientDrawable().apply { setColor(Color.parseColor("#1C1C1E")); cornerRadius = 8f }
            setOnClickListener { onClick() }
            val p = LinearLayout.LayoutParams(-1, -2); p.setMargins(0,4,0,4); layoutParams = p
        }
    }

    private fun requestPerm() {
        Logger.log("Solicitando Permissão")
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
    
    private fun loadQRCode() { /* Gerenciado na thread de UI acima */ }
}