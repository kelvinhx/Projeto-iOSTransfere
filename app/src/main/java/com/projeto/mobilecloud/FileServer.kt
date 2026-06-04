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

object TransferState {
    var isUploading: Boolean = false
    var fileName: String = ""
    var progress: Int = 0
    var lastStatus: String = "Aguardando"
}

class FileServer {
    private val baseDir = File("/storage/emulated/0")

    fun start() {
        thread {
            embeddedServer(Netty, port = 8080) {
                routing {
                    get("/") { call.respondText(WebInterface.getHtml(), ContentType.Text.Html) }

                    // API de Status para a TV e Web consultarem
                    get("/api/status") {
                        val json = JSONObject()
                        json.put("isUploading", TransferState.isUploading)
                        json.put("fileName", TransferState.fileName)
                        json.put("progress", TransferState.progress)
                        json.put("status", TransferState.lastStatus)
                        call.respondText(json.toString(), ContentType.Application.Json)
                    }

                    get("/api/list") {
                        val path = call.parameters["path"] ?: ""
                        val folder = File(baseDir, path)
                        val json = JSONArray()
                        if (folder.exists() && folder.isDirectory) {
                            folder.listFiles()?.sortedBy { !it.isDirectory }?.forEach {
                                val obj = JSONObject()
                                obj.put("name", it.name)
                                obj.put("isDir", it.isDirectory)
                                obj.put("relPath", it.absolutePath.replace(baseDir.absolutePath, ""))
                                json.put(obj)
                            }
                        }
                        call.respondText(json.toString(), ContentType.Application.Json)
                    }

                    post("/upload") {
                        val path = call.parameters["path"] ?: ""
                        val dest = File(baseDir, path)
                        if (!dest.exists()) dest.mkdirs()

                        val multipart = call.receiveMultipart()
                        TransferState.isUploading = true
                        
                        multipart.forEachPart { part ->
                            if (part is PartData.FileItem) {
                                TransferState.fileName = part.originalFileName ?: "Arquivo"
                                val f = File(dest, TransferState.fileName)
                                val contentLength = call.request.header(HttpHeaders.ContentLength)?.toLong() ?: 1L
                                
                                part.streamProvider().use { input ->
                                    f.outputStream().use { output ->
                                        val buffer = ByteArray(8192)
                                        var bytesRead: Long = 0
                                        while (true) {
                                            val read = input.read(buffer)
                                            if (read <= 0) break
                                            output.write(buffer, 0, read)
                                            bytesRead += read
                                            TransferState.progress = ((bytesRead * 100) / contentLength).toInt()
                                        }
                                    }
                                }
                            }
                            part.dispose()
                        }
                        TransferState.isUploading = false
                        TransferState.lastStatus = "Sucesso!"
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }.start(wait = true)
        }
    }
}