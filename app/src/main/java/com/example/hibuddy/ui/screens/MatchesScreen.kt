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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.data.remote.dto.*
import com.example.hibuddy.ui.screens.matches.MatchesViewModel
import com.example.hibuddy.ui.theme.HiBuddyColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

private const val NEW_MATCH_LIFETIME_MILLIS = 24L * 60L * 60L * 1000L

@Composable
fun MatchesScreen(
    onChatClick: (matchId: String, userName: String, targetUserId: String, avatar: String?) -> Unit = { _, _, _, _ -> },
    viewModel: MatchesViewModel = viewModel(factory = MatchesViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        viewModel.loadMatches()
        viewModel.loadInbox()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowMillis = System.currentTimeMillis()
        }
    }

    val matches = uiState.matches
    val inbox = uiState.chatInbox
    val newMatches = matches.filter {
        it.lastMessage == null && newMatchRemainingMillis(it.matchedAt, nowMillis) > 0L
    }
    val existingChats = inbox

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
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
                color = colorScheme.onBackground
            )
        }

        if (uiState.isLoading && matches.isEmpty() && existingChats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colorScheme.primary)
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
                        color = colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(newMatches) { match ->
                            MatchQueueItem(
                                match = match,
                                nowMillis = nowMillis,
                                onClick = {
                                    onChatClick(
                                        match.id,
                                        match.userName ?: "",
                                        match.otherUserId ?: "",
                                        match.userAvatar
                                    )
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.45f), thickness = 1.dp)
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
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            if (existingChats.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No conversations yet. Start swiping!", fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(existingChats) { chat ->
                    ConversationRowItem(
                        chat = chat,
                        onClick = {
                            onChatClick(
                                chat.matchId,
                                chat.userName,
                                chat.userId,
                                chat.userAvatar
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MatchQueueItem(
    match: com.example.hibuddy.data.remote.dto.MatchResponse,
    nowMillis: Long,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val avatarColor = remember(match.id) {
        val colors = listOf(Color(0xFF5B4FCF), Color(0xFFE03055), Color(0xFF06B6D4), Color(0xFF7C6AF7), Color(0xFF059669), Color(0xFFFF8C42))
        colors[kotlin.math.abs(match.id.hashCode()) % colors.size]
    }
    val avatarTextColor = if (avatarColor.luminance() > 0.5f) Color(0xFF15161F) else Color.White
    val remainingMillis = newMatchRemainingMillis(match.matchedAt, nowMillis)
    val progress = (remainingMillis.toFloat() / NEW_MATCH_LIFETIME_MILLIS.toFloat()).coerceIn(0f, 1f)
    val countdownLabel = formatMatchCountdown(remainingMillis)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(76.dp).clickable { onClick() }
    ) {
        Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { 1f }, color = colorScheme.outline.copy(alpha = 0.32f), modifier = Modifier.fillMaxSize(), strokeWidth = 2.dp)
            CircularProgressIndicator(progress = { progress }, color = HiBuddyColors.warning, modifier = Modifier.fillMaxSize(), strokeWidth = 2.dp, strokeCap = StrokeCap.Round)
            Box(modifier = Modifier.size(62.dp).clip(CircleShape).background(avatarColor), contentAlignment = Alignment.Center) {
                if (!match.userAvatar.isNullOrBlank()) {
                    AsyncImage(
                        model = match.userAvatar,
                        contentDescription = "${match.userName} avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(text = (match.userName ?: "?").firstOrNull()?.uppercase() ?: "?", fontSize = 28.sp, color = avatarTextColor)
                }
            }
            PresenceBadge(
                isOnline = match.userIsOnline,
                modifier = Modifier.align(Alignment.BottomEnd).offset((-4).dp, (-4).dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = match.userName?.split(" ")?.lastOrNull() ?: match.userName ?: "User",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = countdownLabel,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = HiBuddyColors.warning,
            maxLines = 1
        )
    }
}

@Composable
fun ConversationRowItem(chat: ChatInboxResponse, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val avatarColor = remember(chat.id) {
        val colors = listOf(Color(0xFF5B4FCF), Color(0xFFE03055), Color(0xFF06B6D4), Color(0xFF7C6AF7), Color(0xFF059669), Color(0xFFFF8C42))
        colors[kotlin.math.abs(chat.id.hashCode()) % colors.size]
    }
    val avatarTextColor = if (avatarColor.luminance() > 0.5f) Color(0xFF15161F) else Color.White
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
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (!chat.userAvatar.isNullOrBlank()) {
                        AsyncImage(
                            model = chat.userAvatar,
                            contentDescription = "${chat.userName} avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(text = chat.userName.firstOrNull()?.uppercase() ?: "?", fontSize = 24.sp, color = avatarTextColor)
                    }
                }
                PresenceBadge(
                    isOnline = chat.userIsOnline,
                    modifier = Modifier.align(Alignment.BottomEnd).offset((-2).dp, (-2).dp)
                )
                if (chat.isUnread) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).offset((-2).dp, 2.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.background)
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
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (chat.userIsOnline) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Online",
                            fontSize = 11.sp,
                            color = HiBuddyColors.success,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (chat.lastMessageTime != null) {
                        Text(
                            text = formatRelativeTime(chat.lastMessageTime),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        color = if (chat.isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (chat.isUnread && chat.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primary) {
                            Text(
                                "${chat.unreadCount}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresenceBadge(isOnline: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = if (isOnline) HiBuddyColors.success else MaterialTheme.colorScheme.outline,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.background)
    ) {
        Box(modifier = Modifier.size(11.dp))
    }
}

private fun formatRelativeTime(isoTime: String): String {
    return try {
        val now = System.currentTimeMillis()
        val msgTime = parseIsoTimeMillis(isoTime) ?: return isoTime
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

private fun newMatchRemainingMillis(matchedAt: String, nowMillis: Long): Long {
    val matchedAtMillis = parseIsoTimeMillis(matchedAt) ?: return NEW_MATCH_LIFETIME_MILLIS
    val expiresAtMillis = matchedAtMillis + NEW_MATCH_LIFETIME_MILLIS
    return (expiresAtMillis - nowMillis).coerceAtLeast(0L)
}

private fun formatMatchCountdown(remainingMillis: Long): String {
    val minutes = (remainingMillis / 60_000L).coerceAtLeast(0L)
    val hours = minutes / 60L
    return when {
        remainingMillis <= 0L -> "Expired"
        hours >= 1L -> "${hours}h"
        minutes >= 1L -> "${minutes}m"
        else -> "<1m"
    }
}

private fun parseIsoTimeMillis(isoTime: String): Long? {
    val normalized = normalizeIsoTime(isoTime)
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss"
    )

    return patterns.firstNotNullOfOrNull { pattern ->
        runCatching {
            SimpleDateFormat(pattern, Locale.US).parse(normalized)?.time
        }.getOrNull()
    }
}

private fun normalizeIsoTime(isoTime: String): String {
    var value = isoTime.trim().replace(Regex("Z$"), "+00:00")
    val fractionalSeconds = Regex("\\.(\\d+)(?=([+-]\\d{2}:?\\d{2})?$)").find(value)
    if (fractionalSeconds != null) {
        val fraction = fractionalSeconds.groupValues[1]
        val millis = fraction.take(3).padEnd(3, '0')
        value = value.replaceRange(fractionalSeconds.range, ".$millis")
    }
    return value
}
