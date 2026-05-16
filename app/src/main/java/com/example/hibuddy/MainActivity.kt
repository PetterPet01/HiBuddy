package com.example.hibuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.hibuddy.ui.theme.HiBuddyTheme
import com.example.hibuddy.ui.screens.DiscoverScreen
import com.example.hibuddy.ui.screens.MatchesScreen
import com.example.hibuddy.ui.screens.TasksScreen
import com.example.hibuddy.ui.screens.ProfileScreen
import com.example.hibuddy.ui.screens.auth.LoginScreen
import com.example.hibuddy.ui.screens.auth.RegisterScreen
import com.example.hibuddy.ui.screens.auth.ForgotPasswordScreen
import com.example.hibuddy.ui.screens.chat.ChatScreen
import com.example.hibuddy.ui.screens.projects.CreateProjectScreen
import com.example.hibuddy.ui.screens.projects.ProjectDetailScreen
import com.example.hibuddy.ui.screens.SimpleCreateTaskScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isLoggedIn = ServiceLocator.authRepository.isLoggedIn()
        setContent {
            HiBuddyTheme {
                HiBuddyApp(isLoggedIn)
            }
        }
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
    const val CHAT = "main/chat/{matchId}/{userName}"
    fun chat(matchId: String, userName: String) = "main/chat/$matchId/$userName"
    const val PROJECT_DETAIL = "main/project/{projectId}"
    fun projectDetail(projectId: String) = "main/project/$projectId"
    const val CREATE_PROJECT = "main/create-project"
    const val CREATE_TASK = "main/create-task/{projectId}"
    fun createTask(projectId: String) = "main/create-task/$projectId"
}

@Composable
fun HiBuddyApp(isLoggedIn: Boolean) {
    val navController = rememberNavController()
    val startDestination = if (isLoggedIn) Routes.DISCOVER else Routes.LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT) },
                onLoginSuccess = {
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
                    }
                }
            ) {
                MatchesScreen(
                    onChatClick = { matchId, userName ->
                        navController.navigate(Routes.chat(matchId, userName))
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
                    }
                }
            ) {
                TasksScreen(
                    onCreateTask = { projectId ->
                        navController.navigate(Routes.createTask(projectId))
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
                    }
                }
            ) {
                ProfileScreen(
                    onLogout = {
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
                navArgument("userName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
            val userName = backStackEntry.arguments?.getString("userName") ?: ""
            ChatScreen(
                matchId = matchId,
                userName = userName,
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
    }
}

@Composable
fun MainScaffold(
    currentTab: String,
    onTabSelect: (String) -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            HiBuddyBottomNav(current = currentTab, onSelect = onTabSelect)
        },
        containerColor = Color(0xFF0D0D14)
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
    val items = listOf(
        Triple("discover", Icons.Filled.Explore, "Discover"),
        Triple("matches", Icons.Filled.Favorite, "Matches"),
        Triple("tasks", Icons.Filled.Assignment, "Tasks"),
        Triple("profile", Icons.Filled.Person, "Profile"),
    )

    NavigationBar(
        containerColor = Color(0xFF13131F),
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
                        contentDescription = label,
                        tint = if (selected) Color(0xFF7C6AF7) else Color(0xFF5A5A7A)
                    )
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
