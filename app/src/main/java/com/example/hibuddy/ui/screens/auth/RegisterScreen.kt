package com.example.hibuddy.ui.screens.auth

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var agreeTerms by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val passwordsMismatch = confirmPassword.isNotBlank() && confirmPassword != password
    val isPasswordValid =
        password.length >= 8 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { it.isDigit() }

    val passwordError =
        password.isNotBlank() && !isPasswordValid

    val canSubmit = fullName.isNotBlank() &&
            username.isNotBlank() &&
            email.isNotBlank() &&
            dateOfBirth.isNotBlank() &&
            password.isNotBlank() &&
            confirmPassword.isNotBlank() &&
            !passwordsMismatch &&
            isPasswordValid &&
            agreeTerms

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onRegisterSuccess()
    }

    AuthScreenFrame(
        title = "Create account",
        subtitle = "Start meeting teammates who fit the work you want to build.",
        onBack = onNavigateBack
    ) {
        AuthTextField(
            value = fullName,
            onValueChange = {
                fullName = it
                if (uiState.error != null) viewModel.clearError()
            },
            label = "Full name",
            leadingIcon = Icons.Filled.Person,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        AuthTextField(
            value = username,
            onValueChange = {
                username = it
                if (uiState.error != null) viewModel.clearError()
            },
            label = "Username",
            leadingIcon = Icons.Filled.Person,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

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
                imeAction = ImeAction.Next
            )
        )

        AuthTextField(
            value = dateOfBirth,
            onValueChange = {
                dateOfBirth = it
                if (uiState.error != null) viewModel.clearError()
            },
            label = "Date of birth",
            placeholder = "DD/MM/YYYY",
            leadingIcon = Icons.Filled.DateRange,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        AuthTextField(
            value = phone,
            onValueChange = {
                phone = it
                if (uiState.error != null) viewModel.clearError()
            },
            label = "Phone",
            leadingIcon = Icons.Filled.Phone,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            )
        )

        AuthTextField(
            value = password,
            isError = passwordError,
            onValueChange = {
                password = it
                if (uiState.error != null) viewModel.clearError()
            },
            label = "Password",
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
            },
            supportingText = {
                Text(
                    text = if (passwordError) {
                        "Password must contain uppercase, lowercase, number and 8+ chars."
                    } else {
                        "8+ chars with uppercase, lowercase, and a number."
                    },
                    color = if (passwordError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
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
            visualTransformation = if (confirmPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = "Toggle confirm password visibility",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            supportingText = if (passwordsMismatch) {
                { Text("Passwords do not match.") }
            } else {
                null
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = agreeTerms,
                onCheckedChange = { agreeTerms = it }
            )
            Text(
                text = "I agree to the Terms of Service",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        AuthPrimaryButton(
            text = "Create account",
            isLoading = uiState.isLoading,
            enabled = canSubmit,
            onClick = {
                viewModel.register(
                    fullName = fullName.trim(),
                    username = username.trim(),
                    email = email.trim(),
                    dateOfBirth = dateOfBirth.trim(),
                    password = password,
                    confirmPassword = confirmPassword,
                    phone = phone.ifBlank { null },
                    agreeTerms = agreeTerms
                )
            }
        )

        uiState.error?.let { error ->
            AuthMessage(text = error, isError = true)
        }

        uiState.message?.let { message ->
            AuthMessage(text = message, isError = false)
        }

        AuthFooterAction(
            prompt = "Already have an account?",
            action = "Sign in",
            onClick = onNavigateBack
        )
    }
}
