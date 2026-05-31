package com.example.hibuddy.data.remote

import com.example.hibuddy.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): TokenResponse

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): TokenResponse

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>

    @POST("api/v1/auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): GenericResponse

    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): GenericResponse

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): GenericResponse

    @POST("api/v1/auth/verify-student")
    suspend fun submitStudentVerification(@Body request: StudentVerificationRequest): GenericResponse

    @GET("api/v1/profiles/me")
    suspend fun getMyProfile(): ProfileResponse

    @PUT("api/v1/profiles/me")
    suspend fun updateMyProfile(@Body request: ProfileUpdateRequest): ProfileResponse

    @POST("api/v1/profiles/me/hide")
    suspend fun hideProfile(): GenericResponse

    @POST("api/v1/profiles/me/unhide")
    suspend fun unhideProfile(): GenericResponse

    @POST("api/v1/profiles/me/skills")
    suspend fun addSkill(@Body request: SkillRequest): SkillResponse

    @DELETE("api/v1/profiles/me/skills/{skillId}")
    suspend fun removeSkill(@Path("skillId") skillId: String): GenericResponse

    @POST("api/v1/profiles/me/roles")
    suspend fun addRole(@Body request: RoleRequest): RoleResponse

    @DELETE("api/v1/profiles/me/roles/{roleId}")
    suspend fun removeRole(@Path("roleId") roleId: String): GenericResponse

    @POST("api/v1/profiles/me/interests")
    suspend fun addInterest(@Body request: InterestRequest): InterestResponse

    @DELETE("api/v1/profiles/me/interests/{interestId}")
    suspend fun removeInterest(@Path("interestId") interestId: String): GenericResponse

    @POST("api/v1/profiles/me/completed-courses")
    suspend fun addCompletedCourse(@Body request: CompletedCourseRequest): CompletedCourseResponse

    @POST("api/v1/projects")
    suspend fun createProject(@Body request: CreateProjectRequest): ProjectResponse

    @GET("api/v1/projects")
    suspend fun getMyProjects(): List<ProjectResponse>

    @GET("api/v1/projects/{id}")
    suspend fun getProject(@Path("id") id: String): ProjectResponse

    @POST("api/v1/projects/{id}/close")
    suspend fun closeProject(@Path("id") id: String): GenericResponse

    @POST("api/v1/projects/{projectId}/members")
    suspend fun addMember(
        @Path("projectId") projectId: String,
        @Query("user_id") userId: String,
        @Query("role") role: String,
        @Query("role_slot_id") roleSlotId: String? = null,
        @Query("match_id") matchId: String? = null
    ): GenericResponse

    @GET("api/v1/swipe/discover")
    suspend fun discoverCards(
        @Query("mode") mode: String = "CONTRIBUTOR",
        @Query("limit") limit: Int = 20
    ): DiscoverResponse

    @POST("api/v1/swipe/action")
    suspend fun swipeAction(@Body request: SwipeActionRequest): SwipeActionResponse

    @GET("api/v1/swipe/matches")
    suspend fun getMatches(): List<MatchResponse>

    @POST("api/v1/swipe/matches/{id}/unmatch")
    suspend fun unmatch(@Path("id") id: String): GenericResponse

    @GET("api/v1/swipe/applicants/{projectId}")
    suspend fun getApplicants(@Path("projectId") projectId: String): List<ApplicantResponse>

    @GET("api/v1/swipe/stats")
    suspend fun getSwipeStats(): SwipeStatsResponse

    @POST("api/v1/projects/{projectId}/tasks")
    suspend fun createTask(
        @Path("projectId") projectId: String,
        @Body request: CreateTaskRequest
    ): TaskResponse

    @GET("api/v1/projects/{projectId}/tasks")
    suspend fun getTasks(
        @Path("projectId") projectId: String,
        @Query("status") status: String? = null,
        @Query("assignee_id") assigneeId: String? = null
    ): List<TaskResponse>

    @PATCH("api/v1/tasks/{taskId}/status")
    suspend fun updateTaskStatus(
        @Path("taskId") taskId: String,
        @Body request: TaskStatusUpdateRequest
    ): GenericResponse

    @POST("api/v1/tasks/{taskId}/checkout")
    suspend fun checkoutTask(@Path("taskId") taskId: String): CheckoutResponse

    @POST("api/v1/tasks/{taskId}/confirm-checkout")
    suspend fun confirmCheckout(@Path("taskId") taskId: String): GenericResponse

    @GET("api/v1/projects/{projectId}/dashboard")
    suspend fun getDashboard(@Path("projectId") projectId: String): DashboardResponse

    @POST("api/v1/projects/{projectId}/evaluate/{memberId}")
    suspend fun evaluateMember(
        @Path("projectId") projectId: String,
        @Path("memberId") memberId: String,
        @Body request: EvaluationRequest
    ): EvaluationResponse

    @GET("api/v1/suggestions/courses")
    suspend fun getCourseSuggestions(): List<CourseSuggestionResponse>

    @POST("api/v1/suggestions/courses/{id}/dismiss")
    suspend fun dismissCourse(@Path("id") id: String): GenericResponse

    @POST("api/v1/suggestions/refresh")
    suspend fun refreshSuggestions(): List<CourseSuggestionResponse>

    @GET("api/v1/suggestions/mentors")
    suspend fun getMentorSuggestions(): List<MentorSuggestionResponse>

    @GET("api/v1/chat/inbox")
    suspend fun getInbox(): List<ChatInboxResponse>

    @GET("api/v1/chat/{matchId}/messages")
    suspend fun getMessages(
        @Path("matchId") matchId: String,
        @Query("limit") limit: Int = 50,
        @Query("before") before: String? = null
    ): List<MessageResponse>

    @GET("api/v1/notifications")
    suspend fun getNotifications(@Query("limit") limit: Int = 20): List<NotificationResponse>

    @POST("api/v1/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): GenericResponse

    @GET("api/v1/notifications/unread-count")
    suspend fun getUnreadCount(): UnreadCountResponse

    @POST("api/v1/trust/block")
    suspend fun blockUser(@Body request: UserBlockRequest): GenericResponse

    @POST("api/v1/trust/report")
    suspend fun reportUser(@Body request: ReportRequest): GenericResponse

    @GET("api/v1/admin/student-verifications")
    suspend fun getStudentVerifications(): List<AdminUserResponse>

    @POST("api/v1/admin/student-verifications/{userId}/approve")
    suspend fun approveStudentVerification(
        @Path("userId") userId: String
    ): AdminUserResponse

    @POST("api/v1/admin/student-verifications/{userId}/reject")
    suspend fun rejectStudentVerification(
        @Path("userId") userId: String,
        @Body request: RejectStudentRequest
    ): AdminUserResponse
    @GET("api/v1/admin/users")
    suspend fun getAdminUsers(): List<AdminUserResponse>

    @POST("api/v1/admin/users/{userId}/ban")
    suspend fun banUserByAdmin(
        @Path("userId") userId: String
    ): AdminUserResponse

    @POST("api/v1/admin/users/{userId}/unban")
    suspend fun unbanUserByAdmin(
        @Path("userId") userId: String
    ): AdminUserResponse
    @GET("api/v1/admin/reports")
    suspend fun getAdminReports(): List<AdminReportResponse>

    @POST("api/v1/admin/reports/{reportId}/resolve")
    suspend fun resolveReport(
        @Path("reportId") reportId: String,
        @Body request: ResolveReportRequest
    ): AdminReportResponse
}
