package com.example.hibuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hibuddy.data.model.Project
import com.example.hibuddy.repository.ProjectRepository

@Composable
fun UserProjectsScreen(
    userId: String,
    onBack: () -> Unit,
    onViewProjectDetail: (String) -> Unit
) {
    val repository = remember { ProjectRepository() }

    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        repository.getRelatedProjects(
            currentUid = userId,
            onSuccess = { result ->
                projects = result
                isLoading = false
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
            .padding(20.dp)
    ) {
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E1D2E)
            )
        ) {
            Text("Back")
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "User Projects",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(16.dp))

        when {
            isLoading -> {
                CircularProgressIndicator(color = Color(0xFF7C6AF7))
            }

            errorMessage.isNotBlank() -> {
                Text(errorMessage, color = Color(0xFFFF4D6D))
            }

            projects.isEmpty() -> {
                Text("No projects found.", color = Color(0xFF8B8AAC))
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(projects) { project ->
                        UserProjectItem(
                            project = project,
                            viewedUserId = userId,
                            onViewProjectDetail = {
                                onViewProjectDetail(project.projectId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserProjectItem(
    project: Project,
    viewedUserId: String,
    onViewProjectDetail: () -> Unit
) {
    val roleText = when {
        project.ownerId == viewedUserId -> "Owner"
        project.memberIds.contains(viewedUserId) -> "Member"
        else -> "Related"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF16152A)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = project.title.ifBlank { "Untitled Project" },
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = project.field,
                color = Color(0xFF8B8AAC)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = roleText,
                color = Color(0xFF7C6AF7),
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = project.description.ifBlank { "No description yet." },
                color = Color(0xFFB0AFC8),
                maxLines = 2
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onViewProjectDetail,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C6AF7)
                )
            ) {
                Text("View Detail")
            }
        }
    }
}