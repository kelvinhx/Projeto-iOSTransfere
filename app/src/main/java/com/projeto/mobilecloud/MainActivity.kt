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
        startService(Intent(this, NexusService::class.java))
        setupUI()
    }

    private fun setupUI() {
        val root = RelativeLayout(this).apply { setBackgroundColor(Color.BLACK) }

        // Sidebar Transparente
        val sidebar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(30, 30, 30, 30)
            background = ColorDrawable(Color.parseColor("#121214"))
            layoutParams = RelativeLayout.LayoutParams(400, -1)
        }

        val qrImage = ImageView(this).apply { layoutParams = LinearLayout.LayoutParams(280, 280); setBackgroundColor(Color.WHITE) }
        val info = TextView(this).apply { text = "CONECTAR IPHONE\n8080"; setTextColor(Color.GRAY); gravity = Gravity.CENTER; setPadding(0,20,0,20) }
        
        sidebar.addView(qrImage); sidebar.addView(info)

        // Explorador em Grade (GRID)
        val scroll = ScrollView(this).apply {
            layoutParams = RelativeLayout.LayoutParams(-1, -1).apply { addRule(RelativeLayout.RIGHT_OF, sidebar.id) }
            isFillViewport = true
        }

        gridContainer = GridLayout(this).apply {
            columnCount = 4
            alignmentMode = GridLayout.ALIGN_BOUNDS
            setPadding(30, 30, 30, 30)
        }
        scroll.addView(gridContainer)

        root.addView(sidebar); root.addView(scroll)
        setContentView(root)

        // QR Code Load
        thread {
            try {
                val url = "http://${NetworkManager.getLocalIpAddress()}:8080"
                val bitmap = BitmapFactory.decodeStream(URL("https://api.qrserver.com/v1/create-qr-code/?size=280x280&data=${Uri.encode(url)}").openStream())
                runOnUiThread { qrImage.setImageBitmap(bitmap) }
            } catch (e: Exception) { }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshGrid()
    }

    private fun refreshGrid() {
        gridContainer.removeAllViews()
        
        // Verifica Permissão Especial
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val btn = createGridItem("🔒 ATIVAR ACESSO", "") {
                startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
            gridContainer.addView(btn)
            return
        }

        val files = currentPath.listFiles()?.sortedBy { !it.isDirectory } ?: listOf()
        
        files.forEach { file ->
            val size = if(file.isDirectory) FileUtils.formatSize(FileUtils.getFolderSize(file)) else FileUtils.formatSize(file.length())
            val view = createGridItem("${FileUtils.getFileIcon(file)}\n${file.name.take(12)}", size) {
                if (file.isDirectory) { currentPath = file; refreshGrid() }
                else { showActionMenu(file) }
            }
            gridContainer.addView(view)
        }
    }

    private fun createGridItem(name: String, sub: String, onClick: () -> Unit): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            isFocusable = true; isClickable = true
            setPadding(20, 20, 20, 20)
            
            val normal = GradientDrawable().apply { setColor(Color.parseColor("#1C1C1E")); cornerRadius = 20f }
            val focused = GradientDrawable().apply { setColor(Color.parseColor(AppConfig.COLOR_ACCENT)); cornerRadius = 20f; setStroke(4, Color.WHITE) }
            
            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_focused), focused)
                addState(intArrayOf(), normal)
            }
            
            setOnClickListener { onClick() }
        }

        val txt = TextView(this).apply { text = name; setTextColor(Color.WHITE); gravity = Gravity.CENTER; textSize = 14f }
        val stxt = TextView(this).apply { text = sub; setTextColor(Color.GRAY); textSize = 10f; gravity = Gravity.CENTER }
        
        layout.addView(txt); layout.addView(stxt)
        
        layout.layoutParams = GridLayout.LayoutParams().apply {
            width = 250; height = 250; setMargins(15, 15, 15, 15)
        }
        
        return layout
    }

    private fun showActionMenu(file: File) {
        val options = arrayOf("Abrir ▶️", "Excluir 🗑️", "Renomear ✏️")
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(file.name)
            .setItems(options) { _, which ->
                when(which) {
                    1 -> { file.deleteRecursively(); refreshGrid() }
                }
            }.show()
    }

    override fun onBackPressed() {
        if (currentPath.absolutePath != AppConfig.ROOT_PATH) {
            currentPath = currentPath.parentFile ?: File(AppConfig.ROOT_PATH)
            refreshGrid()
        } else super.onBackPressed()
    }
}