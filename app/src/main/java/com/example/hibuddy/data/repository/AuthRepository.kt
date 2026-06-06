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

    suspend fun register(request: RegisterRequest): Result<TokenResponse> = apiResult {
        val response = api.register(request)
        persistSession(response)
        response
    }

    suspend fun login(request: LoginRequest): Result<TokenResponse> = apiResult {
        val response = api.login(request)
        persistSession(response)
        response
    }

    suspend fun refreshToken(): Result<TokenResponse> {
        val rt = tokenManager.getRefreshToken()
        if (rt.isNullOrBlank()) {
            tokenManager.clearTokens()
            return Result.failure(Exception("No refresh token"))
        }
        return apiResult {
            val response = api.refreshToken(RefreshTokenRequest(rt))
            persistSession(response)
            response
        }.onFailure { error ->
            if (error is HttpException && (error.code() == 401 || error.code() == 403)) {
                tokenManager.clearTokens()
            }
        }
    }

    suspend fun logout() {
        tokenManager.getRefreshToken()?.let { token ->
            try { api.logout(RefreshTokenRequest(token)) } catch (_: Exception) {}
        }
        tokenManager.clearTokens()
        com.example.hibuddy.ServiceLocator.chatLocalDataSource.clearAll()
    }

    suspend fun verifyEmail(email: String, code: String): Result<GenericResponse> = apiResult {
        api.verifyEmail(VerifyEmailRequest(email, code)).also {
            tokenManager.saveEmailVerified(true)
        }
    }

    suspend fun resendVerification(email: String? = tokenManager.getPendingEmail()): Result<GenericResponse> = apiResult {
        if (tokenManager.hasSession()) {
            api.resendVerification()
        } else {
            api.resendVerificationPublic(ResendVerificationRequest(email.orEmpty()))
        }
    }

    suspend fun googleLogin(idToken: String): Result<TokenResponse> = apiResult {
        val response = api.googleLogin(GoogleLoginRequest(idToken))
        persistSession(response)
        response
    }

    suspend fun forgotPassword(email: String?): Result<GenericResponse> = apiResult {
        api.forgotPassword(ForgotPasswordRequest(email = email))
    }

    suspend fun resetPassword(email: String, code: String, newPassword: String, confirmPassword: String): Result<GenericResponse> = apiResult {
        api.resetPassword(ResetPasswordRequest(email, code, newPassword, confirmPassword))
    }

    fun getAccessToken(): String? = tokenManager.getAccessToken()
    fun getUserId(): String? = tokenManager.getUserId()
    fun isLoggedIn(): Boolean = authState.value
    fun hasSession(): Boolean = tokenManager.hasSession()

    suspend fun submitStudentVerification(request: StudentVerificationRequest): Result<GenericResponse> = runCatching {
        api.submitStudentVerification(request)
    }
    fun getUserRole(): String? = tokenManager.getUserRole()
    fun isAdmin(): Boolean = tokenManager.isAdmin()
    fun isEmailVerified(): Boolean = tokenManager.isEmailVerified()
    fun getPendingEmail(): String? = tokenManager.getPendingEmail()

    private fun persistSession(response: TokenResponse) {
        tokenManager.saveEmailVerified(response.user.emailVerified)
        tokenManager.savePendingEmail(response.user.email.takeUnless { response.user.emailVerified })
        tokenManager.saveUserId(response.user.id)
        tokenManager.saveUserRole(response.user.role)
        tokenManager.saveTokens(response.accessToken, response.refreshToken)
    }
}
