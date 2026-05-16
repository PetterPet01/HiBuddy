package com.example.hibuddy.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hibuddy.ServiceLocator
import com.example.hibuddy.data.remote.dto.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val profile: ProfileResponse? = null,
    val courseSuggestions: List<CourseSuggestionResponse> = emptyList(),
    val mentorSuggestions: List<MentorSuggestionResponse> = emptyList(),
    val error: String? = null,
    val message: String? = null
)

class ProfileViewModel : ViewModel() {

    private val profileRepository = ServiceLocator.profileRepository
    private val suggestionRepository = ServiceLocator.suggestionRepository

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            profileRepository.getMyProfile().fold(
                onSuccess = { profile ->
                    _uiState.value = _uiState.value.copy(isLoading = false, profile = profile)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun loadSuggestions() {
        viewModelScope.launch {
            suggestionRepository.getCourseSuggestions().fold(
                onSuccess = { suggestions ->
                    _uiState.value = _uiState.value.copy(courseSuggestions = suggestions)
                },
                onFailure = { }
            )
        }
    }

    fun loadMentors() {
        viewModelScope.launch {
            suggestionRepository.getMentorSuggestions().fold(
                onSuccess = { mentors ->
                    _uiState.value = _uiState.value.copy(mentorSuggestions = mentors)
                },
                onFailure = { }
            )
        }
    }

    fun updateProfile(displayName: String?, bio: String?, location: String?, mode: String?, portfolioUrl: String?, githubUrl: String?, shortTermGoal: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            profileRepository.updateProfile(
                ProfileUpdateRequest(displayName, bio, location, portfolioUrl, githubUrl, null, shortTermGoal, mode)
            ).fold(
                onSuccess = { profile ->
                    _uiState.value = _uiState.value.copy(isLoading = false, profile = profile, message = "Profile updated")
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun addSkill(skillName: String, level: String, needsImprovement: Boolean = false) {
        viewModelScope.launch {
            profileRepository.addSkill(SkillRequest(skillName, level, needsImprovement)).fold(
                onSuccess = { loadProfile() },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            )
        }
    }

    fun removeSkill(skillId: String) {
        viewModelScope.launch {
            profileRepository.removeSkill(skillId).fold(
                onSuccess = { loadProfile() },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            )
        }
    }

    fun addRole(roleName: String) {
        viewModelScope.launch {
            profileRepository.addRole(RoleRequest(roleName)).fold(
                onSuccess = { loadProfile() },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            )
        }
    }

    fun toggleHideProfile() {
        viewModelScope.launch {
            val isHidden = _uiState.value.profile?.isHidden == true
            val result = if (isHidden) profileRepository.unhideProfile() else profileRepository.hideProfile()
            result.fold(
                onSuccess = { loadProfile() },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            )
        }
    }

    fun dismissCourse(courseId: String) {
        viewModelScope.launch {
            suggestionRepository.dismissCourse(courseId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        courseSuggestions = _uiState.value.courseSuggestions.filter { it.id != courseId }
                    )
                },
                onFailure = { }
            )
        }
    }

    fun refreshSuggestions() {
        viewModelScope.launch {
            suggestionRepository.refreshSuggestions().fold(
                onSuccess = { suggestions ->
                    _uiState.value = _uiState.value.copy(courseSuggestions = suggestions)
                },
                onFailure = { }
            )
        }
    }

    fun addCompletedCourse(title: String, source: String, courseId: String?) {
        viewModelScope.launch {
            profileRepository.addCompletedCourse(CompletedCourseRequest(title, source, courseId)).fold(
                onSuccess = { loadProfile() },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            )
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
    fun clearMessage() { _uiState.value = _uiState.value.copy(message = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ProfileViewModel() as T
        }
    }
}
