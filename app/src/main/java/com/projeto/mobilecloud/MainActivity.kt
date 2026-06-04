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

    private lateinit var container: LinearLayout
    private var currentPath = File(AppConfig.ROOT_PATH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        setupUI()
        FileServer().start()
        
        // Gatilho de permissão automática
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            requestPerm()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun setupUI() {
        val root = RelativeLayout(this).apply { setBackgroundColor(Color.BLACK) }

        // Sidebar com QR Code
        val sidebar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            background = ColorDrawable(Color.parseColor("#111113"))
            layoutParams = RelativeLayout.LayoutParams(400, -1)
        }

        val qrImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(300, 300)
            setBackgroundColor(Color.WHITE)
        }

        val url = "http://${getLocalIpAddress()}:${AppConfig.SERVER_PORT}"
        val info = TextView(this).apply {
            text = "NEXUS PRO\n$url"
            setTextColor(Color.CYAN); textSize = 14f; gravity = Gravity.CENTER; setPadding(0, 30, 0, 10)
        }

        sidebar.addView(qrImage); sidebar.addView(info)

        // Explorer List
        val scroll = ScrollView(this).apply {
            val params = RelativeLayout.LayoutParams(-1, -1)
            params.addRule(RelativeLayout.RIGHT_OF, sidebar.id)
            layoutParams = params
        }
        container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(30, 30, 30, 100) }
        scroll.addView(container)

        root.addView(sidebar); root.addView(scroll)
        setContentView(root)

        // QR Code Engine
        thread {
            try {
                val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${Uri.encode(url)}"
                val bitmap = BitmapFactory.decodeStream(URL(qrUrl).openStream())
                runOnUiThread { qrImage.setImageBitmap(bitmap) }
            } catch (e: Exception) { Logger.log("Erro no QR Code") }
        }
    }

    private fun refreshList() {
        container.removeAllViews()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            container.addView(createFileButton("⚠️ LIBERAR ACESSO TOTAL", "#FF453A") { requestPerm() })
            return
        }

        if (currentPath.absolutePath != AppConfig.ROOT_PATH) {
            container.addView(createFileButton("⬅️ VOLTAR", "#333333") {
                currentPath = currentPath.parentFile ?: currentPath
                refreshList()
            })
        }

        currentPath.listFiles()?.sortedBy { !it.isDirectory }?.forEach { file ->
            val label = "${FileUtils.getFileIcon(file)} ${file.name.uppercase()}\n${FileUtils.formatSize(file.length())}"
            container.addView(createFileButton(label, "#1C1C1E") {
                if (file.isDirectory) { currentPath = file; refreshList() }
                else { showMenu(file) }
            })
        }
    }

    private fun showMenu(file: File) {
        val options = arrayOf("Deletar", "Renomear")
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(file.name)
            .setItems(options) { _, which ->
                if (which == 1) { /* Renomear Dialog */ }
                else { file.deleteRecursively(); refreshList() }
            }.show()
    }

    private fun createFileButton(txt: String, color: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = txt; isFocusable = true; setTextColor(Color.LTGRAY)
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START; setPadding(40, 25, 40, 25)
            
            val normal = GradientDrawable().apply { setColor(Color.parseColor(color)); cornerRadius = 12f }
            val focused = GradientDrawable().apply { 
                setColor(Color.parseColor(AppConfig.THEME_ACCENT))
                cornerRadius = 12f; setStroke(4, Color.WHITE)
            }
            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_focused), focused)
                addState(intArrayOf(), normal)
            }
            setOnFocusChangeListener { _, hasFocus -> setTextColor(if (hasFocus) Color.WHITE else Color.LTGRAY) }
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 6, 0, 6) }
        }
    }

    private fun requestPerm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val i = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            i.data = Uri.parse("package:$packageName")
            try { startActivity(i) } catch(e: Exception) { startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
        }
    }

    private fun getLocalIpAddress(): String {
        val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
        for (i in interfaces) {
            for (a in i.inetAddresses) {
                if (!a.isLoopbackAddress && a is java.net.Inet4Address) return a.hostAddress ?: "0.0.0.0"
            }
        }
        return "0.0.0.0"
    }
}