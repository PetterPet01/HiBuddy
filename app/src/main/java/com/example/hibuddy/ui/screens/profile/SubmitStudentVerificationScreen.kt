package com.example.hibuddy.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SubmitStudentVerificationScreen(
    onBack: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
) {
    val uiState by profileViewModel.uiState.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var studentEmail by remember { mutableStateOf("") }
    var university by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var academicYear by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "Xác thực sinh viên",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Họ tên") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = studentEmail,
            onValueChange = { studentEmail = it },
            label = { Text("Email sinh viên") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = university,
            onValueChange = { university = it },
            label = { Text("Trường đại học") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = studentId,
            onValueChange = { studentId = it },
            label = { Text("Mã số sinh viên") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = academicYear,
            onValueChange = { academicYear = it },
            label = { Text("Năm nhập học / Khóa") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(20.dp))

        uiState.message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
        }

        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = {
                profileViewModel.submitStudentVerification(
                    fullName = fullName,
                    studentEmail = studentEmail,
                    university = university,
                    studentId = studentId,
                    academicYear = academicYear
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Gửi yêu cầu xác thực")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}