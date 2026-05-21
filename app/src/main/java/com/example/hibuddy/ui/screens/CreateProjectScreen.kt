package com.example.hibuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hibuddy.data.model.Project
import com.example.hibuddy.repository.ProjectRepository
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import com.example.hibuddy.data.model.projectOptions
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
@Composable
fun CreateProjectScreen(
    onProjectCreated: () -> Unit = {}
) {
    var title by remember { mutableStateOf("") }
    var field by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedRoles by remember { mutableStateOf(listOf<String>()) }
    var selectedSkills by remember { mutableStateOf(listOf<String>()) }
    var timeline by remember { mutableStateOf("Flexible") }
    var workMode by remember { mutableStateOf("Online") }
    var commitment by remember { mutableStateOf("Casual") }
    var maxMembers by remember { mutableStateOf(5) }

    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val projectRepository = remember { ProjectRepository() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D14))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Create Project",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(20.dp))

        ProjectTextField("Project title", title) { title = it }
        FieldDropdown(
            selectedField = field,
            onFieldSelected = {
                field = it
            }
        )
        ProjectTextField("Description", description) { description = it }
        RoleSkillSelector(
            selectedField = field,
            selectedRoles = selectedRoles,
            onRolesChange = {
                selectedRoles = it
                selectedSkills = emptyList()
            },
            selectedSkills = selectedSkills,
            onSkillsChange = {
                selectedSkills = it
            }
        )
        TimelineDropdown(
            selectedTimeline = timeline,
            onTimelineSelected = {
                timeline = it
            }
        )
        WorkModeDropdown(
            selectedWorkMode = workMode,
            onWorkModeSelected = {
                workMode = it
            }
        )
        CommitmentDropdown(
            selectedCommitment = commitment,
            onCommitmentSelected = {
                commitment = it
            }
        )
        MaxMembersStepper(
            value = maxMembers,
            onValueChange = {
                maxMembers = it
            }
        )

        if (errorMessage.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(errorMessage, color = Color(0xFFFF4D6D))
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid

                if (currentUid == null) {
                    errorMessage = "You must login first"
                    return@Button
                }

                if (title.isBlank() || description.isBlank()) {
                    errorMessage = "Please enter title and description"
                    return@Button
                }

                isLoading = true
                errorMessage = ""

                val projectId = UUID.randomUUID().toString()

                val project = Project(
                    projectId = projectId,
                    ownerId = currentUid,
                    title = title.trim(),
                    field = field.trim(),
                    description = description.trim(),
                    rolesNeeded = selectedRoles,
                    skillsNeeded = selectedSkills,
                    timeline = timeline.trim(),
                    workMode = workMode.trim(),
                    commitment = commitment.trim(),
                    maxMembers = maxMembers,
                    currentMembers = 1,
                    tags = listOf(field.trim()).filter { it.isNotBlank() },
                    isOpen = true
                )

                projectRepository.createProject(
                    project = project,
                    onSuccess = {
                        isLoading = false
                        onProjectCreated()
                    },
                    onFailure = {
                        isLoading = false
                        errorMessage = it
                    }
                )
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7C6AF7)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = if (isLoading) "Creating..." else "Create Project",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProjectTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        singleLine = label != "Description",
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = Color(0xFF7C6AF7),
            unfocusedLabelColor = Color(0xFF8B8AAC),
            focusedBorderColor = Color(0xFF7C6AF7),
            unfocusedBorderColor = Color(0xFF2E2D45)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldDropdown(
    selectedField: String,
    onFieldSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val fieldOptions = projectOptions.map { it.field }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        OutlinedTextField(
            value = selectedField,
            onValueChange = {},
            readOnly = true,
            label = {
                Text("Field")
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color(0xFF7C6AF7),
                unfocusedLabelColor = Color(0xFF8B8AAC),
                focusedBorderColor = Color(0xFF7C6AF7),
                unfocusedBorderColor = Color(0xFF2E2D45)
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            fieldOptions.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(option)
                    },
                    onClick = {
                        onFieldSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoleSkillSelector(
    selectedField: String,
    selectedRoles: List<String>,
    onRolesChange: (List<String>) -> Unit,
    selectedSkills: List<String>,
    onSkillsChange: (List<String>) -> Unit
) {
    val fieldOption = projectOptions.find { it.field == selectedField }

    val roleOptions = fieldOption?.roles?.map { it.role } ?: emptyList()

    val skillOptions = fieldOption?.roles
        ?.filter { it.role in selectedRoles }
        ?.flatMap { it.skills }
        ?.distinct()
        ?: emptyList()

    Text(
        text = "Roles needed",
        color = Color(0xFF8B8AAC),
        fontWeight = FontWeight.Bold
    )

    Spacer(Modifier.height(8.dp))

    if (selectedField.isBlank()) {
        Text(
            text = "Please select a field first",
            color = Color(0xFF6B6A8C),
            fontSize = 13.sp
        )
    } else {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            roleOptions.forEach { role ->
                val selected = role in selectedRoles

                FilterChip(
                    selected = selected,
                    onClick = {
                        onRolesChange(
                            if (selected) selectedRoles - role
                            else selectedRoles + role
                        )
                    },
                    label = { Text(role) }
                )
            }
        }
    }

    Spacer(Modifier.height(20.dp))

    Text(
        text = "Skills needed",
        color = Color(0xFF8B8AAC),
        fontWeight = FontWeight.Bold
    )

    Spacer(Modifier.height(8.dp))

    if (selectedRoles.isEmpty()) {
        Text(
            text = "Please select at least one role first",
            color = Color(0xFF6B6A8C),
            fontSize = 13.sp
        )
    } else {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            skillOptions.forEach { skill ->
                val selected = skill in selectedSkills

                FilterChip(
                    selected = selected,
                    onClick = {
                        onSkillsChange(
                            if (selected) selectedSkills - skill
                            else selectedSkills + skill
                        )
                    },
                    label = { Text(skill) }
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineDropdown(
    selectedTimeline: String,
    onTimelineSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val timelineOptions = listOf(
        "1–2 weeks",
        "1 month",
        "2–3 months",
        "3–6 months",
        "6+ months",
        "Long-term",
        "Flexible",
        "Hackathon only",
        "Until MVP launch"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        OutlinedTextField(
            value = selectedTimeline,
            onValueChange = {},
            readOnly = true,
            label = {
                Text("Timeline")
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color(0xFF7C6AF7),
                unfocusedLabelColor = Color(0xFF8B8AAC),
                focusedBorderColor = Color(0xFF7C6AF7),
                unfocusedBorderColor = Color(0xFF2E2D45)
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            timelineOptions.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(option)
                    },
                    onClick = {
                        onTimelineSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
fun MaxMembersStepper(
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Text(
            text = "Max members",
            color = Color(0xFF8B8AAC),
            fontSize = 13.sp
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    if (value > 2) onValueChange(value - 1)
                }
            ) {
                Text("-")
            }

            OutlinedTextField(
                value = value.toString(),
                onValueChange = { input ->
                    val number = input.toIntOrNull()
                    if (number != null) {
                        onValueChange(number.coerceIn(2, 50))
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    color = Color.White
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF7C6AF7),
                    unfocusedBorderColor = Color(0xFF2E2D45)
                )
            )

            OutlinedButton(
                onClick = {
                    if (value < 50) onValueChange(value + 1)
                }
            ) {
                Text("+")
            }
        }

        Text(
            text = "Allowed range: 2–50 members",
            color = Color(0xFF6B6A8C),
            fontSize = 11.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommitmentDropdown(
    selectedCommitment: String,
    onCommitmentSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val commitmentOptions = listOf(
        "Casual",
        "Part-time",
        "Serious",
        "Full-time",
        "Hackathon",
        "Flexible"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        OutlinedTextField(
            value = selectedCommitment,
            onValueChange = {},
            readOnly = true,
            label = {
                Text("Commitment")
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color(0xFF7C6AF7),
                unfocusedLabelColor = Color(0xFF8B8AAC),
                focusedBorderColor = Color(0xFF7C6AF7),
                unfocusedBorderColor = Color(0xFF2E2D45)
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            commitmentOptions.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(option)
                    },
                    onClick = {
                        onCommitmentSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkModeDropdown(
    selectedWorkMode: String,
    onWorkModeSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val workModeOptions = listOf(
        "Online",
        "Offline",
        "Hybrid",
        "Flexible",
        "Async"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        OutlinedTextField(
            value = selectedWorkMode,
            onValueChange = {},
            readOnly = true,
            label = {
                Text("Work mode")
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color(0xFF7C6AF7),
                unfocusedLabelColor = Color(0xFF8B8AAC),
                focusedBorderColor = Color(0xFF7C6AF7),
                unfocusedBorderColor = Color(0xFF2E2D45)
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            workModeOptions.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(option)
                    },
                    onClick = {
                        onWorkModeSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
}