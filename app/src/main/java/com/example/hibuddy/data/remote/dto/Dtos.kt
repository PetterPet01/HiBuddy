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
    val user: UserResponse
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
    val code: String
)

data class ForgotPasswordRequest(
    val email: String? = null,
    val phone: String? = null
)

data class ResetPasswordRequest(
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
    val mode: String? = null
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
    val ordering: Int
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

data class RoleSlotRequest(
    @SerializedName("role_name") val roleName: String,
    val count: Int,
    @SerializedName("skill_requirements") val skillRequirements: String? = null
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
    @SerializedName("match_score") val matchScore: Double
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
    @SerializedName("match_score") val matchScore: Double
)

data class SwipeActionRequest(
    @SerializedName("target_type") val targetType: String,
    @SerializedName("target_id") val targetId: String,
    val action: String
)

data class SwipeActionResponse(
    val matched: Boolean = false,
    @SerializedName("match_id") val matchId: String? = null,
    val message: String? = null
)

data class MatchResponse(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("project_id") val projectId: String,
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("role_matched") val roleMatched: String?,
    @SerializedName("match_score") val matchScore: Double,
    @SerializedName("matched_at") val matchedAt: String,
    @SerializedName("is_unmatched") val isUnmatched: Boolean,
    @SerializedName("is_member_added") val isMemberAdded: Boolean,
    @SerializedName("user_name") val userName: String?,
    @SerializedName("user_avatar") val userAvatar: String?,
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
    @SerializedName("user_name") val userName: String,
    @SerializedName("user_avatar") val userAvatar: String?,
    @SerializedName("project_title") val projectTitle: String,
    @SerializedName("last_message") val lastMessage: String?,
    @SerializedName("last_message_time") val lastMessageTime: String?,
    @SerializedName("is_unread") val isUnread: Boolean,
    @SerializedName("unread_count") val unreadCount: Int
)

data class MessageResponse(
    val id: String,
    @SerializedName("chat_id") val chatId: String,
    @SerializedName("sender_id") val senderId: String,
    val content: String,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("created_at") val createdAt: String
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
