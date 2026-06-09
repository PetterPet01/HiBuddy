package com.example.hibuddy

import android.content.Context
import com.example.hibuddy.data.local.ChatLocalDataSource
import com.example.hibuddy.data.local.TokenManager
import com.example.hibuddy.data.remote.ApiService
import com.example.hibuddy.data.remote.AuthAuthenticator
import com.example.hibuddy.data.remote.AuthInterceptor
import com.example.hibuddy.data.remote.PresenceWebSocketManager
import com.example.hibuddy.data.repository.*
import com.example.hibuddy.ui.theme.ThemeManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.example.hibuddy.data.repository.AdminRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ServiceLocator {
    private lateinit var appContext: Context
    val tokenManager: TokenManager by lazy { TokenManager(appContext) }
    val chatLocalDataSource: ChatLocalDataSource by lazy { ChatLocalDataSource(appContext) }
    val themeManager: ThemeManager by lazy { ThemeManager(appContext) }

    val authInterceptor: AuthInterceptor by lazy {
        AuthInterceptor { tokenManager.getAccessToken() }
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(AuthAuthenticator(tokenManager, BuildConfig.BASE_URL))
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BASIC
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                    redactHeader("Authorization")
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy { retrofit.create(ApiService::class.java) }
    val presenceWebSocketManager: PresenceWebSocketManager by lazy { PresenceWebSocketManager() }

    val authRepository: AuthRepository by lazy { AuthRepository(apiService, tokenManager) }
    val profileRepository: ProfileRepository by lazy { ProfileRepository(apiService) }
    val projectRepository: ProjectRepository by lazy { ProjectRepository(apiService) }
    val swipeRepository: SwipeRepository by lazy { SwipeRepository(apiService) }
    val taskRepository: TaskRepository by lazy { TaskRepository(apiService) }
    val suggestionRepository: SuggestionRepository by lazy { SuggestionRepository(apiService) }
    val chatRepository: ChatRepository by lazy { ChatRepository(apiService, chatLocalDataSource) }
    val adminRepository: AdminRepository by lazy { AdminRepository(apiService) }
    val feedbackRepository: FeedbackRepository by lazy { FeedbackRepository(apiService) }
    val notificationRepository: NotificationRepository by lazy { NotificationRepository(apiService) }

    var discoverMode: String = "CONTRIBUTOR"
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun registerPushToken(token: String) {
        if (!authRepository.isLoggedIn()) return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                apiService.registerFcmToken(
                    com.example.hibuddy.data.remote.dto.FcmTokenRequest(token)
                )
            }
        }
    }
}
