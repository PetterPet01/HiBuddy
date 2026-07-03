package com.example.hibuddy.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.ui.common.ProfileCatalog

private enum class CompleteProfileStep { BasicInfo, Roles, RoleSkills, Review }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompleteProfileScreen(
    onSkip: () -> Unit,
    onComplete: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val currentProfile = uiState.profile

    var hasLoadedProfile by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf(CompleteProfileStep.BasicInfo) }

    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var githubUrl by remember { mutableStateOf("") }
    var portfolioUrl by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("BOTH") }
    var studentVerification by remember { mutableStateOf(false) }
    var customRoleInput by remember { mutableStateOf("") }
    var customSkillInput by remember { mutableStateOf("") }
    var activeRoleIndex by remember { mutableStateOf(0) }

    val selectedRoles = remember { mutableStateListOf<String>() }
    val selectedInterests = remember { mutableStateListOf<String>() }
    val selectedSkillsByRole = remember { mutableStateMapOf<String, MutableMap<String, String>>() }

    val modes = listOf(
        "CONTRIBUTOR" to "Contributor",
        "OWNER" to "Project Owner",
        "BOTH" to "Both"
    )
    val roleOptions = ProfileCatalog.roleOptions
    val suggestedSkills = ProfileCatalog.roleSkillMap.values.flatten().distinct().sorted()
    val interestOptions = listOf(
        "Technology", "AI", "Data Science", "Mobile App", "Web App", "Game",
        "Education", "Language Learning", "Culture Exchange", "Healthcare", "Mental Health", "Elderly Care",
        "Business", "Startup", "Marketing", "E-commerce", "Finance", "Human Resources",
        "Design", "Art", "Photography", "Video Production", "Music", "Fashion",
        "Environment", "Social Impact", "Volunteer", "Community", "Event", "Travel",
        "Food", "Sports", "Fitness", "Research", "Writing", "Media"
    )

    LaunchedEffect(Unit) { profileViewModel.loadProfile() }

    LaunchedEffect(currentProfile) {
        if (currentProfile != null && !hasLoadedProfile) {
            displayName = currentProfile.displayName
            bio = currentProfile.bio.orEmpty()
            location = currentProfile.location.orEmpty()
            githubUrl = currentProfile.githubUrl.orEmpty()
            portfolioUrl = currentProfile.portfolioUrl.orEmpty()
            goal = currentProfile.shortTermGoal.orEmpty()
            selectedMode = currentProfile.mode
            selectedRoles.clear()
            selectedRoles.addAll(currentProfile.roles.map { it.roleName })
            selectedSkillsByRole.clear()
            currentProfile.roles.forEach { role ->
                selectedSkillsByRole[role.roleName] = role.skills.associateTo(mutableStateMapOf()) { it.skillName to it.level }
            }
            selectedInterests.clear()
            selectedInterests.addAll(currentProfile.interests.map { it.interestName })
            activeRoleIndex = 0
            hasLoadedProfile = true
        }
    }

    LaunchedEffect(selectedRoles.toList()) {
        selectedSkillsByRole.keys.filterNot(selectedRoles::contains).toList().forEach { selectedSkillsByRole.remove(it) }
        selectedRoles.forEach { if (it !in selectedSkillsByRole) selectedSkillsByRole[it] = mutableStateMapOf() }
        if (activeRoleIndex >= selectedRoles.size) activeRoleIndex = 0
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = when (step) {
                    CompleteProfileStep.BasicInfo -> "Complete your profile"
                    CompleteProfileStep.Roles -> "Choose roles"
                    CompleteProfileStep.RoleSkills -> "Add skills by role"
                    CompleteProfileStep.Review -> "Review and finish"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "We keep role and skill data structured by role so matching stays accurate.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            when (step) {
                CompleteProfileStep.BasicInfo -> {
                    OutlinedTextField(displayName, { displayName = it }, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(bio, { bio = it }, label = { Text("Short bio") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(location, { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(16.dp))
                    Text("Participation mode", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    ChoiceChips(modes, selectedMode, onSelect = { selectedMode = it })
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(goal, { goal = it }, label = { Text("Short-term goal") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = studentVerification, onCheckedChange = { studentVerification = it })
                        Text("I want student verification")
                    }
                }

                CompleteProfileStep.Roles -> {
                    Text("Bạn có những vai trò nào trong dự án? Chọn tối đa 3 vai trò hoặc thêm vai trò mới nếu chưa có sẵn.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    MultiSelectChips(roleOptions, selectedRoles, maxSelection = 3)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(customRoleInput, { customRoleInput = it }, label = { Text("Add custom role") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                val value = customRoleInput.trim()
                                if (value.isNotBlank() && selectedRoles.none { it.equals(value, ignoreCase = true) }) {
                                    selectedRoles.add(value)
                                    selectedSkillsByRole[value] = mutableStateMapOf()
                                    activeRoleIndex = selectedRoles.lastIndex
                                    customRoleInput = ""
                                }
                            },
                            enabled = customRoleInput.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) { Text("Add role") }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (selectedRoles.isNotEmpty()) {
                        Text("Selected roles", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedRoles.forEachIndexed { index, role ->
                                FilterChip(
                                    selected = activeRoleIndex == index,
                                    onClick = { activeRoleIndex = index },
                                    label = { Text(role) }
                                )
                            }
                        }
                    }
                }

                CompleteProfileStep.RoleSkills -> {
                    if (selectedRoles.isEmpty()) {
                        Text("Please select at least one role first.")
                    } else {
                        val activeRole = selectedRoles.getOrNull(activeRoleIndex).orEmpty()
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    text = "Role ${activeRoleIndex + 1} of ${selectedRoles.size}",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Bạn có những kỹ năng nào trong vai trò $activeRole?",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Mỗi vai trò là một trang riêng. Chọn kỹ năng, thêm kỹ năng mới, và đặt mức độ phù hợp.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        val roleSkills = selectedSkillsByRole[activeRole].orEmpty()
                        SelectedSkillChips(
                            selectedSkills = roleSkills,
                            onLevelChange = { skill, level -> selectedSkillsByRole[activeRole]?.set(skill, level) },
                            onRemove = { skill -> selectedSkillsByRole[activeRole]?.remove(skill) }
                        )
                        Spacer(Modifier.height(12.dp))
                        fun addCustomSkillToActiveRole() {
                            val value = customSkillInput.trim()
                            if (value.isNotBlank() && activeRole.isNotBlank()) {
                                val current = selectedSkillsByRole[activeRole] ?: mutableStateMapOf()
                                current[value] = "BEGINNER"
                                selectedSkillsByRole[activeRole] = current
                                customSkillInput = ""
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                customSkillInput,
                                { customSkillInput = it },
                                label = { Text("Add custom skill to ${activeRole.ifBlank { "role" }}") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(
                                onClick = { addCustomSkillToActiveRole() },
                                enabled = customSkillInput.isNotBlank() && activeRole.isNotBlank()
                            ) {
                                Text("Add")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            suggestedSkills.forEach { skill ->
                                val selected = roleSkills.containsKey(skill)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        if (activeRole.isNotBlank()) {
                                            val current = selectedSkillsByRole[activeRole] ?: mutableStateMapOf()
                                            if (selected) current.remove(skill) else current[skill] = "BEGINNER"
                                            selectedSkillsByRole[activeRole] = current
                                        }
                                    },
                                    label = { Text(skill) }
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    if (activeRoleIndex > 0) activeRoleIndex-- else step = CompleteProfileStep.Roles
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Back") }
                            Button(
                                onClick = {
                                    addCustomSkillToActiveRole()
                                    if (activeRoleIndex < selectedRoles.lastIndex) activeRoleIndex++ else step = CompleteProfileStep.Review
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text(if (activeRoleIndex < selectedRoles.lastIndex) "Next role" else "Review") }
                        }
                    }
                }

                CompleteProfileStep.Review -> {
                    Text("Review your profile before saving.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("Name: ${displayName.ifBlank { "-" }}")
                    Text("Mode: $selectedMode")
                    Text("Student verification: ${if (studentVerification) "Yes" else "No"}")
                    Spacer(Modifier.height(12.dp))
                    selectedRoles.forEach { role ->
                        Text("Role: $role", fontWeight = FontWeight.Bold)
                        selectedSkillsByRole[role].orEmpty().forEach { (skill, level) ->
                            Text("  - $skill ($level)")
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Interests", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    MultiSelectChips(interestOptions, selectedInterests)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(githubUrl, { githubUrl = it }, label = { Text("GitHub URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(portfolioUrl, { portfolioUrl = it }, label = { Text("Portfolio URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        step = when (step) {
                            CompleteProfileStep.BasicInfo -> CompleteProfileStep.BasicInfo
                            CompleteProfileStep.Roles -> CompleteProfileStep.BasicInfo
                            CompleteProfileStep.RoleSkills -> CompleteProfileStep.Roles
                            CompleteProfileStep.Review -> CompleteProfileStep.RoleSkills
                        }
                    },
                    enabled = step != CompleteProfileStep.BasicInfo,
                    modifier = Modifier.weight(1f)
                ) { Text("Back") }

                Button(
                    onClick = {
                        if (step == CompleteProfileStep.Review) {
                            profileViewModel.saveCompleteProfile(
                                displayName = displayName.ifBlank { null },
                                bio = bio.ifBlank { null },
                                location = location.ifBlank { null },
                                mode = selectedMode,
                                portfolioUrl = portfolioUrl.ifBlank { null },
                                githubUrl = githubUrl.ifBlank { null },
                                shortTermGoal = goal.ifBlank { null },
                                roles = selectedRoles.toList(),
                                skillsByRole = selectedSkillsByRole.mapValues { it.value.toMap() },
                                interests = selectedInterests.toList(),
                                onSuccess = onComplete
                            )
                        } else {
                            step = when (step) {
                                CompleteProfileStep.BasicInfo -> CompleteProfileStep.Roles
                                CompleteProfileStep.Roles -> CompleteProfileStep.RoleSkills
                                CompleteProfileStep.RoleSkills -> CompleteProfileStep.Review
                                CompleteProfileStep.Review -> CompleteProfileStep.Review
                            }
                        }
                    },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (step == CompleteProfileStep.Review) "Save" else "Next")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("Skip for now") }

            uiState.error?.let { error ->
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(error, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(14.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ChoiceChips(
    options: List<Pair<String, String>>,
    selectedValue: String,
    onSelect: (String) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(selected = selectedValue == value, onClick = { onSelect(value) }, label = { Text(label) })
        }
    }
}

@Composable
private fun MultiSelectChips(
    options: List<String>,
    selectedItems: MutableList<String>,
    maxSelection: Int = Int.MAX_VALUE
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { item ->
            val selected = selectedItems.contains(item)
            FilterChip(
                selected = selected,
                onClick = {
                    if (selected) selectedItems.remove(item) else if (selectedItems.size < maxSelection) selectedItems.add(item)
                },
                label = { Text(item) }
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
    val levels = listOf("BEGINNER", "INTERMEDIATE", "ADVANCED")
    Column {
        Text("Selected skills", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            selectedSkills.forEach { (skill, level) ->
                var expanded by remember { mutableStateOf(false) }
                Box {
                    AssistChip(
                        onClick = { expanded = true },
                        label = { Text("$skill • ${level.lowercase().replaceFirstChar { it.uppercase() }}") },
                        trailingIcon = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null) }
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        levels.forEach { option ->
                            DropdownMenuItem(text = { Text(option.lowercase().replaceFirstChar { it.uppercase() }) }, onClick = {
                                onLevelChange(skill, option)
                                expanded = false
                            })
                        }
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("Remove", color = MaterialTheme.colorScheme.error) }, onClick = {
                            onRemove(skill)
                            expanded = false
                        })
                    }
                }
            }
        }
    }
}
