package com.example.hibuddy.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.hibuddy.data.remote.dto.AdminUserResponse
import com.example.hibuddy.ui.theme.HiBuddyColors

@Composable
fun StudentVerificationScreen(
    onBack: () -> Unit,
    viewModel: StudentVerificationViewModel = viewModel(factory = StudentVerificationViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var pendingReject by remember { mutableStateOf<AdminUserResponse?>(null) }
    var rejectionReason by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    AdminScaffold(
        title = "Student verifications",
        subtitle = "Review identity evidence and approve trusted student status",
        onBack = onBack,
        onRefresh = { viewModel.loadUsers() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StudentVerificationSummary(users = uiState.users)
            }

            if (uiState.isLoading) {
                item { AdminLoadingState("Loading verification queue…") }
            }

            uiState.error?.let {
                item { AdminStateBanner(it, AdminChipTone.Error) }
            }

            uiState.message?.let {
                item { AdminStateBanner(it, AdminChipTone.Success) }
            }

            item {
                AdminSectionHeader(
                    title = "Review queue",
                    subtitle = "Newest student verification requests that need a clear admin decision."
                )
            }

            if (!uiState.isLoading && uiState.users.isEmpty()) {
                item {
                    AdminEmptyState(
                        title = "Verification queue is clear",
                        message = "New submissions will appear here with student card evidence and academic details.",
                        icon = Icons.Filled.VerifiedUser
                    )
                }
            }

            items(uiState.users, key = { it.id }) { user ->
                StudentVerificationCard(
                    user = user,
                    onApprove = { viewModel.approve(user.id) },
                    onReject = { pendingReject = user }
                )
            }
        }
    }

    pendingReject?.let { user ->
        AlertDialog(
            onDismissRequest = { pendingReject = null },
            title = { Text("Reject verification?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Explain what the student needs to fix. This reason may be shown to them.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("Reason shown to the student") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = rejectionReason.trim().length >= 3,
                    onClick = {
                        viewModel.reject(user.id, rejectionReason.trim())
                        rejectionReason = ""
                        pendingReject = null
                    }
                ) { Text("Reject request") }
            },
            dismissButton = {
                TextButton(onClick = { pendingReject = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StudentVerificationSummary(users: List<AdminUserResponse>) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AdminMetricCard(
            title = "Pending",
            value = users.size.toString(),
            helper = "Requests awaiting review",
            icon = Icons.Filled.PendingActions,
            accentColor = if (users.isNotEmpty()) HiBuddyColors.warning else HiBuddyColors.success,
            modifier = Modifier.weight(1f)
        )
        AdminMetricCard(
            title = "With evidence",
            value = users.count { !it.studentCardImageUrl.isNullOrBlank() }.toString(),
            helper = "Uploaded card images",
            icon = Icons.Filled.Badge,
            accentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "@${user.username} • ${user.email}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AdminStatusChip(user.verificationStatus, AdminChipTone.Warning)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminStatusChip(user.university ?: "University missing", AdminChipTone.Info)
                AdminStatusChip(user.academicYear ?: "Year missing", AdminChipTone.Neutral)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminInfoRow("Student email", user.studentEmail ?: "Not provided")
                    AdminInfoRow("Student ID", user.studentId ?: "Not provided")
                    AdminInfoRow("University", user.university ?: "Not provided")
                    AdminInfoRow("Academic year", user.academicYear ?: "Not provided")
                    AdminInfoRow("Submitted", user.verificationSubmittedAt ?: "Not available")
                }
            }

            if (!user.studentCardImageUrl.isNullOrBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Student card evidence",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AsyncImage(
                        model = user.studentCardImageUrl,
                        contentDescription = "Student card evidence",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                AdminStateBanner("No student card image is attached to this request.", AdminChipTone.Warning)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Text("  Approve")
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
