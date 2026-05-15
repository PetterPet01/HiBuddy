package com.example.hibuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TasksScreen() {
    var selectedProject by remember { mutableStateOf(SampleData.myProjects.first()) }

    // Filter tasks based on selected project title
    val projectTasks = SampleData.tasks.filter { it.projectTitle == selectedProject.title }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D14))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Project Tasks",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFF0EFF8)
                )
                Spacer(Modifier.height(8.dp))
                ProjectBlobsSelector(
                    projects = SampleData.myProjects,
                    selected = selectedProject,
                    onSelect = { selectedProject = it },
                    label = "Viewing:"
                )
            }
            IconButton(
                onClick = { /* Open Create Task Modal */ },
                modifier = Modifier.background(Color(0xFF1E1D2E), RoundedCornerShape(12.dp)).size(48.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Task", tint = Color.White)
            }
        }

        // Kanban Board (Horizontal Scroll)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TaskStatus.values().forEach { status ->
                KanbanColumn(
                    status = status,
                    tasks = projectTasks.filter { it.status == status }
                )
            }
        }
    }
}

@Composable
fun KanbanColumn(status: TaskStatus, tasks: List<TaskItem>) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(Color(0xFF13131F), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        // Column Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = status.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B8AAC)
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1D2E)
            ) {
                Text(
                    text = tasks.size.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 12.sp,
                    color = Color(0xFFF0EFF8)
                )
            }
        }

        // Tasks List (Vertical Scroll inside column)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tasks.forEach { task ->
                TaskCardItem(task)
            }
        }
    }
}

@Composable
fun TaskCardItem(task: TaskItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E1D2E),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Tag & Priority
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF2A2840)
                ) {
                    Text(
                        text = task.tag,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        color = Color(0xFFB0AFC8)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(task.priority.color, CircleShape))
                    Text(text = task.priority.label, fontSize = 10.sp, color = task.priority.color)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = task.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF0EFF8),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Row: Assignee & Deadline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(20.dp).background(Color(0xFF7C6AF7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = task.assignee.first().toString(), fontSize = 10.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = task.assignee, fontSize = 11.sp, color = Color(0xFF8B8AAC))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⏱ ${task.deadline}", fontSize = 11.sp, color = Color(0xFF8B8AAC))
                }
            }
        }
    }
}