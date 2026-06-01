package com.example.hibuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.data.remote.dto.*
import com.example.hibuddy.ui.screens.tasks.TasksViewModel
import com.example.hibuddy.ui.theme.HiBuddyColors

@Composable
fun TasksScreen(
    onCreateTask: (projectId: String) -> Unit = {},
    onOpenProject: (projectId: String) -> Unit = {},
    viewModel: TasksViewModel = viewModel(factory = TasksViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    var showStatusDialog by remember { mutableStateOf<TaskResponse?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadProjects()
    }

    val projects = uiState.projects
    val selectedProjectId = uiState.selectedProjectId
    val tasks = uiState.tasks
    val selectedProject = projects.find { it.id == selectedProjectId }
    val isOwner = selectedProject?.ownerId == uiState.currentUserId

    val todoTasks = tasks.filter { it.status == "TODO" }
    val inProgressTasks = tasks.filter { it.status == "IN_PROGRESS" }
    val reviewTasks = tasks.filter { it.status == "DONE_REVIEW" }
    val closedTasks = tasks.filter { it.status == "CLOSED" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Project Tasks",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = colorScheme.onBackground
                )
                Spacer(Modifier.height(12.dp))
                if (projects.isEmpty() && !uiState.isLoading) {
                    Text("No projects yet", fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
                } else if (projects.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Viewing:  ", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                        projects.forEach { project ->
                            val isSelected = project.id == selectedProjectId
                            val projectColor = remember(project.id) {
                                val colors = listOf(Color(0xFF7C6AF7), Color(0xFF059669), Color(0xFFE03055))
                                colors[kotlin.math.abs(project.id.hashCode()) % colors.size]
                            }
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 22.dp else 16.dp)
                                    .clip(CircleShape)
                                    .background(projectColor)
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, colorScheme.surface, CircleShape) else Modifier
                                    )
                                    .clickable { viewModel.selectProject(project.id) }
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        if (selectedProject != null) {
                            Column {
                                Text(
                                    text = selectedProject.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onBackground,
                                    modifier = Modifier.clickable { onOpenProject(selectedProject.id) }
                                )
                                Text(
                                    text = "Open project workspace",
                                    fontSize = 11.sp,
                                    color = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clickable { onOpenProject(selectedProject.id) }
                                )
                            }
                        }
                    }
                }
            }
            if (selectedProjectId != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onOpenProject(selectedProjectId) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.onSurface)
                    ) {
                        Text("Open", fontSize = 12.sp)
                    }
                    if (isOwner) {
                        IconButton(
                            onClick = { onCreateTask(selectedProjectId) },
                            modifier = Modifier.background(colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).size(48.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Task", tint = colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (uiState.isLoading && tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colorScheme.primary)
            }
            return@Column
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            KanbanColumn("To Do", colorScheme.onSurfaceVariant, todoTasks) { task ->
                showStatusDialog = task
            }
            KanbanColumn("In Progress", HiBuddyColors.warning, inProgressTasks) { task ->
                showStatusDialog = task
            }
            KanbanColumn("Review", colorScheme.primary, reviewTasks) { task ->
                showStatusDialog = task
            }
            KanbanColumn("Closed", HiBuddyColors.success, closedTasks) { task ->
                showStatusDialog = task
            }
        }
    }

    showStatusDialog?.let { task ->
        TaskActionDialog(
            task = task,
            currentUserId = uiState.currentUserId,
            isOwner = isOwner,
            onDismiss = { showStatusDialog = null },
            onAction = { newStatus ->
                viewModel.updateTaskStatus(task.id, newStatus)
                showStatusDialog = null
            },
            onCheckout = {
                viewModel.checkoutTask(task.id)
                showStatusDialog = null
            },
            onConfirmCheckout = {
                viewModel.confirmCheckout(task.id)
                showStatusDialog = null
            }
        )
    }

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    uiState.message?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(2500)
            viewModel.clearMessage()
        }
    }
}

@Composable
fun KanbanColumn(
    title: String,
    accentColor: Color,
    tasks: List<TaskResponse>,
    onTaskClick: (TaskResponse) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(colorScheme.surface, shape)
            .border(1.dp, colorScheme.outline.copy(alpha = 0.28f), shape)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Surface(shape = RoundedCornerShape(12.dp), color = colorScheme.surfaceVariant) {
                Text(
                    text = tasks.size.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tasks.forEach { task ->
                TaskCardItem(task = task, onClick = { onTaskClick(task) })
            }
        }
    }
}

@Composable
fun TaskCardItem(task: TaskResponse, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = colorScheme.surfaceVariant,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (task.tag != null) {
                    Surface(shape = RoundedCornerShape(6.dp), color = colorScheme.surface) {
                        Text(
                            text = task.tag,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Spacer(Modifier.size(1.dp))
                }

                val priorityColor = when (task.priority) {
                    "LOW" -> HiBuddyColors.info
                    "MEDIUM" -> HiBuddyColors.warning
                    "HIGH" -> Color(0xFF9A5200)
                    "URGENT" -> colorScheme.error
                    else -> HiBuddyColors.warning
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(priorityColor, CircleShape))
                    Text(text = task.priority.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 10.sp, color = priorityColor)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = task.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(20.dp).background(colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = (task.assigneeName ?: "?").first().toString(), fontSize = 10.sp, color = colorScheme.onPrimary)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = task.assigneeName ?: "Unknown", fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
                }

                val deadlineText = task.deadline.take(10)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = deadlineText, fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun TaskActionDialog(
    task: TaskResponse,
    currentUserId: String,
    isOwner: Boolean,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit,
    onCheckout: () -> Unit,
    onConfirmCheckout: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(task.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
        },
        text = {
            Column {
                Text("Status: ${task.status}", fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
                Text("Priority: ${task.priority}", fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
                Text("Assignee: ${task.assigneeName ?: "Unknown"}", fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
                if (task.description != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(task.description, fontSize = 13.sp, color = colorScheme.onSurface)
                }
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (task.status) {
                    "TODO" -> {
                        if (task.assigneeId == currentUserId) {
                            Button(
                                onClick = { onAction("IN_PROGRESS") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HiBuddyColors.warningContainer,
                                    contentColor = HiBuddyColors.onWarningContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("Start Task") }
                        } else {
                            Text(
                                "Only ${task.assigneeName ?: "the assignee"} can start this task",
                                fontSize = 13.sp,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    "IN_PROGRESS" -> {
                        if (task.assigneeId == currentUserId) {
                            Button(
                                onClick = onCheckout,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.primary,
                                    contentColor = colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("Checkout (Done)") }
                        } else {
                            Text(
                                "Waiting for ${task.assigneeName ?: "the assignee"} to finish this task",
                                fontSize = 13.sp,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    "DONE_REVIEW" -> {
                        if (isOwner) {
                            Button(
                                onClick = onConfirmCheckout,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HiBuddyColors.successContainer,
                                    contentColor = HiBuddyColors.onSuccessContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("Approve and Close") }
                        } else {
                            Text("Waiting for project owner review", fontSize = 13.sp, color = colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                    "CLOSED" -> {
                        Text("Task is completed", fontSize = 13.sp, color = HiBuddyColors.success, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = colorScheme.onSurfaceVariant)
            }
        }
    )
}
