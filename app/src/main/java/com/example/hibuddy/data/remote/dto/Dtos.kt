package com.example.hibuddy.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("full_name") val fullName: String,
    val username: String,
    val email: String,
    @SerializedName("date_of_birth") val dateOfBirth: String,
    val password: String,
    @SerializedName("confirm_password") val confirmPassword: String,
    val phone: String? = null,
    @SerializedName("agree_terms") val agreeTerms: Boolean
)

data class LoginRequest(
    val username: String,
    val password: String,
    @SerializedName("remember_me") val rememberMe: Boolean = false
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val tokenType: String,
    val user: UserResponse,
    @SerializedName("requires_email_verification") val requiresEmailVerification: Boolean = false
)

data class UserResponse(
    val id: String,
    val username: String,
    val email: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("email_verified") val emailVerified: Boolean,
    @SerializedName("verified_student") val verifiedStudent: Boolean,
    val role: String,
    @SerializedName("avatar_url") val avatarUrl: String?
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

data class VerifyEmailRequest(
    val email: String,
    val code: String
)

data class ResendVerificationRequest(
    val email: String
)

data class GoogleLoginRequest(
    @SerializedName("id_token") val idToken: String,
    @SerializedName("device_name") val deviceName: String? = "Android"
)

data class ForgotPasswordRequest(
    val email: String? = null,
    val phone: String? = null
)

data class ResetPasswordRequest(
    val email: String,
    val code: String,
    @SerializedName("new_password") val newPassword: String,
    @SerializedName("confirm_password") val confirmPassword: String
)

data class StudentVerificationRequest(
    @SerializedName("full_name") val fullName: String,
    @SerializedName("student_email") val studentEmail: String? = null,
    val university: String,
    @SerializedName("student_id") val studentId: String,
    @SerializedName("academic_year") val academicYear: String
)

data class GenericResponse(
    val message: String
)

data class MediaUploadResponse(
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null
)

data class FcmTokenRequest(
    val token: String,
    @SerializedName("device_type") val deviceType: String = "android"
)

data class ProfileResponse(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("display_name") val displayName: String,
    val bio: String?,
    val location: String?,
    @SerializedName("portfolio_url") val portfolioUrl: String?,
    @SerializedName("github_url") val githubUrl: String?,
    @SerializedName("facebook_url") val facebookUrl: String?,
    @SerializedName("short_term_goal") val shortTermGoal: String?,
    val mode: String,
    @SerializedName("is_hidden") val isHidden: Boolean,
    @SerializedName("reputation_score") val reputationScore: Double,
    @SerializedName("projects_completed") val projectsCompleted: Int,
    @SerializedName("avatar_url") val avatarUrl: String?,
    val email: String,
    @SerializedName("verified_student") val verifiedStudent: Boolean,
    val university: String?,
    val roles: List<RoleResponse>,
    val skills: List<SkillResponse>,
    val interests: List<InterestResponse>,
    @SerializedName("created_at") val createdAt: String
)

data class ProfileUpdateRequest(
    @SerializedName("display_name") val displayName: String? = null,
    val bio: String? = null,
    val location: String? = null,
    @SerializedName("portfolio_url") val portfolioUrl: String? = null,
    @SerializedName("github_url") val githubUrl: String? = null,
    @SerializedName("facebook_url") val facebookUrl: String? = null,
    @SerializedName("short_term_goal") val shortTermGoal: String? = null,
    val mode: String? = null,
    val roles: List<RoleProfileRequest>? = null,
    val interests: List<String>? = null
)

data class RoleSkillRequest(
    @SerializedName("skill_name") val skillName: String,
    val level: String = "BEGINNER",
    @SerializedName("needs_improvement") val needsImprovement: Boolean = false
)

data class RoleProfileRequest(
    @SerializedName("role_name") val roleName: String,
    val ordering: Int,
    val skills: List<RoleSkillRequest> = emptyList()
)

data class SkillRequest(
    @SerializedName("skill_name") val skillName: String,
    val level: String = "BEGINNER",
    @SerializedName("needs_improvement") val needsImprovement: Boolean = false
)

data class SkillResponse(
    val id: String,
    @SerializedName("skill_name") val skillName: String,
    val level: String,
    @SerializedName("needs_improvement") val needsImprovement: Boolean
)

data class RoleRequest(
    @SerializedName("role_name") val roleName: String,
    val ordering: Int = 0
)

data class RoleResponse(
    val id: String,
    @SerializedName("role_name") val roleName: String,
    val ordering: Int,
    val skills: List<SkillResponse> = emptyList()
)

data class InterestRequest(
    @SerializedName("interest_name") val interestName: String
)

data class InterestResponse(
    val id: String,
    @SerializedName("interest_name") val interestName: String
)

data class CompletedCourseRequest(
    @SerializedName("course_title") val courseTitle: String,
    val source: String,
    @SerializedName("course_id") val courseId: String? = null
)

data class CompletedCourseResponse(
    val id: String,
    @SerializedName("course_title") val courseTitle: String,
    val source: String,
    @SerializedName("badge_visible") val badgeVisible: Boolean,
    @SerializedName("completed_date") val completedDate: String
)

data class SkillRequirementRequest(
    @SerializedName("skill_name") val skillName: String,
    @SerializedName("minimum_level") val minimumLevel: String = "BEGINNER",
    @SerializedName("is_required") val isRequired: Boolean = true
)

data class RoleSlotRequest(
    @SerializedName("role_name") val roleName: String,
    val count: Int,
    @SerializedName("skill_requirements") val skillRequirements: List<SkillRequirementRequest> = emptyList()
)

data class CreateProjectRequest(
    val title: String,
    val field: String,
    val description: String,
    @SerializedName("specific_goal") val specificGoal: String? = null,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    @SerializedName("max_members") val maxMembers: Int,
    @SerializedName("work_mode") val workMode: String = "ONLINE",
    @SerializedName("commitment_level") val commitmentLevel: String = "CASUAL",
    @SerializedName("role_slots") val roleSlots: List<RoleSlotRequest>,
    @SerializedName("additional_requirements") val additionalRequirements: String? = null,
    @SerializedName("member_benefits") val memberBenefits: String? = null
)

data class ProjectResponse(
    val id: String,
    @SerializedName("owner_id") val ownerId: String,
    val title: String,
    val field: String,
    val description: String,
    @SerializedName("specific_goal") val specificGoal: String?,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?,
    @SerializedName("work_mode") val workMode: String,
    @SerializedName("commitment_level") val commitmentLevel: String,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    @SerializedName("max_members") val maxMembers: Int,
    val status: String,
    @SerializedName("review_status") val reviewStatus: String = "APPROVED",
    @SerializedName("moderation_categories") val moderationCategories: List<String>? = null,
    @SerializedName("moderation_reasons") val moderationReasons: List<String>? = null,
    @SerializedName("additional_requirements") val additionalRequirements: String?,
    @SerializedName("member_benefits") val memberBenefits: String?,
    @SerializedName("role_slots") val roleSlots: List<RoleSlotResponse>,
    val members: List<MemberResponse>,
    @SerializedName("created_at") val createdAt: String
)

data class RoleSlotResponse(
    val id: String,
    @SerializedName("role_name") val roleName: String,
    val count: Int,
    val filled: Int,
    @SerializedName("skill_requirements") val skillRequirements: Map<String, String>?
)

data class MemberResponse(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("display_name") val displayName: String,
    val role: String,
    @SerializedName("is_owner") val isOwner: Boolean,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("joined_at") val joinedAt: String
)

data class DiscoverResponse(
    @SerializedName("user_cards") val userCards: List<UserCardResponse> = emptyList(),
    @SerializedName("project_cards") val projectCards: List<ProjectCardResponse> = emptyList(),
    @SerializedName("next_cursor") val nextCursor: String? = null,
    @SerializedName("context_project_id") val contextProjectId: String? = null,
    @SerializedName("daily_likes_remaining") val dailyLikesRemaining: Int = 50,
    @SerializedName("daily_superlikes_remaining") val dailySuperlikesRemaining: Int = 3
)

data class UserCardResponse(
    @SerializedName("user_id") val userId: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("verified_student") val verifiedStudent: Boolean,
    val university: String?,
    val bio: String?,
    val roles: List<RoleResponse>,
    val skills: List<SkillResponse>,
    val location: String?,
    @SerializedName("github_url") val githubUrl: String?,
    @SerializedName("reputation_score") val reputationScore: Double,
    @SerializedName("projects_completed") val projectsCompleted: Int,
    @SerializedName("match_score") val matchScore: Double,
    @SerializedName("matched_role") val matchedRole: String? = null,
    @SerializedName("score_explanation") val scoreExplanation: Map<String, Any>? = null
)

data class ProjectCardResponse(
    @SerializedName("project_id") val projectId: String,
    val title: String,
    val field: String,
    val description: String,
    @SerializedName("owner_name") val ownerName: String,
    @SerializedName("owner_avatar") val ownerAvatar: String?,
    @SerializedName("owner_verified") val ownerVerified: Boolean,
    @SerializedName("role_slots") val roleSlots: List<RoleSlotResponse>,
    @SerializedName("work_mode") val workMode: String,
    @SerializedName("commitment_level") val commitmentLevel: String,
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("end_date") val endDate: String?,
    @SerializedName("total_slots") val totalSlots: Int,
    @SerializedName("filled_slots") val filledSlots: Int,
    @SerializedName("match_score") val matchScore: Double,
    @SerializedName("matched_role") val matchedRole: String? = null,
    @SerializedName("score_explanation") val scoreExplanation: Map<String, Any>? = null
)

data class SwipeActionRequest(
    @SerializedName("target_type") val targetType: String,
    @SerializedName("target_id") val targetId: String,
    val action: String,
    @SerializedName("context_project_id") val contextProjectId: String? = null,
    @SerializedName("context_role_slot_id") val contextRoleSlotId: String? = null
)

data class SwipeActionResponse(
    val matched: Boolean = false,
    @SerializedName("match_id") val matchId: String? = null,
    val message: String? = null
)

data class QueueAddRequest(
    @SerializedName("target_type") val targetType: String,
    @SerializedName("target_id") val targetId: String,
    @SerializedName("context_project_id") val contextProjectId: String? = null
)

data class QueueDecisionRequest(
    val action: String
)

data class QueueAddResponse(
    val message: String,
    @SerializedName("queue_item_id") val queueItemId: String? = null
)

data class QueueItemResponse(
    val id: String,
    @SerializedName("target_type") val targetType: String,
    @SerializedName("target_id") val targetId: String,
    @SerializedName("queued_at") val queuedAt: String,
    @SerializedName("expires_at") val expiresAt: String,
    @SerializedName("seconds_remaining") val secondsRemaining: Int,
    @SerializedName("user_card") val userCard: UserCardResponse? = null,
    @SerializedName("project_card") val projectCard: ProjectCardResponse? = null
)

data class QueueResponse(
    @SerializedName("user_profiles") val userProfiles: List<QueueItemResponse> = emptyList(),
    @SerializedName("project_profiles") val projectProfiles: List<QueueItemResponse> = emptyList(),
    @SerializedName("user_capacity_remaining") val userCapacityRemaining: Int = 3,
    @SerializedName("project_capacity_remaining") val projectCapacityRemaining: Int = 3
)

data class MatchResponse(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("project_id") val projectId: String,
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("other_user_id") val otherUserId: String? = null,
    @SerializedName("role_matched") val roleMatched: String?,
    @SerializedName("match_score") val matchScore: Double,
    @SerializedName("matched_at") val matchedAt: String,
    @SerializedName("is_unmatched") val isUnmatched: Boolean,
    @SerializedName("is_member_added") val isMemberAdded: Boolean,
    @SerializedName("user_name") val userName: String?,
    @SerializedName("user_avatar") val userAvatar: String?,
    @SerializedName("user_is_online") val userIsOnline: Boolean = false,
    @SerializedName("user_last_seen_at") val userLastSeenAt: String? = null,
    @SerializedName("project_title") val projectTitle: String?,
    @SerializedName("last_message") val lastMessage: String?,
    @SerializedName("last_message_time") val lastMessageTime: String?,
    @SerializedName("is_unread") val isUnread: Boolean
)

data class ApplicantResponse(
    @SerializedName("user_id") val userId: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    val roles: List<RoleResponse>,
    val skills: List<SkillResponse>,
    @SerializedName("verified_student") val verifiedStudent: Boolean,
    @SerializedName("reputation_score") val reputationScore: Double,
    @SerializedName("match_score") val matchScore: Double,
    @SerializedName("matched_role") val matchedRole: String? = null,
    @SerializedName("score_explanation") val scoreExplanation: Map<String, Any>? = null,
    @SerializedName("is_super_like") val isSuperLike: Boolean = false,
    @SerializedName("swiped_at") val swipedAt: String
)

data class SwipeStatsResponse(
    @SerializedName("daily_likes_remaining") val dailyLikesRemaining: Int,
    @SerializedName("daily_superlikes_remaining") val dailySuperlikesRemaining: Int
)

data class CreateTaskRequest(
    val title: String,
    val description: String? = null,
    @SerializedName("assignee_id") val assigneeId: String,
    @SerializedName("role_related") val roleRelated: String? = null,
    val priority: String = "MEDIUM",
    @SerializedName("start_date") val startDate: String,
    val deadline: String,
    val tag: String? = null
)

data class TaskResponse(
    val id: String,
    @SerializedName("project_id") val projectId: String,
    @SerializedName("assignee_id") val assigneeId: String,
    @SerializedName("creator_id") val creatorId: String,
    val title: String,
    val description: String?,
    @SerializedName("role_related") val roleRelated: String?,
    val priority: String,
    val status: String,
    @SerializedName("start_date") val startDate: String,
    val deadline: String,
    val tag: String?,
    @SerializedName("checkout_at") val checkoutAt: String?,
    @SerializedName("checkout_confirmed_at") val checkoutConfirmedAt: String?,
    @SerializedName("checkout_status") val checkoutStatus: String?,
    @SerializedName("assignee_name") val assigneeName: String?,
    @SerializedName("assignee_avatar") val assigneeAvatar: String?,
    @SerializedName("created_at") val createdAt: String
)

data class TaskStatusUpdateRequest(
    val status: String
)

data class CheckoutResponse(
    val message: String,
    @SerializedName("checkout_status") val checkoutStatus: String
)

data class DashboardResponse(
    @SerializedName("project_id") val projectId: String,
    @SerializedName("project_title") val projectTitle: String,
    @SerializedName("total_tasks") val totalTasks: Int,
    @SerializedName("total_members") val totalMembers: Int,
    @SerializedName("member_stats") val memberStats: List<MemberStatResponse>
)

data class MemberStatResponse(
    @SerializedName("user_id") val userId: String,
    @SerializedName("display_name") val displayName: String,
    val role: String,
    @SerializedName("total_tasks") val totalTasks: Int,
    val early: Int,
    @SerializedName("on_time") val onTime: Int,
    val late: Int,
    @SerializedName("in_progress") val inProgress: Int,
    val todo: Int
)

data class EvaluationRequest(
    @SerializedName("quality_score") val qualityScore: Double,
    @SerializedName("collaboration_score") val collaborationScore: Double,
    @SerializedName("communication_score") val communicationScore: Double,
    @SerializedName("deadline_score") val deadlineScore: Double,
    @SerializedName("feedback_text") val feedbackText: String? = null
)

data class EvaluationResponse(
    val id: String,
    @SerializedName("project_id") val projectId: String,
    @SerializedName("evaluator_id") val evaluatorId: String,
    @SerializedName("evaluatee_id") val evaluateeId: String,
    @SerializedName("quality_score") val qualityScore: Double,
    @SerializedName("collaboration_score") val collaborationScore: Double,
    @SerializedName("communication_score") val communicationScore: Double,
    @SerializedName("deadline_score") val deadlineScore: Double,
    @SerializedName("overall_score") val overallScore: Double,
    @SerializedName("feedback_text") val feedbackText: String?,
    @SerializedName("created_at") val createdAt: String
)

data class CourseSuggestionResponse(
    val id: String,
    @SerializedName("target_skill") val targetSkill: String,
    @SerializedName("course_title") val courseTitle: String,
    @SerializedName("course_id") val courseId: String,
    val source: String,
    val url: String?,
    @SerializedName("match_percent") val matchPercent: Double,
    @SerializedName("is_dismissed") val isDismissed: Boolean
)

data class MentorSuggestionResponse(
    @SerializedName("user_id") val userId: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("verified_student") val verifiedStudent: Boolean,
    val university: String?,
    val bio: String?,
    val roles: List<RoleResponse>,
    val skills: List<SkillResponse>,
    @SerializedName("reputation_score") val reputationScore: Double,
    @SerializedName("match_score") val matchScore: Double
)

data class ChatInboxResponse(
    val id: String,
    @SerializedName("match_id") val matchId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("user_name") val userName: String,
    @SerializedName("user_avatar") val userAvatar: String?,
    @SerializedName("user_is_online") val userIsOnline: Boolean = false,
    @SerializedName("user_last_seen_at") val userLastSeenAt: String? = null,
    @SerializedName("project_title") val projectTitle: String,
    @SerializedName("last_message") val lastMessage: String?,
    @SerializedName("last_message_time") val lastMessageTime: String?,
    @SerializedName("is_unread") val isUnread: Boolean,
    @SerializedName("unread_count") val unreadCount: Int
)

data class ProjectInvitationCreateRequest(
    @SerializedName("role_slot_id") val roleSlotId: String,
    val message: String? = null
)

data class InvitationRoleSlotResponse(
    val id: String,
    @SerializedName("role_name") val roleName: String,
    val count: Int,
    val filled: Int
)

data class ProjectInvitationOptionsResponse(
    @SerializedName("can_invite") val canInvite: Boolean,
    val reason: String? = null,
    @SerializedName("project_id") val projectId: String? = null,
    @SerializedName("project_title") val projectTitle: String? = null,
    @SerializedName("open_role_slots") val openRoleSlots: List<InvitationRoleSlotResponse> = emptyList()
)

data class ProjectInvitationResponse(
    val id: String,
    @SerializedName("match_id") val matchId: String,
    @SerializedName("project_id") val projectId: String,
    @SerializedName("project_title") val projectTitle: String,
    @SerializedName("inviter_id") val inviterId: String,
    @SerializedName("inviter_name") val inviterName: String,
    @SerializedName("invitee_id") val inviteeId: String,
    @SerializedName("invitee_name") val inviteeName: String,
    @SerializedName("role_slot_id") val roleSlotId: String? = null,
    val role: String,
    val message: String? = null,
    val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("responded_at") val respondedAt: String? = null,
    @SerializedName("is_incoming") val isIncoming: Boolean = false,
    @SerializedName("is_outgoing") val isOutgoing: Boolean = false
)

data class MessageResponse(
    val id: String,
    @SerializedName("chat_id") val chatId: String,
    @SerializedName("sender_id") val senderId: String,
    val content: String,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("sender_name") val senderName: String? = null,
    @SerializedName("client_message_id") val clientMessageId: String? = null
)

data class NotificationResponse(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("related_id") val relatedId: String?,
    @SerializedName("created_at") val createdAt: String
)
data class UnreadCountResponse(
    val count: Int
)
data class AdminUserResponse(
    val id: String,
    val username: String,
    val email: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("verified_student") val verifiedStudent: Boolean,
    @SerializedName("student_email") val studentEmail: String?,
    val university: String?,
    @SerializedName("student_id") val studentId: String?,
    @SerializedName("verification_status") val verificationStatus: String,
    @SerializedName("verification_rejection_reason") val verificationRejectionReason: String?,
    @SerializedName("academic_year") val academicYear: String? = null,
    @SerializedName("student_card_image_url") val studentCardImageUrl: String? = null,
    @SerializedName("verification_submitted_at") val verificationSubmittedAt: String? = null,
    val role: String,
    @SerializedName("is_active") val isActive: Boolean
)

data class RejectStudentRequest(
    val reason: String
)

data class AdminReportResponse(
    val id: String,

    val reporter_id: String,
    val reported_id: String,

    val reason: String,
    val description: String?,
    @SerializedName("evidence_url") val evidenceUrl: String? = null,
    @SerializedName("context_type") val contextType: String? = null,
    @SerializedName("context_id") val contextId: String? = null,

    val status: String,
    val created_at: String,

    val reporter_name: String?,
    val reported_name: String?
)

data class ResolveReportRequest(
    val action: String,
    val reason: String
)

data class AdminActionRequest(val reason: String)
