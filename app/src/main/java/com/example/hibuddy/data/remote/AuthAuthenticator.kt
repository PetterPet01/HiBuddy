package com.example.hibuddy.data.remote

import com.example.hibuddy.data.local.TokenManager
import com.example.hibuddy.data.remote.dto.TokenResponse
import com.google.gson.Gson
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.util.concurrent.TimeUnit

class AuthAuthenticator(
    private val tokenManager: TokenManager,
    baseUrl: String
) : Authenticator {
    private val gson = Gson()
    private val refreshUrl = "${baseUrl.trimEnd('/')}/api/v1/auth/refresh"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (shouldSkipRefresh(response.request.url.encodedPath)) return null

        if (responseCount(response) >= 2) {
            expireSession()
            return null
        }

        val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

        synchronized(refreshLock) {
            val currentToken = tokenManager.getAccessToken()
            if (!currentToken.isNullOrBlank() && currentToken != requestToken) {
                return response.request.withBearer(currentToken)
            }

            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                expireSession()
                return null
            }

            return when (val result = refreshTokens(refreshToken)) {
                is RefreshResult.Success -> {
                    val tokenResponse = result.response
                    tokenManager.saveEmailVerified(tokenResponse.user.emailVerified)
                    tokenManager.savePendingEmail(
                        tokenResponse.user.email.takeUnless { tokenResponse.user.emailVerified }
                    )
                    tokenManager.saveUserId(tokenResponse.user.id)
                    tokenManager.saveUserRole(tokenResponse.user.role)
                    tokenManager.saveTokens(tokenResponse.accessToken, tokenResponse.refreshToken)
                    response.request.withBearer(tokenResponse.accessToken)
                }
                RefreshResult.Unauthorized -> {
                    expireSession()
                    null
                }
                RefreshResult.Failed -> null
            }
        }
    }

    private fun refreshTokens(refreshToken: String): RefreshResult {
        val body = gson.toJson(mapOf("refresh_token" to refreshToken)).toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(refreshUrl)
            .post(body)
            .build()

        return try {
            refreshClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: return RefreshResult.Failed
                    val tokenResponse = gson.fromJson(responseBody, TokenResponse::class.java)
                    RefreshResult.Success(tokenResponse)
                } else if (response.code == 401 || response.code == 403) {
                    RefreshResult.Unauthorized
                } else {
                    RefreshResult.Failed
                }
            }
        } catch (_: IOException) {
            RefreshResult.Failed
        } catch (_: RuntimeException) {
            RefreshResult.Failed
        }
    }

    private fun expireSession() {
        tokenManager.clearTokens()
    }

    private fun shouldSkipRefresh(path: String): Boolean {
        return path in setOf(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/google",
            "/api/v1/auth/login-swagger",
            "/api/v1/auth/refresh",
            "/api/v1/auth/resend-verification-public",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
        )
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            count++
            priorResponse = priorResponse.priorResponse
        }
        return count
    }

    private fun Request.withBearer(token: String): Request {
        return newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    }

    private sealed interface RefreshResult {
        data class Success(val response: TokenResponse) : RefreshResult
        data object Unauthorized : RefreshResult
        data object Failed : RefreshResult
    }
}
