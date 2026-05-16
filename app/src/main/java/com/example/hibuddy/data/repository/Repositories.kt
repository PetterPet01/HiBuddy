package com.example.hibuddy.data.repository

import com.example.hibuddy.data.remote.ApiService
import com.example.hibuddy.data.remote.dto.*

class ProfileRepository(private val api: ApiService) {
    suspend fun getMyProfile(): Result<ProfileResponse> = runCatching { api.getMyProfile() }
    suspend fun updateProfile(request: ProfileUpdateRequest): Result<ProfileResponse> = runCatching { api.updateMyProfile(request) }
    suspend fun hideProfile(): Result<GenericResponse> = runCatching { api.hideProfile() }
    suspend fun unhideProfile(): Result<GenericResponse> = runCatching { api.unhideProfile() }
    suspend fun addSkill(request: SkillRequest): Result<SkillResponse> = runCatching { api.addSkill(request) }
    suspend fun removeSkill(skillId: String): Result<GenericResponse> = runCatching { api.removeSkill(skillId) }
    suspend fun addRole(request: RoleRequest): Result<RoleResponse> = runCatching { api.addRole(request) }
    suspend fun removeRole(roleId: String): Result<GenericResponse> = runCatching { api.removeRole(roleId) }
    suspend fun addInterest(request: InterestRequest): Result<InterestResponse> = runCatching { api.addInterest(request) }
    suspend fun removeInterest(interestId: String): Result<GenericResponse> = runCatching { api.removeInterest(interestId) }
    suspend fun addCompletedCourse(request: CompletedCourseRequest): Result<CompletedCourseResponse> = runCatching { api.addCompletedCourse(request) }
}

class ProjectRepository(private val api: ApiService) {
    suspend fun createProject(request: CreateProjectRequest): Result<ProjectResponse> = runCatching { api.createProject(request) }
    suspend fun getMyProjects(): Result<List<ProjectResponse>> = runCatching { api.getMyProjects() }
    suspend fun getProject(id: String): Result<ProjectResponse> = runCatching { api.getProject(id) }
    suspend fun closeProject(id: String): Result<GenericResponse> = runCatching { api.closeProject(id) }
    suspend fun addMember(projectId: String, userId: String, role: String): Result<GenericResponse> = runCatching { api.addMember(projectId, userId, role) }
}

class SwipeRepository(private val api: ApiService) {
    suspend fun discoverCards(mode: String, limit: Int = 20): Result<DiscoverResponse> = runCatching { api.discoverCards(mode, limit) }
    suspend fun swipeAction(request: SwipeActionRequest): Result<SwipeActionResponse> = runCatching { api.swipeAction(request) }
    suspend fun getMatches(): Result<List<MatchResponse>> = runCatching { api.getMatches() }
    suspend fun unmatch(matchId: String): Result<GenericResponse> = runCatching { api.unmatch(matchId) }
    suspend fun getApplicants(projectId: String): Result<List<ApplicantResponse>> = runCatching { api.getApplicants(projectId) }
    suspend fun getSwipeStats(): Result<SwipeStatsResponse> = runCatching { api.getSwipeStats() }
}

class TaskRepository(private val api: ApiService) {
    suspend fun createTask(projectId: String, request: CreateTaskRequest): Result<TaskResponse> = runCatching { api.createTask(projectId, request) }
    suspend fun getTasks(projectId: String, status: String? = null): Result<List<TaskResponse>> = runCatching { api.getTasks(projectId, status) }
    suspend fun updateTaskStatus(taskId: String, status: String): Result<GenericResponse> = runCatching { api.updateTaskStatus(taskId, TaskStatusUpdateRequest(status)) }
    suspend fun checkoutTask(taskId: String): Result<CheckoutResponse> = runCatching { api.checkoutTask(taskId) }
    suspend fun confirmCheckout(taskId: String): Result<GenericResponse> = runCatching { api.confirmCheckout(taskId) }
    suspend fun getDashboard(projectId: String): Result<DashboardResponse> = runCatching { api.getDashboard(projectId) }
    suspend fun evaluateMember(projectId: String, memberId: String, request: EvaluationRequest): Result<EvaluationResponse> = runCatching {
        api.evaluateMember(projectId, memberId, request)
    }
}

class SuggestionRepository(private val api: ApiService) {
    suspend fun getCourseSuggestions(): Result<List<CourseSuggestionResponse>> = runCatching { api.getCourseSuggestions() }
    suspend fun dismissCourse(id: String): Result<GenericResponse> = runCatching { api.dismissCourse(id) }
    suspend fun refreshSuggestions(): Result<List<CourseSuggestionResponse>> = runCatching { api.refreshSuggestions() }
    suspend fun getMentorSuggestions(): Result<List<MentorSuggestionResponse>> = runCatching { api.getMentorSuggestions() }
}

class ChatRepository(private val api: ApiService) {
    suspend fun getInbox(): Result<List<ChatInboxResponse>> = runCatching { api.getInbox() }
    suspend fun getMessages(matchId: String, limit: Int = 50): Result<List<MessageResponse>> = runCatching { api.getMessages(matchId, limit) }
    suspend fun getNotifications(limit: Int = 20): Result<List<NotificationResponse>> = runCatching { api.getNotifications(limit) }
    suspend fun markNotificationRead(id: String): Result<GenericResponse> = runCatching { api.markNotificationRead(id) }
    suspend fun getUnreadCount(): Result<UnreadCountResponse> = runCatching { api.getUnreadCount() }
}
