package com.example.hibuddy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.data.remote.dto.*
import com.example.hibuddy.ui.screens.matches.MatchesViewModel

@Composable
fun MatchesScreen(
    onChatClick: (matchId: String, userName: String) -> Unit = { _, _ -> },
    viewModel: MatchesViewModel = viewModel(factory = MatchesViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadMatches()
        viewModel.loadInbox()
    }

    val matches = uiState.matches
    val inbox = uiState.chatInbox
    val newMatches = matches.filter { it.lastMessage == null }
    val existingChats = inbox

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D14))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Matches & Messages",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFF0EFF8)
            )
        }

        if (uiState.isLoading && matches.isEmpty() && existingChats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF7C6AF7))
            }
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (newMatches.isNotEmpty()) {
                item {
                    Text(
                        text = "New Matches (${newMatches.size})",
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B8AAC)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(newMatches) { match ->
                            MatchQueueItem(match = match, onClick = { onChatClick(match.id, match.userName ?: "") })
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Divider(color = Color(0xFF1E1D2E), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Conversations (${existingChats.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B8AAC)
                    )
                }
            }

            if (existingChats.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No conversations yet. Start swiping!", fontSize = 14.sp, color = Color(0xFF6B6A8C))
                    }
                }
            } else {
                items(existingChats) { chat ->
                    ConversationRowItem(chat = chat, onClick = { onChatClick(chat.matchId, chat.userName) })
                }
            }
        }
    }
}

@Composable
fun MatchQueueItem(match: com.example.hibuddy.data.remote.dto.MatchResponse, onClick: () -> Unit) {
    val avatarColor = remember(match.id) {
        val colors = listOf(Color(0xFF5B4FCF), Color(0xFFE03055), Color(0xFF06B6D4), Color(0xFF7C6AF7), Color(0xFF059669), Color(0xFFFF8C42))
        colors[kotlin.math.abs(match.id.hashCode()) % colors.size]
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp).clickable { onClick() }
    ) {
        Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { 1f }, color = Color(0xFF2A2840), modifier = Modifier.fillMaxSize(), strokeWidth = 2.dp)
            CircularProgressIndicator(progress = { 0.5f }, color = Color(0xFFFFD166), modifier = Modifier.fillMaxSize(), strokeWidth = 2.dp, strokeCap = StrokeCap.Round)
            Box(modifier = Modifier.size(62.dp).clip(CircleShape).background(avatarColor.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Text(text = (match.userName ?: "?").firstOrNull()?.uppercase() ?: "?", fontSize = 28.sp, color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = match.userName?.split(" ")?.lastOrNull() ?: match.userName ?: "User",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFF0EFF8),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ConversationRowItem(chat: ChatInboxResponse, onClick: () -> Unit) {
    val avatarColor = remember(chat.id) {
        val colors = listOf(Color(0xFF5B4FCF), Color(0xFFE03055), Color(0xFF06B6D4), Color(0xFF7C6AF7), Color(0xFF059669), Color(0xFFFF8C42))
        colors[kotlin.math.abs(chat.id.hashCode()) % colors.size]
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(avatarColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = chat.userName.firstOrNull()?.uppercase() ?: "?", fontSize = 24.sp, color = Color.White)
                }
                if (chat.isUnread) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).offset((-2).dp, 2.dp),
                        shape = CircleShape,
                        color = Color(0xFF7C6AF7),
                        border = BorderStroke(2.dp, Color(0xFF0D0D14))
                    ) {
                        Box(modifier = Modifier.size(10.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.userName,
                        fontSize = 16.sp,
                        fontWeight = if (chat.isUnread) FontWeight.Bold else FontWeight.SemiBold,
                        color = Color(0xFFF0EFF8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (chat.lastMessageTime != null) {
                        Text(
                            text = formatRelativeTime(chat.lastMessageTime),
                            fontSize = 11.sp,
                            color = Color(0xFF6B6A8C)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.lastMessage ?: "No messages yet",
                        fontSize = 14.sp,
                        color = if (chat.isUnread) Color(0xFFD0CFF0) else Color(0xFF8B8AAC),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (chat.isUnread && chat.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF7C6AF7)) {
                            Text(
                                "${chat.unreadCount}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatRelativeTime(isoTime: String): String {
    return try {
        val now = System.currentTimeMillis()
        val msgTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).parse(isoTime.take(19))?.time ?: return isoTime
        val diff = now - msgTime
        when {
            diff < 60_000 -> "now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            else -> "${diff / 86_400_000}d ago"
        }
    } catch (e: Exception) {
        isoTime.take(10)
    }
}
