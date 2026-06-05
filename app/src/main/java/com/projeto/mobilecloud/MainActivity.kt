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

    private lateinit var grid: GridLayout
    private lateinit var sidebar: LinearLayout
    private lateinit var connInfo: TextView
    private var currentPath = File(AppConfig.ROOT_PATH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // GATILHO AUTOMÁTICO DE PERMISSÃO (Sistema)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivity(intent)
            Toast.makeText(this, "Nexus Explorer Pro: Conceda acesso para continuar", Toast.LENGTH_LONG).show()
        }

        startService(Intent(this, NexusService::class.java))
        setupUI()
        startConnectionLoop()
    }

    private fun setupUI() {
        val root = RelativeLayout(this).apply { setBackgroundColor(Color.BLACK) }

        // Sidebar Fixa no canto
        sidebar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(30, 30, 30, 30)
            background = ColorDrawable(Color.parseColor("#080808"))
            layoutParams = RelativeLayout.LayoutParams(400, -1)
        }

        val qrImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(250, 250)
            setBackgroundColor(Color.WHITE); setPadding(5, 5, 5, 5)
            id = View.generateViewId()
        }

        connInfo = TextView(this).apply {
            text = "AGUARDANDO..."; setTextColor(Color.GRAY); textSize = 10f
            gravity = Gravity.CENTER; setPadding(0, 15, 0, 0)
        }

        sidebar.addView(qrImage); sidebar.addView(connInfo)

        // Grade de Arquivos
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            layoutParams = RelativeLayout.LayoutParams(-1, -1).apply { addRule(RelativeLayout.RIGHT_OF, sidebar.id) }
        }

        grid = GridLayout(this).apply { columnCount = 4; setPadding(30, 30, 30, 200) }
        scroll.addView(grid)

        // Conexão Mini-Label (Fica sempre no canto)
        val miniConn = TextView(this).apply {
            id = View.generateViewId()
            text = "📡 DISCONNECTED"; setTextColor(Color.RED); textSize = 9f
            setPadding(20, 20, 20, 20)
            val p = RelativeLayout.LayoutParams(-2, -2)
            p.addRule(RelativeLayout.ALIGN_PARENT_TOP); p.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            layoutParams = p
        }

        root.addView(sidebar); root.addView(scroll); root.addView(miniConn)
        setContentView(root)

        // Load QR Code
        thread {
            val url = "http://${NetworkManager.getLocalIpAddress()}:8080"
            try {
                val bitmap = BitmapFactory.decodeStream(URL("https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${Uri.encode(url)}").openStream())
                runOnUiThread { qrImage.setImageBitmap(bitmap) }
            } catch (e: Exception) { }
        }
    }

    private fun startConnectionLoop() {
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                if (ServerState.isConnected) {
                    if (sidebar.visibility == View.VISIBLE) {
                        sidebar.visibility = View.GONE
                        Toast.makeText(this@MainActivity, "Conectado: ${ServerState.clientName}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    sidebar.visibility = View.VISIBLE
                }
                handler.postDelayed(this, 3000)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        grid.removeAllViews()
        currentPath.listFiles()?.sortedBy { !it.isDirectory }?.forEach { file ->
            val size = if(file.isDirectory) FileUtils.getFolderSize(file) else file.length()
            grid.addView(createItem(file, FileUtils.formatSize(size)))
        }
    }

    private fun createItem(file: File, sizeStr: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            isFocusable = true; isClickable = true
            val normal = GradientDrawable().apply { setColor(Color.parseColor("#121214")); cornerRadius = 25f }
            val focused = GradientDrawable().apply { setColor(Color.parseColor("#1C1C1E")); cornerRadius = 25f; setStroke(4, Color.parseColor("#0A84FF")) }
            background = StateListDrawable().apply { addState(intArrayOf(android.R.attr.state_focused), focused); addState(intArrayOf(), normal) }
            
            addView(TextView(context).apply { text = FileUtils.getFileIcon(file); textSize = 35f; gravity = Gravity.CENTER })
            addView(TextView(context).apply { text = file.name.take(12); setTextColor(Color.WHITE); textSize = 13f; gravity = Gravity.CENTER })
            addView(TextView(context).apply { text = sizeStr; setTextColor(Color.DKGRAY); textSize = 9f; gravity = Gravity.CENTER })
            
            setOnClickListener { 
                if(file.isDirectory) { currentPath = file; refresh() } 
                else { showMenu(file) }
            }
            layoutParams = GridLayout.LayoutParams().apply { width = 250; height = 250; setMargins(15, 15, 15, 15) }
        }
    }

    private fun showMenu(file: File) {
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(file.name)
            .setItems(arrayOf("Abrir ▶️", "Excluir 🗑️", "Renomear ✏️")) { _, i ->
                if(i == 1) { file.deleteRecursively(); refresh() }
            }.show()
    }

    override fun onBackPressed() {
        if (currentPath.absolutePath != AppConfig.ROOT_PATH) {
            currentPath = currentPath.parentFile ?: File(AppConfig.ROOT_PATH)
            refresh()
        } else super.onBackPressed()
    }
}