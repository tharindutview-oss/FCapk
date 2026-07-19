package com.example.network

import android.content.Context
import android.util.Log
import com.example.data.ChatMessage
import com.example.data.ChatRepository
import com.example.data.toJsonString
import com.example.ui.WebInterface
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.application.install
import io.ktor.server.application.call
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

class ChatServerManager(
    private val context: Context,
    private val repository: ChatRepository
) {
    private val tag = "ChatServerManager"
    private var server: ApplicationEngine? = null
    
    // Active WebSocket connections (synchronized for multi-client safety)
    private val activeSessions: MutableSet<WebSocketSession> = Collections.synchronizedSet(LinkedHashSet<WebSocketSession>())
    private val serverScope = CoroutineScope(Dispatchers.IO)

    var isRunning = false
        private set

    var currentServerUrl = "Offline"
        private set

    val activeConnectionsCount: Int
        get() = activeSessions.size

    fun startServer(port: Int = 8080, onStatusChanged: () -> Unit) {
        if (isRunning) return

        serverScope.launch {
            try {
                val ip = getLocalIpAddress()
                currentServerUrl = "http://$ip:$port"

                server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
                    install(WebSockets)

                    routing {
                        // Serve Web Client App
                        get("/") {
                            val ipAddr = getLocalIpAddress()
                            call.respondText(
                                text = WebInterface.getHtml(ipAddr, port),
                                contentType = ContentType.Text.Html
                            )
                        }

                        // Serve Static Uploaded Files
                        get("/uploads/{filename}") {
                            val filename = call.parameters["filename"]
                            if (filename != null) {
                                val uploadsDir = File(this@ChatServerManager.context.filesDir, "uploads")
                                val file = File(uploadsDir, filename)
                                if (file.exists()) {
                                    val extension = file.extension.lowercase()
                                    val contentType = when (extension) {
                                        "jpg", "jpeg" -> ContentType.Image.JPEG
                                        "png" -> ContentType.Image.PNG
                                        "gif" -> ContentType.Image.GIF
                                        "webp" -> ContentType.parse("image/webp")
                                        "mp4" -> ContentType.Video.MP4
                                        "pdf" -> ContentType.Application.Pdf
                                        else -> ContentType.Application.OctetStream
                                    }
                                    call.respondFile(file)
                                } else {
                                    call.respondText("File not found", status = HttpStatusCode.NotFound)
                                }
                            } else {
                                call.respondText("Invalid filename", status = HttpStatusCode.BadRequest)
                            }
                        }

                        // Handle Attachment Uploads
                        post("/upload") {
                            try {
                                val multipart = call.receiveMultipart()
                                var sender = "Anonymous"
                                var timestamp = System.currentTimeMillis()
                                var fileType = "file"
                                var fileName = ""
                                var fileBytes: ByteArray? = null

                                multipart.forEachPart { part ->
                                    when (part) {
                                        is PartData.FormItem -> {
                                            when (part.name) {
                                                "sender" -> sender = part.value
                                                "timestamp" -> timestamp = part.value.toLongOrNull() ?: System.currentTimeMillis()
                                                "fileType" -> fileType = part.value
                                            }
                                        }
                                        is PartData.FileItem -> {
                                            fileName = part.originalFileName ?: "file"
                                            fileBytes = part.streamProvider().readBytes()
                                        }
                                        else -> {}
                                    }
                                    part.dispose()
                                }

                                if (fileBytes != null) {
                                    val uploadsDir = File(this@ChatServerManager.context.filesDir, "uploads")
                                    if (!uploadsDir.exists()) {
                                        uploadsDir.mkdirs()
                                    }

                                    val uniqueName = "${System.currentTimeMillis()}_$fileName"
                                    val destFile = File(uploadsDir, uniqueName)
                                    destFile.writeBytes(fileBytes!!)

                                    val fileUrl = "/uploads/$uniqueName"

                                    // Store into database
                                    val message = ChatMessage(
                                        sender = sender,
                                        text = "",
                                        timestamp = timestamp,
                                        fileName = fileName,
                                        fileUrl = fileUrl,
                                        fileType = fileType
                                    )
                                    val savedMessage = repository.insertMessage(message)

                                    // Broadcast file message in real-time
                                    broadcast(savedMessage.toJsonString())

                                    call.respondText(
                                        text = savedMessage.toJsonString(),
                                        contentType = ContentType.Application.Json
                                    )
                                } else {
                                    call.respondText("No file received", status = HttpStatusCode.BadRequest)
                                }
                            } catch (e: Exception) {
                                Log.e(tag, "Upload error", e)
                                call.respondText("Upload error: ${e.message}", status = HttpStatusCode.InternalServerError)
                            }
                        }

                        // WebSockets Communication Handler
                        webSocket("/chat-ws") {
                            activeSessions.add(this)
                            try {
                                // Instantly send database history to newly joined user
                                val history = repository.getAllMessages()
                                send(Frame.Text(history.toJsonString()))

                                // Keep reading frames from client
                                for (frame in incoming) {
                                    if (frame is Frame.Text) {
                                        val text = frame.readText()
                                        try {
                                            val json = JSONObject(text)
                                            val type = json.optString("type")

                                            if (type == "CHAT") {
                                                val sender = json.optString("sender", "Anonymous")
                                                val msgText = json.optString("text", "")
                                                val timestamp = json.optLong("timestamp", System.currentTimeMillis())

                                                val message = ChatMessage(
                                                    sender = sender,
                                                    text = msgText,
                                                    timestamp = timestamp
                                                )
                                                val saved = repository.insertMessage(message)
                                                broadcast(saved.toJsonString())

                                            } else if (type == "DELETE") {
                                                val id = json.optInt("id", -1)
                                                if (id != -1) {
                                                    repository.deleteMessage(id)
                                                    broadcast(JSONObject().apply {
                                                        put("type", "DELETE")
                                                        put("id", id)
                                                    }.toString())
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e(tag, "Failed to parse message", e)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.d(tag, "WebSocket session disconnected: ${e.message}")
                            } finally {
                                activeSessions.remove(this)
                            }
                        }
                    }
                }

                server?.start(wait = true)
            } catch (e: Exception) {
                Log.e(tag, "Error running Ktor Server", e)
                currentServerUrl = "Error: ${e.localizedMessage}"
                isRunning = false
                onStatusChanged()
            }
        }

        isRunning = true
        onStatusChanged()
    }

    fun stopServer(onStatusChanged: () -> Unit) {
        if (!isRunning) return
        serverScope.launch {
            try {
                // Disconnect WebSocket sessions
                activeSessions.clear()
                
                server?.stop(1000, 2000)
                server = null
                currentServerUrl = "Offline"
                isRunning = false
                onStatusChanged()
            } catch (e: Exception) {
                Log.e(tag, "Error stopping server", e)
            }
        }
    }

    // Broadcast messages to all active clients
    private fun broadcast(message: String) {
        val sessionsCopy = synchronized(activeSessions) { ArrayList(activeSessions) }
        serverScope.launch {
            for (session in sessionsCopy) {
                try {
                    session.send(Frame.Text(message))
                } catch (e: Exception) {
                    // Ignore, session might be dead, handled in webSocket block
                }
            }
        }
    }

    // Utility: Fetch device local IP Address
    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()
            for (networkInterface in interfaces) {
                val addresses = networkInterface.inetAddresses?.toList() ?: emptyList()
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        val ip = address.hostAddress ?: ""
                        // Prioritize local network subnets typically used for Hotspots/Wi-Fi
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return ip
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e(tag, "IP Address retrieval failure", ex)
        }
        return "127.0.0.1"
    }

    // Fetch all uploaded files in the uploads folder
    fun getUploadedFiles(): List<File> {
        val uploadsDir = File(context.filesDir, "uploads")
        if (uploadsDir.exists() && uploadsDir.isDirectory) {
            return uploadsDir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
        }
        return emptyList()
    }

    // Clear all uploaded files
    fun clearUploadedFiles(): Boolean {
        val uploadsDir = File(context.filesDir, "uploads")
        if (uploadsDir.exists() && uploadsDir.isDirectory) {
            val files = uploadsDir.listFiles() ?: return true
            var allDeleted = true
            for (file in files) {
                if (file.isFile) {
                    val deleted = file.delete()
                    if (!deleted) allDeleted = false
                }
            }
            return allDeleted
        }
        return true
    }
}
