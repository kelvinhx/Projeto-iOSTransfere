package com.projeto.mobilecloud

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.http.*
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

class FileServer {
    // Aponta para a raiz real do Android (Memória Interna)
    private val baseDir = File("/storage/emulated/0")

    fun start() {
        Thread {
            embeddedServer(Netty, port = 8080) {
                routing {
                    get("/") { call.respondText(WebInterface.getHtml(), ContentType.Text.Html) }

                    // Listagem estilo Explorador Profundo
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
                            // Caminho relativo para o iPhone usar
                            obj.put("relPath", it.absolutePath.replace(baseDir.absolutePath, ""))
                            json.put(obj)
                        }
                        call.respondText(json.toString(), ContentType.Application.Json)
                    }

                    // Ações: Deletar e Renomear
                    post("/api/action") {
                        val params = call.receiveParameters()
                        val action = params["action"]
                        val target = File(baseDir, params["path"] ?: "")
                        
                        val success = when(action) {
                            "delete" -> target.deleteRecursively()
                            "rename" -> target.renameTo(File(target.parent, params["newName"] ?: "new_name"))
                            else -> false
                        }
                        call.respond(if (success) HttpStatusCode.OK else HttpStatusCode.BadRequest)
                    }

                    // Upload para a pasta atual aberta no iPhone
                    post("/upload") {
                        val path = call.parameters["path"] ?: ""
                        val uploadDir = File(baseDir, path)
                        val multipart = call.receiveMultipart()
                        multipart.forEachPart { part ->
                            if (part is io.ktor.http.content.PartData.FileItem) {
                                val file = File(uploadDir, part.originalFileName ?: "file")
                                part.streamProvider().use { input -> file.outputStream().use { input.copyTo(it) } }
                            }
                            part.dispose()
                        }
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }.start(wait = true)
        }.start()
    }
}