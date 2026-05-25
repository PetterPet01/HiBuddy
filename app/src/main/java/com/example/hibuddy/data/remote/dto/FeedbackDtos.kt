package com.example.hibuddy.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FeedbackCreateRequest(
    @SerializedName("feedback_text") val feedbackText: String
)

data class FeedbackResponse(
    val id: String,
    @SerializedName("project_id") val projectId: String,
    @SerializedName("target_id") val targetId: String,
    @SerializedName("feedback_text") val feedbackText: String,
    @SerializedName("analyzed_weaknesses") val analyzedWeaknesses: List<String>?,
    @SerializedName("created_at") val createdAt: String
)

data class MyFeedbackSummaryResponse(
    @SerializedName("total_feedbacks") val totalFeedbacks: Int,
    val weaknesses: List<String>,
    val feedbacks: List<FeedbackResponse>
)

data class MembersToFeedbackResponse(
    @SerializedName("project_id") val projectId: String,
    @SerializedName("project_title") val projectTitle: String,
    val members: List<MemberToFeedbackItem>
)

data class MemberToFeedbackItem(
    @SerializedName("user_id") val userId: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    val role: String,
    @SerializedName("already_feedback") val alreadyFeedback: Boolean
)
