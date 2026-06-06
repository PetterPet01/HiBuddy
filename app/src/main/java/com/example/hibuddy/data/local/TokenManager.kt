package com.example.hibuddy.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "hibuddy_auth_secure",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    init {
        val legacy = context.getSharedPreferences("hibuddy_auth", Context.MODE_PRIVATE)
        if (!prefs.contains("access_token") && legacy.contains("access_token")) {
            if (legacy.contains("email_verified")) {
                prefs.edit()
                    .putString("access_token", legacy.getString("access_token", null))
                    .putString("refresh_token", legacy.getString("refresh_token", null))
                    .putString("user_id", legacy.getString("user_id", null))
                    .putString("user_role", legacy.getString("user_role", null))
                    .putBoolean(
                        "email_verified",
                        legacy.getBoolean("email_verified", false)
                    )
                    .putString("pending_email", legacy.getString("pending_email", null))
                    .apply()
            }
            legacy.edit().clear().apply()
        }
    }
    private val _isLoggedIn = MutableStateFlow(hasSession() && isEmailVerified())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .apply()
        updateAuthState()
    }

    fun getAccessToken(): String? = prefs.getString("access_token", null)
    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)
    fun hasSession(): Boolean = !getAccessToken().isNullOrBlank()

    fun clearTokens() {
        prefs.edit().clear().apply()
        _isLoggedIn.value = false
    }

    fun saveUserId(userId: String) {
        prefs.edit().putString("user_id", userId).apply()
    }

    fun getUserId(): String? = prefs.getString("user_id", null)

    fun saveUserRole(role: String) {
        prefs.edit().putString("user_role", role).apply()
    }

    fun getUserRole(): String? = prefs.getString("user_role", null)

    fun isAdmin(): Boolean = getUserRole() == "ADMIN"

    fun saveEmailVerified(verified: Boolean) {
        val editor = prefs.edit().putBoolean("email_verified", verified)
        if (verified) {
            editor.remove("pending_email")
        }
        editor.apply()
        updateAuthState()
    }

    fun isEmailVerified(): Boolean = prefs.getBoolean("email_verified", false)

    fun savePendingEmail(email: String?) {
        val editor = prefs.edit()
        if (email.isNullOrBlank()) {
            editor.remove("pending_email")
        } else {
            editor.putString("pending_email", email)
        }
        editor.apply()
    }

    fun getPendingEmail(): String? = prefs.getString("pending_email", null)

    private fun updateAuthState() {
        _isLoggedIn.value = hasSession() && isEmailVerified()
    }
}
