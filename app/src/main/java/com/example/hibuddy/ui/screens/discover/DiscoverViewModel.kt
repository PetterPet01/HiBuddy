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
    val isSwiping: Boolean = false,
    val userCards: List<UserCardResponse> = emptyList(),
    val projectCards: List<ProjectCardResponse> = emptyList(),
    val mode: String = "CONTRIBUTOR",
    val dailyLikesRemaining: Int = 50,
    val dailySuperlikesRemaining: Int = 3,
    val queuedUserCount: Int = 0,
    val queuedProjectCount: Int = 0,
    val currentCardIndex: Int = 0,
    val error: String? = null,
    val matchedProjectId: String? = null,
    val matchedUserName: String? = null
)

class DiscoverViewModel : ViewModel() {

    private val swipeRepository = ServiceLocator.swipeRepository

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()
    private val swipedUserIds = mutableSetOf<String>()
    private val swipedProjectIds = mutableSetOf<String>()
    private val queuedUserIds = mutableSetOf<String>()
    private val queuedProjectIds = mutableSetOf<String>()

    fun loadCards() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val mode = _uiState.value.mode
            swipeRepository.discoverCards(mode).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userCards = response.userCards.filterNot { it.userId in swipedUserIds || it.userId in queuedUserIds },
                        projectCards = response.projectCards.filterNot { it.projectId in swipedProjectIds || it.projectId in queuedProjectIds },
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
        loadQueueSummary()
    }

    fun switchMode(mode: String) {
        _uiState.value = _uiState.value.copy(
            mode = mode,
            currentCardIndex = 0,
            userCards = emptyList(),
            projectCards = emptyList(),
            error = null
        )
        loadCards()
    }

    fun swipe(action: String) {
        val state = _uiState.value
        if (state.isSwiping) return
        val cards = if (state.mode == "CONTRIBUTOR") state.projectCards else state.userCards
        val currentIndex = state.currentCardIndex
        if (currentIndex >= cards.size) return
        if ((action == "LIKE" || action == "SUPER_LIKE") && state.dailyLikesRemaining <= 0) {
            _uiState.value = state.copy(error = "Daily like limit reached")
            return
        }
        if (action == "SUPER_LIKE" && state.dailySuperlikesRemaining <= 0) {
            _uiState.value = state.copy(error = "Daily super like limit reached")
            return
        }

        val card = cards[currentIndex]
        val targetId = if (state.mode == "CONTRIBUTOR") {
            (card as ProjectCardResponse).projectId
        } else {
            (card as UserCardResponse).userId
        }
        val targetType = if (state.mode == "CONTRIBUTOR") "PROJECT" else "USER"
        val nextLikesRemaining = if (action == "LIKE" || action == "SUPER_LIKE") {
            (state.dailyLikesRemaining - 1).coerceAtLeast(0)
        } else {
            state.dailyLikesRemaining
        }
        val nextSuperlikesRemaining = if (action == "SUPER_LIKE") {
            (state.dailySuperlikesRemaining - 1).coerceAtLeast(0)
        } else {
            state.dailySuperlikesRemaining
        }

        _uiState.value = state.copy(
            currentCardIndex = currentIndex + 1,
            dailyLikesRemaining = nextLikesRemaining,
            dailySuperlikesRemaining = nextSuperlikesRemaining,
            isSwiping = true,
            error = null,
        )

        viewModelScope.launch {
            swipeRepository.swipeAction(SwipeActionRequest(targetType, targetId, action)).fold(
                onSuccess = { response ->
                    if (targetType == "PROJECT") {
                        swipedProjectIds.add(targetId)
                    } else {
                        swipedUserIds.add(targetId)
                    }
                    if (response.matched) {
                        _uiState.value = _uiState.value.copy(
                            isSwiping = false,
                            matchedProjectId = response.matchId,
                            error = response.message?.takeIf { it.contains("limit", ignoreCase = true) }
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isSwiping = false,
                            error = response.message?.takeIf { it.contains("limit", ignoreCase = true) }
                        )
                    }
                },
                onFailure = { e ->
                    val currentState = _uiState.value
                    _uiState.value = currentState.copy(
                        isSwiping = false,
                        currentCardIndex = (currentState.currentCardIndex - 1).coerceAtLeast(0),
                        dailyLikesRemaining = state.dailyLikesRemaining,
                        dailySuperlikesRemaining = state.dailySuperlikesRemaining,
                        error = e.message ?: "Swipe failed"
                    )
                }
            )
        }
    }

    fun queueCurrentCard() {
        val state = _uiState.value
        if (state.isSwiping) return
        val cards = if (state.mode == "CONTRIBUTOR") state.projectCards else state.userCards
        val currentIndex = state.currentCardIndex
        if (currentIndex >= cards.size) return

        val targetType = if (state.mode == "CONTRIBUTOR") "PROJECT" else "USER"
        if (targetType == "PROJECT" && state.queuedProjectCount >= 3) {
            _uiState.value = state.copy(error = "Project queue is full")
            return
        }
        if (targetType == "USER" && state.queuedUserCount >= 3) {
            _uiState.value = state.copy(error = "User queue is full")
            return
        }

        val card = cards[currentIndex]
        val targetId = if (targetType == "PROJECT") {
            (card as ProjectCardResponse).projectId
        } else {
            (card as UserCardResponse).userId
        }

        _uiState.value = state.copy(
            currentCardIndex = currentIndex + 1,
            queuedProjectCount = if (targetType == "PROJECT") (state.queuedProjectCount + 1).coerceAtMost(3) else state.queuedProjectCount,
            queuedUserCount = if (targetType == "USER") (state.queuedUserCount + 1).coerceAtMost(3) else state.queuedUserCount,
            isSwiping = true,
            error = null
        )

        viewModelScope.launch {
            swipeRepository.addToQueue(QueueAddRequest(targetType, targetId)).fold(
                onSuccess = {
                    if (targetType == "PROJECT") {
                        queuedProjectIds.add(targetId)
                    } else {
                        queuedUserIds.add(targetId)
                    }
                    _uiState.value = _uiState.value.copy(isSwiping = false)
                    loadQueueSummary()
                },
                onFailure = { e ->
                    val currentState = _uiState.value
                    _uiState.value = currentState.copy(
                        isSwiping = false,
                        currentCardIndex = (currentState.currentCardIndex - 1).coerceAtLeast(0),
                        queuedProjectCount = state.queuedProjectCount,
                        queuedUserCount = state.queuedUserCount,
                        error = e.message ?: "Unable to add to queue"
                    )
                }
            )
        }
    }

    fun loadQueueSummary() {
        viewModelScope.launch {
            swipeRepository.getQueue().fold(
                onSuccess = { response ->
                    queuedUserIds.clear()
                    queuedProjectIds.clear()
                    queuedUserIds.addAll(response.userProfiles.map { it.targetId })
                    queuedProjectIds.addAll(response.projectProfiles.map { it.targetId })
                    _uiState.value = _uiState.value.copy(
                        queuedUserCount = response.userProfiles.size,
                        queuedProjectCount = response.projectProfiles.size,
                        userCards = _uiState.value.userCards.filterNot { it.userId in queuedUserIds },
                        projectCards = _uiState.value.projectCards.filterNot { it.projectId in queuedProjectIds }
                    )
                },
                onFailure = { Unit }
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
