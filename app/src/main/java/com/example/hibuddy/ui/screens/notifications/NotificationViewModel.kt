package com.example.hibuddy.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hibuddy.ServiceLocator
import com.example.hibuddy.data.remote.dto.NotificationResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationUiState(
    val isLoading: Boolean = false,
    val notifications: List<NotificationResponse> = emptyList(),
    val unreadCount: Int = 0,
    val error: String? = null
)

class NotificationViewModel : ViewModel() {
    private val repo = ServiceLocator.notificationRepository
    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repo.getNotifications().fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        notifications = it,
                        unreadCount = it.count { notification -> !notification.isRead }
                    )
                },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
            )
        }
    }

    fun markRead(id: String) {
        val state = _uiState.value
        val notification = state.notifications.find { it.id == id } ?: return
        if (notification.isRead) return

        _uiState.value = state.copy(
            notifications = state.notifications.map {
                if (it.id == id) it.copy(isRead = true) else it
            },
            unreadCount = (state.unreadCount - 1).coerceAtLeast(0)
        )
        repo.setUnreadCount(_uiState.value.unreadCount)

        viewModelScope.launch {
            repo.markNotificationRead(id).onFailure {
                loadNotifications()
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = NotificationViewModel() as T
        }
    }
}
