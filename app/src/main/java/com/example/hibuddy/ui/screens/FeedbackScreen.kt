package com.example.hibuddy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.R
import com.example.hibuddy.data.remote.dto.FeedbackResponse
import com.example.hibuddy.data.remote.dto.MemberToFeedbackItem
import com.example.hibuddy.data.remote.dto.CourseSuggestionResponse
import com.example.hibuddy.ui.screens.feedback.FeedbackViewModel
import com.example.hibuddy.ui.theme.HiBuddyColors

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
                title = { Text(stringResource(R.string.feedback_screen_title, state.projectTitle)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleShowReceived() }) {
                        Icon(
                            if (state.showReceived) Icons.Filled.Edit else Icons.Filled.Inbox,
                            stringResource(R.string.feedback_toggle)
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
                ReceivedFeedbackTab(state, viewModel)
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
                    stringResource(R.string.feedback_select_member),
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
                Text(stringResource(R.string.feedback_sent), color = Color(0xFF4CAF50), style = MaterialTheme.typography.labelSmall)
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
                Icon(Icons.Filled.ArrowBack, stringResource(R.string.action_back))
            }
            Text(stringResource(R.string.feedback_give_to, state.selectedMemberName ?: ""), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.feedback_anonymous_notice),
            color = Color.Gray, style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.feedbackText,
            onValueChange = { viewModel.updateFeedbackText(it) },
            label = { Text(stringResource(R.string.feedback_comment_label)) },
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
            else Text(stringResource(R.string.feedback_submit_anonymous))
        }
    }
}

@Composable
private fun ReceivedFeedbackTab(
    state: com.example.hibuddy.ui.screens.feedback.FeedbackUiState,
    viewModel: FeedbackViewModel
) {
    val summary = state.receivedFeedbacks
    if (summary == null || summary.feedbacks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(stringResource(R.string.feedback_empty), color = Color.Gray)
                if (state.courseSuggestions.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    CourseSuggestionsSection(state.courseSuggestions, viewModel)
                }
            }
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Card(
                    Modifier.fillMaxWidth().padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.feedback_total, summary.totalFeedbacks), fontWeight = FontWeight.Bold)
                        if (summary.weaknesses.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.feedback_areas_to_improve), fontWeight = FontWeight.SemiBold)
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
            if (state.courseSuggestions.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        CourseSuggestionsSection(state.courseSuggestions, viewModel)
                        Spacer(Modifier.height(12.dp))
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
private fun CourseSuggestionsSection(
    suggestions: List<CourseSuggestionResponse>,
    viewModel: FeedbackViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    var showTooltip by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.feedback_course_suggestions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground
            )
            IconButton(onClick = { showTooltip = !showTooltip }) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = "Trigger conditions",
                    tint = colorScheme.primary
                )
            }
        }

        if (showTooltip) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "Suggestions are based on your weakness areas from feedbacks or unfinished/late tasks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        suggestions.forEach { course ->
            FeedbackCourseRecommendationCard(
                course = course,
                onDismiss = { viewModel.dismissCourse(course.id) },
                onAddBadge = {
                    viewModel.addCompletedCourse(course.courseTitle, course.source, course.courseId)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FeedbackCourseRecommendationCard(
    course: CourseSuggestionResponse,
    onDismiss: () -> Unit = {},
    onAddBadge: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    var isDismissed by remember { mutableStateOf(false) }

    if (!isDismissed) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.55f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(shape = RoundedCornerShape(6.dp), color = colorScheme.primaryContainer) {
                        Text(
                            text = "To improve: ${course.targetSkill}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            color = colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("${course.matchPercent.toInt()}% Match", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HiBuddyColors.success)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(course.courseTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                Text("Source: ${course.source}", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            isDismissed = true
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.surface),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Pass", tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pass", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = onAddBadge,
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Complete", tint = colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Badge", fontSize = 12.sp, color = colorScheme.onPrimary)
                    }
                }
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
                    stringResource(R.string.feedback_weaknesses, fb.analyzedWeaknesses.joinToString(", ")),
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
