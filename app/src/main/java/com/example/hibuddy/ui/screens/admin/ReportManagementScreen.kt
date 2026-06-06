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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.hibuddy.data.remote.dto.AdminReportResponse
import com.example.hibuddy.ui.theme.HiBuddyColors

private enum class ReportFilter(val label: String) {
    All("All"),
    WithEvidence("Evidence"),
    UserContext("User context"),
    ProjectContext("Project context")
}

@Composable
fun ReportManagementScreen(
    onBack: () -> Unit,
    viewModel: ReportManagementViewModel = viewModel(factory = ReportManagementViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var pendingAction by remember { mutableStateOf<Pair<AdminReportResponse, String>?>(null) }
    var resolutionReason by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(ReportFilter.All) }

    LaunchedEffect(Unit) {
        viewModel.loadReports()
    }

    val filteredReports = remember(uiState.reports, filter) {
        uiState.reports.filter { report ->
            when (filter) {
                ReportFilter.All -> true
                ReportFilter.WithEvidence -> !report.evidenceUrl.isNullOrBlank()
                ReportFilter.UserContext -> report.contextType?.contains("USER", ignoreCase = true) == true
                ReportFilter.ProjectContext -> report.contextType?.contains("PROJECT", ignoreCase = true) == true
            }
        }
    }

    AdminScaffold(
        title = "Report management",
        subtitle = "Investigate safety reports and document outcomes",
        onBack = onBack,
        onRefresh = { viewModel.loadReports() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ReportSummary(reports = uiState.reports) }

            if (uiState.isLoading) {
                item { AdminLoadingState("Loading reports…") }
            }

            uiState.error?.let { item { AdminStateBanner(it, AdminChipTone.Error) } }
            uiState.message?.let { item { AdminStateBanner(it, AdminChipTone.Success) } }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminSectionHeader(
                        title = "Investigation queue",
                        subtitle = "Showing ${filteredReports.size} of ${uiState.reports.size} reports"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ReportFilter.values()) { item ->
                            AdminFilterChip(
                                selected = filter == item,
                                label = item.label,
                                onClick = { filter = item }
                            )
                        }
                    }
                }
            }

            if (!uiState.isLoading && filteredReports.isEmpty()) {
                item {
                    AdminEmptyState(
                        title = "No reports in this view",
                        message = "When users submit safety reports, they will appear here with context and evidence.",
                        icon = Icons.Filled.Shield
                    )
                }
            }

            items(filteredReports, key = { it.id }) { report ->
                ReportCard(
                    report = report,
                    onDismiss = {
                        resolutionReason = ""
                        pendingAction = report to "DISMISS"
                    },
                    onBan = {
                        resolutionReason = ""
                        pendingAction = report to "BAN"
                    }
                )
            }
        }
    }

    pendingAction?.let { (report, action) ->
        val isBan = action == "BAN"
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(if (isBan) "Ban reported user?" else "Dismiss report?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isBan) {
                            "This resolves the report and disables the reported user's account. Include the policy reason."
                        } else {
                            "This resolves the report without account action. Include why no enforcement is needed."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = resolutionReason,
                        onValueChange = { resolutionReason = it },
                        label = { Text("Resolution note") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = resolutionReason.trim().length >= 3,
                    onClick = {
                        if (isBan) {
                            viewModel.banReportedUser(report.id, resolutionReason.trim())
                        } else {
                            viewModel.dismissReport(report.id, resolutionReason.trim())
                        }
                        pendingAction = null
                    },
                    colors = if (isBan) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) { Text(if (isBan) "Ban and resolve" else "Dismiss report") }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ReportSummary(reports: List<AdminReportResponse>) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AdminMetricCard(
            title = "Reports",
            value = reports.size.toString(),
            helper = "Open moderation cases",
            icon = Icons.Filled.Report,
            accentColor = if (reports.isNotEmpty()) MaterialTheme.colorScheme.error else HiBuddyColors.success,
            modifier = Modifier.weight(1f)
        )
        AdminMetricCard(
            title = "Evidence",
            value = reports.count { !it.evidenceUrl.isNullOrBlank() }.toString(),
            helper = "Reports with attachments",
            icon = Icons.Filled.Image,
            accentColor = HiBuddyColors.info,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ReportCard(
    report: AdminReportResponse,
    onDismiss: () -> Unit,
    onBan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.reason,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Reported ${report.reported_name ?: report.reported_id}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AdminStatusChip(report.status, AdminChipTone.Warning)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminStatusChip("Reporter: ${report.reporter_name ?: report.reporter_id}", AdminChipTone.Neutral)
                if (!report.contextType.isNullOrBlank()) {
                    AdminStatusChip(report.contextType, AdminChipTone.Info)
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminInfoRow("Reported user", report.reported_name ?: report.reported_id)
                    AdminInfoRow("Reporter", report.reporter_name ?: report.reporter_id)
                    AdminInfoRow("Created", report.created_at)
                    AdminInfoRow("Context", listOfNotNull(report.contextType, report.contextId).joinToString(" • ").ifBlank { "None" })
                    AdminInfoRow("Description", report.description ?: "No additional description")
                }
            }

            if (!report.evidenceUrl.isNullOrBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Evidence attachment",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AsyncImage(
                        model = report.evidenceUrl,
                        contentDescription = "Report evidence",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Text("  Dismiss")
                }
                Button(
                    onClick = onBan,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(Icons.Filled.Block, contentDescription = null)
                    Text("  Ban")
                }
            }
        }
    }
}
