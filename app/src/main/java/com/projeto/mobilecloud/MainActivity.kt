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
        
        // GATILHO: Garante que o serviço inicie primeiro
        startNexusService()
        setupUI()
    }

    private fun startNexusService() {
        val serviceIntent = Intent(this, NexusService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) { Logger.log("Falha ao iniciar serviço") }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun setupUI() {
        val root = RelativeLayout(this).apply { setBackgroundColor(Color.BLACK) }

        // Sidebar ajustada para não esmagar o QR Code
        val sidebar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 50, 50, 50)
            background = ColorDrawable(Color.parseColor("#0F0F12"))
            layoutParams = RelativeLayout.LayoutParams(500, -1) // Aumentado de 420 para 500
        }

        val qrImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(350, 350)
            setBackgroundColor(Color.WHITE)
            setPadding(10, 10, 10, 10)
        }

        val url = "http://${NetworkManager.getLocalIpAddress()}:${AppConfig.SERVER_PORT}"
        val info = TextView(this).apply {
            text = "SCAN PARA CONECTAR\n$url"
            setTextColor(Color.CYAN); textSize = 15f; gravity = Gravity.CENTER
            setPadding(0, 40, 0, 0)
        }

        sidebar.addView(qrImage); sidebar.addView(info)

        // Explorador
        val scroll = ScrollView(this).apply {
            val params = RelativeLayout.LayoutParams(-1, -1)
            params.addRule(RelativeLayout.RIGHT_OF, sidebar.id)
            layoutParams = params
        }
        container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 40, 40, 100) }
        scroll.addView(container)

        root.addView(sidebar); root.addView(scroll)
        setContentView(root)

        // Gerador de QR Code
        thread {
            try {
                val bitmap = BitmapFactory.decodeStream(URL("https://api.qrserver.com/v1/create-qr-code/?size=350x350&data=${Uri.encode(url)}").openStream())
                runOnUiThread { qrImage.setImageBitmap(bitmap) }
            } catch (e: Exception) { }
        }
    }

    private fun refreshList() {
        container.removeAllViews()
        
        // Lógica de Permissão Universal (TCL/Android TV)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            container.addView(createStyledButton("⚠️ CONFIGURAR ACESSO AO DISCO\n(O sistema abrirá uma tela de permissão)") {
                try {
                    // Intent Genérica (Evita o crash em TVs)
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback para configurações do app
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                }
            })
            return
        }

        val files = currentPath.listFiles()?.sortedBy { !it.isDirectory } ?: listOf()
        if (currentPath.absolutePath != AppConfig.ROOT_PATH) {
            container.addView(createStyledButton("⬅️ VOLTAR") {
                currentPath = currentPath.parentFile ?: File(AppConfig.ROOT_PATH)
                refreshList()
            })
        }

        files.forEach { file ->
            val label = "${FileUtils.getFileIcon(file)} ${file.name.uppercase()}\n${FileUtils.formatSize(file.length())}"
            container.addView(createStyledButton(label) {
                if (file.isDirectory) { currentPath = file; refreshList() }
            })
        }
    }

    private fun createStyledButton(txt: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = txt; isFocusable = true; setTextColor(Color.WHITE)
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START; setPadding(50, 40, 50, 40)
            val normal = GradientDrawable().apply { setColor(Color.parseColor("#1C1C1E")); cornerRadius = 15f }
            val focused = GradientDrawable().apply { 
                setColor(Color.parseColor(AppConfig.COLOR_ACCENT)); cornerRadius = 15f; setStroke(5, Color.WHITE) 
            }
            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_focused), focused)
                addState(intArrayOf(), normal)
            }
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 12, 0, 12) }
        }
    }
}