package com.projeto.mobilecloud

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
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
    private var currentPath = File("/storage/emulated/0")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // UI Base
        val root = RelativeLayout(this).apply { setBackgroundColor(Color.BLACK) }

        // Sidebar de Conexão
        val sidebar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(30, 30, 30, 30)
            background = GradientDrawable().apply { setColor(Color.parseColor("#111111")) }
            val params = RelativeLayout.LayoutParams(450, -1)
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            layoutParams = params
        }

        val qrImage = ImageView(this).apply { layoutParams = LinearLayout.LayoutParams(350, 350); setBackgroundColor(Color.WHITE) }
        val info = TextView(this).apply { 
            text = "IP: ${getLocalIpAddress()}\nPorta: 8080"; setTextColor(Color.CYAN); setPadding(0,20,0,20); gravity = Gravity.CENTER 
        }
        val btnPerm = Button(this).apply {
            text = "LIBERAR ACESSO"; setOnClickListener { requestPerm() }
        }
        sidebar.addView(qrImage); sidebar.addView(info); sidebar.addView(btnPerm)

        // Explorer Principal
        val scroll = ScrollView(this).apply {
            val params = RelativeLayout.LayoutParams(-1, -1)
            params.addRule(RelativeLayout.RIGHT_OF, sidebar.id)
            layoutParams = params
        }
        container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 20, 20, 20) }
        scroll.addView(container)

        root.addView(sidebar); root.addView(scroll)
        setContentView(root)

        // Start
        FileServer().start()
        refreshList()
        
        thread {
            try {
                val url = "http://${getLocalIpAddress()}:8080"
                val bitmap = BitmapFactory.decodeStream(URL("https://chart.googleapis.com/chart?chs=350x350&cht=qr&chl=$url").openStream())
                runOnUiThread { qrImage.setImageBitmap(bitmap) }
            } catch (e: Exception) {}
        }
    }

    private fun refreshList() {
        container.removeAllViews()
        val files = currentPath.listFiles()?.sortedBy { !it.isDirectory } ?: listOf()
        
        // Botão Voltar
        if(currentPath.absolutePath != "/storage/emulated/0") {
            container.addView(createFileButton(".. [VOLTAR]") {
                currentPath = currentPath.parentFile ?: currentPath
                refreshList()
            })
        }

        files.forEach { file ->
            container.addView(createFileButton((if(file.isDirectory) "📂 " else "📄 ") + file.name) {
                if(file.isDirectory) { currentPath = file; refreshList() }
            })
        }
    }

    private fun createFileButton(txt: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = txt; isFocusable = true; setTextColor(Color.WHITE); textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            background = GradientDrawable().apply { setColor(Color.parseColor("#1C1C1E")); cornerRadius = 8f }
            setOnClickListener { onClick() }
            val p = LinearLayout.LayoutParams(-1, -2); p.setMargins(0,4,0,4); layoutParams = p
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
                if (!a.isLoopbackAddress && a is java.net.Inet4Address) return a.hostAddress ?: ""
            }
        }
        return "0.0.0.0"
    }
}