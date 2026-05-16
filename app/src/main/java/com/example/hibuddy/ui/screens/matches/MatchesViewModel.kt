package com.example.hibuddy.ui.screens.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hibuddy.ServiceLocator
import com.example.hibuddy.data.remote.dto.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MatchesUiState(
    val isLoading: Boolean = false,
    val matches: List<MatchResponse> = emptyList(),
    val chatInbox: List<ChatInboxResponse> = emptyList(),
    val unreadCount: Int = 0,
    val error: String? = null
)

class MatchesViewModel : ViewModel() {

    private val swipeRepository = ServiceLocator.swipeRepository
    private val chatRepository = ServiceLocator.chatRepository

    private val _uiState = MutableStateFlow(MatchesUiState())
    val uiState: StateFlow<MatchesUiState> = _uiState.asStateFlow()

    fun loadMatches() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            swipeRepository.getMatches().fold(
                onSuccess = { matches ->
                    _uiState.value = _uiState.value.copy(isLoading = false, matches = matches)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun loadInbox() {
        viewModelScope.launch {
            chatRepository.getInbox().fold(
                onSuccess = { inbox ->
                    val unread = inbox.count { it.isUnread }
                    _uiState.value = _uiState.value.copy(chatInbox = inbox, unreadCount = unread)
                },
                onFailure = { }
            )
        }
    }

    fun unmatch(matchId: String) {
        viewModelScope.launch {
            swipeRepository.unmatch(matchId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        matches = _uiState.value.matches.filter { it.id != matchId }
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            )
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MatchesViewModel() as T
        }
    }
}
