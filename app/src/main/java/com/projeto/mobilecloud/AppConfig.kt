package com.projeto.mobilecloud

object AppConfig {
    const val SERVER_PORT = 8080
    const val ROOT_PATH = "/storage/emulated/0"
    const val THEME_ACCENT = "#007AFF"
    
    // INFRAESTRUTURA
    const val BUFFER_SIZE = 65536 // 64KB para estabilidade em arquivos grandes
    const val SESSION_PIN = "1234" // PIN simples para autorizar o iPhone
}