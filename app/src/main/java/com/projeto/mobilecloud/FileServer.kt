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
            try {
                embeddedServer(Netty, port = 8080) {
                    routing {
                        get("/") { call.respondText(WebInterface.getHtml(), ContentType.Text.Html) }

                        get("/api/list") {
                            val path = call.parameters["path"] ?: ""
                            val folder = File(baseDir, path)
                            
                            if (!folder.exists()) {
                                call.respond(HttpStatusCode.OK, "[]") // Retorna vazio se sem permissão
                                return@get
                            }

                            val json = JSONArray()
                            folder.listFiles()?.sortedBy { !it.isDirectory }?.forEach {
                                val obj = JSONObject()
                                obj.put("name", it.name)
                                obj.put("isDir", it.isDirectory)
                                obj.put("relPath", it.absolutePath.replace(baseDir.absolutePath, ""))
                                json.put(obj)
                            }
                            call.respondText(json.toString(), ContentType.Application.Json)
                        }

                        post("/upload") {
                            val path = call.parameters["path"] ?: ""
                            val dest = File(baseDir, path)
                            if (!dest.exists()) dest.mkdirs()

                            val multipart = call.receiveMultipart()
                            multipart.forEachPart { part ->
                                if (part is PartData.FileItem) {
                                    val f = File(dest, part.originalFileName ?: "file")
                                    part.streamProvider().use { input -> f.outputStream().use { input.copyTo(it) } }
                                }
                                part.dispose()
                            }
                            call.respond(HttpStatusCode.OK)
                        }

                        post("/api/action") {
                            val p = call.receiveParameters()
                            val target = File(baseDir, p["path"] ?: "")
                            val action = p["action"]
                            val success = when(action) {
                                "delete" -> target.deleteRecursively()
                                "rename" -> target.renameTo(File(target.parent, p["new"] ?: "new"))
                                else -> false
                            }
                            call.respond(if(success) HttpStatusCode.OK else HttpStatusCode.BadRequest)
                        }
                    }
                }.start(wait = true)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}