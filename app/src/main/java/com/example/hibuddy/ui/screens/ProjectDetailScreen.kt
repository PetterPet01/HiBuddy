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

@Composable
fun ProjectDetailScreen(
    projectId: String,
    onBack: () -> Unit,
    onViewApplicants: (String) -> Unit
) {
    val repository = remember { ProjectRepository() }

    var project by remember { mutableStateOf<Project?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LaunchedEffect(projectId) {
        repository.getProjectById(
            projectId = projectId,
            onSuccess = {
                project = it
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
            .verticalScroll(rememberScrollState())
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

        Spacer(Modifier.height(20.dp))

        when {
            isLoading -> {
                CircularProgressIndicator(color = Color(0xFF7C6AF7))
            }

            errorMessage.isNotBlank() -> {
                Text(errorMessage, color = Color(0xFFFF4D6D))
            }

            project == null -> {
                Text("Project not found", color = Color.White)
            }

            else -> {
                val p = project!!
                val isOwner = p.ownerId == currentUid
                val isMember = p.memberIds.contains(currentUid)

                Text(
                    text = p.title.ifBlank { "Untitled Project" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = p.field,
                    color = Color(0xFF7C6AF7),
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))

                ProjectInfoCard(title = "Description", value = p.description)
                ProjectInfoCard(title = "Roles Needed", value = p.rolesNeeded.joinToString(", "))
                ProjectInfoCard(title = "Skills Needed", value = p.skillsNeeded.joinToString(", "))
                ProjectInfoCard(title = "Timeline", value = p.timeline)
                ProjectInfoCard(title = "Work Mode", value = p.workMode)
                ProjectInfoCard(title = "Commitment", value = p.commitment)

                ProjectInfoCard(
                    title = "Members",
                    value = "${p.currentMembers}/${p.maxMembers}"
                )

                ProjectInfoCard(
                    title = "Your Role",
                    value = when {
                        isOwner -> "Owner"
                        isMember -> "Member"
                        else -> "Not joined"
                    }
                )

                if (isOwner) {
                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onViewApplicants(p.projectId)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7C6AF7)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("View Applicants")
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectInfoCard(
    title: String,
    value: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF16152A)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title.uppercase(),
                color = Color(0xFF5A5A7A),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = value.ifBlank { "Not updated" },
                color = Color(0xFFB0AFC8)
            )
        }
    }
}