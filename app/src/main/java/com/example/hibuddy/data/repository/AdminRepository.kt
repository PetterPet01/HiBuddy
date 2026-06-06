package com.example.hibuddy.data.repository

import com.example.hibuddy.data.remote.ApiService
import com.example.hibuddy.data.remote.dto.AdminReportResponse
import com.example.hibuddy.data.remote.dto.AdminUserResponse
import com.example.hibuddy.data.remote.dto.RejectStudentRequest
import com.example.hibuddy.data.remote.dto.ResolveReportRequest

class AdminRepository(private val api: ApiService) {

    suspend fun getStudentVerifications(): Result<List<AdminUserResponse>> =
        apiResult { api.getStudentVerifications() }

    suspend fun approveStudentVerification(userId: String): Result<AdminUserResponse> =
        apiResult { api.approveStudentVerification(userId) }

    suspend fun rejectStudentVerification(userId: String, reason: String): Result<AdminUserResponse> =
        apiResult { api.rejectStudentVerification(userId, RejectStudentRequest(reason)) }

    suspend fun getUsers(): Result<List<AdminUserResponse>> =
        apiResult { api.getAdminUsers() }

    suspend fun banUser(userId: String, reason: String): Result<AdminUserResponse> =
        apiResult { api.banUserByAdmin(userId, com.example.hibuddy.data.remote.dto.AdminActionRequest(reason)) }

    suspend fun unbanUser(userId: String, reason: String): Result<AdminUserResponse> =
        apiResult { api.unbanUserByAdmin(userId, com.example.hibuddy.data.remote.dto.AdminActionRequest(reason)) }

    suspend fun getReports(): Result<List<AdminReportResponse>> =
        apiResult { api.getAdminReports() }

    suspend fun resolveReport(reportId: String, action: String, reason: String): Result<AdminReportResponse> =
        apiResult { api.resolveReport(reportId, ResolveReportRequest(action, reason)) }

    suspend fun getFlaggedProjects() = apiResult { api.getFlaggedProjects() }
    suspend fun approveProject(projectId: String, reason: String) =
        apiResult { api.approveFlaggedProject(projectId, com.example.hibuddy.data.remote.dto.AdminActionRequest(reason)) }
    suspend fun rejectProject(projectId: String, reason: String) =
        apiResult { api.rejectFlaggedProject(projectId, com.example.hibuddy.data.remote.dto.AdminActionRequest(reason)) }
}
