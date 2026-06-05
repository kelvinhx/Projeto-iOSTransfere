package com.projeto.mobilecloud

import android.os.Environment
import android.os.StatFs
import java.io.File
import java.text.DecimalFormat

object FileUtils {
    fun getFileIcon(file: File): String {
        if (file.isDirectory) return "📁"
        return when (file.extension.lowercase()) {
            "apk" -> "🤖"
            "jpg", "png", "webp" -> "🖼️"
            "mp4", "mkv", "avi" -> "🎬"
            "mp3", "wav" -> "🎵"
            "zip", "rar", "7z" -> "📦"
            else -> "📄"
        }
    }

    fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }

    // Retorna [Espaço Livre, Espaço Total] formatado
    fun getStorageInfo(): Pair<String, String> {
        val path = Environment.getExternalStorageDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val availableBlocks = stat.availableBlocksLong
        val totalBlocks = stat.blockCountLong
        return Pair(
            formatSize(availableBlocks * blockSize),
            formatSize(totalBlocks * blockSize)
        )
    }
}