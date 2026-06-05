package com.projeto.mobilecloud

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.text.DecimalFormat

object FileUtils {
    fun getFileIcon(file: File) = if (file.isDirectory) "📁" else "📄"

    fun openFile(context: Context, file: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val extension = file.extension.lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
        intent.setDataAndType(uri, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun getFolderSize(file: File): Long {
        var size: Long = 0
        file.listFiles()?.forEach { size += if (it.isDirectory) getFolderSize(it) else it.length() }
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
}