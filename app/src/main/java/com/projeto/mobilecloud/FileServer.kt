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

class FileServer(private var currentRoot: File) {
    fun start() {
        Thread {
            embeddedServer(Netty, port = 8080) {
                routing {
                    get("/") { call.respondText(WebInterface.getHtml(), ContentType.Text.Html) }

                    // Listar arquivos e pastas
                    get("/api/list") {
                        val path = call.parameters["path"] ?: ""
                        val targetDir = File(currentRoot, path)
                        val files = targetDir.listFiles()?.sortedBy { it.isFile } ?: listOf()
                        
                        val json = JSONArray()
                        files.forEach {
                            val obj = JSONObject()
                            obj.put("name", it.name)
                            obj.put("isDir", it.isDirectory)
                            obj.put("size", if(it.isFile) "${it.length()/1024}KB" else "--")
                            json.put(obj)
                        }
                        call.respondText(json.toString(), ContentType.Application.Json)
                    }

                    // Operações: Deletar, Mover, Renomear
                    post("/api/op") {
                        val post = call.receiveParameters()
                        val action = post["action"]
                        val name = post["name"]
                        val newName = post["newName"]
                        val file = File(currentRoot, name ?: "")

                        val success = when(action) {
                            "delete" -> file.deleteRecursively()
                            "rename" -> file.renameTo(File(file.parent, newName ?: ""))
                            "mkdir" -> File(currentRoot, name ?: "Nova Pasta").mkdirs()
                            else -> false
                        }
                        call.respond(if(success) HttpStatusCode.OK else HttpStatusCode.BadRequest)
                    }

                    post("/upload") {
                        val multipart = call.receiveMultipart()
                        val uploadPath = call.parameters["path"] ?: ""
                        val targetDir = File(currentRoot, uploadPath)
                        if(!targetDir.exists()) targetDir.mkdirs()

                        multipart.forEachPart { part ->
                            if (part is PartData.FileItem) {
                                val file = File(targetDir, part.originalFileName ?: "file")
                                part.streamProvider().use { input ->
                                    file.outputStream().buffered().use { output -> input.copyTo(output) }
                                }
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