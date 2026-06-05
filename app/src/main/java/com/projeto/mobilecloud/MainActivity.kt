package com.projeto.mobilecloud

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.*
import android.net.Uri
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var explorerContainer: GridLayout
    private lateinit var statusText: TextView
    private lateinit var sidebar: LinearLayout
    private var currentPath = File(AppConfig.ROOT_PATH)
    private var isGridView = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Solicita Notificações no Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        startService(Intent(this, NexusService::class.java))
        setupUI()
        startRealtimeMonitor()
    }

    private fun setupUI() {
        val root = RelativeLayout(this).apply { setBackgroundColor(Color.BLACK) }

        // Barra Superior iOS
        val topBar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(40, 20, 40, 20)
            background = ColorDrawable(Color.parseColor("#0A0A0A"))
            layoutParams = RelativeLayout.LayoutParams(-1, 100).apply { addRule(RelativeLayout.ALIGN_PARENT_TOP) }
        }

        statusText = TextView(this).apply {
            text = "● DESCONECTADO"; setTextColor(Color.GRAY); textSize = 12f; typeface = Typeface.DEFAULT_BOLD
        }
        topBar.addView(statusText)

        // Sidebar de Configurações (Acessível via D-Pad Left)
        sidebar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40); background = ColorDrawable(Color.parseColor("#0D0D0F"))
            layoutParams = RelativeLayout.LayoutParams(400, -1).apply { addRule(RelativeLayout.BELOW, topBar.id) }
        }

        val qrImage = ImageView(this).apply { layoutParams = LinearLayout.LayoutParams(280, 280); setBackgroundColor(Color.WHITE) }
        sidebar.addView(qrImage)
        sidebar.addView(createMenuButton("MUDAR LAYOUT") { isGridView = !isGridView; refreshExplorer() })

        // Explorer
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

        // QR Code Load
        thread {
            try {
                val url = "http://${NetworkManager.getLocalIpAddress()}:${AppConfig.SERVER_PORT}"
                val bitmap = BitmapFactory.decodeStream(URL("https://api.qrserver.com/v1/create-qr-code/?size=280x280&data=${Uri.encode(url)}").openStream())
                runOnUiThread { qrImage.setImageBitmap(bitmap) }
            } catch (e: Exception) { }
        }
    }

    private fun startRealtimeMonitor() {
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                statusText.text = if (ServerState.isClientConnected) "● IPHONE CONECTADO" else "● AGUARDANDO CONEXÃO"
                statusText.setTextColor(if (ServerState.isClientConnected) Color.parseColor("#30D158") else Color.GRAY)
                if (ServerState.isClientConnected) sidebar.visibility = View.GONE else sidebar.visibility = View.VISIBLE
                handler.postDelayed(this, 1000)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        refreshExplorer()
    }

    private fun refreshExplorer() {
        explorerContainer.removeAllViews()
        explorerContainer.columnCount = if (isGridView) 4 else 1

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            requestSystemPermission()
            return
        }

        currentPath.listFiles()?.sortedBy { !it.isDirectory }?.forEach { file ->
            explorerContainer.addView(createGridItem(file))
        }
    }

    private fun createGridItem(file: File): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            isFocusable = true; isClickable = true; setPadding(20, 20, 20, 20)
            
            val normal = GradientDrawable().apply { setColor(Color.parseColor("#121214")); cornerRadius = 30f }
            val focused = GradientDrawable().apply { 
                setColor(Color.parseColor("#1C1C1E")); cornerRadius = 30f; setStroke(5, Color.parseColor("#0A84FF")) 
            }
            background = StateListDrawable().apply { addState(intArrayOf(android.R.attr.state_focused), focused); addState(intArrayOf(), normal) }

            // Ícone Grande
            val icon = TextView(context).apply { text = FileUtils.getFileIcon(file); textSize = 45f; gravity = Gravity.CENTER }
            val name = TextView(context).apply { 
                text = file.name.take(15); setTextColor(Color.WHITE); textSize = 14f
                gravity = Gravity.CENTER; setPadding(0, 10, 0, 0)
            }

            addView(icon); addView(name)

            setOnClickListener { 
                if (file.isDirectory) { currentPath = file; refreshExplorer() } 
                else { FileUtils.openFile(context, file) }
            }
            
            setOnLongClickListener { 
                showContextDialog(file)
                true
            }

            layoutParams = GridLayout.LayoutParams().apply { 
                width = if(isGridView) 280 else -1
                height = 280; setMargins(15, 15, 15, 15) 
            }
        }
    }

    private fun showContextDialog(file: File) {
        val options = arrayOf("Recortar ✂️", "Renomear ✏️", "Apagar 🗑️")
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(file.name)
            .setItems(options) { _, which ->
                if (which == 2) { file.deleteRecursively(); refreshExplorer() }
            }.show()
    }

    private fun createMenuButton(txt: String, onClick: () -> Unit) = Button(this).apply {
        text = txt; setTextColor(Color.WHITE); background = ColorDrawable(Color.DKGRAY)
        setOnClickListener { onClick() }
    }

    private fun requestSystemPermission() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        }
    }

    override fun onBackPressed() {
        if (currentPath.absolutePath != AppConfig.ROOT_PATH) {
            currentPath = currentPath.parentFile ?: File(AppConfig.ROOT_PATH)
            refreshExplorer()
        } else super.onBackPressed()
    }
}