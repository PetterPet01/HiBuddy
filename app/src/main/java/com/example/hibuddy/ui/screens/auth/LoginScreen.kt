package com.example.hibuddy.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.hibuddy.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import androidx.compose.material3.OutlinedButton

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: () -> Unit,
    onEmailVerificationRequired: (String) -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val canSubmit = username.isNotBlank() && password.isNotBlank()

    val loginError = uiState.error != null

    LaunchedEffect(uiState.isLoggedIn, uiState.requiresEmailVerification) {
        when {
            uiState.isLoggedIn -> onLoginSuccess()
            uiState.requiresEmailVerification ->
                onEmailVerificationRequired(uiState.pendingEmail.orEmpty())
        }
    }

    AuthScreenFrame(
        title = "Welcome back",
        subtitle = "Pick up your matches, projects, and conversations."
    ) {
        AuthTextField(
            value = username,
            onValueChange = {
                username = it
                if (uiState.error != null) viewModel.clearError()
            },
            label = "Username or email",
            leadingIcon = Icons.Filled.Person,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        AuthTextField(
            value = password,
            isError = loginError,
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
                imeAction = ImeAction.Done
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

            supportingText = if (loginError) {
                {
                    Text(
                        text = uiState.error ?: "Đăng nhập thất bại",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                null
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it }
                )
                Text(
                    text = "Remember me",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            TextButton(onClick = onNavigateToForgotPassword) {
                Text("Forgot password?")
            }
        }

        AuthPrimaryButton(
            text = "Sign in",
            isLoading = uiState.isLoading,
            enabled = canSubmit,
            onClick = { viewModel.login(username.trim(), password, rememberMe) }
        )

        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        runCatching {
                            val option = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                                .build()
                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(option)
                                .build()
                            CredentialManager.create(context)
                                .getCredential(context, request)
                                .credential
                        }.onSuccess { credential ->
                            if (
                                credential is CustomCredential &&
                                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                            ) {
                                val googleCredential =
                                    GoogleIdTokenCredential.createFrom(credential.data)
                                viewModel.googleLogin(googleCredential.idToken)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue with Google")
            }
        }

        AuthFooterAction(
            prompt = "New to HiBuddy?",
            action = "Create account",
            onClick = onNavigateToRegister
        )
    }
}
