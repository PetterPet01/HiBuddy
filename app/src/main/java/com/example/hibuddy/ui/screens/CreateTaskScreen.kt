package com.example.hibuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hibuddy.data.remote.dto.*
import com.example.hibuddy.ServiceLocator
import com.example.hibuddy.ui.theme.hiBuddyTextFieldColors
import kotlinx.coroutines.launch
import com.example.hibuddy.ui.components.DatePickerField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleCreateTaskScreen(
    projectId: String,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var project by remember { mutableStateOf<ProjectResponse?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var assigneeId by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var startDate by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var roleRelated by remember { mutableStateOf("") }
    var isProjectLoading by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }
    var assigneeExpanded by remember { mutableStateOf(false) }
    var roleExpanded by remember { mutableStateOf(false) }
    val currentUserId = ServiceLocator.authRepository.getUserId().orEmpty()

    val priorities = listOf("LOW", "MEDIUM", "HIGH", "URGENT")
    val members = project?.members.orEmpty()
    val assignableMembers = members
    val canCreateTask = project?.ownerId == currentUserId
    val availableRoles = members.map { it.role }.distinct()

    LaunchedEffect(success) {
        if (success) onBack()
    }

    LaunchedEffect(projectId) {
        isProjectLoading = true
        ServiceLocator.projectRepository.getProject(projectId).fold(
            onSuccess = { loadedProject ->
                project = loadedProject
                val defaultMember = loadedProject.members.firstOrNull { it.userId == currentUserId }
                    ?: loadedProject.members.firstOrNull()
                if (assigneeId.isBlank()) {
                    assigneeId = defaultMember?.userId.orEmpty()
                }
                if (roleRelated.isBlank()) {
                    roleRelated = defaultMember?.role.orEmpty()
                }
                isProjectLoading = false
            },
            onFailure = { e ->
                error = e.message ?: "Failed to load project"
                isProjectLoading = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Create Task", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = colorScheme.onSurface)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.surface)
        )

        if (isProjectLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colorScheme.primary)
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            project?.let { activeProject ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(activeProject.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${activeProject.members.size} members · ${activeProject.roleSlots.count { it.filled < it.count }} open roles",
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!canCreateTask) {
                Card(colors = CardDefaults.cardColors(containerColor = colorScheme.errorContainer)) {
                    Text(
                        "Only the project owner can create and assign tasks.",
                        modifier = Modifier.padding(16.dp),
                        color = colorScheme.onErrorContainer,
                        fontSize = 14.sp
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = darkTextFieldColors()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                colors = darkTextFieldColors()
            )

            ExposedDropdownMenuBox(
                expanded = assigneeExpanded,
                onExpandedChange = { assigneeExpanded = it }
            ) {
                OutlinedTextField(
                    value = assignableMembers.find { it.userId == assigneeId }?.displayName ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Assignee") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = assigneeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = darkTextFieldColors()
                )
                ExposedDropdownMenu(expanded = assigneeExpanded, onDismissRequest = { assigneeExpanded = false }) {
                    assignableMembers.forEach { member ->
                        DropdownMenuItem(
                            text = { Text("${member.displayName} - ${member.role}${if (member.isOwner) " (Owner)" else ""}") },
                            onClick = {
                                assigneeId = member.userId
                                assigneeExpanded = false
                            }
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DatePickerField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = "Start Date",
                    modifier = Modifier.weight(1f)
                )
                DatePickerField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = "Deadline",
                    modifier = Modifier.weight(1f)
                )
            }

            ExposedDropdownMenuBox(
                expanded = priorityExpanded,
                onExpandedChange = { priorityExpanded = it }
            ) {
                OutlinedTextField(
                    value = priority,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Priority") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = darkTextFieldColors()
                )
                ExposedDropdownMenu(expanded = priorityExpanded, onDismissRequest = { priorityExpanded = false }) {
                    priorities.forEach { selectedPriority ->
                        DropdownMenuItem(
                            text = { Text(selectedPriority) },
                            onClick = {
                                priority = selectedPriority
                                priorityExpanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = roleExpanded,
                onExpandedChange = { roleExpanded = it && availableRoles.isNotEmpty() }
            ) {
                OutlinedTextField(
                    value = roleRelated,
                    onValueChange = { roleRelated = it },
                    readOnly = availableRoles.isNotEmpty(),
                    label = { Text("Related Role (optional)") },
                    trailingIcon = {
                        if (availableRoles.isNotEmpty()) {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = darkTextFieldColors()
                )
                if (availableRoles.isNotEmpty()) {
                    ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                        availableRoles.forEach { roleName ->
                            DropdownMenuItem(
                                text = { Text(roleName) },
                                onClick = {
                                    roleRelated = roleName
                                    roleExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = tag,
                onValueChange = { tag = it },
                label = { Text("Tag (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = darkTextFieldColors()
            )

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        error = null
                        ServiceLocator.taskRepository.createTask(
                            projectId,
                            CreateTaskRequest(
                                title = title,
                                description = description.ifBlank { null },
                                assigneeId = assigneeId,
                                roleRelated = roleRelated.ifBlank { null },
                                priority = priority,
                                startDate = startDate,
                                deadline = deadline,
                                tag = tag.ifBlank { null }
                            )
                        ).fold(
                            onSuccess = {
                                isLoading = false
                                success = true
                            },
                            onFailure = { e ->
                                isLoading = false
                                error = e.message ?: "Failed to create task"
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = canCreateTask && !isLoading && title.isNotBlank() && assigneeId.isNotBlank() && startDate.isNotBlank() && deadline.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = colorScheme.onPrimary)
                } else {
                    Text("Create Task", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            error?.let {
                Card(colors = CardDefaults.cardColors(containerColor = colorScheme.errorContainer)) {
                    Text(it, modifier = Modifier.padding(16.dp), color = colorScheme.onErrorContainer, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun darkTextFieldColors() = hiBuddyTextFieldColors()
