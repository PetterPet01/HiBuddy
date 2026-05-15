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

// Course Data Model specifically for NV-09
data class CourseSuggestion(
    val title: String,
    val source: String,
    val targetSkill: String,
    val matchPercent: Int
)

val courseSuggestions = listOf(
    CourseSuggestion("GraphQL w/ Kotlin Masterclass", "Udemy", "Backend Dev", 92),
    CourseSuggestion("Advanced Machine Learning", "Coursera", "ML Engineer", 88)
)

@Composable
fun ProfileScreen() {
    val user = SampleData.users.first() // Load current user

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D14))
            .verticalScroll(rememberScrollState())
    ) {
        // --- Header & Settings ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Profile", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color(0xFFF0EFF8))
            Icon(Icons.Filled.Settings, "Settings", tint = Color(0xFF8B8AAC))
        }

        // --- Profile Info ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(user.avatarColor.copy(alpha = 0.2f))
                    .border(2.dp, user.avatarColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = user.avatarEmoji, fontSize = 48.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(user.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF0EFF8))
                if (user.isVerified) {
                    Surface(shape = CircleShape, color = Color(0xFF7C6AF7)) {
                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp).padding(2.dp))
                    }
                }
            }
            Text("${user.username} • ${user.university}", fontSize = 13.sp, color = Color(0xFF8B8AAC))

            Spacer(modifier = Modifier.height(20.dp))

            // Profile Stats
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStatColumn(user.projectsCompleted.toString(), "Projects")
                ProfileStatColumn("${user.matchScore}%", "Match Rate")
                ProfileStatColumn(user.reputationStars.toString(), "Rep Score")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Bio ---
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text("ABOUT ME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5A5A7A), letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(user.bio, fontSize = 14.sp, color = Color(0xFFB0AFC8), lineHeight = 20.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- AI Skill & Course Suggestions (NV-09) ---
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("AI SUGGESTED COURSES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5A5A7A), letterSpacing = 1.sp)
                Text("Refresh", fontSize = 11.sp, color = Color(0xFF7C6AF7))
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            courseSuggestions.forEach { course ->
                CourseRecommendationCard(course)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
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
fun CourseRecommendationCard(course: CourseSuggestion) {
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
                    Text("${course.matchPercent}% Match", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(course.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF0EFF8))
                Text("Source: ${course.source}", fontSize = 12.sp, color = Color(0xFF8B8AAC))

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { isDismissed = true },
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
                        onClick = { /* Add to profile action */ },
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
