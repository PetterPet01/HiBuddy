package com.example.hibuddy.data.remote

import com.example.hibuddy.BuildConfig
import com.example.hibuddy.data.remote.dto.ProjectInvitationResponse
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.*
import java.util.concurrent.TimeUnit

class WebSocketManager {
    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var connected = false

    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<WebSocketEvent> = _events

    fun connect(matchId: String, token: String) {
        disconnect()
        connected = false
        val wsUrl = BuildConfig.BASE_URL
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .trimEnd('/')

        val request = Request.Builder()
            .url("$wsUrl/ws/chat/$matchId?token=$token")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                connected = true
                _events.tryEmit(WebSocketEvent.Connected)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = gson.fromJson(text, JsonObject::class.java)
                    val type = json.get("type")?.asString ?: return
                    val data = json.get("data")
                    when (type) {
                        "new_message" -> {
                            val msg = gson.fromJson(data, ChatMessage::class.java)
                            _events.tryEmit(WebSocketEvent.NewMessage(msg))
                        }
                        "message_sent" -> {
                            val msg = gson.fromJson(data, ChatMessage::class.java)
                            _events.tryEmit(WebSocketEvent.MessageSent(msg))
                        }
                        "typing" -> {
                            val userId = json.get("user_id")?.asString ?: return
                            _events.tryEmit(WebSocketEvent.Typing(userId))
                        }
                        "read_receipt" -> {
                            val by = json.get("by")?.asString ?: return
                            _events.tryEmit(WebSocketEvent.ReadReceipt(by))
                        }
                        "error" -> {
                            val dataObject = if (data != null && data.isJsonObject) data.asJsonObject else null
                            val message = json.get("message")?.asString
                                ?: dataObject?.get("message")?.asString
                                ?: "Chat connection error"
                            _events.tryEmit(WebSocketEvent.Error(message))
                        }
                        "notification" -> {
                            _events.tryEmit(WebSocketEvent.Notification(gson.toJson(data)))
                        }
                        "project_invitation" -> {
                            val invitation = gson.fromJson(data, ProjectInvitationResponse::class.java)
                            _events.tryEmit(WebSocketEvent.ProjectInvitation(invitation))
                        }
                    }
                } catch (_: Exception) {}
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                connected = false
                _events.tryEmit(WebSocketEvent.Error(t.message ?: "Connection failed"))
                _events.tryEmit(WebSocketEvent.Closed)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                connected = false
                _events.tryEmit(WebSocketEvent.Closed)
            }
        })
    }

    fun sendMessage(content: String, clientMessageId: String): Boolean {
        val json = gson.toJson(
            mapOf(
                "type" to "message",
                "content" to content,
                "client_message_id" to clientMessageId
            )
        )
        return webSocket?.send(json) == true
    }

    fun sendTyping(): Boolean {
        val json = gson.toJson(mapOf("type" to "typing"))
        return webSocket?.send(json) == true
    }

    fun sendReadReceipt(): Boolean {
        val json = gson.toJson(mapOf("type" to "read"))
        return webSocket?.send(json) == true
    }

    fun disconnect() {
        connected = false
        webSocket?.close(1000, "User left")
        webSocket = null
    }

    fun isConnected(): Boolean = connected
}

data class ChatMessage(
    val id: String = "",
    val chat_id: String = "",
    val sender_id: String = "",
    val content: String = "",
    val is_read: Boolean = false,
    val created_at: String = "",
    val sender_name: String = "",
    val client_message_id: String? = null
)

sealed class WebSocketEvent {
    object Connected : WebSocketEvent()
    object Closed : WebSocketEvent()
    data class NewMessage(val message: ChatMessage) : WebSocketEvent()
    data class MessageSent(val message: ChatMessage) : WebSocketEvent()
    data class Typing(val userId: String) : WebSocketEvent()
    data class ReadReceipt(val by: String) : WebSocketEvent()
    data class Notification(val data: String) : WebSocketEvent()
    data class ProjectInvitation(val invitation: ProjectInvitationResponse) : WebSocketEvent()
    data class Error(val message: String) : WebSocketEvent()
}
