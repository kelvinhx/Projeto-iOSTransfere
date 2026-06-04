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

class FileServer(private val storagePath: File) {
    fun start() {
        Thread {
            embeddedServer(Netty, port = 8080) {
                routing {
                    // Serve a Interface Web
                    get("/") {
                        call.respondText(WebInterface.getHtml(), ContentType.Text.Html)
                    }

                    // API: Listar arquivos na TV
                    get("/api/list") {
                        val files = storagePath.listFiles()?.filter { it.isFile } ?: listOf<File>()
                        val jsonArray = JSONArray()
                        files.forEach {
                            val obj = JSONObject()
                            obj.put("name", it.name)
                            obj.put("size", "${it.length() / 1024} KB")
                            jsonArray.put(obj)
                        }
                        call.respondText(jsonArray.toString(), ContentType.Application.Json)
                    }

                    // API: Upload de Arquivos
                    post("/upload") {
                        val multipart = call.receiveMultipart()
                        multipart.forEachPart { part ->
                            if (part is PartData.FileItem) {
                                val file = File(storagePath, part.originalFileName ?: "file_${System.currentTimeMillis()}")
                                part.streamProvider().use { input ->
                                    file.outputStream().buffered().use { output -> input.copyTo(output) }
                                }
                            }
                            part.dispose()
                        }
                        call.respond(HttpStatusCode.OK, "Transferência Completa")
                    }

                    // API: Deletar Arquivo
                    post("/api/delete") {
                        val name = call.receiveParameters()["name"]
                        val file = File(storagePath, name ?: "")
                        if (file.exists() && file.delete()) {
                            call.respond(HttpStatusCode.OK, "Deletado")
                        } else {
                            call.respond(HttpStatusCode.InternalServerError, "Erro ao deletar")
                        }
                    }
                }
            }.start(wait = true)
        }.start()
    }
}