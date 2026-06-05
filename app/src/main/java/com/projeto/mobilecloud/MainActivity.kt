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

    private lateinit var explorerContainer: GridLayout
    private lateinit var statusText: TextView
    private lateinit var sidebar: LinearLayout
    private var currentPath = File(AppConfig.ROOT_PATH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        startService(Intent(this, NexusService::class.java))
        setupUI()
        startRealtimeMonitor()
    }

    private fun setupUI() {
        val root = RelativeLayout(this).apply { setBackgroundColor(Color.BLACK) }

        // Barra Superior
        val topBar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END
            setPadding(40, 20, 40, 20)
            layoutParams = RelativeLayout.LayoutParams(-1, 80)
        }
        statusText = TextView(this).apply { 
            text = "AGUARDANDO IPHONE..."; setTextColor(Color.DKGRAY); textSize = 10f; typeface = Typeface.DEFAULT_BOLD 
        }
        topBar.addView(statusText)

        // Sidebar de Conexão
        sidebar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40); background = ColorDrawable(Color.parseColor("#080808"))
            layoutParams = RelativeLayout.LayoutParams(480, -1).apply { addRule(RelativeLayout.BELOW, topBar.id) }
        }
        val qrImage = ImageView(this).apply { 
            layoutParams = LinearLayout.LayoutParams(320, 320); setBackgroundColor(Color.WHITE); setPadding(10,10,10,10)
        }
        sidebar.addView(qrImage)

        // Explorador em Grade
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            layoutParams = RelativeLayout.LayoutParams(-1, -1).apply { 
                addRule(RelativeLayout.BELOW, topBar.id)
                addRule(RelativeLayout.RIGHT_OF, sidebar.id)
            }
        }
        explorerContainer = GridLayout(this).apply { columnCount = 4; setPadding(40, 40, 40, 200) }
        scroll.addView(explorerContainer)

        root.addView(topBar); root.addView(sidebar); root.addView(scroll)
        setContentView(root)

        // Carrega QR
        thread {
            try {
                val url = "http://${NetworkManager.getLocalIpAddress()}:${AppConfig.SERVER_PORT}"
                val bitmap = BitmapFactory.decodeStream(URL("https://api.qrserver.com/v1/create-qr-code/?size=320x320&data=${Uri.encode(url)}").openStream())
                runOnUiThread { qrImage.setImageBitmap(bitmap) }
            } catch (e: Exception) { }
        }
    }

    private fun startRealtimeMonitor() {
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                if (ServerState.isClientConnected) {
                    statusText.text = "IPHONE CONECTADO"; statusText.setTextColor(Color.parseColor("#30D158"))
                    sidebar.visibility = View.GONE
                }
                handler.postDelayed(this, 1500)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        refreshExplorer()
    }

    private fun refreshExplorer() {
        explorerContainer.removeAllViews()
        
        // GATILHO DE PERMISSÃO REFORÇADO
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            explorerContainer.addView(createPermissionItem())
            return
        }

        val files = currentPath.listFiles()?.sortedBy { !it.isDirectory }
        if (files == null) {
            explorerContainer.addView(createPermissionItem())
            return
        }

        files.forEach { file ->
            explorerContainer.addView(createFileItem(file))
        }
    }

    private fun createPermissionItem(): LinearLayout {
        return createItemUI("🔒 ATIVAR ACESSO", "Clique para gerenciar arquivos") {
            try {
                // Tenta abrir a tela de gerenciamento de todos os arquivos
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback para a tela de detalhes do app
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }

    private fun createFileItem(file: File): LinearLayout {
        return createItemUI(FileUtils.getFileIcon(file), file.name.take(15)) {
            if (file.isDirectory) { currentPath = file; refreshExplorer() }
            else { FileUtils.openFile(this, file) }
        }
    }

    private fun createItemUI(icon: String, name: String, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            isFocusable = true; isClickable = true; setPadding(20, 20, 20, 20)
            val normal = GradientDrawable().apply { setColor(Color.parseColor("#121214")); cornerRadius = 30f }
            val focused = GradientDrawable().apply { setColor(Color.parseColor("#1C1C1E")); cornerRadius = 30f; setStroke(5, Color.parseColor("#0A84FF")) }
            background = StateListDrawable().apply { addState(intArrayOf(android.R.attr.state_focused), focused); addState(intArrayOf(), normal) }
            
            addView(TextView(context).apply { text = icon; textSize = 35f; gravity = Gravity.CENTER })
            addView(TextView(context).apply { text = name; setTextColor(Color.WHITE); textSize = 12f; gravity = Gravity.CENTER; setPadding(0,10,0,0) })
            
            setOnClickListener { onClick() }
            layoutParams = GridLayout.LayoutParams().apply { width = 280; height = 280; setMargins(15, 15, 15, 15) }
        }
    }

    override fun onBackPressed() {
        if (currentPath.absolutePath != AppConfig.ROOT_PATH) {
            currentPath = currentPath.parentFile ?: File(AppConfig.ROOT_PATH)
            refreshExplorer()
        } else super.onBackPressed()
    }
}