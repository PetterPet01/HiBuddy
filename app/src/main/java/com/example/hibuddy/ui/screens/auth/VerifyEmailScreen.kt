package com.example.hibuddy.ui.screens.auth

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun VerifyEmailScreen(
    initialEmail: String,
    onVerified: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()
    val email = initialEmail.ifBlank { state.pendingEmail.orEmpty() }
    var code by remember { mutableStateOf("") }

    LaunchedEffect(state.verificationSucceeded) {
        if (state.verificationSucceeded) onVerified()
    }

    AuthScreenFrame(
        title = "Verify your email",
        subtitle = "Enter the six-digit code sent to your email address.",
        onBack = onBackToLogin
    ) {
        AuthTextField(
            value = email,
            onValueChange = {},
            label = "Email",
            leadingIcon = Icons.Filled.Email,
            readOnly = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )
        AuthTextField(
            value = code,
            onValueChange = { value -> code = value.filter(Char::isDigit).take(6) },
            label = "Verification code",
            leadingIcon = Icons.Filled.Pin,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            )
        )
        AuthPrimaryButton(
            text = "Verify email",
            isLoading = state.isLoading,
            enabled = email.isNotBlank() && code.length == 6,
            onClick = { viewModel.verifyEmail(email.trim(), code) }
        )
        TextButton(
            onClick = { viewModel.resendVerification(email.trim()) },
            enabled = !state.isLoading && email.isNotBlank()
        ) {
            Text("Resend code")
        }
        state.error?.let { AuthMessage(it, isError = true) }
        state.message?.let { AuthMessage(it, isError = false) }
    }
}
