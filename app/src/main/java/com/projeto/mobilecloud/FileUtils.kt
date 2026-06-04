package com.projeto.mobilecloud

import java.io.File
import java.text.DecimalFormat

object FileUtils {
    fun getFileIcon(file: File): String {
        if (file.isDirectory) return "📁"
        return when (file.extension.lowercase()) {
            "apk" -> "🤖"
            "jpg", "png", "webp" -> "🖼️"
            "mp4", "mkv" -> "🎬"
            "mp3", "wav" -> "🎵"
            "zip", "rar" -> "📦"
            else -> "📄"
        }
    }

    fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }
}