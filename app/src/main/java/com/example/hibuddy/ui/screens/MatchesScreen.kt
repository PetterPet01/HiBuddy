package com.example.hibuddy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

@Composable
fun MatchesScreen() {
    val newMatches = SampleData.matches.filter { it.isNewMatch }
    val conversations = SampleData.matches.filter { !it.isNewMatch }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D14))
    ) {
        // Main Screen Header
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

        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // ── Match Queue (Horizontal Row) ─────────────────────────
            if (newMatches.isNotEmpty()) {
                item {
                    Text(
                        text = "Match Queue (${newMatches.size})",
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
                            MatchQueueItem(match = match)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Divider(color = Color(0xFF1E1D2E), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // ── Conversations List ──────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Conversations (Recent)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B8AAC)
                    )
                }
            }

            items(conversations) { match ->
                ConversationRowItem(match = match)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
//  Match Queue Item (Horizontal scroll)
// ──────────────────────────────────────────────────────────────
@Composable
fun MatchQueueItem(match: MatchItem) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp).clickable { /* Open Chat/Profile */ }
    ) {
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background Track
            CircularProgressIndicator(
                progress = { 1f },
                color = Color(0xFF2A2840),
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 2.dp
            )
            // Yellow Progress Ring representing hours left (max 72)
            CircularProgressIndicator(
                progress = { match.hoursLeft / 72f },
                color = Color(0xFFFFD166),
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 2.dp,
                strokeCap = StrokeCap.Round
            )

            // Avatar Frame
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(match.avatarColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = match.avatarEmoji, fontSize = 32.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = match.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFF0EFF8),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ──────────────────────────────────────────────────────────────
//  Conversation Row Item (Vertical scroll)
// ──────────────────────────────────────────────────────────────
@Composable
fun ConversationRowItem(match: MatchItem) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { /* Open Chat */ },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Area (Conditional Styling)
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!match.isEstablished) {
                    // Show yellow progress ring if relationship is not established
                    CircularProgressIndicator(
                        progress = { 1f },
                        color = Color(0xFF2A2840),
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 2.dp
                    )
                    CircularProgressIndicator(
                        progress = { match.hoursLeft / 72f },
                        color = Color(0xFFFFD166),
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 2.dp,
                        strokeCap = StrokeCap.Round
                    )
                }

                // Avatar Frame inside
                val innerAvatarSize = if (!match.isEstablished) 54.dp else 60.dp
                Box(
                    modifier = Modifier
                        .size(innerAvatarSize)
                        .clip(CircleShape)
                        .background(match.avatarColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = match.avatarEmoji, fontSize = if (!match.isEstablished) 26.sp else 30.sp)
                }

                // Yellow badge dot indicating un-established state
                if (!match.isEstablished) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).offset(2.dp, (-2).dp),
                        shape = CircleShape,
                        color = Color(0xFFFFD166),
                        border = BorderStroke(2.dp, Color(0xFF0D0D14))
                    ) {
                        Box(modifier = Modifier.size(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                // Name & Tags row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = match.name,
                        fontSize = 16.sp,
                        fontWeight = if (match.isUnread) FontWeight.Bold else FontWeight.SemiBold,
                        color = Color(0xFFF0EFF8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Tags: "YOUR MOVE" vs "timeAgo"
                    if (match.isYourMove) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFD166)
                        ) {
                            Text(
                                text = "YOUR MOVE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4A3800)
                            )
                        }
                    } else if (match.isEstablished) {
                        Text(
                            text = match.timeAgo,
                            fontSize = 11.sp,
                            color = if (match.isUnread) Color(0xFF7C6AF7) else Color(0xFF6B6A8C)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Message snippet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = match.lastMessage,
                        fontSize = 14.sp,
                        color = if (match.isUnread) Color(0xFFD0CFF0) else Color(0xFF8B8AAC),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    // Normal Unread red dot
                    if (match.isUnread && match.isEstablished) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFFF4D6D), CircleShape)
                        )
                    }
                }

                // Expiration timer string if not established
                if (!match.isEstablished && match.hoursLeft > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Conversation expires in ${match.hoursLeft} hours",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFFD166)
                    )
                }
            }
        }
    }
}
