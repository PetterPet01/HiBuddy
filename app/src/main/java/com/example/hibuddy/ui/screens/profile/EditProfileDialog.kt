package com.example.hibuddy.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hibuddy.data.remote.dto.ProfileUpdateRequest

@Composable
fun EditProfileDialog(
    currentBio: String,
    onDismiss: () -> Unit,
    onSave: (ProfileUpdateRequest) -> Unit,
    onAddSkill: (String, String) -> Unit,
    onAddRole: (String) -> Unit
) {
    var bio by remember { mutableStateOf(currentBio) }
    var newSkill by remember { mutableStateOf("") }
    var newSkillLevel by remember { mutableStateOf("Beginner") }
    var newRole by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    OutlinedTextField(
                        value = newSkill,
                        onValueChange = { newSkill = it },
                        label = { Text("New Skill") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { if(newSkill.isNotBlank()) { onAddSkill(newSkill, newSkillLevel); newSkill="" } }) {
                        Text("Add")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    OutlinedTextField(
                        value = newRole,
                        onValueChange = { newRole = it },
                        label = { Text("New Role") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { if(newRole.isNotBlank()) { onAddRole(newRole); newRole="" } }) {
                        Text("Add")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(ProfileUpdateRequest(bio = bio)) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
