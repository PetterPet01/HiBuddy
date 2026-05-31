package com.example.hibuddy.ui.screens.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hibuddy.ServiceLocator
import com.example.hibuddy.data.remote.dto.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedbackUiState(
    val isLoading: Boolean = false,
    val projectTitle: String = "",
    val members: List<MemberToFeedbackItem> = emptyList(),
    val selectedMemberId: String? = null,
    val selectedMemberName: String = "",
    val feedbackText: String = "",
    val isSubmitting: Boolean = false,
    val receivedFeedbacks: MyFeedbackSummaryResponse? = null,
    val showReceived: Boolean = false,
    val error: String? = null,
    val success: String? = null
)

class FeedbackViewModel : ViewModel() {
    private val repo = ServiceLocator.feedbackRepository
    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    fun load(projectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repo.getMembersToFeedback(projectId).fold(
                onSuccess = { data ->
                    _uiState.update {
                        it.copy(isLoading = false, projectTitle = data.projectTitle, members = data.members)
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
            repo.getMyFeedback(projectId).fold(
                onSuccess = { summary -> _uiState.update { it.copy(receivedFeedbacks = summary) } },
                onFailure = { }
            )
        }
    }

    fun selectMember(member: MemberToFeedbackItem) {
        _uiState.update { it.copy(selectedMemberId = member.userId, selectedMemberName = member.displayName) }
    }

    fun clearSelectedMember() {
        _uiState.update { it.copy(selectedMemberId = null, selectedMemberName = "", feedbackText = "") }
    }

    fun updateFeedbackText(text: String) {
        _uiState.update { it.copy(feedbackText = text) }
    }

    fun submitFeedback(projectId: String) {
        val state = _uiState.value
        val targetId = state.selectedMemberId ?: return
        val text = state.feedbackText.trim()
        if (text.length < 10) {
            _uiState.update { it.copy(error = "Feedback must be at least 10 characters") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            repo.submitFeedback(projectId, targetId, FeedbackCreateRequest(text)).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false, success = "Feedback sent!", selectedMemberId = null, feedbackText = "") }
                    load(projectId)
                },
                onFailure = { e -> _uiState.update { it.copy(isSubmitting = false, error = e.message) } }
            )
        }
    }

    fun toggleShowReceived() {
        _uiState.update { it.copy(showReceived = !it.showReceived) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, success = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = FeedbackViewModel() as T
        }
    }
}
