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
    val isLoadingMore: Boolean = false,
    val isSwiping: Boolean = false,
    val userCards: List<UserCardResponse> = emptyList(),
    val projectCards: List<ProjectCardResponse> = emptyList(),
    val mode: String = "CONTRIBUTOR",
    val ownerProjects: List<ProjectResponse> = emptyList(),
    val selectedOwnerProjectId: String? = null,
    val nextCursor: String? = null,
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
    private val projectRepository = ServiceLocator.projectRepository

    private val _uiState = MutableStateFlow(
        DiscoverUiState(
            mode = ServiceLocator.discoverMode
        )
    )
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()
    private val swipedUserIds = mutableSetOf<String>()
    private val swipedProjectIds = mutableSetOf<String>()
    private val queuedUserIds = mutableSetOf<String>()
    private val queuedProjectIds = mutableSetOf<String>()

    fun loadCards() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val mode = _uiState.value.mode
            if (mode == "OWNER" && _uiState.value.selectedOwnerProjectId == null) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                loadOwnerProjects()
                return@launch
            }
            swipeRepository.discoverCards(
                mode = mode,
                projectId = _uiState.value.selectedOwnerProjectId
            ).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        userCards = response.userCards.filterNot { it.userId in swipedUserIds || it.userId in queuedUserIds },
                        projectCards = response.projectCards.filterNot { it.projectId in swipedProjectIds || it.projectId in queuedProjectIds },
                        dailyLikesRemaining = response.dailyLikesRemaining,
                        dailySuperlikesRemaining = response.dailySuperlikesRemaining,
                        nextCursor = response.nextCursor,
                        currentCardIndex = 0
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = e.message
                    )
                }
            )
        }
        loadQueueSummary()
    }

    private fun loadMoreCards() {
        val state = _uiState.value
        val cursor = state.nextCursor ?: return
        if (state.isLoading || state.isLoadingMore) return
        val mode = state.mode
        val projectId = state.selectedOwnerProjectId
        _uiState.value = state.copy(isLoadingMore = true)
        viewModelScope.launch {
            swipeRepository.discoverCards(
                mode = mode,
                cursor = cursor,
                projectId = projectId
            ).fold(
                onSuccess = { response ->
                    val current = _uiState.value
                    if (current.mode != mode || current.selectedOwnerProjectId != projectId) {
                        return@fold
                    }
                    _uiState.value = current.copy(
                        isLoadingMore = false,
                        userCards = (
                            current.userCards + response.userCards.filterNot {
                                it.userId in swipedUserIds || it.userId in queuedUserIds
                            }
                        ).distinctBy { it.userId },
                        projectCards = (
                            current.projectCards + response.projectCards.filterNot {
                                it.projectId in swipedProjectIds || it.projectId in queuedProjectIds
                            }
                        ).distinctBy { it.projectId },
                        nextCursor = response.nextCursor,
                        dailyLikesRemaining = response.dailyLikesRemaining,
                        dailySuperlikesRemaining = response.dailySuperlikesRemaining
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        error = error.message ?: "Could not load more profiles"
                    )
                }
            )
        }
    }

    private fun loadMoreIfNeeded() {
        val state = _uiState.value
        val size = if (state.mode == "CONTRIBUTOR") {
            state.projectCards.size
        } else {
            state.userCards.size
        }
        if (state.currentCardIndex >= size - 3) loadMoreCards()
    }

    fun switchMode(mode: String) {

        ServiceLocator.discoverMode = mode

        _uiState.value = _uiState.value.copy(
            mode = mode,
            currentCardIndex = 0,
            userCards = emptyList(),
            projectCards = emptyList(),
            isLoadingMore = false,
            error = null
        )

        if (mode == "OWNER") {
            loadOwnerProjects()
        } else {
            loadCards()
        }
    }

    private fun loadOwnerProjects() {
        viewModelScope.launch {
            projectRepository.getMyProjects().fold(
                onSuccess = { projects ->
                    val recruiting = projects.filter {
                        it.ownerId == ServiceLocator.authRepository.getUserId() &&
                            it.status == "RECRUITING" &&
                            it.reviewStatus == "APPROVED"
                    }
                    val selected = _uiState.value.selectedOwnerProjectId
                        ?.takeIf { id -> recruiting.any { it.id == id } }
                        ?: recruiting.firstOrNull()?.id
                    _uiState.value = _uiState.value.copy(
                        ownerProjects = recruiting,
                        selectedOwnerProjectId = selected,
                        error = if (recruiting.isEmpty()) {
                            "Create an approved recruiting project before browsing contributors"
                        } else null
                    )
                    if (selected != null) loadCards()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun selectOwnerProject(projectId: String) {
        if (projectId == _uiState.value.selectedOwnerProjectId) return
        swipedUserIds.clear()
        _uiState.value = _uiState.value.copy(
            selectedOwnerProjectId = projectId,
            userCards = emptyList(),
            currentCardIndex = 0,
            nextCursor = null,
            isLoadingMore = false
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
            val roleSlotId = if (targetType == "USER") {
                val userCard = card as UserCardResponse
                state.ownerProjects
                    .firstOrNull { it.id == state.selectedOwnerProjectId }
                    ?.roleSlots
                    ?.firstOrNull { it.roleName == userCard.matchedRole }
                    ?.id
            } else null
            swipeRepository.swipeAction(
                SwipeActionRequest(
                    targetType,
                    targetId,
                    action,
                    contextProjectId = state.selectedOwnerProjectId,
                    contextRoleSlotId = roleSlotId
                )
            ).fold(
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
                    loadMoreIfNeeded()
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
            swipeRepository.addToQueue(
                QueueAddRequest(targetType, targetId, state.selectedOwnerProjectId)
            ).fold(
                onSuccess = {
                    if (targetType == "PROJECT") {
                        queuedProjectIds.add(targetId)
                    } else {
                        queuedUserIds.add(targetId)
                    }
                    _uiState.value = _uiState.value.copy(isSwiping = false)
                    loadQueueSummary()
                    loadMoreIfNeeded()
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
