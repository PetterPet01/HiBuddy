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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.data.remote.dto.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    onBack: () -> Unit,
    onChatClick: (matchId: String, userName: String) -> Unit = { _, _ -> },
    viewModel: ProjectDetailViewModel = viewModel(
        key = "project_$projectId",
        factory = ProjectDetailViewModel.factory(projectId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Info", "Members", "Tasks", "Dashboard")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D14))
    ) {
        TopAppBar(
            title = {
                Text(
                    uiState.project?.title ?: "Project",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF0EFF8)
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = Color(0xFFF0EFF8))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF13131F))
        )

        if (uiState.isLoading && uiState.project == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF7C6AF7))
            }
            return@Column
        }

        val project = uiState.project ?: return@Column

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF13131F),
            contentColor = Color(0xFF7C6AF7)
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
            0 -> ProjectInfoTab(project)
            1 -> MembersTab(project.members)
            2 -> TasksTab(uiState.tasks)
            3 -> DashboardTab(uiState.dashboard)
        }
    }
}

@Composable
private fun ProjectInfoTab(project: ProjectResponse) {
    val statusColor = when (project.status) {
        "RECRUITING" -> Color(0xFF4CAF50)
        "ACTIVE" -> Color(0xFF7C6AF7)
        "CLOSED" -> Color(0xFFFF4D6D)
        else -> Color(0xFF8B8AAC)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13131F)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(project.field, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C6AF7))
                        Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.2f)) {
                            Text(project.status, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(project.title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFFF0EFF8))
                    Spacer(Modifier.height(8.dp))
                    Text(project.description, fontSize = 14.sp, color = Color(0xFFB0AFC8), lineHeight = 20.sp)

                    if (project.specificGoal != null) {
                        Spacer(Modifier.height(12.dp))
                        Text("Goal: ${project.specificGoal}", fontSize = 13.sp, color = Color(0xFF8B8AAC))
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row { Text(project.workMode, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF0EFF8)) }
                        Row { Text(project.commitmentLevel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF0EFF8)) }
                        Row { Text("${project.members.size}/${project.maxMembers}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF0EFF8)) }
                    }
                }
            }
        }

        item {
            Text("Role Slots", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B8AAC))
        }

        project.roleSlots.forEach { slot ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1D2E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(slot.roleName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF0EFF8))
                            Text("${slot.filled}/${slot.count} filled", fontSize = 11.sp, color = Color(0xFF8B8AAC))
                        }
                        LinearProgressIndicator(
                            progress = { if (slot.count > 0) slot.filled.toFloat() / slot.count else 0f },
                            modifier = Modifier.width(80.dp).height(6.dp),
                            color = Color(0xFF7C6AF7),
                            trackColor = Color(0xFF2A2840),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MembersTab(members: List<MemberResponse>) {
    if (members.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No members yet", fontSize = 14.sp, color = Color(0xFF6B6A8C))
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(members) { member ->
            val memberColor = remember(member.userId) {
                val colors = listOf(Color(0xFF5B4FCF), Color(0xFFE03055), Color(0xFF06B6D4), Color(0xFF7C6AF7), Color(0xFF059669), Color(0xFFFF8C42))
                colors[kotlin.math.abs(member.userId.hashCode()) % colors.size]
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1D2E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(memberColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(member.displayName.firstOrNull()?.uppercase() ?: "?", fontSize = 18.sp, color = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(member.displayName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF0EFF8))
                        Text(member.role, fontSize = 12.sp, color = Color(0xFF8B8AAC))
                    }
                    if (member.isOwner) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFFD166).copy(alpha = 0.2f)) {
                            Text("Owner", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD166))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TasksTab(tasks: List<TaskResponse>) {
    if (tasks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No tasks yet", fontSize = 14.sp, color = Color(0xFF6B6A8C))
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tasks) { task ->
            val statusColor = when (task.status) {
                "TODO" -> Color(0xFF6B6A8C)
                "IN_PROGRESS" -> Color(0xFFFFD166)
                "DONE_REVIEW" -> Color(0xFF7C6AF7)
                "CLOSED" -> Color(0xFF4CAF50)
                else -> Color(0xFF8B8AAC)
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1D2E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(task.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF0EFF8), modifier = Modifier.weight(1f))
                        Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.2f)) {
                            Text(task.status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(task.assigneeName ?: "Unassigned", fontSize = 12.sp, color = Color(0xFF8B8AAC))
                        Text("Deadline: ${task.deadline.take(10)}", fontSize = 11.sp, color = Color(0xFF6B6A8C))
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardTab(dashboard: DashboardResponse?) {
    if (dashboard == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading dashboard...", fontSize = 14.sp, color = Color(0xFF6B6A8C))
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13131F)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(dashboard.projectTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF0EFF8))
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
                Text("Member Performance", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B8AAC))
            }
            items(dashboard.memberStats) { stat ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1D2E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(stat.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF0EFF8))
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TaskStatChip("Todo", stat.todo, Color(0xFF6B6A8C))
                            TaskStatChip("Progress", stat.inProgress, Color(0xFFFFD166))
                            TaskStatChip("Early", stat.early, Color(0xFF4CAF50))
                            TaskStatChip("On Time", stat.onTime, Color(0xFF7C6AF7))
                            TaskStatChip("Late", stat.late, Color(0xFFFF4D6D))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF7C6AF7))
        Text(label, fontSize = 11.sp, color = Color(0xFF6B6A8C))
    }
}

@Composable
private fun TaskStatChip(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 9.sp, color = Color(0xFF6B6A8C))
    }
}
