package com.example.hibuddy.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hibuddy.ServiceLocator
import com.example.hibuddy.data.remote.ChatMessage
import com.example.hibuddy.data.remote.WebSocketEvent
import com.example.hibuddy.data.remote.WebSocketManager
import com.example.hibuddy.data.remote.dto.MessageResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatUiState(
    val isLoading: Boolean = false,
    val messages: List<MessageItem> = emptyList(),
    val matchId: String = "",
    val userName: String = "",
    val currentUserId: String = "",
    val isConnected: Boolean = false,
    val isTyping: Boolean = false,
    val error: String? = null
)

data class MessageItem(
    val id: String,
    val content: String,
    val senderId: String,
    val senderName: String,
    val isFromMe: Boolean,
    val isRead: Boolean,
    val createdAt: String
)

class ChatViewModel : ViewModel() {

    private val chatRepository = ServiceLocator.chatRepository
    private val tokenManager = ServiceLocator.tokenManager
    private val webSocketManager = WebSocketManager()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun initChat(matchId: String, userName: String) {
        _uiState.value = _uiState.value.copy(matchId = matchId, userName = userName, currentUserId = tokenManager.getUserId() ?: "")
        loadMessages(matchId)
        connectWebSocket(matchId)
    }

    private fun loadMessages(matchId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            chatRepository.getMessages(matchId).fold(
                onSuccess = { messages ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        messages = messages.map { it.toMessageItem() }
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    private fun connectWebSocket(matchId: String) {
        val token = tokenManager.getAccessToken() ?: return
        webSocketManager.connect(matchId, token)

        viewModelScope.launch {
            webSocketManager.events.collect { event ->
                when (event) {
                    is WebSocketEvent.Connected -> {
                        _uiState.value = _uiState.value.copy(isConnected = true)
                    }
                    is WebSocketEvent.NewMessage -> {
                        val msg = event.message
                        if (msg.sender_id != _uiState.value.currentUserId) {
                            addMessage(msg.toMessageItem(_uiState.value.currentUserId))
                        }
                    }
                    is WebSocketEvent.MessageSent -> {
                        addMessage(event.message.toMessageItem(_uiState.value.currentUserId))
                    }
                    is WebSocketEvent.Typing -> {
                        if (event.userId != _uiState.value.currentUserId) {
                            _uiState.value = _uiState.value.copy(isTyping = true)
                            kotlinx.coroutines.delay(2000)
                            _uiState.value = _uiState.value.copy(isTyping = false)
                        }
                    }
                    is WebSocketEvent.Closed -> {
                        _uiState.value = _uiState.value.copy(isConnected = false)
                    }
                    is WebSocketEvent.Error -> {
                        _uiState.value = _uiState.value.copy(error = event.message)
                    }
                    is WebSocketEvent.ReadReceipt -> {}
                    is WebSocketEvent.Notification -> {}
                }
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        webSocketManager.sendMessage(content)
    }

    fun sendTyping() {
        webSocketManager.sendTyping()
    }

    private fun addMessage(item: MessageItem) {
        val existing = _uiState.value.messages.toMutableList()
        existing.add(item)
        _uiState.value = _uiState.value.copy(messages = existing)
    }

    fun cleanup() {
        webSocketManager.disconnect()
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
    }

    private fun MessageResponse.toMessageItem(): MessageItem {
        return MessageItem(
            id = id,
            content = content,
            senderId = senderId,
            senderName = "",
            isFromMe = senderId == _uiState.value.currentUserId,
            isRead = isRead,
            createdAt = createdAt
        )
    }

    private fun ChatMessage.toMessageItem(currentUserId: String): MessageItem {
        return MessageItem(
            id = id,
            content = content,
            senderId = sender_id,
            senderName = sender_name,
            isFromMe = sender_id == currentUserId,
            isRead = is_read,
            createdAt = created_at
        )
    }

    companion object {
        fun factory(matchId: String, userName: String): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val vm = ChatViewModel()
                    vm.initChat(matchId, userName)
                    return vm as T
                }
            }
        }
    }
}
