package com.seuprojeto.app

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Root Layout dinâmico e leve
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#121212")) // Dark Mode nativo
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Elemento Reativo
        val statusText = TextView(this).apply {
            text = "Cloud Engine Active 🚀"
            setTextColor(Color.WHITE)
            textSize = 20f
        }

        rootLayout.addView(statusText)
        setContentView(rootLayout)
        
        // Inicializar ponte de conectividade (Ktor)
        startLocalServer()
    }

    private fun startLocalServer() {
        // Lógica para o Ktor (Netty) será implementada aqui
    }
}