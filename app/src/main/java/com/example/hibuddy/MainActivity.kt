package com.example.hibuddy

import com.example.hibuddy.ui.auth.LoginScreen
import com.example.hibuddy.ui.auth.RegisterScreen
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
import com.example.hibuddy.repository.AuthRepository
import com.example.hibuddy.data.model.UserProfile
import com.example.hibuddy.repository.UserRepository
import com.example.hibuddy.ui.auth.CompleteProfileScreen
import com.google.firebase.auth.FirebaseAuth

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

enum class AuthRoute {
    LOGIN, REGISTER, COMPLETE_PROFILE
}

@Composable
fun HiBuddyApp() {
    val currentUser = FirebaseAuth.getInstance().currentUser

    var isLoggedIn by remember {
        mutableStateOf(currentUser != null)
    }
    var authRoute by remember { mutableStateOf(AuthRoute.LOGIN) }
    val authRepository = remember { AuthRepository() }
    val userRepository = remember { UserRepository() }
    if (!isLoggedIn) {
        when (authRoute) {
            AuthRoute.LOGIN -> LoginScreen(
                onLoginClick = { email, password ->
                    authRepository.login(
                        email = email,
                        password = password,
                        onSuccess = {
                            isLoggedIn = true
                        },
                        onFailure = {
                            println(it)
                        }
                    )
                },
                onRegisterClick = {
                    authRoute = AuthRoute.REGISTER
                },
                onForgotPasswordClick = {}
            )

            AuthRoute.REGISTER -> RegisterScreen(

                onRegisterClick = { fullName, email, password ->
                    authRepository.register(
                        email = email,
                        password = password,
                        onSuccess = { uid ->
                            val userProfile = UserProfile(
                                uid = uid,
                                fullName = fullName,
                                email = email,
                                skills = emptyList(),
                                interests = emptyList(),
                                bio = ""
                            )

                            userRepository.saveUserProfile(
                                userProfile = userProfile,
                                onSuccess = {
                                    authRoute = AuthRoute.COMPLETE_PROFILE
                                },
                                onFailure = {
                                    println(it)
                                }
                            )
                        },
                        onFailure = {
                            println(it)
                        }
                    )
                },
                onLoginClick = {
                    authRoute = AuthRoute.LOGIN
                }
            )
            AuthRoute.COMPLETE_PROFILE -> {
                CompleteProfileScreen(
                    onComplete = {
                        isLoggedIn = true
                    }
                )
            }
        }
    } else {
        MainAppContent(
            onLogout = {
                FirebaseAuth.getInstance().signOut()
                isLoggedIn = false
                authRoute = AuthRoute.LOGIN
            }
        )
    }
}

@Composable
fun MainAppContent(
    onLogout: () -> Unit
) {
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
                    Screen.MATCHES -> MatchesScreen()
                    Screen.TASKS -> TasksScreen()
                    Screen.PROFILE -> ProfileScreen(
                        onLogout = onLogout
                    )
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
