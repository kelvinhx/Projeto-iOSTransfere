package com.projeto.mobilecloud

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider

class FileServer(private val context: Context) {
    private val baseDir = File(AppConfig.ROOT_PATH)

    fun start() {
        thread {
            try {
                embeddedServer(Netty, port = AppConfig.SERVER_PORT) {
                    routing {
                        get("/") { call.respondText(WebInterface.getHtml(), ContentType.Text.Html) }
                        
                        // Rota de Streaming (Permite ver vídeo/foto no iPhone)
                        get("/api/stream") {
                            val path = call.parameters["path"] ?: ""
                            val file = File(baseDir, path)
                            if (file.exists()) {
                                call.respondFile(file)
                            } else {
                                call.respond(HttpStatusCode.NotFound)
                            }
                        }

                        get("/api/storage") {
                            val info = FileUtils.getStorageInfo()
                            val json = JSONObject()
                            json.put("free", info.first).put("total", info.second)
                            call.respondText(json.toString(), ContentType.Application.Json)
                        }

                        get("/api/list") {
                            val path = call.parameters["path"] ?: ""
                            val folder = File(baseDir, path)
                            val json = JSONArray()
                            folder.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))?.forEach {
                                val obj = JSONObject()
                                obj.put("name", it.name).put("isDir", it.isDirectory)
                                obj.put("icon", FileUtils.getFileIcon(it)).put("size", FileUtils.formatSize(it.length()))
                                obj.put("relPath", it.absolutePath.replace(baseDir.absolutePath, ""))
                                json.put(obj)
                            }
                            call.respondText(json.toString(), ContentType.Application.Json)
                        }

                        get("/api/open") {
                            val path = call.parameters["path"] ?: ""
                            val file = File(baseDir, path)
                            if (file.exists()) {
                                openOnTV(file)
                                call.respondText("Comando enviado")
                            } else { call.respond(HttpStatusCode.NotFound) }
                        }

                        post("/upload") {
                            val path = call.parameters["path"] ?: ""
                            val uploadDir = File(baseDir, path)
                            call.receiveMultipart().forEachPart { part ->
                                if (part is PartData.FileItem) {
                                    val f = File(uploadDir, part.originalFileName ?: "file")
                                    part.streamProvider().use { input -> f.outputStream().use { input.copyTo(it) } }
                                }
                                part.dispose()
                            }
                            call.respond(HttpStatusCode.OK)
                        }
                    }
                }.start(wait = true)
            } catch (e: Exception) { }
        }
    }

    private fun openOnTV(file: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            intent.setDataAndType(uri, FileUtils.getMimeType(file))
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) { }
    }
}