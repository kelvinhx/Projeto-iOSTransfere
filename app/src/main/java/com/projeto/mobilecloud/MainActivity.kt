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
        
        // Inicia o Serviço
        val serviceIntent = Intent(this, NexusService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        setupUI()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun setupUI() {
        val root = RelativeLayout(this).apply { setBackgroundColor(Color.BLACK) }

        val sidebar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40); background = ColorDrawable(Color.parseColor("#0A0A0C"))
            layoutParams = RelativeLayout.LayoutParams(420, -1)
        }

        val qrImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(300, 300)
            setBackgroundColor(Color.WHITE); setPadding(8, 8, 8, 8)
        }

        val url = "http://${NetworkManager.getLocalIpAddress()}:${AppConfig.SERVER_PORT}"
        val info = TextView(this).apply {
            text = "NEXUS ENGINE\nSTATUS: ONLINE\n$url"
            setTextColor(Color.parseColor("#34C759")); textSize = 13f; gravity = Gravity.CENTER; setPadding(0, 25, 0, 0)
        }

        sidebar.addView(qrImage); sidebar.addView(info)

        val scroll = ScrollView(this).apply {
            val params = RelativeLayout.LayoutParams(-1, -1)
            params.addRule(RelativeLayout.RIGHT_OF, sidebar.id)
            layoutParams = params
        }
        container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(30, 30, 30, 150) }
        scroll.addView(container)

        root.addView(sidebar); root.addView(scroll)
        setContentView(root)

        thread {
            try {
                val bitmap = BitmapFactory.decodeStream(URL("https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${Uri.encode(url)}").openStream())
                runOnUiThread { qrImage.setImageBitmap(bitmap) }
            } catch (e: Exception) { }
        }
    }

    private fun refreshList() {
        container.removeAllViews()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            container.addView(createStyledButton("⚠️ ATIVAR ACESSO AO DISCO") {
                val i = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                i.data = Uri.parse("package:$packageName")
                startActivity(i)
            })
            return
        }

        val files = currentPath.listFiles()?.sortedBy { !it.isDirectory } ?: listOf()
        
        files.forEach { file ->
            val label = "${FileUtils.getFileIcon(file)} ${file.name.uppercase()}\n${FileUtils.formatSize(file.length())}"
            container.addView(createStyledButton(label) {
                if (file.isDirectory) { currentPath = file; refreshList() }
            })
        }
    }

    private fun createStyledButton(txt: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = txt; isFocusable = true; setTextColor(Color.LTGRAY)
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START; setPadding(40, 30, 40, 30)
            val normal = GradientDrawable().apply { setColor(Color.parseColor("#161618")); cornerRadius = 10f }
            val focused = GradientDrawable().apply { 
                setColor(Color.parseColor(AppConfig.THEME_ACCENT)); cornerRadius = 10f; setStroke(4, Color.WHITE) 
            }
            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_focused), focused)
                addState(intArrayOf(), normal)
            }
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 8, 0, 8) }
        }
    }
}