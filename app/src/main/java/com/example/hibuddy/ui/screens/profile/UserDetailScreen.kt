package com.example.hibuddy.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.data.remote.dto.UserFeedbackDetailResponse
import com.example.hibuddy.data.remote.dto.UserProjectHistoryResponse
import com.example.hibuddy.ui.screens.UserSwipeCardStatic
import com.example.hibuddy.ui.theme.HiBuddyColors

@Composable
fun UserDetailScreen(
    userId: String,
    onBack: () -> Unit,
    onOpenProject: (String) -> Unit = {},
    viewModel: UserDetailViewModel = viewModel(factory = UserDetailViewModel.factory(userId))
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("About", "Projects", "Feedback")

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
            Text("Profile Details", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colorScheme.onBackground)
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

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = colorScheme.surface,
                    contentColor = colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            UserSwipeCardStatic(
                                card = profile.toUserCard(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(600.dp)
                            )
                        }
                        1 -> {
                            ProjectsTab(
                                projectHistory = profile.projectHistory,
                                onOpenProject = onOpenProject,
                                colorScheme = colorScheme
                            )
                        }
                        2 -> {
                            FeedbackTab(
                                feedbacks = profile.receivedFeedbacks,
                                colorScheme = colorScheme
                            )
                        }
                    }
                    Spacer(Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ProjectsTab(
    projectHistory: List<UserProjectHistoryResponse>,
    onOpenProject: (String) -> Unit,
    colorScheme: androidx.compose.material3.ColorScheme
) {
    if (projectHistory.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No projects completed yet.",
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        projectHistory.forEach { project ->
            Card(
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenProject(project.projectId) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Work,
                        contentDescription = "Project",
                        tint = colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = project.projectTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Role: ${project.role}",
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Joined: ${project.joinedAt.take(10)}",
                            fontSize = 11.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    if (project.isOwner) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = HiBuddyColors.warningContainer,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "Owner",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = HiBuddyColors.onWarningContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackTab(
    feedbacks: List<UserFeedbackDetailResponse>,
    colorScheme: androidx.compose.material3.ColorScheme
) {
    if (feedbacks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No feedback received yet.",
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        val averageScore = remember(feedbacks) {
            feedbacks.map { it.overallScore }.average()
        }

        // Summary overall rating
        Card(
            colors = CardDefaults.cardColors(containerColor = colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Average Rating",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Based on ${feedbacks.size} review(s)",
                        fontSize = 11.sp,
                        color = colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "Star",
                        tint = HiBuddyColors.warning,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f", averageScore),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "/5.0",
                        fontSize = 14.sp,
                        color = colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Text(
            text = "Reviews",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        feedbacks.forEach { feedback ->
            Card(
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = feedback.evaluatorName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Project: ${feedback.projectTitle}",
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }

                        // Rating badge
                        val scoreColor = when {
                            feedback.overallScore >= 4.0 -> HiBuddyColors.success
                            feedback.overallScore >= 3.0 -> HiBuddyColors.warning
                            else -> colorScheme.error
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = scoreColor.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = scoreColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("%.1f", feedback.overallScore),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = scoreColor
                                )
                            }
                        }
                    }

                    if (!feedback.feedbackText.isNullOrBlank()) {
                        Text(
                            text = feedback.feedbackText,
                            fontSize = 14.sp,
                            color = colorScheme.onSurface,
                            lineHeight = 20.sp
                        )
                    } else {
                        Text(
                            text = "No written comments provided.",
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }

                    Text(
                        text = feedback.createdAt.take(10),
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
