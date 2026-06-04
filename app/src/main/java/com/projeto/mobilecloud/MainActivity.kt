package com.projeto.mobilecloud

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F0F0F"))
        }

        val text = TextView(this).apply {
            text = "MOBILE-TO-CLOUD\nENGINE ACTIVE"
            setTextColor(Color.GREEN)
            textSize = 24f
            gravity = Gravity.CENTER
        }

        rootLayout.addView(text)
        setContentView(rootLayout)
    }
}