package com.example.hibuddy.ui.screens.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.data.remote.dto.*
import com.example.hibuddy.ui.theme.HiBuddyColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    onBack: () -> Unit,
    viewModel: ProjectDetailViewModel = viewModel(
        key = "project_$projectId",
        factory = ProjectDetailViewModel.factory(projectId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Info", "Members", "Tasks", "Dashboard")
    val project = uiState.project
    val isOwner = project?.ownerId == uiState.currentUserId

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    uiState.project?.title ?: "Project",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = colorScheme.onSurface)
                }
            },
            actions = {
                if (isOwner == true && project?.status != "CLOSED") {
                    TextButton(
                        onClick = { viewModel.closeProject() },
                        enabled = !uiState.isActionLoading
                    ) {
                        Text("Close Project", color = HiBuddyColors.warning)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.surface)
        )

        if (uiState.isLoading && uiState.project == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colorScheme.primary)
            }
            return@Column
        }

        val activeProject = project ?: return@Column

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = colorScheme.surface,
            contentColor = colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 12.sp) }
                )
            }
        }

        when (selectedTab) {
            0 -> ProjectInfoTab(activeProject, isOwner = isOwner == true)
            1 -> MembersTab(
                members = activeProject.members,
                applicants = uiState.applicants,
                roleSlots = activeProject.roleSlots,
                isOwner = isOwner == true,
                isActionLoading = uiState.isActionLoading,
                onAddApplicant = { userId, roleName, roleSlotId ->
                    viewModel.addMember(userId, roleName, roleSlotId)
                }
            )
            2 -> TasksTab(uiState.tasks)
            3 -> DashboardTab(uiState.dashboard)
        }
    }

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    uiState.message?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(2500)
            viewModel.clearMessage()
        }
    }
}

@Composable
private fun ProjectInfoTab(project: ProjectResponse, isOwner: Boolean) {
    val colorScheme = MaterialTheme.colorScheme
    val statusColor = when (project.status) {
        "RECRUITING" -> HiBuddyColors.success
        "ACTIVE" -> colorScheme.primary
        "CLOSED" -> colorScheme.error
        else -> colorScheme.onSurfaceVariant
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(project.field, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                        Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.2f)) {
                            Text(project.status, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(project.title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Text(project.description, fontSize = 14.sp, color = colorScheme.onSurfaceVariant, lineHeight = 20.sp)

                    if (project.specificGoal != null) {
                        Spacer(Modifier.height(12.dp))
                        Text("Goal: ${project.specificGoal}", fontSize = 13.sp, color = colorScheme.onSurfaceVariant)
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row { Text(project.workMode, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface) }
                        Row { Text(project.commitmentLevel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface) }
                        Row { Text("${project.members.size}/${project.maxMembers}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface) }
                    }

                    if (!project.additionalRequirements.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text("Needs: ${project.additionalRequirements}", fontSize = 13.sp, color = colorScheme.onSurfaceVariant)
                    }
                    if (!project.memberBenefits.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Benefits: ${project.memberBenefits}", fontSize = 13.sp, color = colorScheme.onSurfaceVariant)
                    }
                    if (isOwner) {
                        Spacer(Modifier.height(12.dp))
                        Text("Owner tools are available in Members and Tasks.", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Text("Role Slots", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
        }

        project.roleSlots.forEach { slot ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(slot.roleName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                            Text("${slot.filled}/${slot.count} filled", fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
                        }
                        LinearProgressIndicator(
                            progress = { if (slot.count > 0) slot.filled.toFloat() / slot.count else 0f },
                            modifier = Modifier.width(80.dp).height(6.dp),
                            color = colorScheme.primary,
                            trackColor = colorScheme.outline.copy(alpha = 0.24f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MembersTab(
    members: List<MemberResponse>,
    applicants: List<ApplicantResponse>,
    roleSlots: List<RoleSlotResponse>,
    isOwner: Boolean,
    isActionLoading: Boolean,
    onAddApplicant: (userId: String, roleName: String, roleSlotId: String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val openSlots = roleSlots.filter { it.filled < it.count }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Current Members", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
        }

        items(members) { member ->
            val memberColor = remember(member.userId) {
                val colors = listOf(Color(0xFF5B4FCF), Color(0xFFE03055), Color(0xFF06B6D4), Color(0xFF7C6AF7), Color(0xFF059669), Color(0xFFFF8C42))
                colors[kotlin.math.abs(member.userId.hashCode()) % colors.size]
            }
            val memberTextColor = if (memberColor.luminance() > 0.5f) Color(0xFF15161F) else Color.White
            Card(
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(memberColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(member.displayName.firstOrNull()?.uppercase() ?: "?", fontSize = 18.sp, color = memberTextColor)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(member.displayName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                        Text(member.role, fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                    }
                    if (member.isOwner) {
                        Surface(shape = RoundedCornerShape(6.dp), color = HiBuddyColors.warningContainer) {
                            Text("Owner", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HiBuddyColors.onWarningContainer)
                        }
                    }
                }
            }
        }

        if (isOwner) {
            item {
                Spacer(Modifier.height(12.dp))
                Text("Pending Applicants", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
            }

            if (applicants.isEmpty()) {
                item {
                    Text("No applicants yet", fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
                }
            } else {
                items(applicants) { applicant ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(applicant.displayName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                            Text(
                                applicant.roles.joinToString { it.roleName } + " · " + applicant.skills.joinToString { it.skillName },
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                            if (openSlots.isEmpty()) {
                                Text("No open role slots remaining", fontSize = 12.sp, color = HiBuddyColors.warning)
                            } else {
                                openSlots.forEach { slot ->
                                    Button(
                                        onClick = { onAddApplicant(applicant.userId, slot.roleName, slot.id) },
                                        enabled = !isActionLoading,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colorScheme.primary,
                                            contentColor = colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Add as ${slot.roleName}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TasksTab(tasks: List<TaskResponse>) {
    val colorScheme = MaterialTheme.colorScheme
    if (tasks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No tasks yet", fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tasks) { task ->
            val statusColor = when (task.status) {
                "TODO" -> colorScheme.onSurfaceVariant
                "IN_PROGRESS" -> HiBuddyColors.warning
                "DONE_REVIEW" -> colorScheme.primary
                "CLOSED" -> HiBuddyColors.success
                else -> colorScheme.onSurfaceVariant
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(task.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.2f)) {
                            Text(task.status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(task.assigneeName ?: "Unassigned", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                        Text("Deadline: ${task.deadline.take(10)}", fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardTab(dashboard: DashboardResponse?) {
    val colorScheme = MaterialTheme.colorScheme
    if (dashboard == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading dashboard...", fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(dashboard.projectTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatBox("${dashboard.totalTasks}", "Total Tasks")
                        StatBox("${dashboard.totalMembers}", "Members")
                    }
                }
            }
        }

        if (dashboard.memberStats.isNotEmpty()) {
            item {
                Text("Member Performance", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant)
            }
            items(dashboard.memberStats) { stat ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(stat.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TaskStatChip("Todo", stat.todo, colorScheme.onSurfaceVariant)
                            TaskStatChip("Progress", stat.inProgress, HiBuddyColors.warning)
                            TaskStatChip("Early", stat.early, HiBuddyColors.success)
                            TaskStatChip("On Time", stat.onTime, colorScheme.primary)
                            TaskStatChip("Late", stat.late, colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(value: String, label: String) {
    val colorScheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Black, color = colorScheme.primary)
        Text(label, fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TaskStatChip(label: String, count: Int, color: Color) {
    val colorScheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 9.sp, color = colorScheme.onSurfaceVariant)
    }
}
