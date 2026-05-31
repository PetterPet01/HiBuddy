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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "Duyệt xác thực sinh viên",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
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
            Text("Không có yêu cầu xác thực nào.")
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.users) { user ->
                StudentVerificationCard(
                    user = user,
                    onApprove = {
                        viewModel.approve(user.id)
                    },
                    onReject = {
                        viewModel.reject(user.id, "Thông tin sinh viên không hợp lệ")
                    }
                )
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = user.fullName,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text("Username: ${user.username}")
            Text("Email: ${user.email}")
            Text("Student email: ${user.studentEmail ?: "N/A"}")
            Text("University: ${user.university ?: "N/A"}")
            Text("Student ID: ${user.studentId ?: "N/A"}")
            Text("Status: ${user.verificationStatus}")

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