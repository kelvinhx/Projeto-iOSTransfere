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
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.json.JSONArray
import org.json.JSONObject

class FileServer {
    private val rootPath = File("/storage/emulated/0")

    fun start() {
        thread {
            embeddedServer(Netty, port = 8080) {
                routing {
                    get("/") { call.respondText(WebInterface.getHtml(), ContentType.Text.Html) }

                    // Listagem dinâmica de qualquer pasta
                    get("/api/list") {
                        val subPath = call.parameters["path"] ?: ""
                        val targetDir = File(rootPath, subPath)
                        
                        if (!targetDir.exists()) {
                            call.respond(HttpStatusCode.NotFound, "Caminho não encontrado")
                            return@get
                        }

                        val files = targetDir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: listOf()
                        val json = JSONArray()
                        files.forEach {
                            val obj = JSONObject()
                            obj.put("name", it.name)
                            obj.put("isDir", it.isDirectory)
                            obj.put("size", if (it.isFile) "${it.length() / 1024} KB" else "--")
                            obj.put("path", it.absolutePath.replace(rootPath.absolutePath, ""))
                            json.put(obj)
                        }
                        call.respondText(json.toString(), ContentType.Application.Json)
                    }

                    // Operações de Arquivo (Copiar, Mover, Deletar, Renomear)
                    post("/api/action") {
                        val params = call.receiveParameters()
                        val action = params["action"]
                        val source = File(rootPath, params["source"] ?: "")
                        val dest = File(rootPath, params["dest"] ?: "")

                        val result = when (action) {
                            "delete" -> source.deleteRecursively()
                            "rename" -> source.renameTo(dest)
                            "copy" -> {
                                try {
                                    Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
                                    true
                                } catch (e: Exception) { false }
                            }
                            "mkdir" -> dest.mkdirs()
                            else -> false
                        }
                        call.respond(if (result) HttpStatusCode.OK else HttpStatusCode.InternalServerError)
                    }

                    post("/upload") {
                        val subPath = call.parameters["path"] ?: ""
                        val targetDir = File(rootPath, subPath)
                        val multipart = call.receiveMultipart()
                        multipart.forEachPart { part ->
                            if (part is PartData.FileItem) {
                                val file = File(targetDir, part.originalFileName ?: "upload")
                                part.streamProvider().use { input -> file.outputStream().use { input.copyTo(it) } }
                            }
                            part.dispose()
                        }
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }.start(wait = true)
        }
    }
}

private fun thread(block: () -> Unit) = Thread(block).start()