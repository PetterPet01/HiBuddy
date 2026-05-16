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

@Composable
fun TasksScreen(
    onCreateTask: (projectId: String) -> Unit = {},
    viewModel: TasksViewModel = viewModel(factory = TasksViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var showStatusDialog by remember { mutableStateOf<TaskResponse?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadProjects()
    }

    val projects = uiState.projects
    val selectedProjectId = uiState.selectedProjectId
    val tasks = uiState.tasks

    val todoTasks = tasks.filter { it.status == "TODO" }
    val inProgressTasks = tasks.filter { it.status == "IN_PROGRESS" }
    val reviewTasks = tasks.filter { it.status == "DONE_REVIEW" }
    val closedTasks = tasks.filter { it.status == "CLOSED" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D14))
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
                    color = Color(0xFFF0EFF8)
                )
                Spacer(Modifier.height(12.dp))
                if (projects.isEmpty() && !uiState.isLoading) {
                    Text("No projects yet", fontSize = 14.sp, color = Color(0xFF8B8AAC))
                } else if (projects.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Viewing:  ", fontSize = 12.sp, color = Color(0xFF6B6A8C))
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
                                        if (isSelected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier
                                    )
                                    .clickable { viewModel.selectProject(project.id) }
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        val selectedProject = projects.find { it.id == selectedProjectId }
                        if (selectedProject != null) {
                            Text(
                                text = selectedProject.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF0EFF8)
                            )
                        }
                    }
                }
            }
            if (selectedProjectId != null) {
                IconButton(
                    onClick = { onCreateTask(selectedProjectId) },
                    modifier = Modifier.background(Color(0xFF1E1D2E), RoundedCornerShape(12.dp)).size(48.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Task", tint = Color.White)
                }
            }
        }

        if (uiState.isLoading && tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF7C6AF7))
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
            KanbanColumn("To Do", Color(0xFF6B6A8C), todoTasks) { task ->
                showStatusDialog = task
            }
            KanbanColumn("In Progress", Color(0xFFFFD166), inProgressTasks) { task ->
                showStatusDialog = task
            }
            KanbanColumn("Review", Color(0xFF7C6AF7), reviewTasks) { task ->
                showStatusDialog = task
            }
            KanbanColumn("Closed", Color(0xFF4CAF50), closedTasks) { task ->
                showStatusDialog = task
            }
        }
    }

    showStatusDialog?.let { task ->
        TaskActionDialog(
            task = task,
            onDismiss = { showStatusDialog = null },
            onAction = { newStatus ->
                viewModel.updateTaskStatus(task.id, newStatus)
                showStatusDialog = null
            },
            onCheckout = {
                viewModel.checkoutTask(task.id)
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
}

@Composable
fun KanbanColumn(
    title: String,
    accentColor: Color,
    tasks: List<TaskResponse>,
    onTaskClick: (TaskResponse) -> Unit
) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(Color(0xFF13131F), RoundedCornerShape(16.dp))
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
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF1E1D2E)) {
                Text(
                    text = tasks.size.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 12.sp,
                    color = Color(0xFFF0EFF8)
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
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E1D2E),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (task.tag != null) {
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF2A2840)) {
                        Text(
                            text = task.tag,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            color = Color(0xFFB0AFC8)
                        )
                    }
                } else {
                    Spacer(Modifier.size(1.dp))
                }

                val priorityColor = when (task.priority) {
                    "LOW" -> Color(0xFF4ECDC4)
                    "MEDIUM" -> Color(0xFFFFD166)
                    "HIGH" -> Color(0xFFFF8C42)
                    "URGENT" -> Color(0xFFFF4D6D)
                    else -> Color(0xFFFFD166)
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
                color = Color(0xFFF0EFF8),
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
                        modifier = Modifier.size(20.dp).background(Color(0xFF7C6AF7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = (task.assigneeName ?: "?").first().toString(), fontSize = 10.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = task.assigneeName ?: "Unknown", fontSize = 11.sp, color = Color(0xFF8B8AAC))
                }

                val deadlineText = task.deadline.take(10)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = deadlineText, fontSize = 11.sp, color = Color(0xFF8B8AAC))
                }
            }
        }
    }
}

@Composable
fun TaskActionDialog(
    task: TaskResponse,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit,
    onCheckout: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF16152A),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(task.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF0EFF8))
        },
        text = {
            Column {
                Text("Status: ${task.status}", fontSize = 14.sp, color = Color(0xFF8B8AAC))
                Text("Priority: ${task.priority}", fontSize = 14.sp, color = Color(0xFF8B8AAC))
                Text("Assignee: ${task.assigneeName ?: "Unknown"}", fontSize = 14.sp, color = Color(0xFF8B8AAC))
                if (task.description != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(task.description, fontSize = 13.sp, color = Color(0xFFB0AFC8))
                }
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (task.status) {
                    "TODO" -> {
                        Button(
                            onClick = { onAction("IN_PROGRESS") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD166)),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("Start Task", color = Color(0xFF4A3800)) }
                    }
                    "IN_PROGRESS" -> {
                        Button(
                            onClick = onCheckout,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C6AF7)),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("Checkout (Done)", color = Color.White) }
                    }
                    "DONE_REVIEW" -> {
                        Text("Waiting for project owner review", fontSize = 13.sp, color = Color(0xFF8B8AAC), modifier = Modifier.padding(vertical = 8.dp))
                    }
                    "CLOSED" -> {
                        Text("Task is completed", fontSize = 13.sp, color = Color(0xFF4CAF50), modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF8B8AAC))
            }
        }
    )
}
