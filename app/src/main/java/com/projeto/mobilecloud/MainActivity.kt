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
        
        val serviceIntent = Intent(this, NexusService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent) else startService(serviceIntent)

        setupUI()
    }

    override fun onBackPressed() {
        if (currentPath.absolutePath != AppConfig.ROOT_PATH) {
            currentPath = currentPath.parentFile ?: File(AppConfig.ROOT_PATH)
            refreshList()
        } else super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun setupUI() {
        val root = RelativeLayout(this).apply { setBackgroundColor(Color.parseColor(AppConfig.COLOR_BG)) }

        // Sidebar Esquerda Premium
        val sidebar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            val grad = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.parseColor("#121214"), Color.parseColor("#050505")))
            background = grad
            layoutParams = RelativeLayout.LayoutParams(450, -1)
        }

        val qrCard = CardViewHelper.createCard(this).apply {
            val img = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(320, 320)
                setBackgroundColor(Color.WHITE); setPadding(10,10,10,10)
                id = View.generateViewId()
            }
            addView(img)
            tag = img // Guarda referência
        }

        val url = "http://${NetworkManager.getLocalIpAddress()}:${AppConfig.SERVER_PORT}"
        val info = TextView(this).apply {
            text = "NEXUS PRO\nEXPLORER\n\n$url"
            setTextColor(Color.WHITE); textSize = 15f; gravity = Gravity.CENTER
            setLineSpacing(0f, 1.2f); setPadding(0, 40, 0, 0)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }

        sidebar.addView(qrCard); sidebar.addView(info)

        // Explorador Direita
        val scroll = ScrollView(this).apply {
            id = View.generateViewId()
            val params = RelativeLayout.LayoutParams(-1, -1)
            params.addRule(RelativeLayout.RIGHT_OF, sidebar.id)
            layoutParams = params
            isFillViewport = true
        }
        container = LinearLayout(this).apply { 
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 150) 
        }
        scroll.addView(container)

        root.addView(sidebar); root.addView(scroll)
        setContentView(root)

        // Load QR
        thread {
            try {
                val bitmap = BitmapFactory.decodeStream(URL("https://api.qrserver.com/v1/create-qr-code/?size=320x320&data=${Uri.encode(url)}").openStream())
                runOnUiThread { (qrCard.tag as ImageView).setImageBitmap(bitmap) }
            } catch (e: Exception) { }
        }
    }

    private fun refreshList() {
        container.removeAllViews()
        
        // Header da lista
        container.addView(TextView(this).apply {
            text = "ARQUIVOS DA TV"; setTextColor(Color.parseColor(AppConfig.COLOR_ACCENT))
            textSize = 12f; setPadding(10, 0, 0, 20); typeface = Typeface.DEFAULT_BOLD
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            container.addView(createItem("⚠️ LIBERAR PERMISSÃO DE DISCO", true) {
                val i = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                i.data = Uri.parse("package:$packageName")
                startActivity(i)
            })
            return
        }

        val files = currentPath.listFiles()?.sortedBy { !it.isDirectory } ?: listOf()
        files.forEach { file ->
            val label = "${FileUtils.getFileIcon(file)}  ${file.name.uppercase()}"
            container.addView(createItem(label, false) {
                if (file.isDirectory) { currentPath = file; refreshList() }
            })
        }
    }

    private fun createItem(txt: String, isAlert: Boolean, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = txt; isFocusable = true; setTextColor(if(isAlert) Color.parseColor(AppConfig.COLOR_DANGER) else Color.LTGRAY)
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START; setPadding(50, 35, 50, 35)
            textSize = 16f; typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            
            val normal = GradientDrawable().apply { 
                setColor(Color.parseColor(AppConfig.COLOR_CARD))
                cornerRadius = 15f
                setStroke(1, Color.parseColor("#222222"))
            }
            val focused = GradientDrawable().apply { 
                setColor(Color.parseColor(AppConfig.COLOR_ACCENT))
                cornerRadius = 15f
                setStroke(5, Color.WHITE)
            }
            
            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_focused), focused)
                addState(intArrayOf(), normal)
            }
            
            setOnFocusChangeListener { _, hasFocus -> 
                setTextColor(if (hasFocus) Color.WHITE else (if(isAlert) Color.parseColor(AppConfig.COLOR_DANGER) else Color.LTGRAY))
                elevation = if(hasFocus) 20f else 0f
            }
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 10, 0, 10) }
        }
    }
}

// Helper para criar bordas e sombras
object CardViewHelper {
    fun createCard(context: android.content.Context): FrameLayout {
        return FrameLayout(context).apply {
            setPadding(10, 10, 10, 10)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 20f
            }
            elevation = 15f
        }
    }
}