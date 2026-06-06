package com.example.hibuddy.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UserBlockRequest(
    val blocked_id: String,
    val reason: String? = null
)

data class ReportRequest(
    val reported_id: String,
    val reason: String,
    val description: String? = null,
    @SerializedName("evidence_url") val evidenceUrl: String? = null,
    @SerializedName("context_type") val contextType: String? = null,
    @SerializedName("context_id") val contextId: String? = null
)
