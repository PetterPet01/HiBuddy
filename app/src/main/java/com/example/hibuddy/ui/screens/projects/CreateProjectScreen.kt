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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.data.remote.dto.RoleSlotRequest
import com.example.hibuddy.data.remote.dto.SkillRequirementRequest
import com.example.hibuddy.ui.components.DatePickerField
import com.example.hibuddy.ui.theme.hiBuddyTextFieldColors
import com.example.hibuddy.ui.common.ProfileCatalog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RoleSlotEntry(
    val roleName: String = "",
    val count: Int = 1,
    val skillRequirements: List<SkillEntry> = listOf(SkillEntry())
)

data class SkillEntry(
    val skillName: String = "",
    val level: String = "BEGINNER"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateProjectScreen(
    onBack: () -> Unit,
    onProjectCreated: (projectId: String) -> Unit = {},
    viewModel: CreateProjectViewModel = viewModel(factory = CreateProjectViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

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
    var customRoleInput by remember { mutableStateOf("") }

    var fieldExpanded by remember { mutableStateOf(false) }
    var workModeExpanded by remember { mutableStateOf(false) }
    var commitmentExpanded by remember { mutableStateOf(false) }

    val fields = ProfileCatalog.fieldOptions
    val workModes = listOf("ONLINE" to "Online", "OFFLINE" to "Offline", "HYBRID" to "Hybrid")
    val commitments = listOf("CASUAL" to "Casual", "MODERATE" to "Moderate", "INTENSIVE" to "Intensive")
    val roleOptions = ProfileCatalog.roleOptions
    val roleSkillMap = ProfileCatalog.roleSkillMap
    val skillLevels = listOf("BEGINNER", "INTERMEDIATE", "ADVANCED")

    fun parseDate(value: String): Date? = runCatching {
        SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(value)
    }.getOrNull()

    val startDateError = when {
        startDate.isBlank() -> null
        parseDate(startDate) == null -> "Invalid date"
        parseDate(startDate)?.before(Date()) == true -> "Start date must be after today"
        else -> null
    }
    val endDateError = when {
        endDate.isBlank() -> null
        parseDate(endDate) == null -> "Invalid date"
        startDate.isNotBlank() && parseDate(startDate) != null && parseDate(endDate)?.before(parseDate(startDate)) == true -> "End date must be after start date"
        else -> null
    }

    LaunchedEffect(uiState.createdProject) {
        uiState.createdProject?.let {
            onProjectCreated(it.id)
            viewModel.clearCreatedProject()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Create Project", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = colorScheme.onSurface)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.surface)
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
                supportingText = { Text("${description.length}/500", fontSize = 11.sp, color = colorScheme.onSurfaceVariant) },
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
                DatePickerField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = "Start Date",
                    modifier = Modifier.weight(1f)
                )
                DatePickerField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = "End Date",
                    modifier = Modifier.weight(1f)
                )
            }
            if (startDateError != null) {
                Text(startDateError, color = colorScheme.error, fontSize = 12.sp)
            }
            if (endDateError != null) {
                Text(endDateError, color = colorScheme.error, fontSize = 12.sp)
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

            Text("Role Slots", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
            roleSlots.forEachIndexed { index, slot ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Slot ${index + 1}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                            Spacer(Modifier.weight(1f))
                            if (roleSlots.size > 1) {
                                IconButton(
                                    onClick = {
                                        roleSlots = roleSlots.toMutableList().also { it.removeAt(index) }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Close, "Remove", tint = colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        SearchableRoleDropdown(
                            value = slot.roleName,
                            options = roleOptions,
                            onValueChange = { newRole ->
                                val suggested = roleSkillMap[newRole].orEmpty().map { SkillEntry(it) }
                                roleSlots = roleSlots.toMutableList().also {
                                    it[index] = it[index].copy(
                                        roleName = newRole,
                                        skillRequirements = if (suggested.isNotEmpty()) suggested else listOf(SkillEntry())
                                    )
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customRoleInput,
                            onValueChange = { customRoleInput = it },
                            label = { Text("Add new role name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = textFieldColors()
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val value = customRoleInput.trim()
                                if (value.isNotBlank()) {
                                    roleSlots = roleSlots.toMutableList().also {
                                        it[index] = it[index].copy(
                                            roleName = value,
                                            skillRequirements = listOf(SkillEntry())
                                        )
                                    }
                                    customRoleInput = ""
                                }
                            },
                            enabled = customRoleInput.isNotBlank()
                        ) {
                            Text("Add role")
                        }
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
                        Text("Skill requirements", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        slot.skillRequirements.forEachIndexed { skillIndex, skillEntry ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = skillEntry.skillName,
                                    onValueChange = { value ->
                                        roleSlots = roleSlots.toMutableList().also { slots ->
                                            val updated = slots[index].skillRequirements.toMutableList()
                                            updated[skillIndex] = updated[skillIndex].copy(skillName = value)
                                            slots[index] = slots[index].copy(skillRequirements = updated)
                                        }
                                    },
                                    label = { Text("Skill") },
                                    modifier = Modifier.weight(1.2f),
                                    singleLine = true,
                                    colors = textFieldColors()
                                )
                                SkillLevelDropdown(
                                    value = skillEntry.level,
                                    options = skillLevels,
                                    modifier = Modifier.weight(0.8f),
                                    onValueChange = { level ->
                                        roleSlots = roleSlots.toMutableList().also { slots ->
                                            val updated = slots[index].skillRequirements.toMutableList()
                                            updated[skillIndex] = updated[skillIndex].copy(level = level)
                                            slots[index] = slots[index].copy(skillRequirements = updated)
                                        }
                                    }
                                )
                                IconButton(
                                    onClick = {
                                        roleSlots = roleSlots.toMutableList().also { slots ->
                                            val updated = slots[index].skillRequirements.toMutableList()
                                            if (updated.size > 1) {
                                                updated.removeAt(skillIndex)
                                            } else {
                                                updated[skillIndex] = SkillEntry()
                                            }
                                            slots[index] = slots[index].copy(skillRequirements = updated)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove skill", tint = colorScheme.error)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            TextButton(
                                onClick = {
                                    roleSlots = roleSlots.toMutableList().also { slots ->
                                        val updated = slots[index].skillRequirements.toMutableList()
                                        updated += SkillEntry()
                                        slots[index] = slots[index].copy(skillRequirements = updated)
                                    }
                                }
                            ) { Text("Add skill") }
                        }
                    }
                }
            }

            TextButton(onClick = { roleSlots = roleSlots + RoleSlotEntry() }) {
                Icon(Icons.Filled.Add, null, tint = colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Role Slot", color = colorScheme.primary)
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
                        roleSlots = roleSlots.map { slot ->
                            RoleSlotRequest(
                                slot.roleName,
                                slot.count,
                                slot.skillRequirements
                                    .filter { it.skillName.isNotBlank() }
                                    .distinctBy { it.skillName.lowercase() }
                                    .map { SkillRequirementRequest(it.skillName.trim(), it.level) }
                            )
                        },
                        additionalRequirements = additionalRequirements.ifBlank { null },
                        memberBenefits = memberBenefits.ifBlank { null }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !uiState.isLoading && title.isNotBlank() && description.isNotBlank()
                        && startDate.isNotBlank() && endDate.isNotBlank()
                        && startDateError == null && endDateError == null
                        && roleSlots.all { it.roleName.isNotBlank() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = colorScheme.onPrimary)
                } else {
                    Text("Create Project", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            uiState.error?.let { error ->
                Card(colors = CardDefaults.cardColors(containerColor = colorScheme.errorContainer)) {
                    Text(error, modifier = Modifier.padding(16.dp), color = colorScheme.onErrorContainer, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun textFieldColors() = hiBuddyTextFieldColors()
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchableRoleDropdown(
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember(value) { mutableStateOf(value) }

    val filteredOptions = remember(query, options) {
        options
            .filter { it.contains(query, ignoreCase = true) }
            .sorted()
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = it
            if (it) query = ""
        }
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                expanded = true
            },
            label = { Text("Role Name") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            singleLine = true,
            colors = textFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                query = value
            }
        ) {
            if (filteredOptions.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No matching role") },
                    onClick = {}
                )
            } else {
                filteredOptions.forEach { role ->
                    DropdownMenuItem(
                        text = { Text(role) },
                        onClick = {
                            onValueChange(role)
                            query = role
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillLevelDropdown(
    value: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Level") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            singleLine = true,
            colors = textFieldColors()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { level ->
                DropdownMenuItem(
                    text = { Text(level.lowercase().replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onValueChange(level)
                        expanded = false
                    }
                )
            }
        }
    }
}
