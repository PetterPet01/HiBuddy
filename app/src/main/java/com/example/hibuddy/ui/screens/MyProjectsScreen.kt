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
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MyProjectsScreen(
    onBack: () -> Unit,
    onViewApplicants: (String) -> Unit
) {
    val repository = remember { ProjectRepository() }

    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUid == null) {
            errorMessage = "You must login first"
            isLoading = false
            return@LaunchedEffect
        }

        repository.getMyProjects(
            currentUid = currentUid,
            onSuccess = {
                projects = it
                isLoading = false
            },
            onFailure = {
                errorMessage = it
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
            text = "My Projects",
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
                Text("You have not created any projects yet.", color = Color(0xFF8B8AAC))
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(projects) { project ->
                        MyProjectCard(
                            project = project,
                            onViewApplicants = {
                                onViewApplicants(project.projectId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MyProjectCard(
    project: Project,
    onViewApplicants: () -> Unit
) {
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

            Spacer(Modifier.height(8.dp))

            Text(
                text = project.description,
                color = Color(0xFFB0AFC8),
                maxLines = 2
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onViewApplicants,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C6AF7)
                )
            ) {
                Text("View Applicants")
            }
        }
    }
}