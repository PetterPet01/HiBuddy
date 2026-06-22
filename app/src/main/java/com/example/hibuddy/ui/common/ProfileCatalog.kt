package com.example.hibuddy.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.hibuddy.ServiceLocator

object ProfileCatalog {
    val roleOptions = listOf(
        "Project Owner",
        "Project Manager",
        "Team Leader",
        "Business Analyst",
        "Researcher",
        "Frontend Developer",
        "Backend Developer",
        "Mobile Developer",
        "AI Engineer",
        "Data Analyst",
        "UI/UX Designer",
        "Graphic Designer",
        "Content Creator",
        "Copywriter",
        "Video Editor",
        "Photographer",
        "Marketing Specialist",
        "Social Media Manager",
        "Sales Representative",
        "Customer Support",
        "Event Planner",
        "Finance Planner",
        "Accountant",
        "Legal Advisor",
        "HR / Recruiter",
        "Teacher / Tutor",
        "Translator",
        "Community Manager",
        "Product Tester",
        "Volunteer Coordinator"
    ).sorted()

    val roleSkillMap = mapOf(
        "Frontend Developer" to listOf("HTML/CSS", "JavaScript", "React", "UI/UX Design", "GitHub"),
        "Backend Developer" to listOf("Backend Development", "SQL", "Python", "Java", "Node.js", "API Design"),
        "Mobile Developer" to listOf("Mobile Development", "Kotlin", "Jetpack Compose", "Firebase", "UI Design"),
        "AI Engineer" to listOf("Python", "Machine Learning", "Data Analysis", "Research"),
        "Data Analyst" to listOf("Data Analysis", "Excel", "SQL", "Python", "Research"),
        "UI/UX Designer" to listOf("Figma", "UI/UX Design", "User Interview", "Graphic Design"),
        "Project Manager" to listOf("Project Planning", "Task Management", "Leadership", "Communication")
    )

    /**
     * Role options merged from the global server catalog and the local fallback.
     * Falls back to the static list when the catalog request fails or is empty,
     * so the dropdowns always have content even offline.
     */
    @Composable
    fun rememberRoleOptions(): State<List<String>> {
        val state = remember { mutableStateOf(roleOptions) }
        LaunchedEffect(Unit) {
            ServiceLocator.catalogRepository.getRoles().onSuccess { remote ->
                if (remote.isNotEmpty()) {
                    val merged = (roleOptions + remote.map { it.name }).distinct().sorted()
                    state.value = merged
                }
            }
        }
        return state
    }

    /** Global skill names merged with the local fallback (flattened role->skills). */
    @Composable
    fun rememberSkillOptions(): State<List<String>> {
        val localSkills = roleSkillMap.values.flatten().distinct()
        val state = remember { mutableStateOf(localSkills) }
        LaunchedEffect(Unit) {
            ServiceLocator.catalogRepository.getSkills().onSuccess { remote ->
                if (remote.isNotEmpty()) {
                    val merged = (localSkills + remote.map { it.name }).distinct().sorted()
                    state.value = merged
                }
            }
        }
        return state
    }
}