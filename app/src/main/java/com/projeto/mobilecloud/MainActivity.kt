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
        
        startService(Intent(this, NexusService::class.java))
        setupUI()
        startRealtimeMonitor()
    }

    private fun setupUI() {
        val root = RelativeLayout(this).apply { setBackgroundColor(Color.BLACK) }

        // Barra de Status Discreta
        val topBar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END
            setPadding(40, 20, 40, 20)
            layoutParams = RelativeLayout.LayoutParams(-1, 80).apply { addRule(RelativeLayout.ALIGN_PARENT_TOP) }
        }
        statusText = TextView(this).apply { text = "● STANDBY"; setTextColor(Color.DKGRAY); textSize = 10f; typeface = Typeface.DEFAULT_BOLD }
        topBar.addView(statusText)

        // Sidebar de Conexão
        sidebar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40); background = ColorDrawable(Color.parseColor("#080808"))
            layoutParams = RelativeLayout.LayoutParams(450, -1).apply { addRule(RelativeLayout.BELOW, topBar.id) }
        }
        val qrImage = ImageView(this).apply { layoutParams = LinearLayout.LayoutParams(300, 300); setBackgroundColor(Color.WHITE) }
        sidebar.addView(qrImage)

        // Explorador Premium
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

        // QR Engine
        thread {
            try {
                val url = "http://${NetworkManager.getLocalIpAddress()}:${AppConfig.SERVER_PORT}"
                val bitmap = BitmapFactory.decodeStream(URL("https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${Uri.encode(url)}").openStream())
                runOnUiThread { qrImage.setImageBitmap(bitmap) }
            } catch (e: Exception) { }
        }
    }

    private fun startRealtimeMonitor() {
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                if (ServerState.isClientConnected) {
                    statusText.text = "● IPHONE CONECTADO"; statusText.setTextColor(Color.parseColor("#30D158"))
                    sidebar.visibility = View.GONE
                }
                handler.postDelayed(this, 2000)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        refreshExplorer()
    }

    private fun refreshExplorer() {
        explorerContainer.removeAllViews()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            explorerContainer.addView(createItem("🔒 ATIVAR ACESSO", "O Android solicita permissão") {
                try { startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) } catch(e: Exception) {}
            })
            return
        }

        currentPath.listFiles()?.sortedBy { !it.isDirectory }?.forEach { file ->
            explorerContainer.addView(createItem("${FileUtils.getFileIcon(file)}\n${file.name.take(15)}", FileUtils.formatSize(file.length())) {
                if (file.isDirectory) { currentPath = file; refreshExplorer() }
                else { FileUtils.openFile(this, file) }
            }.apply {
                setOnLongClickListener { showMenu(file); true }
            })
        }
    }

    private fun showMenu(file: File) {
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(file.name)
            .setItems(arrayOf("Apagar 🗑️", "Renomear ✏️")) { _, i ->
                if (i == 0) { file.deleteRecursively(); refreshExplorer() }
            }.show()
    }

    private fun createItem(txt: String, sub: String, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; isFocusable = true; isClickable = true
            setPadding(20, 20, 20, 20)
            val normal = GradientDrawable().apply { setColor(Color.parseColor("#121214")); cornerRadius = 30f }
            val focused = GradientDrawable().apply { setColor(Color.parseColor("#1C1C1E")); cornerRadius = 30f; setStroke(5, Color.parseColor("#0A84FF")) }
            background = StateListDrawable().apply { addState(intArrayOf(android.R.attr.state_focused), focused); addState(intArrayOf(), normal) }
            addView(TextView(context).apply { text = txt; setTextColor(Color.WHITE); gravity = Gravity.CENTER; textSize = 14f })
            addView(TextView(context).apply { text = sub; setTextColor(Color.DKGRAY); gravity = Gravity.CENTER; textSize = 10f })
            setOnClickListener { onClick() }
            layoutParams = GridLayout.LayoutParams().apply { width = 280; height = 280; setMargins(12, 12, 12, 12) }
        }
    }

    override fun onBackPressed() {
        if (currentPath.absolutePath != AppConfig.ROOT_PATH) {
            currentPath = currentPath.parentFile ?: File(AppConfig.ROOT_PATH)
            refreshExplorer()
        } else super.onBackPressed()
    }
}