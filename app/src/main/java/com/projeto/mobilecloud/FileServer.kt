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
import kotlin.concurrent.thread

class FileServer {
    fun start() {
        thread {
            try {
                embeddedServer(Netty, port = AppConfig.SERVER_PORT) {
                    routing {
                        get("/") { call.respondText(WebInterface.getHtml(), ContentType.Text.Html) }
                        
                        get("/api/list") {
                            val path = call.parameters["path"] ?: ""
                            val folder = File(AppConfig.ROOT_PATH, path)
                            val json = JSONArray()
                            folder.listFiles()?.forEach {
                                val obj = org.json.JSONObject()
                                obj.put("name", it.name)
                                obj.put("isDir", it.isDirectory)
                                obj.put("relPath", it.absolutePath.replace(AppConfig.ROOT_PATH, ""))
                                json.put(obj)
                            }
                            call.respondText(json.toString(), ContentType.Application.Json)
                        }
                    }
                }.start(wait = true)
            } catch (e: Exception) {
                // Silencioso para não fechar o app se o servidor falhar
            }
        }
    }
}