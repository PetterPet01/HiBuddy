package com.example.hibuddy.data.remote.dto

data class UserBlockRequest(
    val blocked_id: String,
    val reason: String? = null
)

data class ReportRequest(
    val reported_id: String,
    val reason: String,
    val description: String? = null
)
