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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.data.remote.dto.RoleResponse
import com.example.hibuddy.data.remote.dto.SkillResponse
import com.example.hibuddy.data.remote.dto.UserDetailResponse
import com.example.hibuddy.data.remote.dto.UserFeedbackItemResponse
import com.example.hibuddy.data.remote.dto.UserProjectHistoryResponse
import com.example.hibuddy.data.remote.dto.UserCardResponse
import com.example.hibuddy.ui.screens.UserSwipeCardStatic

@Composable
fun UserDetailScreen(
    userId: String,
    onBack: () -> Unit,
    onOpenProject: (String) -> Unit,
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    UserSwipeCardStatic(
                        card = profile.asUserCard(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(560.dp)
                    )

                    ProfileSummarySection(profile)
                    RoleSkillsSection(roles = profile.roles, skills = profile.skills)
                    ProjectHistorySection(profile.projectHistory, onOpenProject)
                    FeedbackSection(profile.receivedFeedbacks)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileSummarySection(profile: UserDetailResponse) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Profile Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
            profile.bio?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = colorScheme.onSurfaceVariant, lineHeight = 20.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryStat("Rep Score", String.format("%.1f", profile.reputationScore))
                SummaryStat("Projects", profile.projectsCompleted.toString())
                SummaryStat("Roles", profile.roles.size.toString())
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RoleSkillsSection(roles: List<RoleResponse>, skills: List<SkillResponse>) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Roles and Skills", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
            if (roles.isEmpty() && skills.isEmpty()) {
                Text("No role or skill details yet.", color = colorScheme.onSurfaceVariant)
                return@Column
            }

            if (roles.isEmpty() && skills.isNotEmpty()) {
                Text(
                    skills.joinToString(" • ") { "${it.skillName} (${it.level})" },
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariant
                )
                return@Column
            }

            roles.forEach { role ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(role.roleName, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                    val scopedSkills = role.skills.ifEmpty { skills }
                    if (scopedSkills.isEmpty()) {
                        Text("No skills listed for this role.", fontSize = 13.sp, color = colorScheme.onSurfaceVariant)
                    } else {
                        Text(
                            scopedSkills.joinToString(" • ") { "${it.skillName} (${it.level})" },
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectHistorySection(history: List<UserProjectHistoryResponse>, onOpenProject: (String) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Project History", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
            if (history.isEmpty()) {
                Text("No project history yet.", color = colorScheme.onSurfaceVariant)
                return@Column
            }

            history.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(color = colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenProject(item.projectId) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            item.projectTitle,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            buildString {
                                append(item.role)
                                if (item.isOwner) append(" • Owner")
                                append(" • Joined ${item.joinedAt.take(10)}")
                            },
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Filled.ArrowForward,
                        contentDescription = "Open project",
                        tint = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackSection(feedbacks: List<UserFeedbackItemResponse>) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Feedback from Teammates", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
            if (feedbacks.isEmpty()) {
                Text("No teammate feedback yet.", color = colorScheme.onSurfaceVariant)
                return@Column
            }

            feedbacks.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(color = colorScheme.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.projectTitle, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                            Text(
                                "From ${item.evaluatorName} • ${item.createdAt.take(10)}",
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                String.format("%.1f", item.overallScore),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(
                        item.feedbackText?.takeIf { it.isNotBlank() } ?: "No written feedback provided.",
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun UserDetailResponse.asUserCard(): UserCardResponse {
    return UserCardResponse(
        userId = userId,
        displayName = displayName,
        avatarUrl = avatarUrl,
        verifiedStudent = verifiedStudent,
        university = university,
        bio = bio,
        roles = roles,
        skills = skills,
        location = location,
        githubUrl = githubUrl,
        reputationScore = reputationScore,
        projectsCompleted = projectsCompleted,
        matchScore = matchScore
    )
}
