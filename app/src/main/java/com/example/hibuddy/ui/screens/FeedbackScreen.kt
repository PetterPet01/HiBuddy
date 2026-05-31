package com.example.hibuddy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.data.remote.dto.FeedbackResponse
import com.example.hibuddy.data.remote.dto.MemberToFeedbackItem
import com.example.hibuddy.ui.screens.feedback.FeedbackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    projectId: String,
    onBack: () -> Unit,
    viewModel: FeedbackViewModel = viewModel(factory = FeedbackViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId) { viewModel.load(projectId) }

    LaunchedEffect(state.error, state.success) {
        if (state.error != null || state.success != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feedback - ${state.projectTitle}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleShowReceived() }) {
                        Icon(
                            if (state.showReceived) Icons.Filled.Edit else Icons.Filled.Inbox,
                            "Toggle"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.error != null) {
                Card(
                    Modifier.fillMaxWidth().padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2))
                ) {
                    Text(state.error!!, Modifier.padding(12.dp), color = Color(0xFFD32F2F))
                }
            }
            if (state.success != null) {
                Card(
                    Modifier.fillMaxWidth().padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9))
                ) {
                    Text(state.success!!, Modifier.padding(12.dp), color = Color(0xFF388E3C))
                }
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.showReceived) {
                ReceivedFeedbackTab(state)
            } else {
                GiveFeedbackTab(state, viewModel, projectId)
            }
        }
    }
}

@Composable
private fun GiveFeedbackTab(
    state: com.example.hibuddy.ui.screens.feedback.FeedbackUiState,
    viewModel: FeedbackViewModel,
    projectId: String
) {
    val selectedId = state.selectedMemberId
    if (selectedId != null) {
        WriteFeedbackPanel(state, viewModel, projectId)
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Text(
                    "Chọn thành viên để gửi feedback:",
                    Modifier.padding(12.dp),
                    fontWeight = FontWeight.Bold
                )
            }
            items(state.members) { member ->
                MemberCard(member, onClick = { viewModel.selectMember(member) })
            }
        }
    }
}

@Composable
private fun MemberCard(member: MemberToFeedbackItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (member.alreadyFeedback) Icons.Filled.CheckCircle else Icons.Filled.Person, null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(member.displayName, fontWeight = FontWeight.Bold)
                Text(member.role, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            if (member.alreadyFeedback) {
                Text("Đã gửi", color = Color(0xFF4CAF50), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun WriteFeedbackPanel(
    state: com.example.hibuddy.ui.screens.feedback.FeedbackUiState,
    viewModel: FeedbackViewModel,
    projectId: String
) {
    Column(Modifier.padding(12.dp).fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                viewModel.clearSelectedMember()
            }) {
                Icon(Icons.Filled.ArrowBack, "Back")
            }
            Text("Gửi feedback cho ${state.selectedMemberName}", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Feedback của bạn là ẩn danh. Người nhận sẽ không biết ai đã gửi.",
            color = Color.Gray, style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.feedbackText,
            onValueChange = { viewModel.updateFeedbackText(it) },
            label = { Text("Nhận xét của bạn") },
            modifier = Modifier.fillMaxWidth().weight(1f),
            minLines = 5
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { viewModel.submitFeedback(projectId) },
            enabled = !state.isSubmitting && state.feedbackText.trim().length >= 10,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSubmitting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
            else Text("Gửi feedback ẩn danh")
        }
    }
}

@Composable
private fun ReceivedFeedbackTab(state: com.example.hibuddy.ui.screens.feedback.FeedbackUiState) {
    val summary = state.receivedFeedbacks
    if (summary == null || summary.feedbacks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Chưa có feedback nào", color = Color.Gray)
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Card(
                    Modifier.fillMaxWidth().padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Tổng: ${summary.totalFeedbacks} feedback", fontWeight = FontWeight.Bold)
                        if (summary.weaknesses.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Điểm cần cải thiện:", fontWeight = FontWeight.SemiBold)
                            summary.weaknesses.forEach { w ->
                                Row(Modifier.padding(start = 8.dp, top = 4.dp)) {
                                    Icon(
                                        Icons.Filled.Warning, null, tint = Color(0xFFFFA000),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(w)
                                }
                            }
                        }
                    }
                }
            }
            items(summary.feedbacks) { fb ->
                FeedbackCard(fb)
            }
        }
    }
}

@Composable
private fun FeedbackCard(fb: FeedbackResponse) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(fb.feedbackText)
            if (!fb.analyzedWeaknesses.isNullOrEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Điểm yếu: ${fb.analyzedWeaknesses.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFA000)
                )
            }
            Text(
                fb.createdAt.take(16).replace("T", " "),
                style = MaterialTheme.typography.labelSmall, color = Color.Gray
            )
        }
    }
}
