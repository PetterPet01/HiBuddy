package com.example.hibuddy.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hibuddy.ServiceLocator
import com.example.hibuddy.data.remote.dto.AdminUserResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserManagementUiState(
    val isLoading: Boolean = false,
    val users: List<AdminUserResponse> = emptyList(),
    val error: String? = null,
    val message: String? = null
)

class UserManagementViewModel : ViewModel() {

    private val adminRepository = ServiceLocator.adminRepository

    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            adminRepository.getUsers().fold(
                onSuccess = { users ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        users = users
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load users"
                    )
                }
            )
        }
    }

    fun banUser(userId: String, reason: String) {
        viewModelScope.launch {
            adminRepository.banUser(userId, reason).fold(
                onSuccess = { updatedUser ->
                    _uiState.value = _uiState.value.copy(
                        message = "User banned successfully",
                        users = _uiState.value.users.map {
                            if (it.id == userId) updatedUser else it
                        }
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to ban user"
                    )
                }
            )
        }
    }

    fun unbanUser(userId: String, reason: String = "Administrative reinstatement") {
        viewModelScope.launch {
            adminRepository.unbanUser(userId, reason).fold(
                onSuccess = { updatedUser ->
                    _uiState.value = _uiState.value.copy(
                        message = "User unbanned successfully",
                        users = _uiState.value.users.map {
                            if (it.id == userId) updatedUser else it
                        }
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to unban user"
                    )
                }
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                UserManagementViewModel() as T
        }
    }
}
