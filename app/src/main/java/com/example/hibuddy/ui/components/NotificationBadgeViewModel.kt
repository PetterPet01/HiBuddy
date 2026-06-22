package com.example.hibuddy.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hibuddy.ServiceLocator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel that polls the unread-notification count and exposes it as a
 * [StateFlow]. Used by the [NotificationIconWithBadge] overlay in the bottom
 * navigation bar so the badge stays current without a dedicated websocket
 * channel.
 */
class NotificationBadgeViewModel(
    private val pollIntervalMs: Long = 30_000L
) : ViewModel() {

    private val repo = ServiceLocator.notificationRepository

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        refresh()
        startPolling()
    }

    fun refresh() {
        viewModelScope.launch {
            if (!ServiceLocator.authRepository.isLoggedIn()) {
                _unreadCount.value = 0
                return@launch
            }
            repo.getUnreadCount().fold(
                onSuccess = { _unreadCount.value = it.count },
                onFailure = { _unreadCount.value = 0 }
            )
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                delay(pollIntervalMs)
                refresh()
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                NotificationBadgeViewModel() as T
        }
    }
}
