package com.example.hibuddy

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.hibuddy.ui.theme.HiBuddyTheme
import com.example.hibuddy.ui.screens.DiscoverScreen
import com.example.hibuddy.ui.screens.MatchesScreen
import com.example.hibuddy.ui.screens.TasksScreen
import com.example.hibuddy.ui.screens.ProfileScreen
import com.example.hibuddy.ui.screens.FeedbackScreen
import com.example.hibuddy.ui.screens.NotificationScreen
import com.example.hibuddy.ui.screens.auth.LoginScreen
import com.example.hibuddy.ui.screens.auth.RegisterScreen
import com.example.hibuddy.ui.screens.auth.ForgotPasswordScreen
import com.example.hibuddy.ui.screens.chat.ChatScreen
import com.example.hibuddy.ui.screens.projects.CreateProjectScreen
import com.example.hibuddy.ui.screens.projects.ProjectDetailScreen
import com.example.hibuddy.ui.screens.SimpleCreateTaskScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkMode by ServiceLocator.themeManager.isDarkMode.collectAsState()
            HiBuddyTheme(darkTheme = isDarkMode) {
                HiBuddyApp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            if (!ServiceLocator.authRepository.isLoggedIn()) return@launch
            ServiceLocator.authRepository.refreshToken()
            ServiceLocator.presenceWebSocketManager.connect(ServiceLocator.authRepository.getAccessToken())
        }
    }

    override fun onStop() {
        ServiceLocator.presenceWebSocketManager.disconnect()
        super.onStop()
    }
}

object Routes {
    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"
    const val FORGOT = "auth/forgot"
    const val DISCOVER = "main/discover"
    const val MATCHES = "main/matches"
    const val TASKS = "main/tasks"
    const val PROFILE = "main/profile"
    const val CHAT = "main/chat/{matchId}/{userName}/{targetUserId}"
    fun chat(matchId: String, userName: String, targetUserId: String) =
        "main/chat/$matchId/${Uri.encode(userName)}/${Uri.encode(targetUserId)}"
    const val PROJECT_DETAIL = "main/project/{projectId}"
    fun projectDetail(projectId: String) = "main/project/$projectId"
    const val CREATE_PROJECT = "main/create-project"
    const val CREATE_TASK = "main/create-task/{projectId}"
    fun createTask(projectId: String) = "main/create-task/$projectId"
    const val NOTIFICATIONS = "main/notifications"
    const val FEEDBACK = "main/feedback/{projectId}"
    fun feedback(projectId: String) = "main/feedback/$projectId"
}

@Composable
fun HiBuddyApp() {
    val navController = rememberNavController()
    val isLoggedIn by ServiceLocator.authRepository.authState.collectAsState()
    val startDestination = remember {
        if (ServiceLocator.authRepository.isLoggedIn()) Routes.DISCOVER else Routes.LOGIN
    }

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            ServiceLocator.presenceWebSocketManager.disconnect()
            val currentRoute = navController.currentDestination?.route
            val authRoutes = setOf(Routes.LOGIN, Routes.REGISTER, Routes.FORGOT)
            if (currentRoute != null && currentRoute !in authRoutes) {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT) },
                onLoginSuccess = {
                    ServiceLocator.presenceWebSocketManager.connect(ServiceLocator.authRepository.getAccessToken())
                    navController.navigate(Routes.DISCOVER) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    ServiceLocator.presenceWebSocketManager.connect(ServiceLocator.authRepository.getAccessToken())
                    navController.navigate(Routes.DISCOVER) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.FORGOT) {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.DISCOVER) {
            MainScaffold(
                currentTab = "discover",
                onTabSelect = { tab ->
                    when (tab) {
                        "discover" -> navController.navigate(Routes.DISCOVER) { launchSingleTop = true }
                        "matches" -> navController.navigate(Routes.MATCHES) { launchSingleTop = true }
                        "tasks" -> navController.navigate(Routes.TASKS) { launchSingleTop = true }
                        "profile" -> navController.navigate(Routes.PROFILE) { launchSingleTop = true }
                        "notifications" -> navController.navigate(Routes.NOTIFICATIONS)
                    }
                }
            ) {
                DiscoverScreen(
                    onCreateProject = {
                        navController.navigate(Routes.CREATE_PROJECT)
                    }
                )
            }
        }
        composable(Routes.MATCHES) {
            MainScaffold(
                currentTab = "matches",
                onTabSelect = { tab ->
                    when (tab) {
                        "discover" -> navController.navigate(Routes.DISCOVER) { launchSingleTop = true }
                        "matches" -> navController.navigate(Routes.MATCHES) { launchSingleTop = true }
                        "tasks" -> navController.navigate(Routes.TASKS) { launchSingleTop = true }
                        "profile" -> navController.navigate(Routes.PROFILE) { launchSingleTop = true }
                        "notifications" -> navController.navigate(Routes.NOTIFICATIONS)
                    }
                }
            ) {
                MatchesScreen(
                    onChatClick = { matchId, userName, targetUserId ->
                        navController.navigate(Routes.chat(matchId, userName, targetUserId))
                    }
                )
            }
        }
        composable(Routes.TASKS) {
            MainScaffold(
                currentTab = "tasks",
                onTabSelect = { tab ->
                    when (tab) {
                        "discover" -> navController.navigate(Routes.DISCOVER) { launchSingleTop = true }
                        "matches" -> navController.navigate(Routes.MATCHES) { launchSingleTop = true }
                        "tasks" -> navController.navigate(Routes.TASKS) { launchSingleTop = true }
                        "profile" -> navController.navigate(Routes.PROFILE) { launchSingleTop = true }
                        "notifications" -> navController.navigate(Routes.NOTIFICATIONS)
                    }
                }
            ) {
                TasksScreen(
                    onCreateTask = { projectId ->
                        navController.navigate(Routes.createTask(projectId))
                    },
                    onOpenProject = { projectId ->
                        navController.navigate(Routes.projectDetail(projectId))
                    }
                )
            }
        }
        composable(Routes.PROFILE) {
            MainScaffold(
                currentTab = "profile",
                onTabSelect = { tab ->
                    when (tab) {
                        "discover" -> navController.navigate(Routes.DISCOVER) { launchSingleTop = true }
                        "matches" -> navController.navigate(Routes.MATCHES) { launchSingleTop = true }
                        "tasks" -> navController.navigate(Routes.TASKS) { launchSingleTop = true }
                        "profile" -> navController.navigate(Routes.PROFILE) { launchSingleTop = true }
                        "notifications" -> navController.navigate(Routes.NOTIFICATIONS)
                    }
                }
            ) {
                ProfileScreen(
                    onLogout = {
                        ServiceLocator.presenceWebSocketManager.disconnect()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(
            Routes.CHAT,
            arguments = listOf(
                navArgument("matchId") { type = NavType.StringType },
                navArgument("userName") { type = NavType.StringType },
                navArgument("targetUserId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
            val userName = Uri.decode(backStackEntry.arguments?.getString("userName") ?: "")
            val targetUserId = Uri.decode(backStackEntry.arguments?.getString("targetUserId") ?: "")
            ChatScreen(
                matchId = matchId,
                userName = userName,
                targetUserId = targetUserId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.PROJECT_DETAIL,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            ProjectDetailScreen(
                projectId = projectId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CREATE_PROJECT) {
            CreateProjectScreen(
                onBack = { navController.popBackStack() },
                onProjectCreated = { projectId ->
                    navController.navigate(Routes.projectDetail(projectId)) {
                        popUpTo(Routes.CREATE_PROJECT) { inclusive = true }
                    }
                }
            )
        }

        composable(
            Routes.CREATE_TASK,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            SimpleCreateTaskScreen(
                projectId = projectId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.NOTIFICATIONS) {
            NotificationScreen(
                onBack = { navController.popBackStack() },
                onNotificationClick = { type, relatedId ->
                    when (type) {
                        "PROJECT_COMPLETED_FEEDBACK", "FEEDBACK_RECEIVED" ->
                            navController.navigate(Routes.feedback(relatedId ?: ""))
                        "NEW_MATCH" ->
                            navController.navigate(Routes.MATCHES)
                        else -> Unit
                    }
                }
            )
        }

        composable(
            Routes.FEEDBACK,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            FeedbackScreen(
                projectId = projectId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun MainScaffold(
    currentTab: String,
    onTabSelect: (String) -> Unit,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Scaffold(
        bottomBar = {
            HiBuddyBottomNav(current = currentTab, onSelect = onTabSelect)
        },
        containerColor = colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}

@Composable
fun HiBuddyBottomNav(
    current: String,
    onSelect: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val items = listOf(
        Triple("discover", Icons.Filled.Explore, "Discover"),
        Triple("matches", Icons.Filled.Favorite, "Matches"),
        Triple("tasks", Icons.AutoMirrored.Filled.Assignment, "Tasks"),
        Triple("profile", Icons.Filled.Person, "Profile"),
        Triple("notifications", Icons.Filled.Notifications, "Notify"),
    )

    NavigationBar(
        containerColor = colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        items.forEach { (key, icon, label) ->
            val selected = current == key
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(key) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colorScheme.primary,
                    selectedTextColor = colorScheme.primary,
                    unselectedIconColor = colorScheme.onSurfaceVariant,
                    unselectedTextColor = colorScheme.onSurfaceVariant,
                    indicatorColor = colorScheme.primaryContainer
                )
            )
        }
    }
}
