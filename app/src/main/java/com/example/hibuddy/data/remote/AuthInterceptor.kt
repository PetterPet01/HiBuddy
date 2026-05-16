package com.example.hibuddy.data.remote

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private var tokenProvider: (() -> String?)?
) : Interceptor {

    fun setTokenProvider(provider: () -> String?) {
        tokenProvider = provider
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenProvider?.invoke()

        val request = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        return chain.proceed(request)
    }
}
