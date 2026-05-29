package com.example.hibuddy.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AdminScreen(
    onBack: () -> Unit = {},
    onOpenStudentVerifications: () -> Unit = {},
    onOpenUserManagement: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "Admin Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onOpenStudentVerifications,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Duyệt xác thực sinh viên")
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Xử lý report")
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onOpenUserManagement,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Khóa tài khoản")
        }

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}