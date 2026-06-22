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
    onContinue: (() -> Unit)? = null,
    allowSkip: Boolean = false,
    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val currentProfile = uiState.profile

    var fullName by remember { mutableStateOf("") }
    var studentEmail by remember { mutableStateOf("") }
    var university by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var academicYear by remember { mutableStateOf("") }
    var studentCardUri by remember { mutableStateOf<Uri?>(null) }
    var hasPrefilled by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val cardPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> studentCardUri = uri }

    LaunchedEffect(Unit) {
        if (currentProfile == null) {
            profileViewModel.loadProfile()
        }
    }

    LaunchedEffect(currentProfile) {
        if (currentProfile != null && !hasPrefilled) {
            fullName = currentProfile.displayName
            university = currentProfile.university.orEmpty()
            academicYear = currentProfile.academicYear.orEmpty()
            hasPrefilled = true
        }
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
                text = "Student Verification",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Upload your student card and, if possible, use an institutional school email for faster review.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            currentProfile?.let { profile ->
                val status = profile.verificationStatus.uppercase()
                if (status == "PENDING" || status == "REJECTED") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (status == "PENDING") {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (status == "PENDING") {
                                    "Your verification is under review."
                                } else {
                                    "Your verification was rejected."
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (status == "PENDING") {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                }
                            )
                            profile.verificationSubmittedAt?.let {
                                Text(
                                    text = "Submitted: $it",
                                    color = if (status == "PENDING") {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    }
                                )
                            }
                            profile.verificationRejectionReason?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    text = "Reason: $it",
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            profile.verificationReviewedAt?.let {
                                Text(
                                    text = "Last reviewed: $it",
                                    color = if (status == "PENDING") {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                }
            }

            OutlinedButton(
                onClick = { cardPicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                val hasExistingCard = !currentProfile?.studentCardImageUrl.isNullOrBlank()
                Text(
                    when {
                        studentCardUri != null -> "New student card selected"
                        hasExistingCard -> "Student card already uploaded"
                        else -> "Select student card"
                    }
                )
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
                label = { Text("Institutional Student Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    Text("Use your school domain email if you have one. Public email providers are rejected.")
                }
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
                            academicYear = academicYear,
                            onSuccess = onContinue
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
                    (studentCardUri != null || !currentProfile?.studentCardImageUrl.isNullOrBlank())
            ) {
                Text(
                    if (currentProfile?.verificationStatus?.uppercase() == "REJECTED") {
                        "Resubmit Verification Request"
                    } else {
                        "Submit Verification Request"
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (allowSkip) "Skip for Now" else "Back")
            }

            if (onContinue != null) {
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue to App")
                }
            }
        }
    }
}
