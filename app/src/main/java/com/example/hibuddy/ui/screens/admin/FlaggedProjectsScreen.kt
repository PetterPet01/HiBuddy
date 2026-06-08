package com.example.hibuddy.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.data.remote.dto.ProjectResponse
import com.example.hibuddy.ui.theme.HiBuddyColors

@Composable
fun FlaggedProjectsScreen(
    onBack: () -> Unit,
    viewModel: FlaggedProjectsViewModel = viewModel(factory = FlaggedProjectsViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    var decision by remember { mutableStateOf<Pair<ProjectResponse, Boolean>?>(null) }
    var reason by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.load() }

    AdminScaffold(
        title = "Flagged projects",
        subtitle = "Review moderation categories and project quality signals",
        onBack = onBack,
        onRefresh = { viewModel.load() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { FlaggedProjectSummary(projects = state.projects) }

            if (state.loading) {
                item { AdminLoadingState("Loading flagged projects…") }
            }

            state.error?.let { item { AdminStateBanner(it, AdminChipTone.Error) } }
            state.message?.let { item { AdminStateBanner(it, AdminChipTone.Success) } }

            item {
                AdminSectionHeader(
                    title = "Moderation queue",
                    subtitle = "${state.projects.size} projects need approve or reject decisions."
                )
            }

            if (!state.loading && state.projects.isEmpty()) {
                item {
                    AdminEmptyState(
                        title = "No flagged projects",
                        message = "Projects requiring manual moderation will appear here with categories and reasons.",
                        icon = Icons.Filled.AssignmentTurnedIn
                    )
                }
            }

            items(state.projects, key = { it.id }) { project ->
                FlaggedProjectCard(
                    project = project,
                    onApprove = { decision = project to true },
                    onReject = { decision = project to false }
                )
            }
        }
    }

    decision?.let { (project, approve) ->
        AlertDialog(
            onDismissRequest = { decision = null },
            title = { Text(if (approve) "Approve project?" else "Reject project?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (approve) {
                            "This clears the moderation flag and allows the project to remain visible."
                        } else {
                            "This rejects the project from the platform. Add a clear reason for the creator."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Decision reason") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = reason.trim().length >= 3,
                    onClick = {
                        viewModel.decide(project.id, approve, reason.trim())
                        reason = ""
                        decision = null
                    }
                ) { Text(if (approve) "Approve project" else "Reject project") }
            },
            dismissButton = {
                TextButton(onClick = { decision = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FlaggedProjectSummary(projects: List<ProjectResponse>) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AdminMetricCard(
            title = "Flagged",
            value = projects.size.toString(),
            helper = "Projects awaiting review",
            icon = Icons.Filled.WarningAmber,
            accentColor = if (projects.isNotEmpty()) HiBuddyColors.warning else HiBuddyColors.success,
            modifier = Modifier.weight(1f)
        )
        AdminMetricCard(
            title = "Categories",
            value = projects.flatMap { it.moderationCategories.orEmpty() }.distinct().size.toString(),
            helper = "Unique moderation signals",
            icon = Icons.Filled.Category,
            accentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FlaggedProjectCard(
    project: ProjectResponse,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        project.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${project.field} • ${project.workMode} • ${project.commitmentLevel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AdminStatusChip(project.reviewStatus, AdminChipTone.Warning)
            }

            Text(
                project.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminStatusChip("${project.members.size}/${project.maxMembers} members", AdminChipTone.Info)
                AdminStatusChip("${project.roleSlots.size} roles", AdminChipTone.Neutral)
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminInfoRow("Timeline", "${project.startDate} → ${project.endDate}")
                    AdminInfoRow("Status", project.status)
                    AdminInfoRow("Created", project.createdAt)
                    AdminInfoRow("Goal", project.specificGoal ?: "Not provided")
                    AdminInfoRow("Benefits", project.memberBenefits ?: "Not provided")
                }
            }

            if (!project.moderationCategories.isNullOrEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Moderation categories",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        project.moderationCategories.take(3).forEach { category ->
                            AdminStatusChip(category, AdminChipTone.Warning)
                        }
                    }
                }
            }

            if (!project.moderationReasons.isNullOrEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f))) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Why it was flagged",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        project.moderationReasons.forEach { reason ->
                            Text("• $reason", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
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
                    Icon(Icons.Filled.Close, contentDescription = null)
                    Text("  Reject")
                }
            }
        }
    }
}
