package com.example.hibuddy.ui.screens.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hibuddy.ServiceLocator
import com.example.hibuddy.data.remote.dto.QueueItemResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QueueUiState(
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val userProfiles: List<QueueItemResponse> = emptyList(),
    val projectProfiles: List<QueueItemResponse> = emptyList(),
    val userCapacityRemaining: Int = 3,
    val projectCapacityRemaining: Int = 3,
    val error: String? = null,
    val message: String? = null,
    val matchedProjectId: String? = null
)

class QueueViewModel : ViewModel() {
    private val swipeRepository = ServiceLocator.swipeRepository

    private val _uiState = MutableStateFlow(QueueUiState())
    val uiState: StateFlow<QueueUiState> = _uiState.asStateFlow()

    fun loadQueue() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            swipeRepository.getQueue().fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userProfiles = response.userProfiles,
                        projectProfiles = response.projectProfiles,
                        userCapacityRemaining = response.userCapacityRemaining,
                        projectCapacityRemaining = response.projectCapacityRemaining
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Unable to load queue")
                }
            )
        }
    }

    fun decide(itemId: String, action: String) {
        if (_uiState.value.isActionLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, error = null, message = null)
            swipeRepository.decideQueueItem(itemId, action).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        message = response.message ?: if (action == "PASS") "Skipped" else "Accepted",
                        matchedProjectId = response.matchId
                    )
                    loadQueue()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        error = e.message ?: "Queue action failed"
                    )
                }
            )
        }
    }

    fun remove(itemId: String) {
        if (_uiState.value.isActionLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, error = null, message = null)
            swipeRepository.removeQueueItem(itemId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isActionLoading = false, message = "Removed from queue")
                    loadQueue()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        error = e.message ?: "Unable to remove item"
                    )
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, message = null)
    }

    fun clearMatch() {
        _uiState.value = _uiState.value.copy(matchedProjectId = null)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = QueueViewModel() as T
        }
    }
}
