package com.example.hibuddy.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.example.hibuddy.repository.ProjectRepository
import com.example.hibuddy.repository.ProjectApplicationRepository
import androidx.compose.foundation.clickable

@Composable
fun TasksScreen(
    onViewProjectDetail: (String) -> Unit
) {
    val projectRepository = remember { ProjectRepository() }
    val applicationRepository = remember { ProjectApplicationRepository() }

    var relatedProjects by remember {
        mutableStateOf<List<com.example.hibuddy.data.model.Project>>(emptyList())
    }

    var applications by remember {
        mutableStateOf<List<com.example.hibuddy.data.model.ProjectApplication>>(emptyList())
    }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val ownedProjects = relatedProjects.filter {
        it.ownerId == currentUid
    }

    val memberProjects = relatedProjects.filter {
        it.ownerId != currentUid && it.memberIds.contains(currentUid)
    }

    Text(
        text = "Owned Projects",
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )

    Spacer(Modifier.height(12.dp))

    ownedProjects.forEach { project ->
        TaskProjectCard(
            title = project.title,
            subtitle = project.field,
            status = "Owner",
            statusColor = Color(0xFFFFD166),
            onClick = {
                onViewProjectDetail(project.projectId)
            }
        )

        Spacer(Modifier.height(10.dp))
    }

    Spacer(Modifier.height(24.dp))

    Text(
        text = "Joined Projects",
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )

    Spacer(Modifier.height(12.dp))

    memberProjects.forEach { project ->
        TaskProjectCard(
            title = project.title,
            subtitle = project.field,
            status = "Member",
            statusColor = Color(0xFF4CAF50),
            onClick = {
                onViewProjectDetail(project.projectId)
            }
        )

        Spacer(Modifier.height(10.dp))
    }

    LaunchedEffect(Unit) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUid == null) {
            errorMessage = "You must login first"
            isLoading = false
            return@LaunchedEffect
        }

        projectRepository.getRelatedProjects(
            currentUid = currentUid,
            onSuccess = { projects ->
                relatedProjects = projects

                applicationRepository.getApplicationsByApplicant(
                    applicantId = currentUid,
                    onSuccess = { result ->
                        applications = result
                        isLoading = false
                    },
                    onFailure = { error ->
                        errorMessage = error
                        isLoading = false
                    }
                )
            },
            onFailure = { error ->
                errorMessage = error
                isLoading = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D14))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Projects & Tasks",
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFF0EFF8)
        )

        Spacer(Modifier.height(20.dp))

        if (isLoading) {
            CircularProgressIndicator(color = Color(0xFF7C6AF7))
            return@Column
        }

        if (errorMessage.isNotBlank()) {
            Text(errorMessage, color = Color(0xFFFF4D6D))
            return@Column
        }

        Spacer(Modifier.height(12.dp))

        if (relatedProjects.isEmpty()) {
            Text("You have no joined or owned projects yet.", color = Color(0xFF8B8AAC))
        } else {
            val currentUid =
                FirebaseAuth.getInstance().currentUser?.uid ?: ""

            val ownedProjects = relatedProjects.filter {
                it.ownerId == currentUid
            }

            val joinedProjects = relatedProjects.filter {
                it.ownerId != currentUid &&
                        it.memberIds.contains(currentUid)
            }
            Text(
                text = "Owned Projects",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(Modifier.height(12.dp))

            if (ownedProjects.isEmpty()) {

                Text(
                    "You don't own any projects yet.",
                    color = Color(0xFF8B8AAC)
                )

            } else {

                ownedProjects.forEach { project ->

                    TaskProjectCard(
                        title = project.title,

                        subtitle = project.field,

                        status = "Owner",

                        statusColor = Color(0xFFFFD166),

                        onClick = {
                            onViewProjectDetail(project.projectId)
                        }
                    )

                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(24.dp))

            Text(
                text = "Joined Projects",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(Modifier.height(12.dp))

            if (joinedProjects.isEmpty()) {

                Text(
                    "You haven't joined any projects yet.",
                    color = Color(0xFF8B8AAC)
                )

            } else {

                joinedProjects.forEach { project ->

                    TaskProjectCard(
                        title = project.title,

                        subtitle = project.field,

                        status = "Member",

                        statusColor = Color(0xFF4CAF50),

                        onClick = {
                            onViewProjectDetail(project.projectId)
                        }
                    )

                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Application Status",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Spacer(Modifier.height(12.dp))

        if (applications.isEmpty()) {
            Text("You have not applied to any projects yet.", color = Color(0xFF8B8AAC))
        } else {
            applications.forEach { application ->
                ApplicationStatusCard(
                    projectId = application.projectId,
                    status = application.status
                )

                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun TaskProjectCard(
    title: String,
    subtitle: String,
    status: String,
    statusColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF16152A)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(title.ifBlank { "Untitled Project" }, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle.ifBlank { "General" }, color = Color(0xFF8B8AAC))
            Spacer(Modifier.height(8.dp))
            Text(
                text = status,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C6AF7))
            ) {
                Text("View Detail")
            }
        }
    }
}

@Composable
fun ApplicationStatusCard(
    projectId: String,
    status: String
) {
    val statusColor = when (status) {
        "approved" -> Color(0xFF4CAF50)
        "rejected" -> Color(0xFFFF4D6D)
        else -> Color(0xFFFFD166)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF16152A)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Project ID", color = Color(0xFF8B8AAC), fontSize = 12.sp)
            Text(projectId, color = Color.White, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Status: ${status.uppercase()}",
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun KanbanColumn(status: TaskStatus, tasks: List<TaskItem>) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(Color(0xFF13131F), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        // Column Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = status.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B8AAC)
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1D2E)
            ) {
                Text(
                    text = tasks.size.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 12.sp,
                    color = Color(0xFFF0EFF8)
                )
            }
        }

        // Tasks List (Vertical Scroll inside column)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tasks.forEach { task ->
                TaskCardItem(task)
            }
        }
    }
}

@Composable
fun TaskCardItem(task: TaskItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E1D2E),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Tag & Priority
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF2A2840)
                ) {
                    Text(
                        text = task.tag,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        color = Color(0xFFB0AFC8)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(task.priority.color, CircleShape))
                    Text(text = task.priority.label, fontSize = 10.sp, color = task.priority.color)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = task.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF0EFF8),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Row: Assignee & Deadline
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
                        Text(text = task.assignee.first().toString(), fontSize = 10.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = task.assignee, fontSize = 11.sp, color = Color(0xFF8B8AAC))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⏱ ${task.deadline}", fontSize = 11.sp, color = Color(0xFF8B8AAC))
                }
            }
        }
    }
}