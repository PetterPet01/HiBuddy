package com.example.hibuddy.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CompleteProfileScreen(
    onComplete: () -> Unit = {}
) {
    val statusOptions = listOf(
        "Student",
        "Working Professional",
        "Freelancer",
        "Founder",
        "Researcher",
        "Job Seeker",
        "Other"
    )

    val majorOptions = listOf(
        "Computer Science",
        "Software Engineering",
        "Information Systems",
        "Data Science",
        "Artificial Intelligence",
        "Cybersecurity",
        "Business Administration",
        "Marketing",
        "Finance",
        "Accounting",
        "Economics",
        "Design",
        "Graphic Design",
        "Communication",
        "Media Studies",
        "Psychology",
        "Education",
        "English Language",
        "Japanese Language",
        "Korean Language",
        "Medicine",
        "Pharmacy",
        "Biotechnology",
        "Environmental Science",
        "Mechanical Engineering",
        "Electrical Engineering",
        "Civil Engineering",
        "Architecture",
        "Law",
        "Logistics",
        "Tourism",
        "Hospitality Management",
        "Other"
    )

    val skillOptions = listOf(
        "Kotlin", "Android", "Firebase", "Java", "Python", "SQL",
        "Web Development", "UI/UX", "Figma", "Machine Learning",
        "Marketing", "Market Research", "Branding", "Sales",
        "Business Planning", "Financial Analysis", "Accounting",
        "Project Management", "Product Management",
        "Graphic Design", "Video Editing", "Photography",
        "Content Writing", "Copywriting", "Social Media Management",
        "Public Speaking", "Presentation Design",
        "Research", "Data Analysis", "Survey Design",
        "Academic Writing", "Report Writing", "Critical Thinking",
        "English", "Japanese", "Korean", "Chinese",
        "Translation", "Interpreting",
        "CAD", "3D Modeling", "Electronics", "IoT",
        "Event Planning", "Teaching", "Leadership", "Teamwork"
    )

    val interestOptions = listOf(
        "AI", "Startup", "Education", "Health", "Environment",
        "Sustainability", "Finance", "E-commerce", "Social Impact",
        "Community Service", "Mental Health", "Language Learning",
        "Travel", "Food", "Music", "Film", "Gaming", "Sports",
        "Fashion", "Beauty", "Architecture", "Urban Planning",
        "Robotics", "Mobile App", "Web Platform", "Research",
        "Hackathon", "Open Source", "Volunteering", "Event",
        "Media", "Content Creation", "Entrepreneurship"
    )

    var organization by remember { mutableStateOf("") }
    var currentStatus by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    var selectedSkills by remember { mutableStateOf(setOf<String>()) }
    var selectedInterests by remember { mutableStateOf(setOf<String>()) }

    var statusExpanded by remember { mutableStateOf(false) }
    var majorExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D14))
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Complete Your Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tell us more about yourself for better matching.",
            color = Color(0xFFB0AFC8)
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = organization,
            onValueChange = {
                organization = it
                errorMessage = ""
            },
            label = { Text("School / Company / Organization") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        ExposedDropdownMenuBox(
            expanded = statusExpanded,
            onExpandedChange = { statusExpanded = !statusExpanded }
        ) {
            OutlinedTextField(
                value = currentStatus,
                onValueChange = {},
                readOnly = true,
                label = { Text("Current Status") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(14.dp)
            )

            ExposedDropdownMenu(
                expanded = statusExpanded,
                onDismissRequest = { statusExpanded = false }
            ) {
                statusOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            currentStatus = option
                            statusExpanded = false
                            errorMessage = ""
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        ExposedDropdownMenuBox(
            expanded = majorExpanded,
            onExpandedChange = { majorExpanded = !majorExpanded }
        ) {
            OutlinedTextField(
                value = major,
                onValueChange = {},
                readOnly = true,
                label = { Text("Field / Major") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = majorExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(14.dp)
            )

            ExposedDropdownMenu(
                expanded = majorExpanded,
                onDismissRequest = { majorExpanded = false }
            ) {
                majorOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            major = option
                            majorExpanded = false
                            errorMessage = ""
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = bio,
            onValueChange = {
                bio = it
                errorMessage = ""
            },
            label = { Text("Bio") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            minLines = 4
        )

        Spacer(modifier = Modifier.height(22.dp))

        Text("Skills", color = Color.White, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            skillOptions.forEach { skill ->
                FilterChip(
                    selected = selectedSkills.contains(skill),
                    onClick = {
                        selectedSkills =
                            if (selectedSkills.contains(skill)) selectedSkills - skill
                            else selectedSkills + skill
                    },
                    label = { Text(skill) }
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text("Interests", color = Color.White, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            interestOptions.forEach { interest ->
                FilterChip(
                    selected = selectedInterests.contains(interest),
                    onClick = {
                        selectedInterests =
                            if (selectedInterests.contains(interest)) selectedInterests - interest
                            else selectedInterests + interest
                    },
                    label = { Text(interest) }
                )
            }
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                val uid = currentUser?.uid

                when {
                    uid == null -> {
                        errorMessage = "User not found"
                    }

                    organization.isBlank() -> {
                        errorMessage = "Please enter school, company, or organization"
                    }

                    currentStatus.isBlank() -> {
                        errorMessage = "Please select current status"
                    }

                    major.isBlank() -> {
                        errorMessage = "Please select field / major"
                    }

                    selectedSkills.isEmpty() -> {
                        errorMessage = "Please select at least one skill"
                    }

                    selectedInterests.isEmpty() -> {
                        errorMessage = "Please select at least one interest"
                    }

                    else -> {
                        isLoading = true

                        val updates = mapOf(
                            "organization" to organization.trim(),
                            "currentStatus" to currentStatus,
                            "major" to major,
                            "bio" to bio.trim(),
                            "skills" to selectedSkills.toList(),
                            "interests" to selectedInterests.toList()
                        )

                        db.collection("users")
                            .document(uid)
                            .update(updates)
                            .addOnSuccessListener {
                                isLoading = false
                                onComplete()
                            }
                            .addOnFailureListener {
                                isLoading = false
                                errorMessage = it.message ?: "Update failed"
                            }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !isLoading,
            shape = RoundedCornerShape(14.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Save Profile")
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}