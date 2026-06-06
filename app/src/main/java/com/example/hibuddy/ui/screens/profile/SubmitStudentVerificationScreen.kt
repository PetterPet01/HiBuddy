package com.example.hibuddy.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

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
    var studentCardUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val cardPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> studentCardUri = uri }

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
                text = "Student Verification",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(20.dp))

            OutlinedButton(
                onClick = { cardPicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (studentCardUri == null) "Select student card" else "Student card selected")
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = studentEmail,
                onValueChange = { studentEmail = it },
                label = { Text("Student Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = university,
                onValueChange = { university = it },
                label = { Text("University") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = studentId,
                onValueChange = { studentId = it },
                label = { Text("Student ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = academicYear,
                onValueChange = { academicYear = it },
                label = { Text("Enrollment Year / Intake Year") },
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
                    val submit = {
                        profileViewModel.submitStudentVerification(
                            fullName = fullName,
                            studentEmail = studentEmail,
                            university = university,
                            studentId = studentId,
                            academicYear = academicYear
                        )
                    }
                    studentCardUri?.let { uri ->
                        profileViewModel.uploadStudentCard(context, uri, submit)
                    } ?: submit()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading &&
                    fullName.isNotBlank() &&
                    university.isNotBlank() &&
                    studentId.isNotBlank() &&
                    academicYear.isNotBlank() &&
                    studentCardUri != null
            ) {
                Text("Submit Verification Request")
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
}
