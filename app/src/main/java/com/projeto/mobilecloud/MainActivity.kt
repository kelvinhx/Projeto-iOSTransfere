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
    private lateinit var sidebar: LinearLayout
    private var currentPath = File(AppConfig.ROOT_PATH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        startService(Intent(this, NexusService::class.java))
        setupUI()
        startConnectionMonitor()
    }

    private fun setupUI() {
        val root = RelativeLayout(this).apply { setBackgroundColor(Color.BLACK) }

        // Sidebar Premium Dark
        sidebar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            background = ColorDrawable(Color.parseColor("#0A0A0A"))
            layoutParams = RelativeLayout.LayoutParams(450, -1)
        }

        val qrImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(300, 300)
            setBackgroundColor(Color.WHITE); setPadding(10, 10, 10, 10)
        }

        val url = "http://${NetworkManager.getLocalIpAddress()}:${AppConfig.SERVER_PORT}"
        sidebar.addView(qrImage)
        sidebar.addView(TextView(this).apply { 
            text = "SCAN IPHONE\n$url"; setTextColor(Color.DKGRAY); textSize = 11f
            gravity = Gravity.CENTER; setPadding(0, 30, 0, 0)
        })

        // Explorador em GRADE (Estilo Apple)
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            layoutParams = RelativeLayout.LayoutParams(-1, -1).apply { addRule(RelativeLayout.RIGHT_OF, sidebar.id) }
        }

        explorerContainer = GridLayout(this).apply {
            columnCount = 4
            setPadding(40, 40, 40, 200)
        }
        scroll.addView(explorerContainer)

        root.addView(sidebar); root.addView(scroll)
        setContentView(root)

        thread {
            try {
                val bitmap = BitmapFactory.decodeStream(URL("https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${Uri.encode(url)}").openStream())
                runOnUiThread { qrImage.setImageBitmap(bitmap) }
            } catch (e: Exception) { }
        }
    }

    private fun startConnectionMonitor() {
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                if (ServerState.isClientConnected) {
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
            explorerContainer.addView(createGridItem("🔒 ACESSO", "Clique aqui") {
                startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            })
            return
        }

        val files = currentPath.listFiles()?.sortedBy { !it.isDirectory } ?: listOf()
        files.forEach { file ->
            val size = if(file.isDirectory) FileUtils.formatSize(FileUtils.getFolderSize(file)) else FileUtils.formatSize(file.length())
            explorerContainer.addView(createGridItem("${FileUtils.getFileIcon(file)}\n${file.name.take(15)}", size) {
                if (file.isDirectory) { currentPath = file; refreshExplorer() }
            })
        }
    }

    private fun createGridItem(name: String, sub: String, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            isFocusable = true; isClickable = true; setPadding(20, 20, 20, 20)
            val normal = GradientDrawable().apply { setColor(Color.parseColor("#121214")); cornerRadius = 25f }
            val focused = GradientDrawable().apply { setColor(Color.parseColor("#1C1C1E")); cornerRadius = 25f; setStroke(4, Color.parseColor("#0A84FF")) }
            background = StateListDrawable().apply { addState(intArrayOf(android.R.attr.state_focused), focused); addState(intArrayOf(), normal) }
            
            addView(TextView(context).apply { text = name; setTextColor(Color.WHITE); gravity = Gravity.CENTER; textSize = 14f })
            addView(TextView(context).apply { text = sub; setTextColor(Color.GRAY); gravity = Gravity.CENTER; textSize = 10f })
            setOnClickListener { onClick() }
            layoutParams = GridLayout.LayoutParams().apply { width = 260; height = 260; setMargins(12, 12, 12, 12) }
        }
    }

    override fun onBackPressed() {
        if (currentPath.absolutePath != AppConfig.ROOT_PATH) {
            currentPath = currentPath.parentFile ?: File(AppConfig.ROOT_PATH)
            refreshExplorer()
        } else super.onBackPressed()
    }
}