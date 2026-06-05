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

class FileServer(private val androidContext: Context) {
    private val baseDir = File(AppConfig.ROOT_PATH)

    fun start() {
        thread {
            try {
                embeddedServer(Netty, port = AppConfig.SERVER_PORT) {
                    routing {
                        get("/") { 
                            ServerState.isClientConnected = true
                            call.respondText(WebInterface.getHtml(), ContentType.Text.Html) 
                        }
                        
                        get("/api/storage") {
                            val info = FileUtils.getStorageInfo()
                            call.respondText("{\"free\":\"${info.first}\",\"total\":\"${info.second}\"}", ContentType.Application.Json)
                        }

                        get("/api/list") {
                            val path = call.parameters["path"] ?: ""
                            val folder = File(baseDir, path)
                            val json = JSONArray()
                            folder.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))?.forEach {
                                val obj = JSONObject()
                                obj.put("name", it.name).put("isDir", it.isDirectory)
                                obj.put("icon", FileUtils.getFileIcon(it))
                                val sizeVal = if(it.isDirectory) FileUtils.getFolderSize(it) else it.length()
                                obj.put("size", FileUtils.formatSize(sizeVal))
                                obj.put("relPath", it.absolutePath.replace(baseDir.absolutePath, ""))
                                json.put(obj)
                            }
                            call.respondText(json.toString(), ContentType.Application.Json)
                        }

                        get("/api/open") {
                            val path = call.parameters["path"] ?: ""
                            val file = File(baseDir, path)
                            if (file.exists()) {
                                FileUtils.openFile(androidContext, file)
                                call.respondText("OK")
                            } else { call.respond(HttpStatusCode.NotFound) }
                        }

                        get("/api/stream") {
                            val path = call.parameters["path"] ?: ""
                            val file = File(baseDir, path)
                            if (file.exists()) call.respondFile(file) else call.respond(HttpStatusCode.NotFound)
                        }

                        post("/api/action") {
                            val p = call.receiveParameters()
                            val action = p["action"]
                            val target = File(baseDir, p["path"] ?: "")
                            val success = when(action) {
                                "delete" -> target.deleteRecursively()
                                "rename" -> target.renameTo(File(target.parent, p["dest"] ?: "novo"))
                                else -> false
                            }
                            call.respond(if (success) HttpStatusCode.OK else HttpStatusCode.BadRequest)
                        }

                        post("/upload") {
                            val path = call.parameters["path"] ?: ""
                            val uploadDir = File(baseDir, path)
                            if (!uploadDir.exists()) uploadDir.mkdirs()
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
}