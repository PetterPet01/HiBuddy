package com.example.hibuddy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.ServiceLocator
import com.example.hibuddy.data.remote.dto.CourseSuggestionResponse
import com.example.hibuddy.data.remote.dto.MyFeedbackSummaryResponse
import com.example.hibuddy.ui.theme.HiBuddyColors
import com.example.hibuddy.ui.screens.auth.AuthViewModel
import com.example.hibuddy.ui.screens.profile.EditProfileDialog
import com.example.hibuddy.ui.screens.profile.ProfileViewModel
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.material.icons.automirrored.filled.Logout
@Composable
private fun FeedbackSummarySection() {
    var feedbackSummary by remember { mutableStateOf<MyFeedbackSummaryResponse?>(null) }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ServiceLocator.feedbackRepository.getMyFeedbackSummary()
            .onSuccess { feedbackSummary = it }
            .onFailure { isError = true }
    }

    val summary = feedbackSummary ?: return
    if (isError || summary.totalFeedbacks <= 0) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Feedback received (${summary.totalFeedbacks})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (summary.weaknesses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Needs improvement: ${summary.weaknesses.joinToString(", ")}",
                    color = HiBuddyColors.warning,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {},
    onCompleteProfile: () -> Unit = {},
    onOpenAdmin: () -> Unit = {},
    onOpenStudentVerification: () -> Unit = {},
    dismissCompletionHint: Boolean,
    onDismissCompletionHint: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
    authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val isDarkMode by ServiceLocator.themeManager.isDarkMode.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    val profile = uiState.profile
    val isProfileIncomplete =
        profile != null &&
                (
                        profile.roles.isEmpty() ||
                                profile.skills.isEmpty()
                        )
    var showEditDialog by remember { mutableStateOf(false) }

    val shouldShowCompletionHint =
        isProfileIncomplete && !dismissCompletionHint

    LaunchedEffect(Unit) {
        profileViewModel.loadProfile()
    }

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            profileViewModel.clearError()
        }
    }

    uiState.message?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(2500)
            profileViewModel.clearMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Profile", fontSize = 26.sp, fontWeight = FontWeight.Black, color = colorScheme.onBackground)

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = {
                        onDismissCompletionHint()
                        onCompleteProfile()
                    }
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit / Complete Profile",
                        tint = if (isProfileIncomplete) {
                            colorScheme.primary
                        } else {
                            colorScheme.onBackground
                        }
                    )
                }

                IconButton(onClick = {
                    authViewModel.logout()
                    onLogout()
                }) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = colorScheme.error)
                }
            }
        }
        if (shouldShowCompletionHint) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 26.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colorScheme.primaryContainer,
                        tonalElevation = 4.dp,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .width(240.dp)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Complete your profile",
                                color = colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = { onDismissCompletionHint() },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Dismiss",
                                    tint = colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Canvas(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 38.dp)
                            .size(width = 18.dp, height = 10.dp)
                    ) {
                        val path = Path().apply {
                            moveTo(size.width / 2f, 0f)
                            lineTo(0f, size.height)
                            lineTo(size.width, size.height)
                            close()
                        }
                        drawPath(
                            path = path,
                            color = colorScheme.primaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (uiState.isLoading && profile == null) {
            Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colorScheme.primary)
            }
            return@Column
        }

        // Message/error banners shown regardless of profile load state
        uiState.message?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = HiBuddyColors.successContainer)
            ) {
                Text(message, modifier = Modifier.padding(14.dp), color = HiBuddyColors.onSuccessContainer, fontSize = 13.sp)
            }
        }

        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.errorContainer)
            ) {
                Text(error, modifier = Modifier.padding(14.dp), color = colorScheme.onErrorContainer, fontSize = 13.sp)
            }
        }

        profile?.let { currentProfile ->
            val uniqueRoles = currentProfile.roles
                .distinctBy { it.roleName.trim().lowercase() }

            val uniqueSkills = currentProfile.skills
                .distinctBy { it.skillName.trim().lowercase() }

            val uniqueInterests = currentProfile.interests
                .distinctBy { it.interestName.trim().lowercase() }
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .border(2.dp, colorScheme.primary, CircleShape)
                        .background(colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentProfile.displayName.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(currentProfile.displayName, fontSize = 24.sp, fontWeight = FontWeight.Black, color = colorScheme.onBackground)
                    if (currentProfile.verifiedStudent) {
                        Surface(shape = CircleShape, color = colorScheme.primary) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = colorScheme.onPrimary, modifier = Modifier.padding(4.dp).size(12.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = listOfNotNull(currentProfile.email, currentProfile.university, currentProfile.location)
                        .filter { it.isNotBlank() }
                        .joinToString(" \u2022 "),
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStatColumn("${currentProfile.projectsCompleted}", "Projects")
                    ProfileStatColumn("${uniqueRoles.size}", "Roles")
                    ProfileStatColumn(String.format("%.1f", currentProfile.reputationScore), "Rep Score")
                }

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedButton(
                    onClick = { profileViewModel.toggleHideProfile() },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.onSurface)
                ) {
                    Icon(
                        imageVector = if (currentProfile.isHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = if (currentProfile.isHidden) HiBuddyColors.warning else HiBuddyColors.success
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (currentProfile.isHidden) "Hidden from Discover" else "Visible in Discover")

                }
            }

            if (!currentProfile.verifiedStudent) {
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onOpenStudentVerification,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text("Student Verification")
                }
            }

            if (ServiceLocator.authRepository.isAdmin()) {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onOpenAdmin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text("Admin Dashboard")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            ProfileSection(title = "ABOUT ME") {
                Text(
                    text = currentProfile.bio ?: "No bio added yet",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
                if (!currentProfile.shortTermGoal.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Goal: ${currentProfile.shortTermGoal}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (
                !currentProfile.githubUrl.isNullOrBlank() ||
                !currentProfile.portfolioUrl.isNullOrBlank() ||
                !currentProfile.facebookUrl.isNullOrBlank()
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                ProfileSection(title = "LINKS") {
                    currentProfile.githubUrl?.takeIf { it.isNotBlank() }?.let { url ->
                        ProfileInfoRow(label = "GitHub", value = url)
                    }

                    currentProfile.portfolioUrl?.takeIf { it.isNotBlank() }?.let { url ->
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileInfoRow(label = "Portfolio", value = url)
                    }

                    currentProfile.facebookUrl?.takeIf { it.isNotBlank() }?.let { url ->
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileInfoRow(label = "Facebook", value = url)
                    }
                }
            }

            if (uniqueInterests.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                ProfileSection(title = "INTERESTS") {
                    uniqueInterests.chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            rowItems.forEach { interest ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        interest.interestName,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            if (uniqueRoles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                ProfileSection(title = "ROLES") {
                    uniqueRoles.chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            rowItems.forEach { role ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        role.roleName,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            if (uniqueSkills.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                ProfileSection(title = "SKILLS") {
                    uniqueSkills.chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            rowItems.forEach { skill ->
                                val chipColor = when (skill.level) {
                                    "BEGINNER" -> HiBuddyColors.info
                                    "INTERMEDIATE" -> MaterialTheme.colorScheme.primary
                                    "ADVANCED", "EXPERT" -> HiBuddyColors.warning
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = chipColor.copy(alpha = 0.16f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "${skill.skillName} \u2022 ${skill.level}",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        fontSize = 12.sp,
                                        color = chipColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

// ... existing code ...

            Spacer(modifier = Modifier.height(20.dp))
            ProfileSection(title = "AI COURSE SUGGESTIONS") {
                Button(
                    onClick = { profileViewModel.refreshSuggestions() },
                    enabled = !uiState.isSuggestionLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (uiState.isSuggestionLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.isSuggestionLoading) "Generating..." else "AI Course Suggestions")
                }

                if (uiState.hasRequestedSuggestions && uiState.courseSuggestions.isEmpty() && !uiState.isSuggestionLoading) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No course suggestions yet. Suggestions appear after late tasks, unfinished tasks, or feedback weaknesses are available.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }

                if (uiState.courseSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    uiState.courseSuggestions.forEach { course ->
                        CourseRecommendationCard(
                            course = course,
                            onDismiss = { courseId -> profileViewModel.dismissCourse(courseId) },
                            onAddBadge = { completedCourse ->
                                profileViewModel.addCompletedCourse(completedCourse.courseTitle, completedCourse.source, completedCourse.courseId)
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            if (showEditDialog) {
                EditProfileDialog(
                    currentBio = currentProfile.bio ?: "",
                    onDismiss = { showEditDialog = false },
                    onSave = { req ->
                        profileViewModel.updateProfile(
                            displayName = req.displayName ?: currentProfile.displayName,
                            bio = req.bio ?: currentProfile.bio,
                            location = req.location ?: currentProfile.location,
                            mode = req.mode ?: currentProfile.mode,
                            portfolioUrl = req.portfolioUrl ?: currentProfile.portfolioUrl,
                            githubUrl = req.githubUrl ?: currentProfile.githubUrl,
                            shortTermGoal = req.shortTermGoal ?: currentProfile.shortTermGoal
                        )
                        showEditDialog = false
                    },
                    onAddSkill = { skill, level -> profileViewModel.addSkill(skill, level, false) },
                    onAddRole = { role -> profileViewModel.addRole(role) }
                )
            }
        }

        // Feedback summary is independent of profile — shown even when profile fails to load
        FeedbackSummarySection()

        Spacer(modifier = Modifier.height(20.dp))

        // Settings are always accessible, regardless of profile load state
        ProfileSection(title = "SETTINGS") {
            ThemeSettingRow(
                isDarkMode = isDarkMode,
                onToggle = { ServiceLocator.themeManager.setDarkMode(it) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ThemeSettingRow(
    isDarkMode: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Appearance",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isDarkMode) "Dark mode" else "Light mode",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isDarkMode,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
fun ProfileStatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun CourseRecommendationCard(
    course: CourseSuggestionResponse,
    onDismiss: (String) -> Unit = {},
    onAddBadge: (CourseSuggestionResponse) -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    var isDismissed by remember { mutableStateOf(false) }

    if (!isDismissed) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = colorScheme.surface,
            border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.55f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(shape = RoundedCornerShape(6.dp), color = colorScheme.primaryContainer) {
                        Text(
                            text = "To improve: ${course.targetSkill}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            color = colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("${course.matchPercent.toInt()}% Match", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HiBuddyColors.success)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(course.courseTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                Text("Source: ${course.source}", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            isDismissed = true
                            onDismiss(course.id)
                        },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Pass", tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pass", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = { onAddBadge(course) },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Complete", tint = colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Badge", fontSize = 12.sp, color = colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}
