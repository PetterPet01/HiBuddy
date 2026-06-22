package com.example.hibuddy.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications

/**
 * Circular badge overlay for a notifications icon.
 *
 * Shows a red dot when [unreadCount] is 0, a small "!" indicator for 1-9
 * unread items, and "99+" for large counts.
 */
@Composable
fun NotificationBadgeDot(
    unreadCount: Int,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val visible = unreadCount > 0
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "notification_badge_scale"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(if (unreadCount in 1..9) 14.dp else 16.dp)
                .clip(CircleShape)
                .background(colorScheme.error),
            contentAlignment = Alignment.Center
        ) {
            if (unreadCount > 9) {
                Text(
                    text = "99+",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            } else if (unreadCount == 1) {
                Text(
                    text = "!",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
    // Suppress unused scale warning - keeps state alive for transitions.
    @Suppress("UNUSED_EXPRESSION") scale
}

/**
 * Notifications bell icon with an unread-count badge.
 */
@Composable
fun NotificationIconWithBadge(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        IconButton(onClick = onClick) {
            BadgedBox(
                badge = {
                    if (unreadCount > 0) {
                        Badge {
                            Text(
                                text = if (unreadCount > 99) "99+" else "$unreadCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "Notifications"
                )
            }
        }
        // Red dot corner overlay for visual prominence even when count is 0 or 1
        NotificationBadgeDot(
            unreadCount = unreadCount,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(12.dp)
        )
    }
}
