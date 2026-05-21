package com.example.hibuddy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.hibuddy.repository.ProjectApplicationRepository
import com.example.hibuddy.repository.ProjectRepository

@Composable
fun ApplicantProfileScreen(
    projectId: String,
    applicantId: String,
    applicationId: String,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val applicationRepository = remember { ProjectApplicationRepository() }
    val projectRepository = remember { ProjectRepository() }

    var errorMessage by remember { mutableStateOf("") }

    UserProfileScreen(
        userId = applicantId,
        onBack = onBack,
        bottomContent = {
            if (errorMessage.isNotBlank()) {
                Text(errorMessage, color = Color(0xFFFF4D6D))
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    applicationRepository.approveApplication(
                        applicationId = applicationId,
                        onSuccess = {
                            projectRepository.addMemberToProject(
                                projectId = projectId,
                                userId = applicantId,
                                onSuccess = onDone,
                                onFailure = { errorMessage = it }
                            )
                        },
                        onFailure = { errorMessage = it }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Approve")
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    applicationRepository.rejectApplication(
                        applicationId = applicationId,
                        onSuccess = onDone,
                        onFailure = { errorMessage = it }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D6D)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Reject")
            }
        }
    )
}