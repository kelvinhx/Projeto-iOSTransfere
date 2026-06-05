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
                        
                        get("/api/logs") {
                            val status = Logger.getSystemStatus(context)
                            call.respondText("$status\n\n${Logger.getLogs()}", ContentType.Text.Plain)
                        }

                        get("/api/storage") {
                            val info = FileUtils.getStorageInfo()
                            call.respondText("{\"free\":\"${info.first}\",\"total\":\"${info.second}\"}", ContentType.Application.Json)
                        }

                        get("/api/list") {
                            val path = call.parameters["path"] ?: ""
                            val folder = File(baseDir, path)
                            val json = JSONArray()
                            folder.listFiles()?.sortedBy { !it.isDirectory }?.forEach {
                                val obj = JSONObject()
                                obj.put("name", it.name).put("isDir", it.isDirectory)
                                obj.put("icon", FileUtils.getFileIcon(it)).put("size", FileUtils.formatSize(it.length()))
                                obj.put("relPath", it.absolutePath.replace(baseDir.absolutePath, ""))
                                json.put(obj)
                            }
                            call.respondText(json.toString(), ContentType.Application.Json)
                        }

                        // UPLOAD OTIMIZADO PARA ARQUIVOS GRANDES
                        post("/upload") {
                            val path = call.parameters["path"] ?: ""
                            val uploadDir = File(baseDir, path)
                            if (!uploadDir.exists()) uploadDir.mkdirs()

                            call.receiveMultipart().forEachPart { part ->
                                if (part is PartData.FileItem) {
                                    val f = File(uploadDir, part.originalFileName ?: "file")
                                    Logger.log("Iniciando Stream: ${f.name}")
                                    
                                    part.streamProvider().use { input ->
                                        f.outputStream().use { output ->
                                            val buffer = ByteArray(AppConfig.BUFFER_SIZE)
                                            var bytesRead: Int
                                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                                output.write(buffer, 0, bytesRead)
                                            }
                                        }
                                    }
                                }
                                part.dispose()
                            }
                            call.respond(HttpStatusCode.OK)
                        }
                        
                        // DELETAR E RENOMEAR (Protegidos)
                        post("/api/action") {
                            val p = call.receiveParameters()
                            val action = p["action"]
                            val target = File(baseDir, p["path"] ?: "")
                            val success = when(action) {
                                "delete" -> target.deleteRecursively()
                                "rename" -> target.renameTo(File(target.parent, p["dest"] ?: "novo"))
                                else -> false
                            }
                            call.respond(if(success) HttpStatusCode.OK else HttpStatusCode.BadRequest)
                        }
                    }
                }.start(wait = true)
            } catch (e: Exception) { Logger.log("Falha Crítica: ${e.message}") }
        }
    }
}