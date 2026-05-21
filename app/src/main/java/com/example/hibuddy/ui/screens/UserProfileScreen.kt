package com.example.hibuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hibuddy.data.model.UserProfile
import com.example.hibuddy.repository.UserRepository

@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    bottomContent: @Composable (() -> Unit)? = null
) {
    val userRepository = remember { UserRepository() }

    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        userRepository.getUserProfile(
            uid = userId,
            onSuccess = {
                userProfile = it
                isLoading = false
            },
            onFailure = {
                errorMessage = it
                isLoading = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D14))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Button(onClick = onBack) {
            Text("Back")
        }

        Spacer(Modifier.height(20.dp))

        when {
            isLoading -> CircularProgressIndicator(color = Color(0xFF7C6AF7))

            errorMessage.isNotBlank() -> Text(errorMessage, color = Color(0xFFFF4D6D))

            userProfile == null -> Text("User profile not found", color = Color.White)

            else -> {
                val user = userProfile!!

                Text(
                    text = user.fullName.ifBlank { "Unnamed User" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(user.email, color = Color(0xFF8B8AAC))

                Spacer(Modifier.height(20.dp))

                ProfileInfoBlock("Organization", user.organization)
                ProfileInfoBlock("Major", user.major)
                ProfileInfoBlock("Current status", user.currentStatus)
                ProfileInfoBlock("Bio", user.bio)
                ProfileInfoBlock("Skills", user.skills.joinToString(", "))
                ProfileInfoBlock("Interests", user.interests.joinToString(", "))

                Spacer(Modifier.height(24.dp))

                bottomContent?.invoke()
            }
        }
    }
}

@Composable
fun ProfileInfoBlock(
    label: String,
    value: String
) {
    Spacer(Modifier.height(14.dp))

    Text(
        text = label.uppercase(),
        color = Color(0xFF5A5A7A),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold
    )

    Spacer(Modifier.height(4.dp))

    Text(
        text = value.ifBlank { "Not updated" },
        color = Color(0xFFB0AFC8)
    )
}