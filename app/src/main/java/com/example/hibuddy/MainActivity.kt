package com.example.hibuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.example.hibuddy.ui.theme.HiBuddyTheme
import com.example.hibuddy.ui.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HiBuddyTheme {
                HiBuddyApp()
            }
        }
    }
}

enum class Screen { DISCOVER, MATCHES, TASKS, PROFILE }

@Composable
fun HiBuddyApp() {
    var currentScreen by remember { mutableStateOf(Screen.DISCOVER) }
    var notificationCount by remember { mutableStateOf(3) }

    Scaffold(
        bottomBar = {
            HiBuddyBottomNav(
                current = currentScreen,
                notificationCount = notificationCount,
                onSelect = { currentScreen = it }
            )
        },
        containerColor = Color(0xFF0D0D14)
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    Screen.DISCOVER -> DiscoverScreen()
                    Screen.MATCHES  -> MatchesScreen()
                    Screen.TASKS    -> TasksScreen()
                    Screen.PROFILE  -> ProfileScreen()
                }
            }
        }
    }
}

@Composable
fun HiBuddyBottomNav(
    current: Screen,
    notificationCount: Int,
    onSelect: (Screen) -> Unit
) {
    val items = listOf(
        Triple(Screen.DISCOVER, Icons.Filled.Explore,      "Discover"),
        Triple(Screen.MATCHES,  Icons.Filled.Favorite,     "Matches"),
        Triple(Screen.TASKS,    Icons.Filled.Assignment,   "Tasks"),
        Triple(Screen.PROFILE,  Icons.Filled.Person,       "Profile"),
    )

    NavigationBar(
        containerColor = Color(0xFF13131F),
        tonalElevation = 0.dp
    ) {
        items.forEach { (screen, icon, label) ->
            val selected = current == screen
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(screen) },
                icon = {
                    Box {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (selected) Color(0xFF7C6AF7) else Color(0xFF5A5A7A)
                        )
                        if (screen == Screen.MATCHES && notificationCount > 0) {
                            Badge(
                                containerColor = Color(0xFFFF4D6D),
                                modifier = Modifier.align(Alignment.TopEnd).offset(6.dp, (-4).dp)
                            ) {
                                Text(
                                    text = notificationCount.toString(),
                                    fontSize = 9.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = if (selected) Color(0xFF7C6AF7) else Color(0xFF5A5A7A)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFF7C6AF7).copy(alpha = 0.15f)
                )
            )
        }
    }
}
