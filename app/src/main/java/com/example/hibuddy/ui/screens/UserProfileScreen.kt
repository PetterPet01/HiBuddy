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
import com.example.hibuddy.ui.screens.toUserCard
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.hibuddy.repository.ProjectRepository
import androidx.compose.foundation.clickable

enum class ProfileTab {
    ABOUT,
    PROJECTS,
    REVIEWS
}
@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onProjectsClick: ((String) -> Unit)? = null,
    bottomContent: @Composable (() -> Unit)? = null
) {
    var selectedTab by remember { mutableStateOf(ProfileTab.ABOUT) }
    val userRepository = remember { UserRepository() }

    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var projectCount by remember { mutableStateOf(0) }
    val projectRepository = remember { ProjectRepository() }

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

        projectRepository.getRelatedProjects(
            currentUid = userId,
            onSuccess = { result ->
                projectCount = result.size
            },
            onFailure = { error ->
                println(error)
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
                val userCard = user.toUserCard(0)

                UserSwipeCard(
                    user = userCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(560.dp)
                )

                Spacer(Modifier.height(20.dp))

                ProfileTabs(
                    selectedTab = selectedTab,
                    onTabSelected = {
                        selectedTab = it
                    }
                )

                Spacer(Modifier.height(16.dp))

                when (selectedTab) {
                    ProfileTab.ABOUT -> {
                        AboutTab(user = user)
                    }

                    ProfileTab.PROJECTS -> {
                        ProjectsTab(userId = user.uid)
                    }

                    ProfileTab.REVIEWS -> {
                        ReviewsTab()
                    }
                }

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

@Composable
fun ProfileTabs(
    selectedTab: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProfileTabButton(
            text = "About",
            selected = selectedTab == ProfileTab.ABOUT,
            onClick = { onTabSelected(ProfileTab.ABOUT) },
            modifier = Modifier.weight(1f)
        )

        ProfileTabButton(
            text = "Projects",
            selected = selectedTab == ProfileTab.PROJECTS,
            onClick = { onTabSelected(ProfileTab.PROJECTS) },
            modifier = Modifier.weight(1f)
        )

        ProfileTabButton(
            text = "Reviews",
            selected = selectedTab == ProfileTab.REVIEWS,
            onClick = { onTabSelected(ProfileTab.REVIEWS) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ProfileTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF7C6AF7) else Color(0xFF1E1D2E)
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AboutTab(user: UserProfile) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF16152A)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            ProfileInfoBlock("Organization", user.organization)
            ProfileInfoBlock("Major", user.major)
            ProfileInfoBlock("Current status", user.currentStatus)
            ProfileInfoBlock("Bio", user.bio)
            ProfileInfoBlock("Skills", user.skills.joinToString(", "))
            ProfileInfoBlock("Interests", user.interests.joinToString(", "))
        }
    }
}

@Composable
fun ProjectsTab(userId: String) {
    Text(
        text = "Projects joined or owned by this user will be shown here.",
        color = Color(0xFF8B8AAC)
    )
}

@Composable
fun ReviewsTab() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF16152A)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "No reviews yet.",
                color = Color(0xFF8B8AAC)
            )
        }
    }
}