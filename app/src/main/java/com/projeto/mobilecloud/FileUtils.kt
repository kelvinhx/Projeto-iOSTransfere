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
            "jpg", "jpeg", "png", "webp" -> "🖼️"
            "mp4", "mkv" -> "🎬"
            "mp3" -> "🎵"
            else -> "📄"
        }
    }

    // Soma o tamanho de todos os arquivos dentro de uma pasta
    fun getFolderSize(file: File): Long {
        if (file.isFile) return file.length()
        var size: Long = 0
        val files = file.listFiles()
        if (files != null) {
            for (f in files) {
                size += if (f.isDirectory) getFolderSize(f) else f.length()
            }
        }
        return size
    }

    fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }

    fun getStorageInfo(): Pair<String, String> {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        return Pair(formatSize(stat.availableBytes), formatSize(stat.totalBytes))
    }

    fun getMimeType(file: File): String {
        return when(file.extension.lowercase()) {
            "jpg", "jpeg", "png" -> "image/jpeg"
            "mp4" -> "video/mp4"
            "apk" -> "application/vnd.android.package-archive"
            else -> "application/octet-stream"
        }
    }
}