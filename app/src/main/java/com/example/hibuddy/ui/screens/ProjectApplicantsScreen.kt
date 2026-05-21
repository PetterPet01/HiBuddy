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
import com.example.hibuddy.data.model.ProjectApplication
import com.example.hibuddy.repository.ProjectApplicationRepository
import com.example.hibuddy.data.model.UserProfile
import com.example.hibuddy.repository.UserRepository
import com.example.hibuddy.repository.ProjectRepository

data class ApplicantDisplayItem(
    val application: ProjectApplication,
    val userProfile: UserProfile?
)
@Composable
fun ProjectApplicantsScreen(
    projectId: String,
    onBack: () -> Unit,
    onViewApplicantProfile: (
        projectId: String,
        applicantId: String,
        applicationId: String
    ) -> Unit
) {
    val repository = remember { ProjectApplicationRepository() }
    val userRepository = remember { UserRepository() }
    val projectRepository = remember { ProjectRepository() }

    var applicants by remember {
        mutableStateOf<List<ApplicantDisplayItem>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    LaunchedEffect(projectId) {
        repository.getApplicationsByProject(
            projectId = projectId,
            onSuccess = { applications ->

                if (applications.isEmpty()) {
                    applicants = emptyList()
                    isLoading = false
                    return@getApplicationsByProject
                }

                val loadedApplicants = mutableListOf<ApplicantDisplayItem>()
                var loadedCount = 0

                applications.forEach { application ->
                    userRepository.getUserProfile(
                        uid = application.applicantId,
                        onSuccess = { userProfile ->
                            loadedApplicants.add(
                                ApplicantDisplayItem(
                                    application = application,
                                    userProfile = userProfile
                                )
                            )

                            loadedCount++

                            if (loadedCount == applications.size) {
                                applicants = loadedApplicants
                                isLoading = false
                            }
                        },
                        onFailure = {
                            loadedApplicants.add(
                                ApplicantDisplayItem(
                                    application = application,
                                    userProfile = null
                                )
                            )

                            loadedCount++

                            if (loadedCount == applications.size) {
                                applicants = loadedApplicants
                                isLoading = false
                            }
                        }
                    )
                }
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
            text = "Project Applicants",
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

            applicants.isEmpty() -> {
                Text(
                    text = "No applicants yet.",
                    color = Color(0xFF8B8AAC)
                )
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(applicants) { applicant ->
                        ApplicantCard(
                            applicant = applicant,
                            onViewProfile = {
                                onViewApplicantProfile(
                                    projectId,
                                    applicant.application.applicantId,
                                    applicant.application.applicationId
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ApplicantCard(
    applicant: ApplicantDisplayItem,
    onViewProfile: () -> Unit
) {
    val user = applicant.userProfile
    val application = applicant.application

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF16152A)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = user?.fullName ?: "Unknown User",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = user?.email ?: application.applicantId,
                color = Color(0xFF8B8AAC)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Skills: ${
                    user?.skills?.takeIf { it.isNotEmpty() }?.joinToString(", ")
                        ?: "No skills"
                }",
                color = Color(0xFFB0AFC8)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Status: ${application.status}",
                color = Color(0xFF7C6AF7)
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onViewProfile
                ) {
                    Text("View Profile")
                }
            }
        }
    }
}