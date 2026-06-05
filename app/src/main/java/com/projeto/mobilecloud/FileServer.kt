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

class FileServer {
    fun start() {
        thread {
            try {
                embeddedServer(Netty, port = AppConfig.SERVER_PORT) {
                    routing {
                        get("/") { call.respondText(WebInterface.getHtml(), ContentType.Text.Html) }
                        
                        // Nova rota de info de armazenamento
                        get("/api/storage") {
                            val info = FileUtils.getStorageInfo()
                            val json = JSONObject()
                            json.put("free", info.first)
                            json.put("total", info.second)
                            call.respondText(json.toString(), ContentType.Application.Json)
                        }

                        get("/api/list") {
                            val path = call.parameters["path"] ?: ""
                            val folder = File(AppConfig.ROOT_PATH, path)
                            val json = JSONArray()
                            folder.listFiles()?.sortedBy { !it.isDirectory }?.forEach {
                                val obj = JSONObject()
                                obj.put("name", it.name)
                                obj.put("isDir", it.isDirectory)
                                obj.put("icon", FileUtils.getFileIcon(it))
                                obj.put("size", FileUtils.formatSize(it.length()))
                                obj.put("relPath", it.absolutePath.replace(AppConfig.ROOT_PATH, ""))
                                json.put(obj)
                            }
                            call.respondText(json.toString(), ContentType.Application.Json)
                        }

                        post("/upload") {
                            val path = call.parameters["path"] ?: ""
                            val uploadDir = File(AppConfig.ROOT_PATH, path)
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