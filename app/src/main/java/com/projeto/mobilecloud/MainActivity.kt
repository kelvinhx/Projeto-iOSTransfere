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

    private lateinit var gridContainer: GridLayout
    private var currentPath = File(AppConfig.ROOT_PATH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        val serviceIntent = Intent(this, NexusService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        setupUI()
    }

    override fun onBackPressed() {
        if (currentPath.absolutePath != AppConfig.ROOT_PATH) {
            currentPath = currentPath.parentFile ?: File(AppConfig.ROOT_PATH)
            refreshGrid()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshGrid()
    }

    private fun setupUI() {
        val root = RelativeLayout(this).apply { setBackgroundColor(Color.BLACK) }

        // Sidebar Premium
        val sidebar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            background = ColorDrawable(Color.parseColor("#0A0A0C"))
            layoutParams = RelativeLayout.LayoutParams(450, -1)
        }

        val qrImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(300, 300)
            setBackgroundColor(Color.WHITE); setPadding(8, 8, 8, 8)
        }

        val url = "http://${NetworkManager.getLocalIpAddress()}:${AppConfig.SERVER_PORT}"
        val info = TextView(this).apply {
            text = "NEXUS PRO\n$url"
            setTextColor(Color.WHITE); textSize = 14f; gravity = Gravity.CENTER; setPadding(0, 30, 0, 0)
        }

        sidebar.addView(qrImage); sidebar.addView(info)

        // Explorador em GRADE (Grid)
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            val params = RelativeLayout.LayoutParams(-1, -1)
            params.addRule(RelativeLayout.RIGHT_OF, sidebar.id)
            layoutParams = params
        }

        gridContainer = GridLayout(this).apply {
            columnCount = 3 // 3 colunas para ficar elegante na TV
            setPadding(30, 30, 30, 150)
        }
        scroll.addView(gridContainer)

        root.addView(sidebar); root.addView(scroll)
        setContentView(root)

        // Carrega QR
        thread {
            try {
                val bitmap = BitmapFactory.decodeStream(URL("https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${Uri.encode(url)}").openStream())
                runOnUiThread { qrImage.setImageBitmap(bitmap) }
            } catch (e: Exception) { }
        }
    }

    private fun refreshGrid() {
        gridContainer.removeAllViews()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            gridContainer.addView(createGridItem("⚠️ ATIVAR ACESSO", "Clique para permitir") {
                val i = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                i.data = Uri.parse("package:$packageName")
                startActivity(i)
            })
            return
        }

        val files = currentPath.listFiles()?.sortedBy { !it.isDirectory } ?: listOf()
        
        files.forEach { file ->
            val size = if(file.isDirectory) FileUtils.formatSize(FileUtils.getFolderSize(file)) else FileUtils.formatSize(file.length())
            val label = "${FileUtils.getFileIcon(file)}\n${file.name.take(15)}"
            
            gridContainer.addView(createGridItem(label, size) {
                if (file.isDirectory) {
                    currentPath = file
                    refreshGrid()
                } else {
                    showActionMenu(file)
                }
            })
        }
    }

    private fun createGridItem(name: String, sub: String, onClick: () -> Unit): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            isFocusable = true; isClickable = true
            setPadding(20, 20, 20, 20)
            
            val normal = GradientDrawable().apply { setColor(Color.parseColor("#161618")); cornerRadius = 20f }
            val focused = GradientDrawable().apply { 
                setColor(Color.parseColor("#0A84FF")); cornerRadius = 20f; setStroke(5, Color.WHITE) 
            }
            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_focused), focused)
                addState(intArrayOf(), normal)
            }
            setOnClickListener { onClick() }
        }

        layout.addView(TextView(this).apply { text = name; setTextColor(Color.WHITE); gravity = Gravity.CENTER; textSize = 16f })
        layout.addView(TextView(this).apply { text = sub; setTextColor(Color.GRAY); gravity = Gravity.CENTER; textSize = 11f })
        
        layout.layoutParams = GridLayout.LayoutParams().apply {
            width = 300; height = 300; setMargins(15, 15, 15, 15)
        }
        return layout
    }

    private fun showActionMenu(file: File) {
        val options = arrayOf("Abrir na TV ▶️", "Excluir 🗑️")
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(file.name)
            .setItems(options) { _, which ->
                if (which == 1) { file.deleteRecursively(); refreshGrid() }
            }.show()
    }
}