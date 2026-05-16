package com.example.hibuddy.ui.auth

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit = { _, _ -> },
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    fun validate(): Boolean {
        errorMessage = when {
            email.isBlank() -> "Vui lòng nhập email"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Email không hợp lệ"
            password.isBlank() -> "Vui lòng nhập mật khẩu"
            password.length < 6 -> "Mật khẩu phải có ít nhất 6 ký tự"
            else -> ""
        }
        return errorMessage.isEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "HiBuddy",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Đăng nhập để tìm teammate phù hợp",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = ""
            },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Filled.Mail, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = ""
            },
            label = { Text("Mật khẩu") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(14.dp)
        )

        if (errorMessage.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(22.dp))

        Button(
            onClick = {
                if (validate()) onLoginClick(email.trim(), password)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Đăng nhập", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = onForgotPasswordClick, modifier = Modifier.fillMaxWidth()) {
            Text("Quên mật khẩu?")
        }

        TextButton(onClick = onRegisterClick, modifier = Modifier.fillMaxWidth()) {
            Text("Chưa có tài khoản? Đăng ký")
        }
    }
}