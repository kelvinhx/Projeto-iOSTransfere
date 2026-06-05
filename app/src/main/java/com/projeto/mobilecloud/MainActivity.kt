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
        // Impede a TV de desligar a tela
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        try {
            setupUI()
            
            // Inicia o servidor com um pequeno atraso para evitar crash na abertura
            Handler(Looper.getMainLooper()).postDelayed({
                FileServer().start()
            }, 1000)

        } catch (e: Exception) {
            // Se houver erro na UI, mostra uma mensagem simples
            val tv = TextView(this)
            tv.text = "Erro ao carregar interface: ${e.message}"
            setContentView(tv)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun setupUI() {
        val root = RelativeLayout(this).apply { setBackgroundColor(Color.BLACK) }

        // Sidebar lateral
        val sidebar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            background = ColorDrawable(Color.parseColor("#111115"))
            val params = RelativeLayout.LayoutParams(400, -1)
            layoutParams = params
        }

        val qrImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(300, 300)
            setBackgroundColor(Color.WHITE)
        }

        val url = "http://${getLocalIpAddress()}:${AppConfig.SERVER_PORT}"
        val info = TextView(this).apply {
            text = "CONECTAR:\n$url"
            setTextColor(Color.CYAN); textSize = 14f; gravity = Gravity.CENTER; setPadding(0, 20, 0, 10)
        }

        val btnPerm = createStyledButton("ATIVAR ARQUIVOS") { requestPerm() }

        sidebar.addView(qrImage); sidebar.addView(info); sidebar.addView(btnPerm)

        // Explorador de Arquivos
        val scroll = ScrollView(this).apply {
            val params = RelativeLayout.LayoutParams(-1, -1)
            params.addRule(RelativeLayout.RIGHT_OF, sidebar.id)
            layoutParams = params
        }
        container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(30, 30, 30, 150) }
        scroll.addView(container)

        root.addView(sidebar); root.addView(scroll)
        setContentView(root)

        // Carregar QR Code em segundo plano
        thread {
            try {
                val bitmap = BitmapFactory.decodeStream(URL("https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${Uri.encode(url)}").openStream())
                runOnUiThread { qrImage.setImageBitmap(bitmap) }
            } catch (e: Exception) { }
        }
    }

    private fun refreshList() {
        container.removeAllViews()
        
        // Verifica permissão no Android 11+ (Scoped Storage)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            container.addView(createStyledButton("⚠️ CLIQUE AQUI PARA PERMITIR ACESSO") { requestPerm() })
            return
        }

        val files = currentPath.listFiles()?.sortedBy { !it.isDirectory } ?: listOf()
        
        if (currentPath.absolutePath != AppConfig.ROOT_PATH) {
            container.addView(createStyledButton("⬅️ VOLTAR") {
                currentPath = currentPath.parentFile ?: currentPath
                refreshList()
            })
        }

        files.forEach { file ->
            val icon = if (file.isDirectory) "📂" else "📄"
            container.addView(createStyledButton("$icon ${file.name.uppercase()}") {
                if (file.isDirectory) { currentPath = file; refreshList() }
            })
        }
    }

    private fun createStyledButton(txt: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = txt; isFocusable = true; setTextColor(Color.LTGRAY)
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START; setPadding(40, 30, 40, 30)
            
            // Selector de Foco: Fica Azul quando o controle da TV passa por cima
            val normal = GradientDrawable().apply { setColor(Color.parseColor("#1C1C1E")); cornerRadius = 12f }
            val focused = GradientDrawable().apply { 
                setColor(Color.parseColor(AppConfig.THEME_ACCENT))
                cornerRadius = 12f; setStroke(4, Color.WHITE)
            }
            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_focused), focused)
                addState(intArrayOf(), normal)
            }
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 8, 0, 8) }
        }
    }

    private fun requestPerm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                } catch (ex: Exception) {
                    Toast.makeText(this, "Erro ao abrir configurações", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            for (i in interfaces) {
                for (a in i.inetAddresses) {
                    if (!a.isLoopbackAddress && a is java.net.Inet4Address) return a.hostAddress ?: ""
                }
            }
        } catch (e: Exception) {}
        return "0.0.0.0"
    }
}