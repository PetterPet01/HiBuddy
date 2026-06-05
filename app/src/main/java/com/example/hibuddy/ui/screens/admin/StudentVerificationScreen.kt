package com.example.hibuddy.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.data.remote.dto.AdminUserResponse

@Composable
fun StudentVerificationScreen(
    onBack: () -> Unit,
    viewModel: StudentVerificationViewModel = viewModel(factory = StudentVerificationViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                text = "Student Verification Requests",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }

            Spacer(Modifier.height(16.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator()
            }

            uiState.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }

            uiState.message?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (!uiState.isLoading && uiState.users.isEmpty()) {
                Text(
                    text = "No verification requests found.",
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.users) { user ->
                    StudentVerificationCard(
                        user = user,
                        onApprove = {
                            viewModel.approve(user.id)
                        },
                        onReject = {
                            viewModel.reject(user.id, "Invalid student information")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StudentVerificationCard(
    user: AdminUserResponse,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = user.fullName,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(4.dp))

            Text("Username: ${user.username}", color = MaterialTheme.colorScheme.onSurface)
            Text("Email: ${user.email}", color = MaterialTheme.colorScheme.onSurface)
            Text("Student Email: ${user.studentEmail ?: "N/A"}", color = MaterialTheme.colorScheme.onSurface)
            Text("University: ${user.university ?: "N/A"}", color = MaterialTheme.colorScheme.onSurface)
            Text("Student ID: ${user.studentId ?: "N/A"}", color = MaterialTheme.colorScheme.onSurface)
            Text("Status: ${user.verificationStatus}", color = MaterialTheme.colorScheme.onSurface)

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Approve")
                }

                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reject")
                }
            }
        }
    }
}