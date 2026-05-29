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

data class StudentVerificationUiState(
    val isLoading: Boolean = false,
    val users: List<AdminUserResponse> = emptyList(),
    val error: String? = null,
    val message: String? = null
)

class StudentVerificationViewModel : ViewModel() {

    private val adminRepository = ServiceLocator.adminRepository

    private val _uiState = MutableStateFlow(StudentVerificationUiState())
    val uiState: StateFlow<StudentVerificationUiState> = _uiState.asStateFlow()

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            adminRepository.getStudentVerifications().fold(
                onSuccess = { users ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        users = users
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load student verifications"
                    )
                }
            )
        }
    }

    fun approve(userId: String) {
        viewModelScope.launch {
            adminRepository.approveStudentVerification(userId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        message = "Approved successfully",
                        users = _uiState.value.users.filter { user -> user.id != userId }
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to approve"
                    )
                }
            )
        }
    }

    fun reject(userId: String, reason: String) {
        viewModelScope.launch {
            adminRepository.rejectStudentVerification(userId, reason).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        message = "Rejected successfully",
                        users = _uiState.value.users.filter { user -> user.id != userId }
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Failed to reject"
                    )
                }
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                StudentVerificationViewModel() as T
        }
    }
}