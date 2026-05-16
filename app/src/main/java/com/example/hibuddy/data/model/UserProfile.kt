package com.example.hibuddy.data.model

data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val university: String = "",
    val major: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val skills: List<String> = emptyList(),
    val interests: List<String> = emptyList()
)