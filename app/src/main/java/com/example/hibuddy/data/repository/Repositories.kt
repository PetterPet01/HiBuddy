package com.example.hibuddy.data.repository

import com.example.hibuddy.data.local.CachedChatMessage
import com.example.hibuddy.data.local.ChatLocalDataSource
import com.example.hibuddy.data.remote.ApiService
import com.example.hibuddy.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Locale
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ProfileRepository(private val api: ApiService) {
    suspend fun getMyProfile(): Result<ProfileResponse> = apiResult { api.getMyProfile() }
    suspend fun getUserProfile(userId: String): Result<UserCardResponse> = apiResult { api.getUserProfile(userId) }
    suspend fun updateProfile(request: ProfileUpdateRequest): Result<ProfileResponse> = apiResult { api.updateMyProfile(request) }
    suspend fun hideProfile(): Result<GenericResponse> = apiResult { api.hideProfile() }
    suspend fun unhideProfile(): Result<GenericResponse> = apiResult { api.unhideProfile() }
    suspend fun addSkill(request: SkillRequest): Result<SkillResponse> = runCatching { api.addSkill(request) }
    suspend fun removeSkill(skillId: String): Result<GenericResponse> = runCatching { api.removeSkill(skillId) }
    suspend fun addRole(request: RoleRequest): Result<RoleResponse> = runCatching { api.addRole(request) }
    suspend fun removeRole(roleId: String): Result<GenericResponse> = runCatching { api.removeRole(roleId) }
    suspend fun addInterest(request: InterestRequest): Result<InterestResponse> = runCatching { api.addInterest(request) }
    suspend fun removeInterest(interestId: String): Result<GenericResponse> = runCatching { api.removeInterest(interestId) }
    suspend fun addCompletedCourse(request: CompletedCourseRequest): Result<CompletedCourseResponse> = runCatching { api.addCompletedCourse(request) }
    suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): Result<MediaUploadResponse> =
        uploadImage(bytes, mimeType, "avatar.jpg") { api.uploadAvatar(it) }
    suspend fun deleteAvatar(): Result<GenericResponse> = runCatching { api.deleteAvatar() }
    suspend fun uploadStudentCard(bytes: ByteArray, mimeType: String): Result<MediaUploadResponse> =
        uploadImage(bytes, mimeType, "student-card.jpg") { api.uploadStudentCard(it) }

    private suspend fun uploadImage(
        bytes: ByteArray,
        mimeType: String,
        fileName: String,
        request: suspend (MultipartBody.Part) -> MediaUploadResponse
    ): Result<MediaUploadResponse> = runCatching {
        val body = bytes.toRequestBody(mimeType.toMediaType())
        request(MultipartBody.Part.createFormData("file", fileName, body))
    }
}

class ProjectRepository(private val api: ApiService) {
    suspend fun createProject(request: CreateProjectRequest): Result<ProjectResponse> = apiResult { api.createProject(request) }
    suspend fun getMyProjects(): Result<List<ProjectResponse>> = apiResult { api.getMyProjects() }
    suspend fun getProject(id: String): Result<ProjectResponse> = apiResult { api.getProject(id) }
    suspend fun closeProject(id: String): Result<GenericResponse> = apiResult { api.closeProject(id) }
    suspend fun addMember(
        projectId: String,
        userId: String,
        role: String,
        roleSlotId: String? = null,
        matchId: String? = null
    ): Result<GenericResponse> = apiResult { api.addMember(projectId, userId, role, roleSlotId, matchId) }
}

class SwipeRepository(private val api: ApiService) {
    suspend fun discoverCards(
        mode: String,
        limit: Int = 20,
        cursor: String? = null,
        projectId: String? = null
    ): Result<DiscoverResponse> = apiResult {
        api.discoverCards(mode, limit, cursor, projectId)
    }
    suspend fun swipeAction(request: SwipeActionRequest): Result<SwipeActionResponse> = apiResult { api.swipeAction(request) }
    suspend fun getQueue(): Result<QueueResponse> = apiResult { api.getQueue() }
    suspend fun addToQueue(request: QueueAddRequest): Result<QueueAddResponse> = apiResult { api.addToQueue(request) }
    suspend fun decideQueueItem(id: String, action: String): Result<SwipeActionResponse> = runCatching {
        api.decideQueueItem(id, QueueDecisionRequest(action))
    }
    suspend fun removeQueueItem(id: String): Result<GenericResponse> = runCatching { api.removeQueueItem(id) }
    suspend fun getMatches(): Result<List<MatchResponse>> = runCatching { api.getMatches() }
    suspend fun unmatch(matchId: String): Result<GenericResponse> = runCatching { api.unmatch(matchId) }
    suspend fun getApplicants(projectId: String): Result<List<ApplicantResponse>> = runCatching { api.getApplicants(projectId) }
    suspend fun getSwipeStats(): Result<SwipeStatsResponse> = runCatching { api.getSwipeStats() }
}

class TaskRepository(private val api: ApiService) {
    suspend fun createTask(projectId: String, request: CreateTaskRequest): Result<TaskResponse> = apiResult { api.createTask(projectId, request) }
    suspend fun getTasks(projectId: String, status: String? = null): Result<List<TaskResponse>> = apiResult { api.getTasks(projectId, status) }
    suspend fun updateTaskStatus(taskId: String, status: String): Result<GenericResponse> = apiResult { api.updateTaskStatus(taskId, TaskStatusUpdateRequest(status)) }
    suspend fun checkoutTask(taskId: String): Result<CheckoutResponse> = apiResult { api.checkoutTask(taskId) }
    suspend fun confirmCheckout(taskId: String): Result<GenericResponse> = apiResult { api.confirmCheckout(taskId) }
    suspend fun getDashboard(projectId: String): Result<DashboardResponse> = apiResult { api.getDashboard(projectId) }
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

class ChatRepository(
    private val api: ApiService,
    private val localDataSource: ChatLocalDataSource? = null
) {
    suspend fun getInbox(): Result<List<ChatInboxResponse>> = runCatching { api.getInbox() }
    suspend fun getMessages(
        matchId: String,
        limit: Int = 50,
        before: String? = null
    ): Result<List<MessageResponse>> = runCatching {
        api.getMessages(matchId, limit, before).also { messages ->
            cacheRemoteMessages(matchId, messages)
        }
    }
    suspend fun getProjectInvitationOptions(matchId: String): Result<ProjectInvitationOptionsResponse> =
        runCatching { api.getProjectInvitationOptions(matchId) }
    suspend fun getProjectInvitations(matchId: String): Result<List<ProjectInvitationResponse>> =
        runCatching { api.getProjectInvitations(matchId) }
    suspend fun createProjectInvitation(matchId: String, roleSlotId: String, message: String? = null): Result<ProjectInvitationResponse> =
        runCatching { api.createProjectInvitation(matchId, ProjectInvitationCreateRequest(roleSlotId, message)) }
    suspend fun acceptProjectInvitation(id: String): Result<ProjectInvitationResponse> =
        runCatching { api.acceptProjectInvitation(id) }
    suspend fun declineProjectInvitation(id: String): Result<ProjectInvitationResponse> =
        runCatching { api.declineProjectInvitation(id) }
    suspend fun getNotifications(limit: Int = 20): Result<List<NotificationResponse>> = runCatching { api.getNotifications(limit) }
    suspend fun markNotificationRead(id: String): Result<GenericResponse> = runCatching { api.markNotificationRead(id) }
    suspend fun getUnreadCount(): Result<UnreadCountResponse> = runCatching { api.getUnreadCount() }

    suspend fun getCachedMessages(matchId: String, limit: Int = 100): List<CachedChatMessage> = withContext(Dispatchers.IO) {
        localDataSource?.getMessages(matchId, limit).orEmpty()
    }

    suspend fun cacheMessage(message: CachedChatMessage) = withContext(Dispatchers.IO) {
        localDataSource?.upsertMessage(message)
    }

    suspend fun cacheMessages(messages: List<CachedChatMessage>) = withContext(Dispatchers.IO) {
        localDataSource?.upsertMessages(messages)
    }

    suspend fun deletePendingByClientId(matchId: String, clientMessageId: String) = withContext(Dispatchers.IO) {
        localDataSource?.deletePendingByClientId(matchId, clientMessageId)
    }

    suspend fun markMessageFailed(messageId: String) = withContext(Dispatchers.IO) {
        localDataSource?.markMessageFailed(messageId)
    }

    suspend fun markMessageSending(messageId: String, clientMessageId: String) = withContext(Dispatchers.IO) {
        localDataSource?.markMessageSending(messageId, clientMessageId)
    }

    suspend fun markOutgoingRead(matchId: String, currentUserId: String) = withContext(Dispatchers.IO) {
        localDataSource?.markOutgoingRead(matchId, currentUserId)
    }

    private suspend fun cacheRemoteMessages(matchId: String, messages: List<MessageResponse>) {
        cacheMessages(messages.map { it.toCachedMessage(matchId) })
    }

    private fun MessageResponse.toCachedMessage(matchId: String): CachedChatMessage {
        return CachedChatMessage(
            id = id,
            matchId = matchId,
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            content = content,
            isRead = isRead,
            createdAt = createdAt,
            clientMessageId = clientMessageId,
            deliveryState = if (isRead) {
                ChatLocalDataSource.DeliveryStateRead
            } else {
                ChatLocalDataSource.DeliveryStateSent
            },
            localSortTime = createdAt.toSortTime()
        )
    }

    private fun String.toSortTime(): Long {
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                .parse(take(19))
                ?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}

class FeedbackRepository(private val api: ApiService) {
    suspend fun submitFeedback(projectId: String, memberId: String, request: FeedbackCreateRequest): Result<FeedbackResponse> =
        runCatching { api.submitFeedback(projectId, memberId, request) }
    suspend fun getMyFeedback(projectId: String): Result<MyFeedbackSummaryResponse> =
        runCatching { api.getMyFeedback(projectId) }
    suspend fun getMyFeedbackSummary(): Result<MyFeedbackSummaryResponse> =
        runCatching { api.getMyFeedbackSummary() }
    suspend fun getMembersToFeedback(projectId: String): Result<MembersToFeedbackResponse> =
        runCatching { api.getMembersToFeedback(projectId) }
}

class NotificationRepository(private val api: ApiService) {
    suspend fun getNotifications(limit: Int = 20): Result<List<NotificationResponse>> =
        runCatching { api.getNotifications(limit) }
    suspend fun markNotificationRead(id: String): Result<GenericResponse> =
        runCatching { api.markNotificationRead(id) }
    suspend fun getUnreadCount(): Result<UnreadCountResponse> =
        runCatching { api.getUnreadCount() }
}
