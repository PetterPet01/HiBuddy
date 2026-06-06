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
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import com.example.hibuddy.ui.screens.auth.VerifyEmailScreen
import com.example.hibuddy.ui.screens.chat.ChatScreen
import com.example.hibuddy.ui.screens.projects.CreateProjectScreen
import com.example.hibuddy.ui.screens.projects.ProjectDetailScreen
import com.example.hibuddy.ui.screens.queue.QueueScreen
import com.example.hibuddy.ui.screens.SimpleCreateTaskScreen
import kotlinx.coroutines.launch
import com.example.hibuddy.ui.screens.profile.CompleteProfileScreen
import com.example.hibuddy.ui.screens.profile.UserDetailScreen
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.hibuddy.ui.screens.admin.AdminScreen
import com.example.hibuddy.ui.screens.admin.StudentVerificationScreen
import com.example.hibuddy.ui.screens.profile.SubmitStudentVerificationScreen
import com.example.hibuddy.ui.screens.admin.UserManagementScreen
import com.example.hibuddy.ui.screens.admin.ReportManagementScreen
import com.example.hibuddy.ui.screens.admin.FlaggedProjectsScreen
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import com.google.firebase.messaging.FirebaseMessaging

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
    const val VERIFY_EMAIL = "auth/verify-email?email={email}"
    fun verifyEmail(email: String) = "auth/verify-email?email=${Uri.encode(email)}"
    const val DISCOVER = "main/discover"
    const val QUEUE = "main/queue"
    const val MATCHES = "main/matches"
    const val TASKS = "main/tasks"
    const val PROFILE = "main/profile"
    const val CHAT = "main/chat/{matchId}/{userName}/{targetUserId}?avatar={avatar}"
    const val COMPLETE_PROFILE = "auth/complete-profile/{from}"
    fun completeProfile(from: String) = "auth/complete-profile/$from"
    fun chat(matchId: String, userName: String, targetUserId: String, avatar: String?) =
        "main/chat/$matchId/${Uri.encode(userName)}/${Uri.encode(targetUserId)}?avatar=${Uri.encode(avatar.orEmpty())}"
    const val PROJECT_DETAIL = "main/project/{projectId}"
    fun projectDetail(projectId: String) = "main/project/$projectId"
    const val USER_DETAIL = "main/user/{userId}"
    fun userDetail(userId: String) = "main/user/$userId"
    const val CREATE_PROJECT = "main/create-project"
    const val CREATE_TASK = "main/create-task/{projectId}"
    fun createTask(projectId: String) = "main/create-task/$projectId"
    const val ADMIN = "main/admin"
    const val ADMIN_STUDENT_VERIFICATIONS = "main/admin/student-verifications"
    const val STUDENT_VERIFICATION = "main/profile/student-verification"
    const val ADMIN_USER_MANAGEMENT = "main/admin/users"
    const val ADMIN_REPORT_MANAGEMENT = "main/admin/reports"
    const val ADMIN_FLAGGED_PROJECTS = "main/admin/projects"
    const val NOTIFICATIONS = "main/notifications"
    const val FEEDBACK = "main/feedback/{projectId}"
    fun feedback(projectId: String) = "main/feedback/$projectId"
}

private fun NavHostController.navigateMain(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun HiBuddyApp() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val isLoggedIn by ServiceLocator.authRepository.authState.collectAsState()
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    var dismissProfileCompletionHint by rememberSaveable {
        mutableStateOf(false)
    }
    val startDestination = remember {
        if (ServiceLocator.authRepository.hasSession()) {
            if (!ServiceLocator.authRepository.isEmailVerified()) {
                Routes.verifyEmail(ServiceLocator.authRepository.getPendingEmail().orEmpty())
            } else if (ServiceLocator.authRepository.isAdmin()) Routes.ADMIN else Routes.DISCOVER
        } else {
            Routes.LOGIN
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            if (Build.VERSION.SDK_INT >= 33) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            runCatching {
                FirebaseMessaging.getInstance().token.addOnSuccessListener(
                    ServiceLocator::registerPushToken
                )
            }
        }
        if (!isLoggedIn) {
            ServiceLocator.presenceWebSocketManager.disconnect()
            val currentRoute = navController.currentDestination?.route
            val authRoutes = setOf(Routes.LOGIN, Routes.REGISTER, Routes.FORGOT, Routes.VERIFY_EMAIL)
            if (currentRoute != null && currentRoute !in authRoutes) {
                val destination = if (ServiceLocator.authRepository.hasSession()) {
                    Routes.verifyEmail(
                        ServiceLocator.authRepository.getPendingEmail().orEmpty()
                    )
                } else {
                    Routes.LOGIN
                }
                navController.navigate(destination) {
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
        composable(
            Routes.COMPLETE_PROFILE,
            arguments = listOf(
                navArgument("from") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val from = backStackEntry.arguments?.getString("from") ?: "signup"

            if (!isLoggedIn) {
                LaunchedEffect(Unit) {
                    val destination = if (ServiceLocator.authRepository.hasSession()) {
                        Routes.verifyEmail(
                            ServiceLocator.authRepository.getPendingEmail().orEmpty()
                        )
                    } else {
                        Routes.LOGIN
                    }
                    navController.navigate(destination) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } else {
                CompleteProfileScreen(
                    onSkip = {
                        if (from == "signup") {
                            navController.navigate(Routes.DISCOVER) {
                                popUpTo(Routes.COMPLETE_PROFILE) { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },

                    onComplete = {
                        if (from == "signup") {
                            navController.navigate(Routes.DISCOVER) {
                                popUpTo(Routes.COMPLETE_PROFILE) { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
                            navController.popBackStack(
                                Routes.PROFILE,
                                false
                            )
                        }
                    }
                )
            }
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT) },
                onEmailVerificationRequired = { email ->
                    navController.navigate(Routes.verifyEmail(email)) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onLoginSuccess = {
                    ServiceLocator.presenceWebSocketManager.connect(ServiceLocator.authRepository.getAccessToken())

                    val destination = if (ServiceLocator.authRepository.isAdmin()) {
                        Routes.ADMIN
                    } else {
                        Routes.DISCOVER
                    }

                    navController.navigate(destination) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = { email ->
                    navController.navigate(Routes.verifyEmail(email)) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(
            Routes.VERIFY_EMAIL,
            arguments = listOf(
                navArgument("email") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { entry ->
            VerifyEmailScreen(
                initialEmail = Uri.decode(entry.arguments?.getString("email").orEmpty()),
                onVerified = {
                    ServiceLocator.presenceWebSocketManager.connect(
                        ServiceLocator.authRepository.getAccessToken()
                    )
                    navController.navigate(Routes.completeProfile("signup")) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBackToLogin = {
                    scope.launch { ServiceLocator.authRepository.logout() }
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
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
                        "discover" -> navController.navigateMain(Routes.DISCOVER)
                        "matches" -> navController.navigateMain(Routes.MATCHES)
                        "tasks" -> navController.navigateMain(Routes.TASKS)
                        "profile" -> navController.navigateMain(Routes.PROFILE)
                        "notifications" -> navController.navigate(Routes.NOTIFICATIONS)
                    }
                }
            ) {
                DiscoverScreen(
                    onCreateProject = {
                        navController.navigate(Routes.CREATE_PROJECT)
                    },
                    onOpenQueue = {
                        navController.navigate(Routes.QUEUE)
                    }
                )
            }
        }
        composable(Routes.QUEUE) {
            MainScaffold(
                currentTab = "discover",
                onTabSelect = { tab ->
                    when (tab) {
                        "discover" -> navController.navigateMain(Routes.DISCOVER)
                        "matches" -> navController.navigateMain(Routes.MATCHES)
                        "tasks" -> navController.navigateMain(Routes.TASKS)
                        "profile" -> navController.navigateMain(Routes.PROFILE)
                        "notifications" -> navController.navigate(Routes.NOTIFICATIONS)
                    }
                }
            ) {
                QueueScreen(
                    onBack = { navController.popBackStack() },
                    onOpenUser = { userId -> navController.navigate(Routes.userDetail(userId)) },
                    onOpenProject = { projectId -> navController.navigate(Routes.projectDetail(projectId)) }
                )
            }
        }
        composable(Routes.MATCHES) {
            MainScaffold(
                currentTab = "matches",
                onTabSelect = { tab ->
                    when (tab) {
                        "discover" -> navController.navigateMain(Routes.DISCOVER)
                        "matches" -> navController.navigateMain(Routes.MATCHES)
                        "tasks" -> navController.navigateMain(Routes.TASKS)
                        "profile" -> navController.navigateMain(Routes.PROFILE)
                        "notifications" -> navController.navigate(Routes.NOTIFICATIONS)
                    }
                }
            ) {
                MatchesScreen(
                    onChatClick = { matchId, userName, targetUserId, avatar ->
                        navController.navigate(Routes.chat(matchId, userName, targetUserId, avatar))
                    }
                )
            }
        }
        composable(Routes.TASKS) {
            MainScaffold(
                currentTab = "tasks",
                onTabSelect = { tab ->
                    when (tab) {
                        "discover" -> navController.navigateMain(Routes.DISCOVER)
                        "matches" -> navController.navigateMain(Routes.MATCHES)
                        "tasks" -> navController.navigateMain(Routes.TASKS)
                        "profile" -> navController.navigateMain(Routes.PROFILE)
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
                        "discover" -> navController.navigateMain(Routes.DISCOVER)
                        "matches" -> navController.navigateMain(Routes.MATCHES)
                        "tasks" -> navController.navigateMain(Routes.TASKS)
                        "profile" -> navController.navigateMain(Routes.PROFILE)
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
                    },
                    onCompleteProfile = {
                        navController.navigate(Routes.completeProfile("profile"))
                    },
                    onOpenAdmin = {
                        navController.navigate(Routes.ADMIN)
                    },
                    onOpenStudentVerification = {
                        navController.navigate(Routes.STUDENT_VERIFICATION)
                    },
                    dismissCompletionHint = dismissProfileCompletionHint,
                    onDismissCompletionHint = {
                        dismissProfileCompletionHint = true
                    }
                )
            }
        }

        composable(
            Routes.CHAT,
            arguments = listOf(
                navArgument("matchId") { type = NavType.StringType },
                navArgument("userName") { type = NavType.StringType },
                navArgument("targetUserId") { type = NavType.StringType },
                navArgument("avatar") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
            val userName = Uri.decode(backStackEntry.arguments?.getString("userName") ?: "")
            val targetUserId = Uri.decode(backStackEntry.arguments?.getString("targetUserId") ?: "")
            val avatar = Uri.decode(backStackEntry.arguments?.getString("avatar") ?: "")
            ChatScreen(
                matchId = matchId,
                userName = userName,
                targetUserId = targetUserId,
                userAvatar = avatar.takeIf { it.isNotBlank() },
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

        composable(
            Routes.USER_DETAIL,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserDetailScreen(
                userId = userId,
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

        composable(Routes.ADMIN) {
            AdminScreen(
                onLogout = {
                    scope.launch {
                        ServiceLocator.presenceWebSocketManager.disconnect()
                        ServiceLocator.authRepository.logout()

                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onOpenStudentVerifications = {
                    navController.navigate(Routes.ADMIN_STUDENT_VERIFICATIONS)
                },
                onOpenReportManagement = {
                    navController.navigate(Routes.ADMIN_REPORT_MANAGEMENT)
                },
                onOpenUserManagement = {
                    navController.navigate(Routes.ADMIN_USER_MANAGEMENT)
                },
                onOpenFlaggedProjects = {
                    navController.navigate(Routes.ADMIN_FLAGGED_PROJECTS)
                }
            )
        }
        composable(Routes.ADMIN_STUDENT_VERIFICATIONS) {
            StudentVerificationScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.STUDENT_VERIFICATION) {
            SubmitStudentVerificationScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.ADMIN_USER_MANAGEMENT) {
            UserManagementScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.ADMIN_REPORT_MANAGEMENT) {
            ReportManagementScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.ADMIN_FLAGGED_PROJECTS) {
            FlaggedProjectsScreen(onBack = { navController.popBackStack() })
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
