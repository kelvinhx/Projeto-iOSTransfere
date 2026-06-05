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

    private lateinit var mainLayout: RelativeLayout
    private lateinit var explorerContainer: GridLayout
    private lateinit var sidebar: LinearLayout
    private var currentPath = File(AppConfig.ROOT_PATH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Inicia Serviço
        val serviceIntent = Intent(this, NexusService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent) else startService(serviceIntent)

        setupUI()
        startConnectionMonitor()
    }

    private fun setupUI() {
        mainLayout = RelativeLayout(this).apply { setBackgroundColor(Color.BLACK) }

        // Sidebar com Design Liquid
        sidebar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0A0A0A"))
                setStroke(1, Color.parseColor("#1C1C1E"))
            }
            layoutParams = RelativeLayout.LayoutParams(450, -1)
        }

        val qrImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(320, 320)
            setBackgroundColor(Color.WHITE); setPadding(10, 10, 10, 10)
        }

        val url = "http://${NetworkManager.getLocalIpAddress()}:${AppConfig.SERVER_PORT}"
        sidebar.addView(qrImage)
        sidebar.addView(TextView(this).apply { 
            text = "SCAN PARA CONECTAR\n$url"
            setTextColor(Color.GRAY); textSize = 12f; gravity = Gravity.CENTER; setPadding(0, 30, 0, 0)
        })

        // Explorer Grade (Apple Style)
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            layoutParams = RelativeLayout.LayoutParams(-1, -1).apply { addRule(RelativeLayout.RIGHT_OF, sidebar.id) }
        }

        explorerContainer = GridLayout(this).apply {
            columnCount = 4
            setPadding(40, 40, 40, 200)
        }
        scroll.addView(explorerContainer)

        mainLayout.addView(sidebar)
        mainLayout.addView(scroll)
        setContentView(mainLayout)

        thread {
            try {
                val bitmap = BitmapFactory.decodeStream(URL("https://api.qrserver.com/v1/create-qr-code/?size=320x320&data=${Uri.encode(url)}").openStream())
                runOnUiThread { qrImage.setImageBitmap(bitmap) }
            } catch (e: Exception) { }
        }
    }

    private fun startConnectionMonitor() {
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                // Se o iPhone conectou, esconde a sidebar (QR Code) para focar nos arquivos
                if (ServerState.isClientConnected) {
                    sidebar.visibility = View.GONE
                    val params = explorerContainer.parent.let { (it as View).layoutParams as RelativeLayout.LayoutParams }
                    params.removeRule(RelativeLayout.RIGHT_OF)
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
            explorerContainer.addView(createGridItem("🔒 ATIVAR ACESSO", "O Android solicita permissão") {
                requestSystemPermission()
            })
            return
        }

        currentPath.listFiles()?.sortedBy { !it.isDirectory }?.forEach { file ->
            val size = if(file.isDirectory) FileUtils.formatSize(FileUtils.getFolderSize(file)) else FileUtils.formatSize(file.length())
            explorerContainer.addView(createGridItem("${FileUtils.getFileIcon(file)}\n${file.name.take(15)}", size) {
                if (file.isDirectory) { currentPath = file; refreshExplorer() }
            })
        }
    }

    private fun createGridItem(name: String, sub: String, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            isFocusable = true; isClickable = true
            setPadding(20, 20, 20, 20)
            
            val normal = GradientDrawable().apply { setColor(Color.parseColor("#121214")); cornerRadius = 25f }
            val focused = GradientDrawable().apply { 
                setColor(Color.parseColor("#1C1C1E")); cornerRadius = 25f; setStroke(4, Color.parseColor("#0A84FF")) 
            }
            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_focused), focused)
                addState(intArrayOf(), normal)
            }

            addView(TextView(context).apply { text = name; setTextColor(Color.WHITE); gravity = Gravity.CENTER; textSize = 14f })
            addView(TextView(context).apply { text = sub; setTextColor(Color.DKGRAY); gravity = Gravity.CENTER; textSize = 10f })
            
            setOnClickListener { onClick() }
            layoutParams = GridLayout.LayoutParams().apply { width = 280; height = 280; setMargins(15, 15, 15, 15) }
        }
    }

    private fun requestSystemPermission() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }

    override fun onBackPressed() {
        if (currentPath.absolutePath != AppConfig.ROOT_PATH) {
            currentPath = currentPath.parentFile ?: File(AppConfig.ROOT_PATH)
            refreshExplorer()
        } else super.onBackPressed()
    }
}