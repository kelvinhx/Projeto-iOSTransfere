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

object ServerState {
    var isConnected = false
    var clientName = "Desconhecido"
    var lastInteraction = 0L
}

class FileServer(private val androidContext: Context) {
    private val baseDir = File(AppConfig.ROOT_PATH)

    fun start() {
        thread {
            try {
                embeddedServer(Netty, port = AppConfig.SERVER_PORT) {
                    routing {
                        get("/") { 
                            val userAgent = call.request.headers["User-Agent"] ?: ""
                            ServerState.clientName = if (userAgent.contains("iPhone")) "iPhone" else "Navegador"
                            ServerState.isConnected = true
                            ServerState.lastInteraction = System.currentTimeMillis()
                            call.respondText(WebInterface.getHtml(), ContentType.Text.Html) 
                        }
                        
                        get("/api/status") {
                            val json = JSONObject().put("connected", ServerState.isConnected).put("client", ServerState.clientName)
                            call.respondText(json.toString(), ContentType.Application.Json)
                        }

                        get("/api/list") {
                            val path = call.parameters["path"] ?: ""
                            val folder = File(baseDir, path)
                            val json = JSONArray()
                            folder.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))?.forEach {
                                val obj = JSONObject()
                                obj.put("name", it.name).put("isDir", it.isDirectory).put("icon", FileUtils.getFileIcon(it))
                                val size = if(it.isDirectory) FileUtils.getFolderSize(it) else it.length()
                                obj.put("size", FileUtils.formatSize(size)).put("relPath", it.absolutePath.replace(baseDir.absolutePath, ""))
                                json.put(obj)
                            }
                            call.respondText(json.toString(), ContentType.Application.Json)
                        }

                        post("/api/action") {
                            val p = call.receiveParameters()
                            val action = p["action"]
                            val src = File(baseDir, p["path"] ?: "")
                            val dst = File(baseDir, p["dest"] ?: "")
                            
                            val success = when(action) {
                                "delete" -> src.deleteRecursively()
                                "rename", "move" -> src.renameTo(dst)
                                "copy" -> try { src.copyRecursively(dst, true); true } catch(e: Exception) { false }
                                "mkdir" -> File(src, p["name"] ?: "Nova Pasta").mkdirs()
                                else -> false
                            }
                            call.respond(if (success) HttpStatusCode.OK else HttpStatusCode.BadRequest)
                        }

                        get("/api/stream") {
                            val file = File(baseDir, call.parameters["path"] ?: "")
                            if (file.exists()) call.respondFile(file) else call.respond(HttpStatusCode.NotFound)
                        }

                        post("/upload") {
                            val uploadDir = File(baseDir, call.parameters["path"] ?: "")
                            if (!uploadDir.exists()) uploadDir.mkdirs()
                            call.receiveMultipart().forEachPart { part ->
                                if (part is PartData.FileItem) {
                                    val f = File(uploadDir, part.originalFileName ?: "file")
                                    part.streamProvider().use { i -> f.outputStream().use { o -> i.copyTo(o) } }
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
            val uri = FileProvider.getUriForFile(androidContext, "${androidContext.packageName}.provider", file)
            intent.setDataAndType(uri, FileUtils.getMimeType(file))
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            androidContext.startActivity(intent)
        } catch (e: Exception) { }
    }
}