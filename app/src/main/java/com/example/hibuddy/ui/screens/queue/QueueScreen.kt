package com.example.hibuddy.ui.screens.queue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.data.remote.dto.ProjectCardResponse
import com.example.hibuddy.data.remote.dto.QueueItemResponse
import com.example.hibuddy.data.remote.dto.UserCardResponse
import com.example.hibuddy.ui.screens.MatchDialog
import com.example.hibuddy.ui.theme.HiBuddyColors

@Composable
fun QueueScreen(
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenProject: (String) -> Unit,
    viewModel: QueueViewModel = viewModel(factory = QueueViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        viewModel.loadQueue()
    }
    LaunchedEffect(uiState.error, uiState.message) {
        val message = uiState.error ?: uiState.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Queue", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colorScheme.onBackground)
                    Text(
                        "Users ${uiState.userProfiles.size}/3 - Projects ${uiState.projectProfiles.size}/3",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("User Profiles (${uiState.userCapacityRemaining} open)") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Project Profiles (${uiState.projectCapacityRemaining} open)") }
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val items = if (selectedTab == 0) uiState.userProfiles else uiState.projectProfiles
                if (items.isEmpty()) {
                    QueueEmptyState(if (selectedTab == 0) "No queued user profiles" else "No queued project profiles")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items, key = { it.id }) { item ->
                            if (selectedTab == 0) {
                                item.userCard?.let { card ->
                                    UserQueueRow(
                                        item = item,
                                        card = card,
                                        enabled = !uiState.isActionLoading,
                                        onOpen = { onOpenUser(card.userId) },
                                        onAccept = { viewModel.decide(item.id, "LIKE") },
                                        onReject = { viewModel.decide(item.id, "PASS") },
                                        onRemove = { viewModel.remove(item.id) }
                                    )
                                }
                            } else {
                                item.projectCard?.let { card ->
                                    ProjectQueueRow(
                                        item = item,
                                        card = card,
                                        enabled = !uiState.isActionLoading,
                                        onOpen = { onOpenProject(card.projectId) },
                                        onAccept = { viewModel.decide(item.id, "LIKE") },
                                        onReject = { viewModel.decide(item.id, "PASS") },
                                        onRemove = { viewModel.remove(item.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (uiState.matchedProjectId != null) {
        MatchDialog(onDismiss = { viewModel.clearMatch() })
    }
}

@Composable
private fun UserQueueRow(
    item: QueueItemResponse,
    card: UserCardResponse,
    enabled: Boolean,
    onOpen: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onRemove: () -> Unit
) {
    val roles = card.roles.take(2).joinToString(" - ") { it.roleName }
    QueueRowFrame(
        title = card.displayName,
        subtitle = roles.ifBlank { card.university.orEmpty() },
        body = card.bio.orEmpty(),
        expires = formatRemaining(item.secondsRemaining),
        accentKey = card.userId,
        thumbnailText = card.displayName.firstOrNull()?.uppercase() ?: "?",
        enabled = enabled,
        onOpen = onOpen,
        onAccept = onAccept,
        onReject = onReject,
        onRemove = onRemove
    )
}

@Composable
private fun ProjectQueueRow(
    item: QueueItemResponse,
    card: ProjectCardResponse,
    enabled: Boolean,
    onOpen: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onRemove: () -> Unit
) {
    QueueRowFrame(
        title = card.title,
        subtitle = card.field,
        body = card.description,
        expires = formatRemaining(item.secondsRemaining),
        accentKey = card.projectId,
        thumbnailText = card.title.firstOrNull()?.uppercase() ?: "?",
        enabled = enabled,
        onOpen = onOpen,
        onAccept = onAccept,
        onReject = onReject,
        onRemove = onRemove,
        thumbnailIcon = true
    )
}

@Composable
private fun QueueRowFrame(
    title: String,
    subtitle: String,
    body: String,
    expires: String,
    accentKey: String,
    thumbnailText: String,
    enabled: Boolean,
    onOpen: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onRemove: () -> Unit,
    thumbnailIcon: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val accentColor = remember(accentKey) {
        val colors = listOf(Color(0xFF5B4FCF), Color(0xFFE03055), Color(0xFF06B6D4), Color(0xFF059669), Color(0xFFFF8C42))
        colors[kotlin.math.abs(accentKey.hashCode()) % colors.size]
    }
    val textColor = if (accentColor.luminance() > 0.5f) Color(0xFF15161F) else Color.White

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onOpen),
        shape = RoundedCornerShape(8.dp),
        color = colorScheme.surface,
        border = BorderStroke(1.dp, colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailIcon) {
                    Icon(Icons.Filled.Work, contentDescription = null, tint = textColor)
                } else {
                    Text(thumbnailText, color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, fontSize = 12.sp, color = colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (body.isNotBlank()) {
                    Text(body, fontSize = 12.sp, color = colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(4.dp))
                Text("Expires in $expires", fontSize = 11.sp, color = HiBuddyColors.warning, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onAccept, enabled = enabled, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Accept", tint = HiBuddyColors.success)
                }
                IconButton(onClick = onReject, enabled = enabled, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Reject", tint = colorScheme.error)
                }
                IconButton(onClick = onRemove, enabled = enabled, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove", tint = colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun QueueEmptyState(text: String) {
    val colorScheme = MaterialTheme.colorScheme
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .border(1.dp, colorScheme.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Work, contentDescription = null, tint = colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Text(text, color = colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
    }
}

private fun formatRemaining(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val hours = safeSeconds / 3600
    val minutes = (safeSeconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "less than 1m"
    }
}
