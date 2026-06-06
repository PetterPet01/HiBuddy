package com.example.hibuddy.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hibuddy.ServiceLocator
import com.example.hibuddy.data.remote.dto.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val currentUser: UserResponse? = null,
    val requiresEmailVerification: Boolean = false,
    val pendingEmail: String? = null,
    val verificationSucceeded: Boolean = false
)

class AuthViewModel : ViewModel() {

    private val authRepository = ServiceLocator.authRepository

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isLoggedIn = authRepository.isLoggedIn(),
            requiresEmailVerification =
                authRepository.hasSession() && !authRepository.isEmailVerified(),
            pendingEmail = authRepository.getPendingEmail()
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun register(
        fullName: String, username: String, email: String, dateOfBirth: String,
        password: String, confirmPassword: String, phone: String? = null, agreeTerms: Boolean = true
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.register(
                RegisterRequest(fullName, username, email, dateOfBirth, password, confirmPassword, phone, agreeTerms)
            ).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = response.user.emailVerified,
                        currentUser = response.user,
                        requiresEmailVerification = response.requiresEmailVerification,
                        pendingEmail = response.user.email,
                        verificationSucceeded = false,
                        message = "Registration successful! Please verify your email."
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun login(username: String, password: String, rememberMe: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            authRepository.login(LoginRequest(username, password, rememberMe)).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = response.user.emailVerified,
                        currentUser = response.user,
                        requiresEmailVerification = response.requiresEmailVerification,
                        pendingEmail = response.user.email.takeUnless { response.user.emailVerified },
                        verificationSucceeded = false
                    )
                },
                onFailure = { e ->
                    val errorMessage = when (e) {
                        is HttpException -> {
                            when (e.code()) {
                                403 -> "Tài khoản đã bị khóa"
                                423 -> "Tài khoản bị khóa tạm thời do đăng nhập sai quá nhiều lần"
                                401 -> "Sai tài khoản hoặc mật khẩu"
                                else -> "Đăng nhập thất bại (${e.code()})"
                            }
                        }

                        else -> e.message ?: "Đăng nhập thất bại"
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                }
            )
        }
    }

    fun googleLogin(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.googleLogin(idToken).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        currentUser = response.user
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Google sign-in failed"
                    )
                }
            )
        }
    }

    fun verifyEmail(email: String, code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.verifyEmail(email, code).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        requiresEmailVerification = false,
                        pendingEmail = null,
                        verificationSucceeded = true,
                        message = "Email verified!"
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun resendVerification(email: String? = _uiState.value.pendingEmail) {
        viewModelScope.launch {
            val targetEmail = email?.trim().orEmpty()
            if (targetEmail.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Enter your email address to resend the verification code"
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.resendVerification(targetEmail).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        pendingEmail = targetEmail,
                        message = "Verification code sent"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Could not resend verification code"
                    )
                }
            )
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.forgotPassword(email).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = "If the account exists, a reset code has been sent")
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun resetPassword(email: String, code: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.resetPassword(email, code, newPassword, confirmPassword).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = "Password reset successfully")
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun submitStudentVerification(
        fullName: String, studentEmail: String?, university: String,
        studentId: String, academicYear: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.submitStudentVerification(
                StudentVerificationRequest(fullName, studentEmail, university, studentId, academicYear)
            ).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, message = "Verification submitted")
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState()
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
    fun clearMessage() { _uiState.value = _uiState.value.copy(message = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel() as T
        }
    }
}
