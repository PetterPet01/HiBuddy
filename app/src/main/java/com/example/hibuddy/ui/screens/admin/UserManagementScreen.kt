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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.data.remote.dto.AdminUserResponse
import com.example.hibuddy.ui.theme.HiBuddyColors

private enum class UserFilter(val label: String) {
    All("All"),
    Active("Active"),
    Banned("Banned"),
    Verified("Verified"),
    Admins("Admins")
}

@Composable
fun UserManagementScreen(
    onBack: () -> Unit,
    viewModel: UserManagementViewModel = viewModel(factory = UserManagementViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var pendingBan by remember { mutableStateOf<AdminUserResponse?>(null) }
    var banReason by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(UserFilter.All) }

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    val filteredUsers = remember(uiState.users, query, filter) {
        uiState.users
            .filter { user ->
                when (filter) {
                    UserFilter.All -> true
                    UserFilter.Active -> user.isActive
                    UserFilter.Banned -> !user.isActive
                    UserFilter.Verified -> user.verifiedStudent
                    UserFilter.Admins -> user.role.equals("ADMIN", ignoreCase = true)
                }
            }
            .filter { user ->
                val needle = query.trim()
                needle.isBlank() || listOf(user.fullName, user.username, user.email, user.role)
                    .any { it.contains(needle, ignoreCase = true) }
            }
    }

    AdminScaffold(
        title = "User management",
        subtitle = "Search, audit, ban, and reinstate accounts",
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
                UserManagementSummary(users = uiState.users)
            }

            if (uiState.isLoading) {
                item { AdminLoadingState("Loading user accounts…") }
            }

            uiState.error?.let { error ->
                item { AdminStateBanner(error, AdminChipTone.Error) }
            }

            uiState.message?.let { message ->
                item { AdminStateBanner(message, AdminChipTone.Success) }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Search users") },
                        placeholder = { Text("Name, username, email, or role") },
                        leadingIcon = { Icon(Icons.Filled.PersonSearch, contentDescription = null) }
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(UserFilter.values()) { item ->
                            AdminFilterChip(
                                selected = filter == item,
                                label = item.label,
                                onClick = { filter = item }
                            )
                        }
                    }
                }
            }

            item {
                AdminSectionHeader(
                    title = "Accounts",
                    subtitle = "Showing ${filteredUsers.size} of ${uiState.users.size} users"
                )
            }

            if (!uiState.isLoading && filteredUsers.isEmpty()) {
                item {
                    AdminEmptyState(
                        title = "No users match your view",
                        message = "Try clearing search or switching filters.",
                        icon = Icons.Filled.PersonSearch
                    )
                }
            }

            items(filteredUsers, key = { it.id }) { user ->
                UserManagementCard(
                    user = user,
                    onBan = { pendingBan = user },
                    onUnban = { viewModel.unbanUser(user.id) }
                )
            }
        }
    }

    pendingBan?.let { user ->
        AlertDialog(
            onDismissRequest = { pendingBan = null },
            title = { Text("Ban ${user.fullName}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "This immediately disables account access. Add a clear reason for the audit trail.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = banReason,
                        onValueChange = { banReason = it },
                        label = { Text("Required reason") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = banReason.trim().length >= 3,
                    onClick = {
                        viewModel.banUser(user.id, banReason.trim())
                        banReason = ""
                        pendingBan = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Ban account") }
            },
            dismissButton = {
                TextButton(onClick = { pendingBan = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun UserManagementSummary(users: List<AdminUserResponse>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AdminMetricCard(
                title = "Users",
                value = users.size.toString(),
                helper = "Registered accounts",
                icon = Icons.Filled.Groups,
                modifier = Modifier.weight(1f)
            )
            AdminMetricCard(
                title = "Active",
                value = users.count { it.isActive }.toString(),
                helper = "Can access HiBuddy",
                icon = Icons.Filled.CheckCircle,
                accentColor = HiBuddyColors.success,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AdminMetricCard(
                title = "Banned",
                value = users.count { !it.isActive }.toString(),
                helper = "Disabled accounts",
                icon = Icons.Filled.Block,
                accentColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
            AdminMetricCard(
                title = "Verified",
                value = users.count { it.verifiedStudent }.toString(),
                helper = "Student accounts",
                icon = Icons.Filled.VerifiedUser,
                accentColor = HiBuddyColors.info,
                modifier = Modifier.weight(1f)
            )
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AdminStatusChip(
                    text = if (user.isActive) "Active" else "Banned",
                    tone = if (user.isActive) AdminChipTone.Success else AdminChipTone.Error
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminStatusChip(user.role, if (user.role.equals("ADMIN", true)) AdminChipTone.Info else AdminChipTone.Neutral)
                AdminStatusChip(
                    text = if (user.verifiedStudent) "Verified student" else user.verificationStatus,
                    tone = if (user.verifiedStudent) AdminChipTone.Success else AdminChipTone.Warning
                )
            }

            AdminInfoRow("Email", user.email)
            AdminInfoRow("University", user.university ?: "Not provided")
            AdminInfoRow("Student email", user.studentEmail ?: "Not provided")
            user.verificationRejectionReason?.takeIf { it.isNotBlank() }?.let {
                AdminInfoRow("Last rejection", it)
            }

            Spacer(Modifier.height(2.dp))

            if (user.isActive) {
                AdminDestructiveOutlinedButton(
                    text = "Ban user",
                    onClick = onBan,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Button(
                    onClick = onUnban,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Text("  Reinstate user")
                }
            }
        }
    }
}
