package com.example.hibuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.data.remote.dto.*
import com.example.hibuddy.ui.screens.tasks.TasksViewModel
import com.example.hibuddy.ServiceLocator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleCreateTaskScreen(
    projectId: String,
    onBack: () -> Unit,
    tasksViewModel: TasksViewModel = viewModel(factory = TasksViewModel.Factory)
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var assigneeId by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var startDate by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var roleRelated by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }

    val priorities = listOf("LOW", "MEDIUM", "HIGH", "URGENT")

    LaunchedEffect(success) {
        if (success) onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D14))
    ) {
        TopAppBar(
            title = { Text("Create Task", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF0EFF8)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = Color(0xFFF0EFF8))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF13131F))
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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

            OutlinedTextField(
                value = assigneeId,
                onValueChange = { assigneeId = it },
                label = { Text("Assignee User ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = darkTextFieldColors()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start Date") },
                    placeholder = { Text("DD/MM/YYYY") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = darkTextFieldColors()
                )
                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text("Deadline") },
                    placeholder = { Text("DD/MM/YYYY") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = darkTextFieldColors()
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
                    priorities.forEach { p ->
                        DropdownMenuItem(text = { Text(p) }, onClick = { priority = p; priorityExpanded = false })
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

            OutlinedTextField(
                value = roleRelated,
                onValueChange = { roleRelated = it },
                label = { Text("Related Role (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = darkTextFieldColors()
            )

            Spacer(Modifier.height(8.dp))

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
                                success = true
                            },
                            onFailure = { e ->
                                isLoading = false
                                error = e.message
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading && title.isNotBlank() && assigneeId.isNotBlank() && startDate.isNotBlank() && deadline.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C6AF7)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Create Task", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            error?.let {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0x33FF4D6D))) {
                    Text(it, modifier = Modifier.padding(16.dp), color = Color(0xFFFF4D6D), fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun darkTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF7C6AF7),
    unfocusedBorderColor = Color(0xFF2A2840),
    focusedLabelColor = Color(0xFF7C6AF7),
    unfocusedLabelColor = Color(0xFF6B6A8C),
    cursorColor = Color(0xFF7C6AF7),
    focusedTextColor = Color(0xFFF0EFF8),
    unfocusedTextColor = Color(0xFFF0EFF8),
    focusedContainerColor = Color(0xFF13131F),
    unfocusedContainerColor = Color(0xFF13131F),
)
