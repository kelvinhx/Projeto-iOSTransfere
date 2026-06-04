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
                    get("/") { call.respondText(WebInterface.getHtml(), ContentType.Text.Html) }

                    get("/api/list") {
                        val path = call.parameters["path"] ?: ""
                        val folder = File(baseDir, path)
                        if (!folder.exists()) return@get call.respond(HttpStatusCode.NotFound)

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

                    post("/api/action") {
                        val p = call.receiveParameters()
                        val action = p["action"]
                        val file = File(baseDir, p["path"] ?: "")
                        val dest = File(baseDir, p["dest"] ?: "")

                        val success = when(action) {
                            "delete" -> file.deleteRecursively()
                            "rename" -> file.renameTo(dest)
                            "move" -> file.renameTo(File(dest, file.name))
                            "copy" -> {
                                try { file.copyRecursively(File(dest, file.name), true); true } catch(e: Exception) { false }
                            }
                            "mkdir" -> File(file, p["name"] ?: "Nova Pasta").mkdirs()
                            else -> false
                        }
                        call.respond(if (success) HttpStatusCode.OK else HttpStatusCode.BadRequest)
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
        }
    }
}