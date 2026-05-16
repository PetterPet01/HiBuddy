package com.example.hibuddy.ui.screens.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hibuddy.ServiceLocator
import com.example.hibuddy.data.remote.dto.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val isLoading: Boolean = false,
    val userCards: List<UserCardResponse> = emptyList(),
    val projectCards: List<ProjectCardResponse> = emptyList(),
    val mode: String = "CONTRIBUTOR",
    val dailyLikesRemaining: Int = 50,
    val dailySuperlikesRemaining: Int = 3,
    val currentCardIndex: Int = 0,
    val error: String? = null,
    val matchedProjectId: String? = null,
    val matchedUserName: String? = null
)

class DiscoverViewModel : ViewModel() {

    private val swipeRepository = ServiceLocator.swipeRepository

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    fun loadCards() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val mode = _uiState.value.mode
            swipeRepository.discoverCards(mode).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userCards = response.userCards,
                        projectCards = response.projectCards,
                        dailyLikesRemaining = response.dailyLikesRemaining,
                        dailySuperlikesRemaining = response.dailySuperlikesRemaining,
                        currentCardIndex = 0
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun switchMode(mode: String) {
        _uiState.value = _uiState.value.copy(mode = mode, currentCardIndex = 0)
        loadCards()
    }

    fun swipe(action: String) {
        val state = _uiState.value
        val cards = if (state.mode == "CONTRIBUTOR") state.projectCards else state.userCards
        val currentIndex = state.currentCardIndex
        if (currentIndex >= cards.size) return

        val card = cards[currentIndex]
        val targetId = if (state.mode == "CONTRIBUTOR") {
            (card as ProjectCardResponse).projectId
        } else {
            (card as UserCardResponse).userId
        }
        val targetType = if (state.mode == "CONTRIBUTOR") "PROJECT" else "USER"

        viewModelScope.launch {
            swipeRepository.swipeAction(SwipeActionRequest(targetType, targetId, action)).fold(
                onSuccess = { response ->
                    val newState = _uiState.value.copy(currentCardIndex = currentIndex + 1)
                    if (response.matched) {
                        _uiState.value = newState.copy(matchedProjectId = response.matchId)
                    } else {
                        _uiState.value = newState
                    }
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            )
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
    fun clearMatch() { _uiState.value = _uiState.value.copy(matchedProjectId = null, matchedUserName = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = DiscoverViewModel() as T
        }
    }
}
