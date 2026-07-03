package com.example.hibuddy.ui.screens.auth

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import com.example.hibuddy.ui.components.DatePickerField
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onRegisterSuccess: (String) -> Unit,
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
    val fullNameErrorMessage = fullNameValidationMessage(fullName)
    val usernameErrorMessage = usernameValidationMessage(username)
    val emailErrorMessage = emailValidationMessage(email)
    val dateOfBirthErrorMessage = dateOfBirthValidationMessage(dateOfBirth)
    val phoneErrorMessage = phoneValidationMessage(phone)

    val canSubmit = fullName.isNotBlank() &&
            fullNameErrorMessage == null &&
            username.isNotBlank() &&
            usernameErrorMessage == null &&
            email.isNotBlank() &&
            emailErrorMessage == null &&
            dateOfBirth.isNotBlank() &&
            dateOfBirthErrorMessage == null &&
            password.isNotBlank() &&
            confirmPassword.isNotBlank() &&
            phoneErrorMessage == null &&
            !passwordsMismatch &&
            isPasswordValid &&
            agreeTerms

    LaunchedEffect(uiState.requiresEmailVerification) {
        if (uiState.requiresEmailVerification) {
            onRegisterSuccess(uiState.pendingEmail ?: email.trim())
        }
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
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            isError = fullNameErrorMessage != null,
            supportingText = {
                Text(
                    text = fullNameErrorMessage ?: "Letters and spaces only.",
                    color = if (fullNameErrorMessage != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        )

        AuthTextField(
            value = username,
            onValueChange = {
                username = it
                if (uiState.error != null) viewModel.clearError()
            },
            label = "Username",
            leadingIcon = Icons.Filled.Person,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            isError = usernameErrorMessage != null,
            supportingText = {
                Text(
                    text = usernameErrorMessage ?: "3+ chars. Letters, numbers, . , _ @ only. No spaces.",
                    color = if (usernameErrorMessage != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
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
            ),
            isError = emailErrorMessage != null,
            supportingText = {
                Text(
                    text = emailErrorMessage ?: "Use a valid email with @ and domain.",
                    color = if (emailErrorMessage != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        )

        DatePickerField(
            value = dateOfBirth,
            onValueChange = {
                dateOfBirth = it
                if (uiState.error != null) viewModel.clearError()
            },
            label = "Date of birth",
            isError = dateOfBirthErrorMessage != null,
            supportingText = {
                Text(
                    text = dateOfBirthErrorMessage ?: "You must be at least 18 years old.",
                    color = if (dateOfBirthErrorMessage != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
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
            ),
            isError = phoneErrorMessage != null,
            supportingText = {
                Text(
                    text = phoneErrorMessage ?: "Use 10 digits starting with 0, or +84 plus 9 digits.",
                    color = if (phoneErrorMessage != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
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
                color = if (agreeTerms) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
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

private fun fullNameValidationMessage(value: String): String? {
    val fullName = value.trim()
    if (fullName.isBlank()) return null
    if (fullName.length < 2) {
        return "Full name is too short."
    }
    if (fullName.any { !it.isLetter() && !it.isWhitespace() }) {
        return "Full name must contain letters only."
    }
    return null
}

private fun usernameValidationMessage(value: String): String? {
    val username = value.trim()
    if (username.isBlank()) return null
    if (username.length < 3) {
        return "Username must be at least 3 characters."
    }
    if (username.any { it.isWhitespace() }) {
        return "Username cannot contain spaces."
    }
    if (!Regex("""^[A-Za-z0-9._,@]+$""").matches(username)) {
        return "Username can only contain letters, numbers, . , _ @."
    }
    return null
}

private fun emailValidationMessage(value: String): String? {
    val email = value.trim()
    if (email.isBlank()) return null
    if (!Regex("""^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$""").matches(email)) {
        return "Email must include @ and a valid domain."
    }
    return null
}

private fun dateOfBirthValidationMessage(value: String): String? {
    if (value.isBlank()) return null

    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply {
        isLenient = false
    }
    val birthDate = try {
        formatter.parse(value.trim())
    } catch (_: ParseException) {
        return "Date of birth must be in DD/MM/YYYY format."
    } ?: return "Date of birth must be in DD/MM/YYYY format."

    val birth = Calendar.getInstance().apply { time = birthDate }
    val today = Calendar.getInstance()
    val earliestAllowed = Calendar.getInstance().apply {
        set(1900, Calendar.JANUARY, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }

    if (!birth.after(earliestAllowed)) {
        return "Date of birth must be later than 01/01/1900."
    }
    if (
        birth.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        birth.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) ||
        birth.after(today)
    ) {
        return "Date of birth must be before today."
    }

    var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
    val birthdayHasNotPassed =
        today.get(Calendar.MONTH) < birth.get(Calendar.MONTH) ||
                (
                        today.get(Calendar.MONTH) == birth.get(Calendar.MONTH) &&
                                today.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH)
                        )
    if (birthdayHasNotPassed) {
        age -= 1
    }

    return if (age < 18) {
        "You must be at least 18 years old."
    } else {
        null
    }
}

private fun phoneValidationMessage(value: String): String? {
    val phone = value.trim()
    if (phone.isBlank()) return null

    if (!Regex("""^\+?\d+$""").matches(phone)) {
        return "Phone must contain digits only, with optional +84 prefix."
    }

    return if (!Regex("""^(0\d{9}|\+84\d{9})$""").matches(phone)) {
        "Phone must be 10 digits starting with 0, or +84 plus 9 digits."
    } else {
        null
    }
}
