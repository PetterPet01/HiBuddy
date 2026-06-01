package com.example.hibuddy.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hibuddy.ServiceLocator
import com.example.hibuddy.data.remote.dto.UserCardResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserDetailUiState(
    val isLoading: Boolean = false,
    val profile: UserCardResponse? = null,
    val error: String? = null
)

class UserDetailViewModel(private val userId: String) : ViewModel() {
    private val profileRepository = ServiceLocator.profileRepository

    private val _uiState = MutableStateFlow(UserDetailUiState())
    val uiState: StateFlow<UserDetailUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            profileRepository.getUserProfile(userId).fold(
                onSuccess = { profile ->
                    _uiState.value = _uiState.value.copy(isLoading = false, profile = profile)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Unable to load profile")
                }
            )
        }
    }

    companion object {
        fun factory(userId: String): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = UserDetailViewModel(userId) as T
        }
    }
}
