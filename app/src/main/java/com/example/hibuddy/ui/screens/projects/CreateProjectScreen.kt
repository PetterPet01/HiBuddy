package com.example.hibuddy.ui.screens.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.data.remote.dto.RoleSlotRequest

data class RoleSlotEntry(
    val roleName: String = "",
    val count: Int = 1,
    val skillRequirements: String = ""
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateProjectScreen(
    onBack: () -> Unit,
    onProjectCreated: (projectId: String) -> Unit = {},
    viewModel: CreateProjectViewModel = viewModel(factory = CreateProjectViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    var title by remember { mutableStateOf("") }
    var field by remember { mutableStateOf("EdTech") }
    var description by remember { mutableStateOf("") }
    var specificGoal by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var maxMembers by remember { mutableStateOf("4") }
    var workMode by remember { mutableStateOf("ONLINE") }
    var commitmentLevel by remember { mutableStateOf("CASUAL") }
    var additionalRequirements by remember { mutableStateOf("") }
    var memberBenefits by remember { mutableStateOf("") }
    var roleSlots by remember { mutableStateOf(listOf(RoleSlotEntry())) }

    var fieldExpanded by remember { mutableStateOf(false) }
    var workModeExpanded by remember { mutableStateOf(false) }
    var commitmentExpanded by remember { mutableStateOf(false) }

    val fields = listOf("EdTech", "Climate Tech", "HealthTech", "FinTech", "AI/ML", "Mobile", "Web", "Gaming", "IoT", "Other")
    val workModes = listOf("ONLINE" to "Online", "OFFLINE" to "Offline", "HYBRID" to "Hybrid")
    val commitments = listOf("CASUAL" to "Casual", "SERIOUS" to "Serious", "FULLTIME" to "Full-time")

    LaunchedEffect(uiState.createdProject) {
        uiState.createdProject?.let {
            onProjectCreated(it.id)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D14))
    ) {
        TopAppBar(
            title = { Text("Create Project", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF0EFF8)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = Color(0xFFF0EFF8))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF13131F))
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Project Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = textFieldColors()
            )

            ExposedDropdownMenuBox(
                expanded = fieldExpanded,
                onExpandedChange = { fieldExpanded = it }
            ) {
                OutlinedTextField(
                    value = field,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Field") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fieldExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = textFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = fieldExpanded,
                    onDismissRequest = { fieldExpanded = false }
                ) {
                    fields.forEach { f ->
                        DropdownMenuItem(
                            text = { Text(f) },
                            onClick = { field = f; fieldExpanded = false }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 500) description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                supportingText = { Text("${description.length}/500", fontSize = 11.sp, color = Color(0xFF6B6A8C)) },
                colors = textFieldColors()
            )

            OutlinedTextField(
                value = specificGoal,
                onValueChange = { if (it.length <= 500) specificGoal = it },
                label = { Text("Specific Goal (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3,
                colors = textFieldColors()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start Date") },
                    placeholder = { Text("DD/MM/YYYY") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = textFieldColors()
                )
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("End Date") },
                    placeholder = { Text("DD/MM/YYYY") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = textFieldColors()
                )
            }

            OutlinedTextField(
                value = maxMembers,
                onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) maxMembers = it },
                label = { Text("Max Members") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = textFieldColors()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = workModeExpanded,
                    onExpandedChange = { workModeExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = workModes.find { it.first == workMode }?.second ?: workMode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Work Mode") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = workModeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = textFieldColors()
                    )
                    ExposedDropdownMenu(expanded = workModeExpanded, onDismissRequest = { workModeExpanded = false }) {
                        workModes.forEach { (value, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { workMode = value; workModeExpanded = false })
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = commitmentExpanded,
                    onExpandedChange = { commitmentExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = commitments.find { it.first == commitmentLevel }?.second ?: commitmentLevel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Commitment") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = commitmentExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = textFieldColors()
                    )
                    ExposedDropdownMenu(expanded = commitmentExpanded, onDismissRequest = { commitmentExpanded = false }) {
                        commitments.forEach { (value, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { commitmentLevel = value; commitmentExpanded = false })
                        }
                    }
                }
            }

            Text("Role Slots", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B8AAC))
            roleSlots.forEachIndexed { index, slot ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF13131F)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Slot ${index + 1}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C6AF7))
                            Spacer(Modifier.weight(1f))
                            if (roleSlots.size > 1) {
                                IconButton(
                                    onClick = {
                                        roleSlots = roleSlots.toMutableList().also { it.removeAt(index) }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Close, "Remove", tint = Color(0xFFFF4D6D), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = slot.roleName,
                            onValueChange = { newName ->
                                roleSlots = roleSlots.toMutableList().also { it[index] = it[index].copy(roleName = newName) }
                            },
                            label = { Text("Role Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = textFieldColors()
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = slot.count.toString(),
                                onValueChange = { newCount ->
                                    val n = newCount.toIntOrNull() ?: return@OutlinedTextField
                                    roleSlots = roleSlots.toMutableList().also { it[index] = it[index].copy(count = n) }
                                },
                                label = { Text("Count") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = textFieldColors()
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = slot.skillRequirements,
                            onValueChange = { newSkills ->
                                roleSlots = roleSlots.toMutableList().also { it[index] = it[index].copy(skillRequirements = newSkills) }
                            },
                            label = { Text("Skill Requirements (comma-separated, optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = textFieldColors()
                        )
                    }
                }
            }

            TextButton(onClick = { roleSlots = roleSlots + RoleSlotEntry() }) {
                Icon(Icons.Filled.Add, null, tint = Color(0xFF7C6AF7), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Role Slot", color = Color(0xFF7C6AF7))
            }

            OutlinedTextField(
                value = additionalRequirements,
                onValueChange = { additionalRequirements = it },
                label = { Text("Additional Requirements (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3,
                colors = textFieldColors()
            )

            OutlinedTextField(
                value = memberBenefits,
                onValueChange = { memberBenefits = it },
                label = { Text("Member Benefits (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3,
                colors = textFieldColors()
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.createProject(
                        title = title,
                        field = field,
                        description = description,
                        specificGoal = specificGoal.ifBlank { null },
                        startDate = startDate,
                        endDate = endDate,
                        maxMembers = maxMembers.toIntOrNull() ?: 4,
                        workMode = workMode,
                        commitmentLevel = commitmentLevel,
                        roleSlots = roleSlots.map { RoleSlotRequest(it.roleName, it.count, it.skillRequirements.ifBlank { null }) },
                        additionalRequirements = additionalRequirements.ifBlank { null },
                        memberBenefits = memberBenefits.ifBlank { null }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !uiState.isLoading && title.isNotBlank() && description.isNotBlank()
                        && startDate.isNotBlank() && endDate.isNotBlank()
                        && roleSlots.all { it.roleName.isNotBlank() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C6AF7)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Create Project", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            uiState.error?.let { error ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0x33FF4D6D))) {
                    Text(error, modifier = Modifier.padding(16.dp), color = Color(0xFFFF4D6D), fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF7C6AF7),
    unfocusedBorderColor = Color(0xFF2A2840),
    focusedLabelColor = Color(0xFF7C6AF7),
    unfocusedLabelColor = Color(0xFF6B6A8C),
    cursorColor = Color(0xFF7C6AF7),
    focusedTextColor = Color(0xFFF0EFF8),
    unfocusedTextColor = Color(0xFFF0EFF8),
    focusedContainerColor = Color(0xFF13131F),
    unfocusedContainerColor = Color(0xFF13131F),
)
