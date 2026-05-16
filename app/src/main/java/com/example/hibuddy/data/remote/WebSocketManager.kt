package com.example.hibuddy.data.remote

import com.example.hibuddy.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.*

class WebSocketManager {
    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()

    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 0)
    val events: SharedFlow<WebSocketEvent> = _events

    fun connect(matchId: String, token: String) {
        disconnect()
        val wsUrl = BuildConfig.BASE_URL
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .trimEnd('/')

        val request = Request.Builder()
            .url("$wsUrl/ws/chat/$matchId?token=$token")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
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
                        "notification" -> {
                            _events.tryEmit(WebSocketEvent.Notification(gson.toJson(data)))
                        }
                    }
                } catch (_: Exception) {}
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _events.tryEmit(WebSocketEvent.Error(t.message ?: "Connection failed"))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _events.tryEmit(WebSocketEvent.Closed)
            }
        })
    }

    fun sendMessage(content: String) {
        val json = gson.toJson(mapOf("type" to "message", "content" to content))
        webSocket?.send(json)
    }

    fun sendTyping() {
        val json = gson.toJson(mapOf("type" to "typing"))
        webSocket?.send(json)
    }

    fun disconnect() {
        webSocket?.close(1000, "User left")
        webSocket = null
    }
}

data class ChatMessage(
    val id: String = "",
    val chat_id: String = "",
    val sender_id: String = "",
    val content: String = "",
    val is_read: Boolean = false,
    val created_at: String = "",
    val sender_name: String = ""
)

sealed class WebSocketEvent {
    object Connected : WebSocketEvent()
    object Closed : WebSocketEvent()
    data class NewMessage(val message: ChatMessage) : WebSocketEvent()
    data class MessageSent(val message: ChatMessage) : WebSocketEvent()
    data class Typing(val userId: String) : WebSocketEvent()
    data class ReadReceipt(val by: String) : WebSocketEvent()
    data class Notification(val data: String) : WebSocketEvent()
    data class Error(val message: String) : WebSocketEvent()
}
