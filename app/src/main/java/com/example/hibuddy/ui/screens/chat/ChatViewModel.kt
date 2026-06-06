package com.example.hibuddy.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hibuddy.ServiceLocator
import com.example.hibuddy.data.local.CachedChatMessage
import com.example.hibuddy.data.local.ChatLocalDataSource
import com.example.hibuddy.data.remote.ChatMessage
import com.example.hibuddy.data.remote.PresenceState
import com.example.hibuddy.data.remote.WebSocketEvent
import com.example.hibuddy.data.remote.WebSocketManager
import com.example.hibuddy.data.remote.dto.MessageResponse
import com.example.hibuddy.data.remote.dto.ProjectInvitationOptionsResponse
import com.example.hibuddy.data.remote.dto.ProjectInvitationResponse
import com.example.hibuddy.data.remote.dto.ReportRequest
import com.example.hibuddy.data.remote.dto.UserBlockRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.min

enum class ChatConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

enum class MessageDeliveryState {
    SENDING,
    SENT,
    READ,
    FAILED
}

data class ChatUiState(
    val isLoading: Boolean = false,
    val isLoadingOlder: Boolean = false,
    val canLoadMore: Boolean = false,
    val messages: List<MessageItem> = emptyList(),
    val matchId: String = "",
    val userName: String = "",
    val targetUserId: String = "",
    val currentUserId: String = "",
    val connectionState: ChatConnectionState = ChatConnectionState.DISCONNECTED,
    val otherUserIsOnline: Boolean = false,
    val otherUserLastSeenAt: String? = null,
    val isTyping: Boolean = false,
    val projectInvitations: List<ProjectInvitationResponse> = emptyList(),
    val invitationOptions: ProjectInvitationOptionsResponse? = null,
    val isInvitationActionLoading: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null,
    val isModerationActionInProgress: Boolean = false
) {
    val isConnected: Boolean
        get() = connectionState == ChatConnectionState.CONNECTED
}

data class MessageItem(
    val id: String,
    val content: String,
    val senderId: String,
    val senderName: String,
    val isFromMe: Boolean,
    val isRead: Boolean,
    val createdAt: String,
    val clientMessageId: String? = null,
    val deliveryState: MessageDeliveryState = MessageDeliveryState.SENT,
    val localSortTime: Long = System.currentTimeMillis()
)

class ChatViewModel : ViewModel() {

    private val chatRepository = ServiceLocator.chatRepository
    private val tokenManager = ServiceLocator.tokenManager
    private val webSocketManager = WebSocketManager()
    private val presenceWebSocketManager = ServiceLocator.presenceWebSocketManager
    private val apiService = ServiceLocator.apiService

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var eventJob: Job? = null
    private var reconnectJob: Job? = null
    private var typingJob: Job? = null
    private val pendingAckJobs = mutableMapOf<String, Job>()
    private val inFlightClientIds = mutableSetOf<String>()
    private var reconnectAttempts = 0
    private var lastTypingAt = 0L
    private var cleanedUp = false

    init {
        viewModelScope.launch {
            presenceWebSocketManager.presence.collect { presenceByUser ->
                val targetUserId = _uiState.value.targetUserId
                if (targetUserId.isBlank()) return@collect
                presenceByUser[targetUserId]?.let(::applyPresence)
            }
        }
    }

    fun initChat(matchId: String, userName: String, targetUserId: String) {
        if (_uiState.value.matchId == matchId && eventJob != null) return

        val initialPresence = presenceWebSocketManager.getPresence(targetUserId)
        cleanedUp = false
        _uiState.value = ChatUiState(
            isLoading = true,
            matchId = matchId,
            userName = userName,
            targetUserId = targetUserId,
            currentUserId = tokenManager.getUserId() ?: "",
            connectionState = ChatConnectionState.CONNECTING,
            otherUserIsOnline = initialPresence?.isOnline == true,
            otherUserLastSeenAt = initialPresence?.lastSeenAt
        )
        presenceWebSocketManager.watchUsers(setOf(targetUserId))

        viewModelScope.launch { loadCachedMessages(matchId) }
        refreshMessages(initial = true)
        loadProjectInvitations()
        loadInvitationOptions()
        connectWebSocket(matchId)
    }

    private fun applyPresence(presence: PresenceState) {
        _uiState.update {
            it.copy(
                otherUserIsOnline = presence.isOnline,
                otherUserLastSeenAt = presence.lastSeenAt
            )
        }
    }

    fun refreshMessages() {
        refreshMessages(initial = false)
    }

    private suspend fun loadCachedMessages(matchId: String) {
        val cachedMessages = chatRepository.getCachedMessages(matchId)
        if (cachedMessages.isNotEmpty()) {
            val currentUserId = _uiState.value.currentUserId
            val fallbackName = _uiState.value.userName
            _uiState.update {
                it.copy(
                    isLoading = false,
                    messages = normalizeMessages(
                        cachedMessages.map { cached -> cached.toMessageItem(currentUserId, fallbackName) }
                    )
                )
            }
        }
    }

    private fun refreshMessages(initial: Boolean) {
        val matchId = _uiState.value.matchId
        if (matchId.isBlank()) return

        viewModelScope.launch {
            if (initial && _uiState.value.messages.isEmpty()) {
                _uiState.update { it.copy(isLoading = true) }
            }

            chatRepository.getMessages(matchId, MESSAGE_PAGE_SIZE).fold(
                onSuccess = { messages ->
                    val currentUserId = _uiState.value.currentUserId
                    val fallbackName = _uiState.value.userName
                    val remoteItems = messages.map { it.toMessageItem(currentUserId, fallbackName) }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            canLoadMore = messages.size >= MESSAGE_PAGE_SIZE,
                            messages = normalizeMessages(it.messages + remoteItems)
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Unable to refresh messages"
                        )
                    }
                }
            )
        }
    }

    fun loadOlderMessages() {
        val state = _uiState.value
        val oldestMessage = state.messages.firstOrNull() ?: return
        if (state.isLoadingOlder || !state.canLoadMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingOlder = true) }
            chatRepository.getMessages(state.matchId, MESSAGE_PAGE_SIZE, oldestMessage.createdAt).fold(
                onSuccess = { messages ->
                    val currentUserId = _uiState.value.currentUserId
                    val fallbackName = _uiState.value.userName
                    val olderItems = messages.map { it.toMessageItem(currentUserId, fallbackName) }
                    _uiState.update {
                        it.copy(
                            isLoadingOlder = false,
                            canLoadMore = messages.size >= MESSAGE_PAGE_SIZE,
                            messages = normalizeMessages(olderItems + it.messages)
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoadingOlder = false,
                            error = e.message ?: "Unable to load earlier messages"
                        )
                    }
                }
            )
        }
    }

    private fun connectWebSocket(matchId: String) {
        eventJob?.cancel()
        eventJob = viewModelScope.launch {
            webSocketManager.events.collect { event -> handleWebSocketEvent(event) }
        }
        connectNow(matchId, reconnect = false)
    }

    private fun connectNow(matchId: String = _uiState.value.matchId, reconnect: Boolean) {
        if (cleanedUp || matchId.isBlank()) return

        val token = tokenManager.getAccessToken()
        if (token.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    connectionState = ChatConnectionState.DISCONNECTED,
                    error = "Please sign in again to use chat"
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                connectionState = if (reconnect) {
                    ChatConnectionState.RECONNECTING
                } else {
                    ChatConnectionState.CONNECTING
                }
            )
        }
        webSocketManager.connect(matchId, token)
    }

    private fun handleWebSocketEvent(event: WebSocketEvent) {
        when (event) {
            is WebSocketEvent.Connected -> {
                reconnectAttempts = 0
                reconnectJob?.cancel()
                _uiState.update { it.copy(connectionState = ChatConnectionState.CONNECTED) }
                flushPendingMessages()
            }

            is WebSocketEvent.NewMessage -> {
                if (event.message.sender_id != _uiState.value.currentUserId) {
                    addOrReplaceMessage(event.message.toMessageItem(_uiState.value.currentUserId))
                    webSocketManager.sendReadReceipt()
                }
            }

            is WebSocketEvent.MessageSent -> {
                acknowledgeMessage(event.message)
            }

            is WebSocketEvent.Typing -> {
                if (event.userId != _uiState.value.currentUserId) {
                    typingJob?.cancel()
                    _uiState.update { it.copy(isTyping = true) }
                    typingJob = viewModelScope.launch {
                        delay(TYPING_VISIBLE_MS)
                        _uiState.update { it.copy(isTyping = false) }
                    }
                }
            }

            is WebSocketEvent.ReadReceipt -> {
                if (event.by != _uiState.value.currentUserId) {
                    markOutgoingMessagesRead()
                }
            }

            is WebSocketEvent.Closed -> {
                if (!cleanedUp) {
                    _uiState.update { it.copy(connectionState = ChatConnectionState.RECONNECTING) }
                    scheduleReconnect()
                }
            }

            is WebSocketEvent.Error -> {
                if (!cleanedUp) {
                    _uiState.update {
                        it.copy(
                            connectionState = ChatConnectionState.RECONNECTING,
                            error = event.message
                        )
                    }
                    scheduleReconnect()
                }
            }

            is WebSocketEvent.Notification -> Unit
            is WebSocketEvent.ProjectInvitation -> {
                upsertInvitation(event.invitation)
                loadInvitationOptions()
            }
        }
    }

    private fun scheduleReconnect() {
        if (cleanedUp) return
        reconnectJob?.cancel()
        val delayMs = min(MAX_RECONNECT_DELAY_MS, BASE_RECONNECT_DELAY_MS * (1 shl min(reconnectAttempts, 4)))
        reconnectAttempts += 1
        reconnectJob = viewModelScope.launch {
            delay(delayMs)
            connectNow(reconnect = true)
        }
    }

    fun sendMessage(content: String) {
        val trimmed = content.trim()
        if (trimmed.isBlank()) return

        if (trimmed.length > MAX_MESSAGE_LENGTH) {
            _uiState.update { it.copy(error = "Messages are limited to $MAX_MESSAGE_LENGTH characters") }
            return
        }

        val currentUserId = _uiState.value.currentUserId
        if (currentUserId.isBlank()) {
            _uiState.update { it.copy(error = "Please sign in again to send messages") }
            return
        }

        val clientMessageId = "local-${UUID.randomUUID()}"
        val pendingMessage = MessageItem(
            id = clientMessageId,
            content = trimmed,
            senderId = currentUserId,
            senderName = "",
            isFromMe = true,
            isRead = false,
            createdAt = nowIsoString(),
            clientMessageId = clientMessageId,
            deliveryState = if (_uiState.value.isConnected) {
                MessageDeliveryState.SENDING
            } else {
                MessageDeliveryState.FAILED
            },
            localSortTime = System.currentTimeMillis()
        )

        addOrReplaceMessage(pendingMessage)

        if (_uiState.value.isConnected) {
            sendPendingMessage(pendingMessage)
        } else {
            _uiState.update { it.copy(error = "Chat is offline. Tap the failed message to retry.") }
            scheduleReconnect()
        }
    }

    fun retryMessage(messageId: String) {
        val message = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        if (!message.isFromMe || message.deliveryState != MessageDeliveryState.FAILED) return

        val clientMessageId = message.clientMessageId ?: message.id
        val retrying = message.copy(
            clientMessageId = clientMessageId,
            deliveryState = MessageDeliveryState.SENDING,
            localSortTime = System.currentTimeMillis()
        )
        addOrReplaceMessage(retrying)
        viewModelScope.launch { chatRepository.markMessageSending(message.id, clientMessageId) }

        if (_uiState.value.isConnected) {
            sendPendingMessage(retrying)
        } else {
            scheduleReconnect()
        }
    }

    private fun flushPendingMessages() {
        _uiState.value.messages
            .filter { it.isFromMe && it.deliveryState == MessageDeliveryState.SENDING }
            .forEach { sendPendingMessage(it) }
    }

    private fun sendPendingMessage(message: MessageItem) {
        val clientMessageId = message.clientMessageId ?: message.id
        if (!inFlightClientIds.add(clientMessageId)) return

        val accepted = webSocketManager.sendMessage(message.content, clientMessageId)
        if (accepted) {
            startAckTimeout(message.id, clientMessageId)
        } else {
            inFlightClientIds.remove(clientMessageId)
            markMessageFailed(message.id, "Unable to send. Tap the message to retry.")
        }
    }

    private fun startAckTimeout(messageId: String, clientMessageId: String) {
        pendingAckJobs[clientMessageId]?.cancel()
        pendingAckJobs[clientMessageId] = viewModelScope.launch {
            delay(SEND_ACK_TIMEOUT_MS)
            val stillPending = _uiState.value.messages.any {
                it.id == messageId && it.deliveryState == MessageDeliveryState.SENDING
            }
            if (stillPending) {
                inFlightClientIds.remove(clientMessageId)
                markMessageFailed(messageId, "Message was not confirmed. Tap to retry.")
            }
        }
    }

    private fun acknowledgeMessage(message: ChatMessage) {
        val clientMessageId = message.client_message_id
        if (clientMessageId != null) {
            pendingAckJobs.remove(clientMessageId)?.cancel()
            inFlightClientIds.remove(clientMessageId)
        }

        val acknowledged = message.toMessageItem(_uiState.value.currentUserId)
        _uiState.update { state ->
            state.copy(
                messages = normalizeMessages(
                    state.messages.filterNot {
                        it.id == acknowledged.id || (clientMessageId != null && it.clientMessageId == clientMessageId)
                    } + acknowledged
                )
            )
        }

        viewModelScope.launch {
            if (clientMessageId != null) {
                chatRepository.deletePendingByClientId(_uiState.value.matchId, clientMessageId)
            }
            chatRepository.cacheMessage(acknowledged.toCachedMessage(_uiState.value.matchId))
        }
    }

    private fun addOrReplaceMessage(message: MessageItem) {
        _uiState.update { state ->
            state.copy(messages = normalizeMessages(state.messages.filterNot { it.id == message.id } + message))
        }
        viewModelScope.launch {
            chatRepository.cacheMessage(message.toCachedMessage(_uiState.value.matchId))
        }
    }

    private fun markMessageFailed(messageId: String, error: String) {
        _uiState.update { state ->
            state.copy(
                error = error,
                messages = state.messages.map {
                    if (it.id == messageId) it.copy(deliveryState = MessageDeliveryState.FAILED) else it
                }
            )
        }
        viewModelScope.launch { chatRepository.markMessageFailed(messageId) }
    }

    private fun markOutgoingMessagesRead() {
        val matchId = _uiState.value.matchId
        val currentUserId = _uiState.value.currentUserId
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map {
                    if (it.isFromMe && it.deliveryState != MessageDeliveryState.FAILED) {
                        it.copy(isRead = true, deliveryState = MessageDeliveryState.READ)
                    } else {
                        it
                    }
                }
            )
        }
        viewModelScope.launch { chatRepository.markOutgoingRead(matchId, currentUserId) }
    }

    fun sendTyping() {
        val now = System.currentTimeMillis()
        if (!_uiState.value.isConnected || now - lastTypingAt < TYPING_THROTTLE_MS) return
        lastTypingAt = now
        webSocketManager.sendTyping()
    }

    fun loadInvitationOptions() {
        val matchId = _uiState.value.matchId
        if (matchId.isBlank()) return
        viewModelScope.launch {
            chatRepository.getProjectInvitationOptions(matchId).fold(
                onSuccess = { options ->
                    _uiState.update { it.copy(invitationOptions = options) }
                },
                onFailure = { }
            )
        }
    }

    fun loadProjectInvitations() {
        val matchId = _uiState.value.matchId
        if (matchId.isBlank()) return
        viewModelScope.launch {
            chatRepository.getProjectInvitations(matchId).fold(
                onSuccess = { invitations ->
                    _uiState.update {
                        it.copy(projectInvitations = invitations.map(::normalizeInvitation).sortedByDescending { invite -> invite.createdAt })
                    }
                },
                onFailure = { }
            )
        }
    }

    fun createProjectInvitation(roleSlotId: String, message: String? = null) {
        val matchId = _uiState.value.matchId
        if (matchId.isBlank() || roleSlotId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isInvitationActionLoading = true, error = null) }
            chatRepository.createProjectInvitation(matchId, roleSlotId, message?.takeIf { it.isNotBlank() }).fold(
                onSuccess = { invitation ->
                    upsertInvitation(invitation)
                    _uiState.update {
                        it.copy(
                            isInvitationActionLoading = false,
                            actionMessage = "Invitation sent"
                        )
                    }
                    loadInvitationOptions()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isInvitationActionLoading = false,
                            error = e.message ?: "Unable to send invitation"
                        )
                    }
                }
            )
        }
    }

    fun acceptProjectInvitation(invitationId: String) {
        if (invitationId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isInvitationActionLoading = true, error = null) }
            chatRepository.acceptProjectInvitation(invitationId).fold(
                onSuccess = { invitation ->
                    upsertInvitation(invitation)
                    _uiState.update {
                        it.copy(
                            isInvitationActionLoading = false,
                            actionMessage = "You joined the project"
                        )
                    }
                    loadInvitationOptions()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isInvitationActionLoading = false,
                            error = e.message ?: "Unable to accept invitation"
                        )
                    }
                }
            )
        }
    }

    fun declineProjectInvitation(invitationId: String) {
        if (invitationId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isInvitationActionLoading = true, error = null) }
            chatRepository.declineProjectInvitation(invitationId).fold(
                onSuccess = { invitation ->
                    upsertInvitation(invitation)
                    _uiState.update {
                        it.copy(
                            isInvitationActionLoading = false,
                            actionMessage = "Invitation declined"
                        )
                    }
                    loadInvitationOptions()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isInvitationActionLoading = false,
                            error = e.message ?: "Unable to decline invitation"
                        )
                    }
                }
            )
        }
    }

    private fun upsertInvitation(invitation: ProjectInvitationResponse) {
        val normalized = normalizeInvitation(invitation)
        _uiState.update { state ->
            state.copy(
                projectInvitations = (state.projectInvitations.filterNot { it.id == normalized.id } + normalized)
                    .sortedByDescending { it.createdAt }
            )
        }
    }

    private fun normalizeInvitation(invitation: ProjectInvitationResponse): ProjectInvitationResponse {
        val currentUserId = _uiState.value.currentUserId
        return invitation.copy(
            isIncoming = invitation.inviteeId == currentUserId,
            isOutgoing = invitation.inviterId == currentUserId
        )
    }

    fun cleanup() {
        cleanedUp = true
        reconnectJob?.cancel()
        typingJob?.cancel()
        pendingAckJobs.values.forEach { it.cancel() }
        pendingAckJobs.clear()
        inFlightClientIds.clear()
        webSocketManager.disconnect()
    }

    fun blockUser(userId: String, reason: String? = null) {
        if (userId.isBlank()) {
            _uiState.update { it.copy(error = "Unable to identify this user") }
            return
        }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isModerationActionInProgress = true) }
                apiService.blockUser(UserBlockRequest(userId, reason))
                _uiState.update {
                    it.copy(
                        isModerationActionInProgress = false,
                        actionMessage = "User blocked successfully"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isModerationActionInProgress = false,
                        error = e.message ?: "Failed to block user"
                    )
                }
            }
        }
    }

    fun reportUser(userId: String, reason: String, description: String? = null) {
        if (userId.isBlank()) {
            _uiState.update { it.copy(error = "Unable to identify this user") }
            return
        }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isModerationActionInProgress = true) }
                apiService.reportUser(
                    ReportRequest(
                        reported_id = userId,
                        reason = reason,
                        description = description,
                        contextType = "CHAT",
                        contextId = _uiState.value.matchId
                    )
                )
                _uiState.update {
                    it.copy(
                        isModerationActionInProgress = false,
                        actionMessage = "Report submitted"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isModerationActionInProgress = false,
                        error = e.message ?: "Failed to report user"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }

    private fun MessageResponse.toMessageItem(currentUserId: String, fallbackName: String): MessageItem {
        val fromMe = senderId == currentUserId
        return MessageItem(
            id = id,
            content = content,
            senderId = senderId,
            senderName = if (fromMe) "" else (senderName ?: fallbackName),
            isFromMe = fromMe,
            isRead = isRead,
            createdAt = createdAt,
            deliveryState = if (fromMe && isRead) MessageDeliveryState.READ else MessageDeliveryState.SENT,
            localSortTime = createdAt.toSortTime()
        )
    }

    private fun ChatMessage.toMessageItem(currentUserId: String): MessageItem {
        val fromMe = sender_id == currentUserId
        return MessageItem(
            id = id,
            content = content,
            senderId = sender_id,
            senderName = if (fromMe) "" else sender_name,
            isFromMe = fromMe,
            isRead = is_read,
            createdAt = created_at,
            clientMessageId = client_message_id,
            deliveryState = if (fromMe && is_read) MessageDeliveryState.READ else MessageDeliveryState.SENT,
            localSortTime = created_at.toSortTime()
        )
    }

    private fun CachedChatMessage.toMessageItem(currentUserId: String, fallbackName: String): MessageItem {
        val fromMe = senderId == currentUserId
        val state = deliveryState.toDeliveryState()
        return MessageItem(
            id = id,
            content = content,
            senderId = senderId,
            senderName = if (fromMe) "" else (senderName ?: fallbackName),
            isFromMe = fromMe,
            isRead = isRead || state == MessageDeliveryState.READ,
            createdAt = createdAt,
            clientMessageId = clientMessageId,
            deliveryState = state,
            localSortTime = localSortTime
        )
    }

    private fun MessageItem.toCachedMessage(matchId: String): CachedChatMessage {
        return CachedChatMessage(
            id = id,
            matchId = matchId,
            chatId = null,
            senderId = senderId,
            senderName = senderName.takeIf { it.isNotBlank() },
            content = content,
            isRead = isRead,
            createdAt = createdAt,
            clientMessageId = clientMessageId,
            deliveryState = deliveryState.cacheValue(),
            localSortTime = localSortTime
        )
    }

    private fun MessageDeliveryState.cacheValue(): String {
        return when (this) {
            MessageDeliveryState.SENDING -> ChatLocalDataSource.DeliveryStateSending
            MessageDeliveryState.SENT -> ChatLocalDataSource.DeliveryStateSent
            MessageDeliveryState.READ -> ChatLocalDataSource.DeliveryStateRead
            MessageDeliveryState.FAILED -> ChatLocalDataSource.DeliveryStateFailed
        }
    }

    private fun String.toDeliveryState(): MessageDeliveryState {
        return when (this) {
            ChatLocalDataSource.DeliveryStateSending -> MessageDeliveryState.SENDING
            ChatLocalDataSource.DeliveryStateRead -> MessageDeliveryState.READ
            ChatLocalDataSource.DeliveryStateFailed -> MessageDeliveryState.FAILED
            else -> MessageDeliveryState.SENT
        }
    }

    private fun normalizeMessages(messages: List<MessageItem>): List<MessageItem> {
        return messages
            .groupBy { it.id }
            .map { (_, duplicates) -> duplicates.maxBy { it.deliveryState.rank } }
            .sortedWith(compareBy<MessageItem> { it.localSortTime }.thenBy { it.id })
    }

    private val MessageDeliveryState.rank: Int
        get() = when (this) {
            MessageDeliveryState.FAILED -> 0
            MessageDeliveryState.SENDING -> 1
            MessageDeliveryState.SENT -> 2
            MessageDeliveryState.READ -> 3
        }

    private fun String.toSortTime(): Long {
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                .parse(take(19))
                ?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun nowIsoString(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).format(Date())
    }

    companion object {
        private const val MESSAGE_PAGE_SIZE = 50
        private const val MAX_MESSAGE_LENGTH = 4000
        private const val SEND_ACK_TIMEOUT_MS = 15_000L
        private const val TYPING_THROTTLE_MS = 1_200L
        private const val TYPING_VISIBLE_MS = 2_400L
        private const val BASE_RECONNECT_DELAY_MS = 1_000L
        private const val MAX_RECONNECT_DELAY_MS = 20_000L

        fun factory(matchId: String, userName: String, targetUserId: String): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val vm = ChatViewModel()
                    vm.initChat(matchId, userName, targetUserId)
                    return vm as T
                }
            }
        }
    }
}
