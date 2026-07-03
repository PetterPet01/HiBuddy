package com.example.hibuddy.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.data.remote.dto.UserCardResponse
import com.example.hibuddy.ui.screens.UserSwipeCardStatic

@Composable
fun UserDetailScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: UserDetailViewModel = viewModel(factory = UserDetailViewModel.factory(userId))
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(userId) {
        viewModel.loadProfile()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colorScheme.onBackground)
        }

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.error.orEmpty(), color = colorScheme.error)
                }
            }
            uiState.profile != null -> {
                val profile = uiState.profile!!
                val card = UserCardResponse(
                    userId = profile.userId,
                    displayName = profile.displayName,
                    avatarUrl = profile.avatarUrl,
                    verifiedStudent = profile.verifiedStudent,
                    university = profile.university,
                    bio = profile.bio,
                    roles = profile.roles,
                    skills = profile.skills,
                    location = profile.location,
                    githubUrl = profile.githubUrl,
                    reputationScore = profile.reputationScore,
                    projectsCompleted = profile.projectsCompleted,
                    matchScore = profile.matchScore
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    UserSwipeCardStatic(
                        card = card,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(640.dp)
                    )

                    Card(colors = CardDefaults.cardColors(containerColor = colorScheme.surface)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Project history", fontWeight = FontWeight.Bold)
                            if (profile.projectHistory.isEmpty()) {
                                Text("No project history yet", color = colorScheme.onSurfaceVariant)
                            } else {
                                profile.projectHistory.forEach { item ->
                                    Text("• ${item.projectTitle} - ${item.role}", fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = colorScheme.surface)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Feedback from teammates", fontWeight = FontWeight.Bold)
                            if (profile.receivedFeedbacks.isEmpty()) {
                                Text("No feedback yet", color = colorScheme.onSurfaceVariant)
                            } else {
                                profile.receivedFeedbacks.take(5).forEach { fb ->
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("${fb.projectTitle} by ${fb.evaluatorName}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Score: ${fb.overallScore}", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                                        fb.feedbackText?.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 12.sp) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
