package com.example.hibuddy.ui.screens.auth

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    var email by remember { mutableStateOf("") }
    var resetCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val passwordsMismatch = confirmPassword.isNotBlank() && confirmPassword != newPassword

    AuthScreenFrame(
        title = if (codeSent) "Choose new password" else "Reset password",
        subtitle = if (codeSent) {
            "Use the reset code to finish signing back in."
        } else {
            "We will send a reset code to your account email."
        },
        onBack = onNavigateBack
    ) {
        if (!codeSent) {
            AuthTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (uiState.error != null) viewModel.clearError()
                },
                label = "Email",
                leadingIcon = Icons.Filled.Email,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                )
            )

            AuthPrimaryButton(
                text = "Send reset code",
                isLoading = uiState.isLoading,
                enabled = email.isNotBlank(),
                onClick = {
                    viewModel.forgotPassword(email.trim())
                    codeSent = true
                }
            )
        } else {
            AuthTextField(
                value = resetCode,
                onValueChange = {
                    resetCode = it
                    if (uiState.error != null) viewModel.clearError()
                },
                label = "Reset code",
                leadingIcon = Icons.Filled.Pin,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )
            )

            AuthTextField(
                value = newPassword,
                onValueChange = {
                    newPassword = it
                    if (uiState.error != null) viewModel.clearError()
                },
                label = "New password",
                leadingIcon = Icons.Filled.Lock,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = "Toggle password visibility",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            AuthTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    if (uiState.error != null) viewModel.clearError()
                },
                label = "Confirm password",
                leadingIcon = Icons.Filled.Lock,
                isError = passwordsMismatch,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                supportingText = if (passwordsMismatch) {
                    { Text("Passwords do not match.") }
                } else {
                    null
                }
            )

            AuthPrimaryButton(
                text = "Reset password",
                isLoading = uiState.isLoading,
                enabled = resetCode.isNotBlank() &&
                        newPassword.isNotBlank() &&
                        confirmPassword.isNotBlank() &&
                        !passwordsMismatch,
                onClick = {
                    viewModel.resetPassword(
                        resetCode.trim(),
                        newPassword,
                        confirmPassword
                    )
                }
            )
        }

        uiState.error?.let { error ->
            AuthMessage(text = error, isError = true)
        }

        uiState.message?.let { message ->
            AuthMessage(text = message, isError = false)
        }
    }
}
