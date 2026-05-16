package com.example.hibuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.example.hibuddy.data.remote.dto.CourseSuggestionResponse
import com.example.hibuddy.ui.screens.profile.ProfileViewModel
import com.example.hibuddy.ui.screens.auth.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {},
    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
    authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val uiState by profileViewModel.uiState.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        profileViewModel.loadProfile()
        profileViewModel.loadSuggestions()
    }

    val profile = uiState.profile
    val suggestions = uiState.courseSuggestions

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D14))
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Profile", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color(0xFFF0EFF8))
            Row {
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color.White)
                }
                IconButton(onClick = { profileViewModel.refreshSuggestions() }) {
                    Icon(Icons.Filled.Settings, "Settings", tint = Color(0xFF8B8AAC))
                }
                IconButton(onClick = {
                    authViewModel.logout()
                    onLogout()
                }) {
                    Icon(Icons.Filled.Logout, "Logout", tint = Color(0xFFFF4D6D))
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7C6AF7).copy(alpha = 0.2f))
                    .border(2.dp, Color(0xFF7C6AF7), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile?.displayName?.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 48.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    profile?.displayName ?: "Loading...",
                    fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF0EFF8)
                )
                if (profile?.verifiedStudent == true) {
                    Surface(shape = CircleShape, color = Color(0xFF7C6AF7)) {
                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp).padding(2.dp))
                    }
                }
            }
            Text(
                "${profile?.email ?: ""} ${if (profile?.university != null) "• ${profile.university}" else ""}",
                fontSize = 13.sp, color = Color(0xFF8B8AAC)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStatColumn("${profile?.projectsCompleted ?: 0}", "Projects")
                ProfileStatColumn("${profile?.roles?.size ?: 0}", "Roles")
                ProfileStatColumn(String.format("%.1f", profile?.reputationScore ?: 0.0), "Rep Score")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text("ABOUT ME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5A5A7A), letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                profile?.bio ?: "No bio added yet",
                fontSize = 14.sp, color = Color(0xFFB0AFC8), lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (profile?.roles?.isNotEmpty() == true) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("ROLES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5A5A7A), letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    profile.roles.forEach { role ->
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF7C6AF7).copy(alpha = 0.15f)) {
                            Text(
                                role.roleName,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 12.sp, color = Color(0xFF7C6AF7), fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (profile?.skills?.isNotEmpty() == true) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("SKILLS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5A5A7A), letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    profile.skills.take(6).forEach { skill ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (skill.level) {
                                "BEGINNER" -> Color(0xFF4ECDC4).copy(alpha = 0.15f)
                                "INTERMEDIATE" -> Color(0xFF7C6AF7).copy(alpha = 0.15f)
                                "ADVANCED" -> Color(0xFFFF6B6B).copy(alpha = 0.15f)
                                else -> Color(0xFF1E1D2E)
                            }
                        ) {
                            Text(
                                "${skill.skillName} (${skill.level.first()})",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                color = when (skill.level) {
                                    "BEGINNER" -> Color(0xFF4ECDC4)
                                    "INTERMEDIATE" -> Color(0xFF7C6AF7)
                                    "ADVANCED" -> Color(0xFFFF6B6B)
                                    else -> Color(0xFF8B8AAC)
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (suggestions.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AI SUGGESTED COURSES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5A5A7A), letterSpacing = 1.sp)
                    TextButton(onClick = { profileViewModel.refreshSuggestions() }) {
                        Text("Refresh", fontSize = 11.sp, color = Color(0xFF7C6AF7))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                suggestions.forEach { course ->
                    CourseRecommendationCard(course) { courseId ->
                        profileViewModel.dismissCourse(courseId)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showEditDialog) {
        com.example.hibuddy.ui.screens.profile.EditProfileDialog(
            currentBio = profile?.bio ?: "",
            onDismiss = { showEditDialog = false },
            onSave = { req -> 
                profileViewModel.updateProfile(
                    displayName = req.displayName ?: profile?.displayName,
                    bio = req.bio ?: profile?.bio,
                    location = req.location ?: profile?.location,
                    mode = req.mode ?: profile?.mode,
                    portfolioUrl = req.portfolioUrl ?: profile?.portfolioUrl,
                    githubUrl = req.githubUrl ?: profile?.githubUrl,
                    shortTermGoal = req.shortTermGoal ?: profile?.shortTermGoal
                )
                showEditDialog = false 
            },
            onAddSkill = { skill, level -> profileViewModel.addSkill(skill, level, false) },
            onAddRole = { role -> profileViewModel.addRole(role) }
        )
    }
}

@Composable
fun ProfileStatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 11.sp, color = Color(0xFF6B6A8C))
    }
}

@Composable
fun CourseRecommendationCard(course: CourseSuggestionResponse, onDismiss: (String) -> Unit = {}) {
    var isDismissed by remember { mutableStateOf(false) }

    if (!isDismissed) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF13131F),
            border = BorderStroke(1.dp, Color(0xFF1E1D2E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF7C6AF7).copy(alpha = 0.15f)) {
                        Text(
                            text = "To improve: ${course.targetSkill}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            color = Color(0xFF7C6AF7),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("${course.matchPercent.toInt()}% Match", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(course.courseTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF0EFF8))
                Text("Source: ${course.source}", fontSize = 12.sp, color = Color(0xFF8B8AAC))

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            isDismissed = true
                            onDismiss(course.id)
                        },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1D2E)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Pass", tint = Color(0xFF8B8AAC), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pass", fontSize = 12.sp, color = Color(0xFF8B8AAC))
                    }

                    Button(
                        onClick = { /* Add badge action */ },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C6AF7)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Complete", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Badge", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
