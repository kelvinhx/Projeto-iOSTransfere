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
import android.net.Uri
import androidx.core.content.FileProvider

class FileServer(private val context: Context) {
    private val baseDir = File(AppConfig.ROOT_PATH)

    fun start() {
        thread {
            try {
                embeddedServer(Netty, port = AppConfig.SERVER_PORT) {
                    routing {
                        // Página inicial do iPhone
                        get("/") { 
                            call.respondText(WebInterface.getHtml(), ContentType.Text.Html) 
                        }
                        
                        // Informações de memória
                        get("/api/storage") {
                            val info = FileUtils.getStorageInfo()
                            val json = JSONObject()
                            json.put("free", info.first)
                            json.put("total", info.second)
                            call.respondText(json.toString(), ContentType.Application.Json)
                        }

                        // Listagem de arquivos
                        get("/api/list") {
                            val path = call.parameters["path"] ?: ""
                            val folder = File(baseDir, path)
                            val json = JSONArray()
                            if (folder.exists() && folder.isDirectory) {
                                folder.listFiles()?.sortedBy { !it.isDirectory }?.forEach {
                                    val obj = JSONObject()
                                    obj.put("name", it.name)
                                    obj.put("isDir", it.isDirectory)
                                    obj.put("icon", FileUtils.getFileIcon(it))
                                    obj.put("size", FileUtils.formatSize(it.length()))
                                    obj.put("relPath", it.absolutePath.replace(baseDir.absolutePath, ""))
                                    json.put(obj)
                                }
                            }
                            call.respondText(json.toString(), ContentType.Application.Json)
                        }

                        // Comando para abrir arquivo na TV
                        get("/api/open") {
                            val path = call.parameters["path"] ?: ""
                            val file = File(baseDir, path)
                            if (file.exists()) {
                                openOnTV(file)
                                call.respondText("Comando enviado")
                            } else {
                                call.respond(HttpStatusCode.NotFound)
                            }
                        }

                        // Ações (Mover, Deletar, Renomear)
                        post("/api/action") {
                            val p = call.receiveParameters()
                            val action = p["action"]
                            val source = File(baseDir, p["path"] ?: "")
                            val dest = File(baseDir, p["dest"] ?: "")
                            
                            val success = when(action) {
                                "delete" -> source.deleteRecursively()
                                "rename", "move" -> source.renameTo(dest)
                                "mkdir" -> File(source, p["name"] ?: "Nova").mkdirs()
                                else -> false
                            }
                            call.respond(if (success) HttpStatusCode.OK else HttpStatusCode.BadRequest)
                        }

                        // Receber arquivos do iPhone
                        post("/upload") {
                            val path = call.parameters["path"] ?: ""
                            val uploadDir = File(baseDir, path)
                            if (!uploadDir.exists()) uploadDir.mkdirs()
                            
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
            } catch (e: Exception) {
                Logger.log("Erro Servidor: ${e.message}")
            }
        }
    }

    // Função auxiliar para abrir arquivos usando o contexto correto do Android
    private fun openOnTV(file: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            // Resolve o erro de permissão e contexto
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            intent.setDataAndType(uri, FileUtils.getMimeType(file))
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Logger.log("Erro ao abrir arquivo: ${e.message}")
        }
    }
}