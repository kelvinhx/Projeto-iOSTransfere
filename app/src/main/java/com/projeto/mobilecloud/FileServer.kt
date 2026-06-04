package com.projeto.mobilecloud

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.http.content.*
import java.io.File

class FileServer(private val storagePath: File) {
    fun start() {
        Thread {
            embeddedServer(Netty, port = 8080) {
                routing {
                    // Serve a interface do iPhone
                    get("/") {
                        call.respondText(WebInterface.getHtml(), io.ktor.http.ContentType.Text.Html)
                    }

                    // Recebe o arquivo do iPhone
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
                        call.respondText("Enviado com Sucesso!")
                    }
                }
            }.start(wait = true)
        }.start()
    }
}