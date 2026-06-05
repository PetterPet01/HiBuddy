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
import com.example.hibuddy.data.remote.dto.AdminReportResponse

@Composable
fun ReportManagementScreen(
    onBack: () -> Unit,
    viewModel: ReportManagementViewModel = viewModel(factory = ReportManagementViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadReports()
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
                text = "Report Management",
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

            if (!uiState.isLoading && uiState.reports.isEmpty()) {
                Text(
                    text = "No reports found.",
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.reports) { report ->
                    ReportCard(
                        report = report,
                        onDismiss = { viewModel.dismissReport(report.id) },
                        onBan = { viewModel.banReportedUser(report.id) }
                    )
                }
            }
        }
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Reported User: ${report.reported_name ?: report.reported_id}",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Reporter: ${report.reporter_name ?: report.reporter_id}",
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Reason: ${report.reason}",
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Description: ${report.description ?: "N/A"}",
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Status: ${report.status}",
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Dismiss")
                }

                Button(
                    onClick = onBan,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Ban User")
                }
            }
        }
    }
}