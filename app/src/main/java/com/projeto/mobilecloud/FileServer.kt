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
    private val baseDir = File("/storage/emulated/0")

    fun start() {
        thread {
            embeddedServer(Netty, port = 8080) {
                routing {
                    get("/") { 
                        call.respondText(WebInterface.getHtml(), ContentType.Text.Html) 
                    }

                    // Listagem de Arquivos
                    get("/api/list") {
                        val subPath = call.parameters["path"] ?: ""
                        val folder = if (subPath.isEmpty()) baseDir else File(baseDir, subPath)
                        
                        if (!folder.exists() || !folder.isDirectory) {
                            call.respond(HttpStatusCode.NotFound, "Pasta não encontrada")
                            return@get
                        }

                        val files = folder.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: listOf()
                        val json = JSONArray()
                        files.forEach {
                            val obj = JSONObject()
                            obj.put("name", it.name)
                            obj.put("isDir", it.isDirectory)
                            obj.put("size", if (it.isFile) "${it.length() / 1024} KB" else "--")
                            obj.put("relPath", it.absolutePath.replace(baseDir.absolutePath, ""))
                            json.put(obj)
                        }
                        call.respondText(json.toString(), ContentType.Application.Json)
                    }

                    // Upload com correção de forEachPart
                    post("/upload") {
                        val path = call.parameters["path"] ?: ""
                        val uploadDir = File(baseDir, path)
                        if (!uploadDir.exists()) uploadDir.mkdirs()
                        
                        val multipart = call.receiveMultipart()
                        multipart.forEachPart { part ->
                            if (part is PartData.FileItem) {
                                val fileName = part.originalFileName ?: "file_${System.currentTimeMillis()}"
                                val file = File(uploadDir, fileName)
                                part.streamProvider().use { input ->
                                    file.outputStream().buffered().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                            part.dispose()
                        }
                        call.respond(HttpStatusCode.OK, "Upload concluído")
                    }

                    // Ações de Gerenciamento
                    post("/api/action") {
                        val params = call.receiveParameters()
                        val action = params["action"]
                        val target = File(baseDir, params["path"] ?: "")
                        
                        val success = when(action) {
                            "delete" -> target.deleteRecursively()
                            "rename" -> target.renameTo(File(target.parent, params["newName"] ?: "renomeado"))
                            "mkdir" -> File(target, params["newName"] ?: "Nova Pasta").mkdirs()
                            else -> false
                        }
                        call.respond(if (success) HttpStatusCode.OK else HttpStatusCode.BadRequest)
                    }
                }
            }.start(wait = true)
        }
    }
}