package com.example.hibuddy.data.remote

import com.example.hibuddy.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class PresenceState(
    val userId: String,
    val isOnline: Boolean,
    val lastSeenAt: String? = null
)

class PresenceWebSocketManager {
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private val watchedUserIds = linkedSetOf<String>()

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var currentToken: String? = null
    private var reconnectJob: Job? = null

    @Volatile
    private var connected = false

    @Volatile
    private var manuallyDisconnected = true

    private val _presence = MutableStateFlow<Map<String, PresenceState>>(emptyMap())
    val presence: StateFlow<Map<String, PresenceState>> = _presence.asStateFlow()

    fun connect(token: String?) {
        if (token.isNullOrBlank()) return

        synchronized(lock) {
            currentToken = token
            manuallyDisconnected = false
            if (connected || webSocket != null) return
            openSocket(token)
        }
    }

    fun disconnect() {
        synchronized(lock) {
            manuallyDisconnected = true
            reconnectJob?.cancel()
            reconnectJob = null
            connected = false
            currentToken = null
            webSocket?.close(1000, "Presence stopped")
            webSocket = null
        }
    }

    fun watchUsers(userIds: Set<String>) {
        synchronized(lock) {
            watchedUserIds.clear()
            watchedUserIds.addAll(userIds.filter { it.isNotBlank() })
            sendSubscribeLocked()
        }
    }

    fun getPresence(userId: String): PresenceState? = presence.value[userId]

    private fun openSocket(token: String) {
        val wsUrl = BuildConfig.BASE_URL
            .replace("http://", "ws://")
            .replace("https://", "wss://")
            .trimEnd('/')
        val encodedToken = URLEncoder.encode(token, Charsets.UTF_8.name())
        val request = Request.Builder()
            .url("$wsUrl/ws/presence?token=$encodedToken")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                synchronized(lock) {
                    connected = true
                    this@PresenceWebSocketManager.webSocket = webSocket
                    sendSubscribeLocked()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                synchronized(lock) {
                    connected = false
                    if (this@PresenceWebSocketManager.webSocket == webSocket) {
                        this@PresenceWebSocketManager.webSocket = null
                    }
                }
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                synchronized(lock) {
                    connected = false
                    if (this@PresenceWebSocketManager.webSocket == webSocket) {
                        this@PresenceWebSocketManager.webSocket = null
                    }
                }
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        val token = currentToken ?: return
        if (manuallyDisconnected) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(5_000)
            synchronized(lock) {
                if (!manuallyDisconnected && webSocket == null) {
                    openSocket(token)
                }
            }
        }
    }

    private fun sendSubscribeLocked() {
        val socket = webSocket ?: return
        val payload = mapOf(
            "type" to "subscribe",
            "user_ids" to watchedUserIds.toList()
        )
        socket.send(gson.toJson(payload))
    }

    private fun handleMessage(text: String) {
        runCatching {
            val json = gson.fromJson(text, JsonObject::class.java)
            when (json.get("type")?.asString) {
                "presence_snapshot" -> {
                    val users = json.getAsJsonArray("users") ?: return
                    users.forEach { item ->
                        if (item.isJsonObject) {
                            parsePresence(item.asJsonObject)?.let(::applyPresence)
                        }
                    }
                }
                "presence" -> parsePresence(json)?.let(::applyPresence)
            }
        }
    }

    private fun parsePresence(json: JsonObject): PresenceState? {
        val userId = json.get("user_id")?.asString ?: return null
        val lastSeenElement = json.get("last_seen_at")
        return PresenceState(
            userId = userId,
            isOnline = json.get("is_online")?.asBoolean == true,
            lastSeenAt = if (lastSeenElement == null || lastSeenElement.isJsonNull) null else lastSeenElement.asString
        )
    }

    private fun applyPresence(state: PresenceState) {
        _presence.update { current ->
            current + (state.userId to state)
        }
    }
}
