package com.example.hibuddy.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.ServiceLocator
import com.example.hibuddy.data.remote.dto.AdminReportResponse
import com.example.hibuddy.data.remote.dto.AdminUserResponse
import com.example.hibuddy.data.remote.dto.ProjectResponse
import com.example.hibuddy.ui.theme.HiBuddyColors
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Composable
fun AdminScreen(
    onLogout: () -> Unit = {},
    onOpenStudentVerifications: () -> Unit = {},
    onOpenReportManagement: () -> Unit = {},
    onOpenUserManagement: () -> Unit = {},
    onOpenFlaggedProjects: () -> Unit = {},
    viewModel: AdminDashboardViewModel = viewModel(factory = AdminDashboardViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }

    AdminScaffold(
        title = "Admin command center",
        subtitle = "Trust, safety, and student operations",
        onRefresh = { viewModel.loadDashboard() },
        actions = {
            IconButton(onClick = onLogout) {
                Icon(Icons.Filled.Logout, contentDescription = "Logout")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AdminHeroCard(state = state, onLogout = onLogout)
            }

            if (state.isLoading) {
                item { AdminLoadingState("Loading admin health signals…") }
            }

            state.error?.let { error ->
                item { AdminStateBanner(error, AdminChipTone.Error) }
            }

            item {
                AdminSectionHeader(
                    title = "Platform overview",
                    subtitle = "Live metrics derived from the current admin queues."
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AdminMetricCard(
                            title = "Total users",
                            value = state.totalUsers.toString(),
                            helper = "${state.activeUsers} active • ${state.bannedUsers} banned",
                            icon = Icons.Filled.Groups,
                            modifier = Modifier.weight(1f)
                        )
                        AdminMetricCard(
                            title = "Verified students",
                            value = state.verifiedStudents.toString(),
                            helper = "${state.pendingVerifications} awaiting review",
                            icon = Icons.Filled.VerifiedUser,
                            accentColor = HiBuddyColors.success,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AdminMetricCard(
                            title = "Open reports",
                            value = state.openReports.toString(),
                            helper = if (state.openReports > 0) "Needs safety review" else "No active reports",
                            icon = Icons.Filled.Report,
                            accentColor = if (state.openReports > 0) MaterialTheme.colorScheme.error else HiBuddyColors.success,
                            modifier = Modifier.weight(1f)
                        )
                        AdminMetricCard(
                            title = "Flagged projects",
                            value = state.flaggedProjects.toString(),
                            helper = "Moderation queue",
                            icon = Icons.Filled.WarningAmber,
                            accentColor = if (state.flaggedProjects > 0) HiBuddyColors.warning else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                AdminSectionHeader(
                    title = "Needs attention",
                    subtitle = "Prioritized admin workflows with pending workload counts."
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AdminActionCard(
                            title = "Student verifications",
                            description = "Review IDs, universities, student emails, and uploaded card evidence.",
                            count = state.pendingVerifications,
                            icon = Icons.Filled.School,
                            urgent = state.pendingVerifications > 0,
                            onClick = onOpenStudentVerifications,
                            modifier = Modifier.weight(1f)
                        )
                        AdminActionCard(
                            title = "Reports",
                            description = "Investigate user reports, evidence, and ban or dismiss decisions.",
                            count = state.openReports,
                            icon = Icons.Filled.Report,
                            urgent = state.openReports > 0,
                            onClick = onOpenReportManagement,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AdminActionCard(
                            title = "Users",
                            description = "Search accounts, verify status, and handle bans or reinstatements.",
                            count = state.totalUsers,
                            icon = Icons.Filled.AdminPanelSettings,
                            onClick = onOpenUserManagement,
                            modifier = Modifier.weight(1f)
                        )
                        AdminActionCard(
                            title = "Projects",
                            description = "Approve or reject projects flagged by moderation signals.",
                            count = state.flaggedProjects,
                            icon = Icons.Filled.AssignmentTurnedIn,
                            urgent = state.flaggedProjects > 0,
                            onClick = onOpenFlaggedProjects,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                AdminSectionHeader(
                    title = "Risk snapshot",
                    subtitle = "Fast read of account and moderation posture."
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AdminInfoRow("Active account rate", state.activeRateLabel)
                        AdminInfoRow("Student verification rate", state.verifiedRateLabel)
                        AdminInfoRow("Admin accounts", state.adminUsers.toString())
                        AdminInfoRow("Banned accounts", state.bannedUsers.toString())
                        AdminInfoRow("Safety workload", state.safetyWorkloadLabel)
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun AdminHeroCard(
    state: AdminDashboardUiState,
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Welcome back, admin",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Your dashboard highlights the highest-impact moderation and verification work first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .height(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminStatusChip(
                    text = if (state.urgentWorkCount > 0) "${state.urgentWorkCount} urgent" else "All clear",
                    tone = if (state.urgentWorkCount > 0) AdminChipTone.Warning else AdminChipTone.Success
                )
                AdminStatusChip(
                    text = "${state.totalUsers} users",
                    tone = AdminChipTone.Info
                )
            }
            Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Logout, contentDescription = null)
                Text("  Secure logout")
            }
        }
    }
}

data class AdminDashboardUiState(
    val isLoading: Boolean = false,
    val users: List<AdminUserResponse> = emptyList(),
    val reports: List<AdminReportResponse> = emptyList(),
    val projects: List<ProjectResponse> = emptyList(),
    val error: String? = null
) {
    val totalUsers: Int = users.size
    val activeUsers: Int = users.count { it.isActive }
    val bannedUsers: Int = users.count { !it.isActive }
    val verifiedStudents: Int = users.count { it.verifiedStudent }
    val pendingVerifications: Int = users.count { it.verificationStatus.equals("PENDING", ignoreCase = true) }
    val adminUsers: Int = users.count { it.role.equals("ADMIN", ignoreCase = true) }
    val openReports: Int = reports.count { it.status.equals("OPEN", ignoreCase = true) || it.status.equals("PENDING", ignoreCase = true) }
        .takeIf { it > 0 } ?: reports.size
    val flaggedProjects: Int = projects.size
    val urgentWorkCount: Int = pendingVerifications + openReports + flaggedProjects
    val activeRateLabel: String = percentage(activeUsers, totalUsers)
    val verifiedRateLabel: String = percentage(verifiedStudents, totalUsers)
    val safetyWorkloadLabel: String = "$openReports reports • $flaggedProjects projects • $pendingVerifications verifications"

    private fun percentage(part: Int, total: Int): String =
        if (total == 0) "No users yet" else "${((part.toDouble() / total.toDouble()) * 100).toInt()}% ($part of $total)"
}

class AdminDashboardViewModel : ViewModel() {
    private val adminRepository = ServiceLocator.adminRepository
    private val _state = MutableStateFlow(AdminDashboardUiState())
    val state = _state.asStateFlow()

    fun loadDashboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val usersDeferred = async { adminRepository.getUsers() }
            val reportsDeferred = async { adminRepository.getReports() }
            val projectsDeferred = async { adminRepository.getFlaggedProjects() }

            val usersResult = usersDeferred.await()
            val reportsResult = reportsDeferred.await()
            val projectsResult = projectsDeferred.await()

            val failures = listOf(usersResult, reportsResult, projectsResult)
                .mapNotNull { it.exceptionOrNull()?.message }

            _state.value = AdminDashboardUiState(
                isLoading = false,
                users = usersResult.getOrElse { _state.value.users },
                reports = reportsResult.getOrElse { _state.value.reports },
                projects = projectsResult.getOrElse { _state.value.projects },
                error = failures.takeIf { it.isNotEmpty() }?.joinToString(" • ")
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AdminDashboardViewModel() as T
        }
    }
}
