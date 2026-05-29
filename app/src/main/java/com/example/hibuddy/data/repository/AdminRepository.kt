package com.example.hibuddy.data.repository

import com.example.hibuddy.data.remote.ApiService
import com.example.hibuddy.data.remote.dto.AdminUserResponse
import com.example.hibuddy.data.remote.dto.RejectStudentRequest

class AdminRepository(private val api: ApiService) {

    suspend fun getStudentVerifications(): Result<List<AdminUserResponse>> =
        runCatching { api.getStudentVerifications() }

    suspend fun approveStudentVerification(userId: String): Result<AdminUserResponse> =
        runCatching { api.approveStudentVerification(userId) }

    suspend fun rejectStudentVerification(userId: String, reason: String): Result<AdminUserResponse> =
        runCatching { api.rejectStudentVerification(userId, RejectStudentRequest(reason)) }
    suspend fun getUsers(): Result<List<AdminUserResponse>> =
        runCatching { api.getAdminUsers() }

    suspend fun banUser(userId: String): Result<AdminUserResponse> =
        runCatching { api.banUserByAdmin(userId) }

    suspend fun unbanUser(userId: String): Result<AdminUserResponse> =
        runCatching { api.unbanUserByAdmin(userId) }
}