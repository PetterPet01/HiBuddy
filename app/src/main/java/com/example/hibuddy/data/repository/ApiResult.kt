package com.example.hibuddy.data.repository

import com.google.gson.JsonParser
import retrofit2.HttpException

internal suspend fun <T> apiResult(block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (error: Throwable) {
        Result.failure(Exception(error.readableApiMessage()))
    }
}

internal fun Throwable.readableApiMessage(): String {
    if (this is HttpException) {
        val raw = response()?.errorBody()?.string().orEmpty()
        val detail = runCatching {
            val value = JsonParser.parseString(raw).asJsonObject.get("detail")
            when {
                value == null -> null
                value.isJsonPrimitive -> value.asString
                value.isJsonArray -> value.asJsonArray
                    .mapNotNull { item ->
                        item.asJsonObject.get("msg")?.asString
                    }
                    .joinToString("\n")
                else -> null
            }
        }.getOrNull()
        return detail?.takeIf(String::isNotBlank) ?: message()
    }
    return message ?: "Request failed"
}
