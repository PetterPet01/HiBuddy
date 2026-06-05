package com.example.hibuddy.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AdminScreen(
    onLogout: () -> Unit = {},
    onOpenStudentVerifications: () -> Unit = {},
    onOpenReportManagement: () -> Unit = {},
    onOpenUserManagement: () -> Unit = {}
) {
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
                text = "Admin Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onOpenStudentVerifications,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Review Student Verifications")
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onOpenUserManagement,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Manage User Accounts")
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onOpenReportManagement,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Handle Reports")
            }

            Spacer(Modifier.height(24.dp))

            HorizontalDivider()

            Spacer(Modifier.height(24.dp))

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }
        }
    }
}