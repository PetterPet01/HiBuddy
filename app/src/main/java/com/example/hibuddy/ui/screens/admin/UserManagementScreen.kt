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
fun UserManagementScreen(
    onBack: () -> Unit,
    viewModel: UserManagementViewModel = viewModel(factory = UserManagementViewModel.Factory)
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
                text = "User Management",
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
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            uiState.message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
            }

            if (!uiState.isLoading && uiState.users.isEmpty()) {
                Text(
                    text = "No users found.",
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.users) { user ->
                    UserManagementCard(
                        user = user,
                        onBan = { viewModel.banUser(user.id) },
                        onUnban = { viewModel.unbanUser(user.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UserManagementCard(
    user: AdminUserResponse,
    onBan: () -> Unit,
    onUnban: () -> Unit
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
            Text("Role: ${user.role}", color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "Verified Student: ${if (user.verifiedStudent) "Yes" else "No"}",
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Status: ${if (user.isActive) "Active" else "Banned"}",
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(12.dp))

            if (user.isActive) {
                Button(
                    onClick = onBan,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Ban User")
                }
            } else {
                OutlinedButton(
                    onClick = onUnban,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unban User")
                }
            }
        }
    }
}