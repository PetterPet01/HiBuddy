package com.example.hibuddy.data.repository

import com.example.hibuddy.data.local.TokenManager
import com.example.hibuddy.data.remote.ApiService
import com.example.hibuddy.data.remote.dto.*
import kotlinx.coroutines.flow.StateFlow
import retrofit2.HttpException

class AuthRepository(
    private val api: ApiService,
    private val tokenManager: TokenManager
) {
    val authState: StateFlow<Boolean> = tokenManager.isLoggedIn

    suspend fun register(request: RegisterRequest): Result<TokenResponse> = runCatching {
        val response = api.register(request)
        tokenManager.saveTokens(response.accessToken, response.refreshToken)
        tokenManager.saveUserId(response.user.id)
        tokenManager.saveUserRole(response.user.role)
        response
    }

    suspend fun login(request: LoginRequest): Result<TokenResponse> = runCatching {
        val response = api.login(request)
        tokenManager.saveTokens(response.accessToken, response.refreshToken)
        tokenManager.saveUserId(response.user.id)
        tokenManager.saveUserRole(response.user.role)
        response
    }

    suspend fun refreshToken(): Result<TokenResponse> {
        val rt = tokenManager.getRefreshToken()
        if (rt.isNullOrBlank()) {
            tokenManager.clearTokens()
            return Result.failure(Exception("No refresh token"))
        }
        return runCatching {
            val response = api.refreshToken(RefreshTokenRequest(rt))
            tokenManager.saveTokens(response.accessToken, response.refreshToken)
            tokenManager.saveUserId(response.user.id)
            response
        }.onFailure { error ->
            if (error is HttpException && (error.code() == 401 || error.code() == 403)) {
                tokenManager.clearTokens()
            }
        }
    }

    suspend fun logout() {
        try { api.logout() } catch (_: Exception) {}
        tokenManager.clearTokens()
    }

    suspend fun verifyEmail(code: String): Result<GenericResponse> = runCatching {
        api.verifyEmail(VerifyEmailRequest(code))
    }

    suspend fun forgotPassword(email: String?): Result<GenericResponse> = runCatching {
        api.forgotPassword(ForgotPasswordRequest(email = email))
    }

    suspend fun resetPassword(code: String, newPassword: String, confirmPassword: String): Result<GenericResponse> = runCatching {
        api.resetPassword(ResetPasswordRequest(code, newPassword, confirmPassword))
    }

    fun getAccessToken(): String? = tokenManager.getAccessToken()
    fun getUserId(): String? = tokenManager.getUserId()
    fun isLoggedIn(): Boolean = authState.value

    suspend fun submitStudentVerification(request: StudentVerificationRequest): Result<GenericResponse> = runCatching {
        api.submitStudentVerification(request)
    }
    fun getUserRole(): String? = tokenManager.getUserRole()
    fun isAdmin(): Boolean = tokenManager.isAdmin()
}
