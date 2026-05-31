package com.example.hibuddy.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.Icons
import androidx.compose.material3.HorizontalDivider

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun CompleteProfileScreen(
    onSkip: () -> Unit,
    onComplete: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
) {
    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var githubUrl by remember { mutableStateOf("") }
    var portfolioUrl by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }

    var selectedMode by remember { mutableStateOf("BOTH") }

    val selectedRoles = remember { mutableStateListOf<String>() }
    val selectedInterests = remember { mutableStateListOf<String>() }
    val selectedSkills = remember { mutableStateMapOf<String, String>() }

    val modes = listOf(
        "CONTRIBUTOR" to "Contributor",
        "PROJECT_OWNER" to "Project Owner",
        "BOTH" to "Both"
    )

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
    )
    val roleSkillMap = mapOf(
        "Frontend Developer" to listOf("HTML/CSS", "JavaScript", "React", "UI/UX Design", "GitHub"),
        "Backend Developer" to listOf("Backend Development", "SQL", "Python", "Java", "Node.js", "API Design"),
        "Mobile Developer" to listOf("Mobile Development", "Kotlin", "Jetpack Compose", "Firebase", "UI Design"),
        "AI Engineer" to listOf("Python", "Machine Learning", "Data Analysis", "Research"),
        "Data Analyst" to listOf("Data Analysis", "Excel", "SQL", "Python", "Research"),
        "UI/UX Designer" to listOf("Figma", "UI/UX Design", "User Interview", "Graphic Design"),
        "Marketing Specialist" to listOf("Marketing", "SEO", "Content Writing", "Branding", "Social Media Marketing"),
        "Content Creator" to listOf("Content Writing", "Video Editing", "Photography", "Storytelling"),
        "Project Manager" to listOf("Project Planning", "Task Management", "Leadership", "Communication"),
        "Teacher / Tutor" to listOf("Teaching", "Presentation", "Communication", "English", "Japanese"),
        "Translator" to listOf("Translation", "English", "Japanese", "Korean", "Chinese"),
        "Researcher" to listOf("Research", "Writing", "Critical Thinking", "Data Analysis"),
        "Project Owner" to listOf("Leadership", "Business Strategy", "Project Planning", "Communication"),
        "Team Leader" to listOf("Leadership", "Teamwork", "Task Management", "Communication"),
        "Business Analyst" to listOf("Business Strategy", "Market Research", "User Interview", "Critical Thinking"),
    )

    val otherSkills = listOf(
        "Leadership", "Teamwork", "Communication", "Presentation", "Problem Solving",
        "Critical Thinking", "Time Management", "Sales", "Negotiation", "Customer Service",
        "Canva", "Figma", "Excel", "GitHub", "Writing", "Event Management"
    )

    var selectedSkillCategory by remember { mutableStateOf("Tech") }
    var skillSearchQuery by remember { mutableStateOf("") }

    val interestOptions = listOf(
        "Technology",
        "AI",
        "Data Science",
        "Mobile App",
        "Web App",
        "Game",

        "Education",
        "Language Learning",
        "Culture Exchange",
        "Healthcare",
        "Mental Health",
        "Elderly Care",

        "Business",
        "Startup",
        "Marketing",
        "E-commerce",
        "Finance",
        "Human Resources",

        "Design",
        "Art",
        "Photography",
        "Video Production",
        "Music",
        "Fashion",

        "Environment",
        "Social Impact",
        "Volunteer",
        "Community",
        "Event",
        "Travel",

        "Food",
        "Sports",
        "Fitness",
        "Research",
        "Writing",
        "Media"
    )
    val uiState by profileViewModel.uiState.collectAsState()
    val currentProfile = uiState.profile
    var hasLoadedProfile by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        profileViewModel.loadProfile()
    }

    LaunchedEffect(currentProfile) {
        if (currentProfile != null && !hasLoadedProfile) {
            displayName = currentProfile.displayName
            bio = currentProfile.bio ?: ""
            location = currentProfile.location ?: ""
            githubUrl = currentProfile.githubUrl ?: ""
            portfolioUrl = currentProfile.portfolioUrl ?: ""
            goal = currentProfile.shortTermGoal ?: ""
            selectedMode = currentProfile.mode

            selectedRoles.clear()
            selectedRoles.addAll(currentProfile.roles.map { it.roleName })

            selectedSkills.clear()
            currentProfile.skills.forEach { skill ->
                selectedSkills[skill.skillName] = skill.level
            }

            selectedInterests.clear()
            selectedInterests.addAll(currentProfile.interests.map { it.interestName })

            hasLoadedProfile = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            Text(
                text = "Complete your profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Choose your roles, skills and interests so HiBuddy can match you better.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Short bio") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(20.dp))

            SectionTitle("Participation mode")
            SingleChoiceChips(
                options = modes,
                selectedValue = selectedMode,
                onSelected = { selectedMode = it },
            )

            Spacer(Modifier.height(20.dp))

            SectionTitle("Roles you want")
            Text(
                text = "Choose up to 3 roles.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            MultiSelectChips(
                options = roleOptions,
                selectedItems = selectedRoles,
                maxSelection = 3,
            )

            Spacer(Modifier.height(20.dp))

            SectionTitle("Skills")

            Text(
                text = "Suggested skills are based on your selected roles. You can also choose other skills.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(12.dp))

            SelectedSkillChips(
                selectedSkills = selectedSkills,
                onLevelChange = { skill, level ->
                    selectedSkills[skill] = level
                },
                onRemove = { skill ->
                    selectedSkills.remove(skill)
                }
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = skillSearchQuery,
                onValueChange = { skillSearchQuery = it },
                label = { Text("Search skills") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            val suggestedSkills = selectedRoles
                .toList()
                .flatMap { role ->
                    roleSkillMap[role].orEmpty()
                }
                .distinct()

            LaunchedEffect(selectedRoles.toList()) {
                val validRoleSkills = selectedRoles
                    .toList()
                    .flatMap { role -> roleSkillMap[role].orEmpty() }
                    .toSet()

                val skillsToRemove = selectedSkills.keys.filter { skill ->
                    skill !in validRoleSkills && skill !in otherSkills
                }

                skillsToRemove.forEach { skill ->
                    selectedSkills.remove(skill)
                }
            }

            val visibleSkills = when {
                skillSearchQuery.isNotBlank() -> {
                    (roleSkillMap.values.flatten() + otherSkills)
                        .distinct()
                        .filter { it.contains(skillSearchQuery, ignoreCase = true) }
                }

                suggestedSkills.isNotEmpty() -> {
                    suggestedSkills + listOf("Other")
                }

                else -> {
                    otherSkills
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                visibleSkills.forEach { skill ->
                    if (skill == "Other") {
                        OutlinedButton(
                            onClick = {
                                selectedSkillCategory =
                                    if (selectedSkillCategory == "Other") "" else "Other"
                            },
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = if (selectedSkillCategory == "Other") "Hide other skills" else "+ Browse other skills",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        val selected = selectedSkills.containsKey(skill)

                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (selected) selectedSkills.remove(skill)
                                else selectedSkills[skill] = "BEGINNER"
                            },
                            label = { Text(skill) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            if (selectedSkillCategory == "Other") {
                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Other skills",
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    otherSkills.forEach { skill ->
                        val selected = selectedSkills.containsKey(skill)

                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (selected) selectedSkills.remove(skill)
                                else selectedSkills[skill] = "BEGINNER"
                            },
                            label = { Text(skill) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            SectionTitle("Interests")
            MultiSelectChips(
                options = interestOptions,
                selectedItems = selectedInterests,
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = goal,
                onValueChange = { goal = it },
                label = { Text("Short-term goal") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = githubUrl,
                onValueChange = { githubUrl = it },
                label = { Text("GitHub URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = portfolioUrl,
                onValueChange = { portfolioUrl = it },
                label = { Text("Portfolio URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    profileViewModel.saveCompleteProfile(
                        displayName = displayName.ifBlank { null },
                        bio = bio.ifBlank { null },
                        location = location.ifBlank { null },
                        mode = selectedMode,
                        portfolioUrl = portfolioUrl.ifBlank { null },
                        githubUrl = githubUrl.ifBlank { null },
                        shortTermGoal = goal.ifBlank { null },
                        roles = selectedRoles.toList(),
                        skills = selectedSkills.toMap(),
                        interests = selectedInterests.toList(),
                        onSuccess = {
                            println("DEBUG_SAVE_SUCCESS_CALL_ON_COMPLETE")
                            onComplete()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save and continue")
                }
            }

            uiState.error?.let { error ->
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for now")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SingleChoiceChips(
    options: List<Pair<String, String>>,
    selectedValue: String,
    onSelected: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selectedValue == value,
                onClick = { onSelected(value) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun MultiSelectChips(
    options: List<String>,
    selectedItems: MutableList<String>,
    maxSelection: Int = Int.MAX_VALUE
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { item ->
            val selected = selectedItems.contains(item)
            FilterChip(
                selected = selected,
                onClick = {
                    if (selected) {
                        selectedItems.remove(item)
                    } else if (selectedItems.size < maxSelection) {
                        selectedItems.add(item)
                    }
                },
                label = { Text(item) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectedSkillChips(
    selectedSkills: Map<String, String>,
    onLevelChange: (String, String) -> Unit,
    onRemove: (String) -> Unit
) {
    if (selectedSkills.isEmpty()) return

    val levels = listOf("BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT")

    Column {
        Text(
            text = "Selected skills",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectedSkills.forEach { (skill, level) ->
                var expanded by remember { mutableStateOf(false) }

                Box {
                    AssistChip(
                        onClick = { expanded = true },
                        label = {
                            Text(
                                "$skill • ${
                                    level.lowercase().replaceFirstChar { it.uppercase() }
                                }"
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Choose level"
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            trailingIconContentColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        levels.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(option.lowercase().replaceFirstChar { it.uppercase() })
                                },
                                onClick = {
                                    onLevelChange(skill, option)
                                    expanded = false
                                }
                            )
                        }

                        HorizontalDivider()

                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Remove",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                onRemove(skill)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}