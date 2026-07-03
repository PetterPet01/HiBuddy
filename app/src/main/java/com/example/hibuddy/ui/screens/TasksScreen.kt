package com.example.hibuddy.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.ServiceLocator
import com.example.hibuddy.data.remote.dto.*
import com.example.hibuddy.ui.components.DatePickerField
import com.example.hibuddy.ui.screens.tasks.TasksViewModel
import com.example.hibuddy.ui.theme.HiBuddyColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TasksScreen(
    onCreateTask: (projectId: String) -> Unit = {},
    onOpenProject: (projectId: String) -> Unit = {},
    viewModel: TasksViewModel = viewModel(factory = TasksViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    val lifecycleOwner = LocalLifecycleOwner.current
    var showStatusDialog by remember { mutableStateOf<TaskResponse?>(null) }
    var editTask by remember { mutableStateOf<TaskResponse?>(null) }
    var submitTask by remember { mutableStateOf<TaskResponse?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && uiState.currentUserId.isNotBlank()) {
                viewModel.loadProjects()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isOwner && selectedProject?.status != "CLOSED") {
                        OutlinedButton(
                            onClick = { viewModel.closeProject(selectedProjectId) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = HiBuddyColors.warning)
                        ) {
                            Text("Close Project", fontSize = 12.sp)
                        }
                    }
                    if (isOwner) {
                        IconButton(
                            onClick = { onCreateTask(selectedProjectId) },
                            modifier = Modifier
                                .background(colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .size(48.dp)
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
            project = selectedProject,
            currentUserId = uiState.currentUserId,
            isOwner = isOwner,
            onDismiss = { showStatusDialog = null },
            onEdit = {
                editTask = task
                showStatusDialog = null
            },
            onDelete = {
                viewModel.deleteTask(task.id)
                showStatusDialog = null
            },
            onAction = { newStatus ->
                viewModel.updateTaskStatus(task.id, newStatus)
                showStatusDialog = null
            },
            onCheckout = {
                submitTask = task
                showStatusDialog = null
            },
            onConfirmCheckout = {
                viewModel.confirmCheckout(task.id)
                showStatusDialog = null
            }
        )
    }

    editTask?.let { task ->
        TaskEditDialog(
            task = task,
            project = selectedProject,
            onDismiss = { editTask = null },
            onSave = { request ->
                viewModel.updateTask(task.id, request)
                editTask = null
            }
        )
    }

    submitTask?.let { task ->
        TaskSubmissionDialog(
            task = task,
            onDismiss = { submitTask = null },
            onSubmit = { request ->
                viewModel.checkoutTask(task.id, request)
                submitTask = null
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
                        Text(
                            text = task.assigneeName?.firstOrNull()?.toString() ?: "?",
                            fontSize = 10.sp,
                            color = colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = task.assigneeName?.takeIf { it.isNotBlank() } ?: "Unknown", fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
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
    project: ProjectResponse?,
    currentUserId: String,
    isOwner: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(task.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                if (isOwner && task.status != "CLOSED") {
                    Row {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit task", tint = colorScheme.primary)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete task", tint = colorScheme.error)
                        }
                    }
                }
            }
        },
        text = {
            Column {
                Text("Status: ${task.status}", fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
                Text("Priority: ${task.priority}", fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
                Text("Assignee: ${task.assigneeName ?: "Unknown"}", fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
                project?.let {
                    Text("Project: ${it.title}", fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
                }
                if (task.description != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(task.description, fontSize = 13.sp, color = colorScheme.onSurface)
                }
                if (task.attachmentUrls.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Task attachments", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
                    task.attachmentUrls.forEach { attachment ->
                        Text(
                            "• ${attachment.name ?: attachment.url}",
                            fontSize = 12.sp,
                            color = colorScheme.primary
                        )
                    }
                }
                if (task.submissionNote != null || task.submissionLinks.isNotEmpty() || task.submissionAttachments.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Submission", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
                    task.submissionNote?.takeIf { it.isNotBlank() }?.let {
                        Text(it, fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                    }
                    task.submissionLinks.forEach { link ->
                        Text("• $link", fontSize = 12.sp, color = colorScheme.primary)
                    }
                    task.submissionAttachments.forEach { attachment ->
                        Text(
                            "• ${attachment.name ?: attachment.url}",
                            fontSize = 12.sp,
                            color = colorScheme.primary
                        )
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditDialog(
    task: TaskResponse,
    project: ProjectResponse?,
    onDismiss: () -> Unit,
    onSave: (UpdateTaskRequest) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val members = project?.members.orEmpty()
    var title by remember(task.id) { mutableStateOf(task.title) }
    var description by remember(task.id) { mutableStateOf(task.description.orEmpty()) }
    var assigneeId by remember(task.id) { mutableStateOf(task.assigneeId) }
    var priority by remember(task.id) { mutableStateOf(task.priority) }
    var startDate by remember(task.id) { mutableStateOf(task.startDate.take(10).split("-").reversed().joinToString("/")) }
    var deadline by remember(task.id) { mutableStateOf(task.deadline.take(10).split("-").reversed().joinToString("/")) }
    var tag by remember(task.id) { mutableStateOf(task.tag.orEmpty()) }
    var roleRelated by remember(task.id) { mutableStateOf(task.roleRelated.orEmpty()) }
    var attachments by remember(task.id) { mutableStateOf(task.attachmentUrls) }
    var assigneeExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }
    var roleExpanded by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val availableRoles = members.map { it.role }.distinct()
    val attachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isUploading = true
            error = null
            val payload = runCatching { readUploadPayload(context, uri) }.getOrElse {
                isUploading = false
                error = it.message ?: "Could not read selected file"
                return@launch
            }
            ServiceLocator.taskRepository.uploadTaskAttachment(
                task.id,
                payload.bytes,
                payload.mimeType,
                payload.fileName
            ).fold(
                onSuccess = { response ->
                    val fileUrl = response.fileUrl
                    if (fileUrl != null) {
                        attachments = attachments + TaskAttachmentDto(
                            url = fileUrl,
                            name = response.fileName ?: payload.fileName,
                            contentType = response.contentType ?: payload.mimeType
                        )
                    }
                    isUploading = false
                },
                onFailure = {
                    isUploading = false
                    error = it.message ?: "Upload failed"
                }
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Edit Task", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                ExposedDropdownMenuBox(
                    expanded = assigneeExpanded,
                    onExpandedChange = { assigneeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = members.find { it.userId == assigneeId }?.displayName.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assignee") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = assigneeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = assigneeExpanded, onDismissRequest = { assigneeExpanded = false }) {
                        members.forEach { member ->
                            DropdownMenuItem(
                                text = { Text("${member.displayName} - ${member.role}") },
                                onClick = {
                                    assigneeId = member.userId
                                    assigneeExpanded = false
                                }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DatePickerField(value = startDate, onValueChange = { startDate = it }, label = "Start Date", modifier = Modifier.weight(1f))
                    DatePickerField(value = deadline, onValueChange = { deadline = it }, label = "Deadline", modifier = Modifier.weight(1f))
                }
                ExposedDropdownMenuBox(expanded = priorityExpanded, onExpandedChange = { priorityExpanded = it }) {
                    OutlinedTextField(
                        value = priority,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = priorityExpanded, onDismissRequest = { priorityExpanded = false }) {
                        listOf("LOW", "MEDIUM", "HIGH", "URGENT").forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = {
                                priority = option
                                priorityExpanded = false
                            })
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
                        label = { Text("Related Role") },
                        trailingIcon = {
                            if (availableRoles.isNotEmpty()) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    if (availableRoles.isNotEmpty()) {
                        ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                            availableRoles.forEach { roleName ->
                                DropdownMenuItem(text = { Text(roleName) }, onClick = {
                                    roleRelated = roleName
                                    roleExpanded = false
                                })
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Tag") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Task Attachments", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                attachments.forEach { attachment ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            attachment.name ?: attachment.url,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp
                        )
                        TextButton(onClick = { attachments = attachments.filterNot { it.url == attachment.url } }) {
                            Text("Remove")
                        }
                    }
                }
                OutlinedButton(
                    onClick = { attachmentPicker.launch("*/*") },
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.AttachFile, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Upload attachment")
                    }
                }
                error?.let { Text(it, color = colorScheme.error, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        UpdateTaskRequest(
                            title = title.trim(),
                            description = description.trim().ifBlank { null },
                            assigneeId = assigneeId,
                            roleRelated = roleRelated.trim().ifBlank { null },
                            priority = priority,
                            startDate = startDate,
                            deadline = deadline,
                            tag = tag.trim().ifBlank { null },
                            attachmentUrls = attachments
                        )
                    )
                },
                enabled = title.isNotBlank() && assigneeId.isNotBlank() && startDate.isNotBlank() && deadline.isNotBlank() && !isUploading
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskSubmissionDialog(
    task: TaskResponse,
    onDismiss: () -> Unit,
    onSubmit: (TaskSubmissionRequest) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var submissionNote by remember(task.id) { mutableStateOf(task.submissionNote.orEmpty()) }
    var linksText by remember(task.id) { mutableStateOf(task.submissionLinks.joinToString("\n")) }
    var attachments by remember(task.id) { mutableStateOf(task.submissionAttachments) }
    var isUploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val attachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isUploading = true
            error = null
            val payload = runCatching { readUploadPayload(context, uri) }.getOrElse {
                isUploading = false
                error = it.message ?: "Could not read selected file"
                return@launch
            }
            ServiceLocator.taskRepository.uploadTaskAttachment(
                task.id,
                payload.bytes,
                payload.mimeType,
                payload.fileName
            ).fold(
                onSuccess = { response ->
                    val fileUrl = response.fileUrl
                    if (fileUrl != null) {
                        attachments = attachments + TaskAttachmentDto(
                            url = fileUrl,
                            name = response.fileName ?: payload.fileName,
                            contentType = response.contentType ?: payload.mimeType
                        )
                    }
                    isUploading = false
                },
                onFailure = {
                    isUploading = false
                    error = it.message ?: "Upload failed"
                }
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Submit Task", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Send your notes, links, or files before moving this task to review.",
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = submissionNote,
                    onValueChange = { submissionNote = it },
                    label = { Text("Submission Note") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                OutlinedTextField(
                    value = linksText,
                    onValueChange = { linksText = it },
                    label = { Text("Submission Links") },
                    placeholder = { Text("One link per line") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                attachments.forEach { attachment ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            attachment.name ?: attachment.url,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp
                        )
                        TextButton(onClick = { attachments = attachments.filterNot { it.url == attachment.url } }) {
                            Text("Remove")
                        }
                    }
                }
                OutlinedButton(
                    onClick = { attachmentPicker.launch("*/*") },
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.AttachFile, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Attach file")
                    }
                }
                error?.let { Text(it, color = colorScheme.error, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        TaskSubmissionRequest(
                            submissionNote = submissionNote.trim().ifBlank { null },
                            submissionLinks = linksText.lines().map { it.trim() }.filter { it.isNotBlank() },
                            submissionAttachments = attachments
                        )
                    )
                },
                enabled = !isUploading
            ) {
                Text("Send For Review")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private data class UploadPayload(
    val bytes: ByteArray,
    val mimeType: String,
    val fileName: String
)

private suspend fun readUploadPayload(context: Context, uri: Uri): UploadPayload = withContext(Dispatchers.IO) {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: throw IllegalArgumentException("Could not read selected file")
    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
    val fileName = queryDisplayName(context, uri) ?: "task-attachment"
    UploadPayload(bytes, mimeType, fileName)
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }
}
