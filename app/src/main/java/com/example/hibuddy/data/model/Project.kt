package com.example.hibuddy.data.model

data class Project(
    val projectId: String = "",
    val ownerId: String = "",

    val title: String = "",
    val field: String = "",
    val description: String = "",

    val rolesNeeded: List<String> = emptyList(),
    val skillsNeeded: List<String> = emptyList(),

    val timeline: String = "",
    val workMode: String = "",
    val commitment: String = "",

    val maxMembers: Int = 0,
    val currentMembers: Int = 0,
    val memberIds: List<String> = emptyList(),

    val tags: List<String> = emptyList(),

    val isOpen: Boolean = true
)