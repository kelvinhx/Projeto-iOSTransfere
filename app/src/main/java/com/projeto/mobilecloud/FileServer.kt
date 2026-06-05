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
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider

class FileServer(private val context: Context) {
    fun start() {
        thread {
            try {
                embeddedServer(Netty, port = AppConfig.SERVER_PORT) {
                    routing {
                        get("/") { call.respondText(WebInterface.getHtml(), ContentType.Text.Html) }
                        
                        get("/api/storage") {
                            val info = FileUtils.getStorageInfo()
                            call.respondText("{\"free\":\"${info.first}\",\"total\":\"${info.second}\"}", ContentType.Application.Json)
                        }

                        get("/api/list") {
                            val path = call.parameters["path"] ?: ""
                            val folder = File(AppConfig.ROOT_PATH, path)
                            val json = JSONArray()
                            folder.listFiles()?.sortedBy { !it.isDirectory }?.forEach {
                                val obj = JSONObject()
                                obj.put("name", it.name).put("isDir", it.isDirectory)
                                obj.put("icon", FileUtils.getFileIcon(it)).put("size", FileUtils.formatSize(it.length()))
                                obj.put("relPath", it.absolutePath.replace(AppConfig.ROOT_PATH, ""))
                                json.put(obj)
                            }
                            call.respondText(json.toString(), ContentType.Application.Json)
                        }

                        // NOVA ROTA: Abrir arquivo na TV
                        get("/api/open") {
                            val path = call.parameters["path"] ?: ""
                            val file = File(AppConfig.ROOT_PATH, path)
                            if (file.exists()) {
                                openFileOnTV(file)
                                call.respond(HttpStatusCode.OK, "Abrindo na TV...")
                            } else {
                                call.respond(HttpStatusCode.NotFound)
                            }
                        }

                        post("/upload") {
                            val path = call.parameters["path"] ?: ""
                            val uploadDir = File(AppConfig.ROOT_PATH, path)
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
            } catch (e: Exception) { Logger.log("Erro: ${e.message}") }
        }
    }

    private fun openFileOnTV(file: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = Uri.fromFile(file) // Para simplicidade no momento
            intent.setDataAndType(uri, FileUtils.getMimeType(file))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) { Logger.log("Erro ao abrir: ${e.message}") }
    }
}