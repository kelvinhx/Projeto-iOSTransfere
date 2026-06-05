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

    private fun setupUI() {
        val root = RelativeLayout(this).apply { setBackgroundColor(Color.BLACK) }
        val sidebar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40); background = ColorDrawable(Color.parseColor("#111113"))
            layoutParams = RelativeLayout.LayoutParams(400, -1)
        }

        val qrImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(300, 300)
            setBackgroundColor(Color.WHITE)
        }

        val url = "http://${NetworkManager.getLocalIpAddress()}:${AppConfig.SERVER_PORT}"
        sidebar.addView(qrImage)
        sidebar.addView(TextView(this).apply { text = url; setTextColor(Color.CYAN); gravity = Gravity.CENTER })

        val scroll = ScrollView(this).apply {
            val p = RelativeLayout.LayoutParams(-1, -1); p.addRule(RelativeLayout.RIGHT_OF, sidebar.id)
            layoutParams = p
        }
        container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(30, 30, 30, 30) }
        scroll.addView(container)
        root.addView(sidebar); root.addView(scroll)
        setContentView(root)

        thread {
            try {
                val bitmap = BitmapFactory.decodeStream(URL("https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${Uri.encode(url)}").openStream())
                runOnUiThread { qrImage.setImageBitmap(bitmap) }
            } catch (e: Exception) {}
        }
    }
}