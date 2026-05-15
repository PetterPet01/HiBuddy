package com.example.hibuddy.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import kotlin.math.abs
import kotlin.math.sign

// ──────────────────────────────────────────────────────────────
//  Discover Screen
// ──────────────────────────────────────────────────────────────

@Composable
fun DiscoverScreen() {
    var cardMode by remember { mutableStateOf(CardMode.PEOPLE) }
    var selectedProject by remember { mutableStateOf(SampleData.myProjects.first()) }

    // Separate card stacks for each mode
    var peopleStack by remember { mutableStateOf(SampleData.users.toMutableList()) }
    var projectStack by remember { mutableStateOf(SampleData.projects.toMutableList()) }

    var superLikesLeft by remember { mutableStateOf(3) }
    var likesLeft by remember { mutableStateOf(50) }
    var lastAction by remember { mutableStateOf<SwipeAction?>(null) }
    var showMatchDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D14))
    ) {
        // ── Header (Compact Switcher) ───────────────────────
        DiscoverHeader(
            cardMode = cardMode,
            onModeChange = { cardMode = it }
        )

        // ── Project Context Blobs (Only for Project Owner) ──
        if (cardMode == CardMode.PEOPLE) {
            ProjectBlobsSelector(
                projects = SampleData.myProjects,
                selected = selectedProject,
                onSelect = { selectedProject = it },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                label = "Recruiting for:"
            )
        }

        // ── Card Stack ──────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (cardMode == CardMode.PEOPLE) {
                if (peopleStack.isEmpty()) {
                    EmptyStackView(mode = cardMode)
                } else {
                    // Background cards (peek effect)
                    if (peopleStack.size >= 3) {
                        UserSwipeCard(
                            user = peopleStack[2],
                            modifier = Modifier
                                .fillMaxSize()
                                .offset(y = 16.dp)
                                .scale(0.92f)
                                .alpha(0.5f)
                        )
                    }
                    if (peopleStack.size >= 2) {
                        UserSwipeCard(
                            user = peopleStack[1],
                            modifier = Modifier
                                .fillMaxSize()
                                .offset(y = 8.dp)
                                .scale(0.96f)
                                .alpha(0.75f)
                        )
                    }
                    // Top card (interactive)
                    SwipeableUserCard(
                        user = peopleStack[0],
                        modifier = Modifier.fillMaxSize(),
                        onSwipeLeft = {
                            peopleStack = peopleStack.drop(1).toMutableList()
                            lastAction = SwipeAction.PASS
                            likesLeft = (likesLeft - 1).coerceAtLeast(0)
                        },
                        onSwipeRight = {
                            if (likesLeft > 0) {
                                peopleStack = peopleStack.drop(1).toMutableList()
                                lastAction = SwipeAction.LIKE
                                likesLeft--
                                if (likesLeft % 5 == 0) showMatchDialog = true
                            }
                        },
                        onSuperLike = {
                            if (superLikesLeft > 0) {
                                peopleStack = peopleStack.drop(1).toMutableList()
                                lastAction = SwipeAction.SUPER_LIKE
                                superLikesLeft--
                            }
                        }
                    )
                }
            } else {
                // Project mode
                if (projectStack.isEmpty()) {
                    EmptyStackView(mode = cardMode)
                } else {
                    if (projectStack.size >= 2) {
                        ProjectSwipeCard(
                            project = projectStack[1],
                            modifier = Modifier
                                .fillMaxSize()
                                .offset(y = 8.dp)
                                .scale(0.96f)
                                .alpha(0.7f)
                        )
                    }
                    SwipeableProjectCard(
                        project = projectStack[0],
                        modifier = Modifier.fillMaxSize(),
                        onSwipeLeft = {
                            projectStack = projectStack.drop(1).toMutableList()
                            lastAction = SwipeAction.PASS
                        },
                        onSwipeRight = {
                            if (likesLeft > 0) {
                                projectStack = projectStack.drop(1).toMutableList()
                                lastAction = SwipeAction.LIKE
                                likesLeft--
                            }
                        },
                        onSuperLike = {
                            if (superLikesLeft > 0) {
                                projectStack = projectStack.drop(1).toMutableList()
                                lastAction = SwipeAction.SUPER_LIKE
                                superLikesLeft--
                            }
                        }
                    )
                }
            }
        }

        // ── Action Buttons ───────────────────────────────────
        ActionButtons(
            onPass = {
                if (cardMode == CardMode.PEOPLE && peopleStack.isNotEmpty()) {
                    peopleStack = peopleStack.drop(1).toMutableList(); lastAction = SwipeAction.PASS
                } else if (cardMode == CardMode.PROJECTS && projectStack.isNotEmpty()) {
                    projectStack = projectStack.drop(1).toMutableList(); lastAction = SwipeAction.PASS
                }
            },
            onSuperLike = {
                if (superLikesLeft > 0) {
                    if (cardMode == CardMode.PEOPLE && peopleStack.isNotEmpty()) {
                        peopleStack = peopleStack.drop(1).toMutableList()
                    } else if (cardMode == CardMode.PROJECTS && projectStack.isNotEmpty()) {
                        projectStack = projectStack.drop(1).toMutableList()
                    }
                    lastAction = SwipeAction.SUPER_LIKE; superLikesLeft--
                }
            },
            onLike = {
                if (likesLeft > 0) {
                    if (cardMode == CardMode.PEOPLE && peopleStack.isNotEmpty()) {
                        peopleStack = peopleStack.drop(1).toMutableList()
                    } else if (cardMode == CardMode.PROJECTS && projectStack.isNotEmpty()) {
                        projectStack = projectStack.drop(1).toMutableList()
                    }
                    lastAction = SwipeAction.LIKE; likesLeft--
                }
            },
            superLikesLeft = superLikesLeft,
            likesLeft = likesLeft,
        )

        Spacer(Modifier.height(4.dp))
    }

    // ── Match Dialog ─────────────────────────────────────────
    if (showMatchDialog) {
        MatchDialog(onDismiss = { showMatchDialog = false })
    }
}

// ──────────────────────────────────────────────────────────────
//  Common Shared Components
// ──────────────────────────────────────────────────────────────

@Composable
fun ProjectBlobsSelector(
    projects: List<MyProject>,
    selected: MyProject,
    onSelect: (MyProject) -> Unit,
    modifier: Modifier = Modifier,
    label: String
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = Color(0xFF6B6A8C))
        Spacer(Modifier.width(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            projects.forEach { project ->
                val isSelected = project == selected
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 22.dp else 16.dp)
                        .clip(CircleShape)
                        .background(project.color)
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onSelect(project) }
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = selected.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = selected.color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ──────────────────────────────────────────────────────────────
//  Header
// ──────────────────────────────────────────────────────────────

enum class CardMode { PEOPLE, PROJECTS }
enum class SwipeAction { LIKE, PASS, SUPER_LIKE }

@Composable
fun DiscoverHeader(
    cardMode: CardMode,
    onModeChange: (CardMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1D2E),
            modifier = Modifier.clickable {
                onModeChange(if (cardMode == CardMode.PEOPLE) CardMode.PROJECTS else CardMode.PEOPLE)
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (cardMode == CardMode.PEOPLE) "👑 Owner Mode" else "🛠️ Contributor Mode",
                    fontSize = 13.sp,
                    color = if (cardMode == CardMode.PEOPLE) Color(0xFFFFD166) else Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Swap Mode",
                    tint = Color(0xFF8B8AAC),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
//  Swipeable User Card (with gesture detection)
// ──────────────────────────────────────────────────────────────

@Composable
fun SwipeableUserCard(
    user: UserCard,
    modifier: Modifier = Modifier,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSuperLike: () -> Unit,
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val rotation = (offsetX / 25f).coerceIn(-15f, 15f)
    val likeAlpha = (offsetX / 300f).coerceIn(0f, 1f)
    val passAlpha = (-offsetX / 300f).coerceIn(0f, 1f)
    val superLikeAlpha = (-offsetY / 200f).coerceIn(0f, 1f)

    val animOffsetX by animateFloatAsState(
        targetValue = if (isDragging) offsetX else 0f,
        animationSpec = if (isDragging) tween(0) else spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "offsetX"
    )

    Box(
        modifier = modifier
            .offset { IntOffset(animOffsetX.toInt(), offsetY.toInt()) }
            .rotate(rotation)
            .pointerInput(user.id) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        when {
                            offsetX > 180f  -> { offsetX = 1200f; onSwipeRight() }
                            offsetX < -180f -> { offsetX = -1200f; onSwipeLeft() }
                            offsetY < -150f -> { onSuperLike(); offsetY = 0f }
                            else -> { offsetX = 0f; offsetY = 0f }
                        }
                    },
                    onDragCancel = { isDragging = false; offsetX = 0f; offsetY = 0f },
                    onDrag = { change, drag ->
                        change.consume()
                        offsetX += drag.x
                        offsetY += drag.y
                    }
                )
            }
    ) {
        UserSwipeCard(user = user, modifier = Modifier.fillMaxSize())

        // LIKE overlay
        if (likeAlpha > 0.05f) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Color(0xFF4CAF50).copy(alpha = likeAlpha * 0.3f), RoundedCornerShape(24.dp))
            )
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(20.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF4CAF50).copy(alpha = likeAlpha),
            ) {
                Text(
                    "LIKE 💚", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White
                )
            }
        }

        // PASS overlay
        if (passAlpha > 0.05f) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Color(0xFFFF4D6D).copy(alpha = passAlpha * 0.3f), RoundedCornerShape(24.dp))
            )
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(20.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFF4D6D).copy(alpha = passAlpha),
            ) {
                Text(
                    "PASS 👋", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White
                )
            }
        }

        // SUPER LIKE overlay
        if (superLikeAlpha > 0.05f) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Color(0xFFFFD166).copy(alpha = superLikeAlpha * 0.3f), RoundedCornerShape(24.dp))
            )
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFD166).copy(alpha = superLikeAlpha),
            ) {
                Text(
                    "SUPER ⭐", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF0D0D14)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
//  User Card (static, non-interactive version for stack)
// ──────────────────────────────────────────────────────────────

@Composable
fun UserSwipeCard(user: UserCard, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF16152A))
    ) {
        // Avatar area gradient background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            user.avatarColor.copy(alpha = 0.6f),
                            Color(0xFF0D0D14)
                        ),
                        radius = 400f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(user.avatarColor.copy(alpha = 0.3f), CircleShape)
                    .border(2.dp, user.avatarColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = user.avatarEmoji, fontSize = 48.sp)
            }

            // Match score badge
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0D0D14).copy(alpha = 0.8f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎯", fontSize = 12.sp)
                    Text(
                        text = "${user.matchScore}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            user.matchScore >= 90 -> Color(0xFF4CAF50)
                            user.matchScore >= 75 -> Color(0xFF7C6AF7)
                            else -> Color(0xFFFFD166)
                        }
                    )
                }
            }

            // Verified badge
            if (user.isVerified) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF7C6AF7).copy(alpha = 0.9f),
                ) {
                    Text(
                        text = "🎓 Verified",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Content section
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Name + university
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = user.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF0EFF8)
                )
            }
            Text(
                text = "${user.username}  ·  ${user.university}",
                fontSize = 12.sp,
                color = Color(0xFF6B6A8C)
            )

            Spacer(Modifier.height(10.dp))

            // Bio
            Text(
                text = user.bio,
                fontSize = 13.sp,
                color = Color(0xFFB0AFC8),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 19.sp
            )

            Spacer(Modifier.height(12.dp))

            // Roles
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                user.roles.forEach { role ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF7C6AF7).copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color(0xFF7C6AF7).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = role,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            fontSize = 11.sp,
                            color = Color(0xFF7C6AF7),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Skills
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                user.skills.take(3).forEach { skill ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = skill.level.color.copy(alpha = 0.15f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(6.dp).background(skill.level.color, CircleShape)
                            )
                            Text(
                                text = skill.name,
                                fontSize = 11.sp,
                                color = skill.level.color
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatChip(icon = "✅", value = "${user.projectsCompleted}", label = "Projects")
                StatChip(icon = "⭐", value = "${user.reputationStars}", label = "Rep Score")
                StatChip(icon = "📍", value = user.location.split(" ").first(), label = "Location")
            }
        }
    }
}

@Composable
fun StatChip(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 12.sp)
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF0EFF8))
        }
        Text(text = label, fontSize = 10.sp, color = Color(0xFF6B6A8C))
    }
}

// ──────────────────────────────────────────────────────────────
//  Project Card
// ──────────────────────────────────────────────────────────────

@Composable
fun SwipeableProjectCard(
    project: ProjectCard,
    modifier: Modifier = Modifier,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSuperLike: () -> Unit,
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val rotation = (offsetX / 25f).coerceIn(-15f, 15f)
    val likeAlpha = (offsetX / 300f).coerceIn(0f, 1f)
    val passAlpha = (-offsetX / 300f).coerceIn(0f, 1f)

    val animOffsetX by animateFloatAsState(
        targetValue = if (isDragging) offsetX else 0f,
        animationSpec = if (isDragging) tween(0) else spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "projOffsetX"
    )

    Box(
        modifier = modifier
            .offset { IntOffset(animOffsetX.toInt(), offsetY.toInt()) }
            .rotate(rotation)
            .pointerInput(project.id) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        when {
                            offsetX > 180f  -> { offsetX = 1200f; onSwipeRight() }
                            offsetX < -180f -> { offsetX = -1200f; onSwipeLeft() }
                            offsetY < -150f -> { onSuperLike(); offsetY = 0f }
                            else -> { offsetX = 0f; offsetY = 0f }
                        }
                    },
                    onDragCancel = { isDragging = false; offsetX = 0f; offsetY = 0f },
                    onDrag = { change, drag ->
                        change.consume()
                        offsetX += drag.x
                        offsetY += drag.y
                    }
                )
            }
    ) {
        ProjectSwipeCard(project = project, modifier = Modifier.fillMaxSize())

        if (likeAlpha > 0.05f) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF4CAF50).copy(alpha = likeAlpha * 0.3f), RoundedCornerShape(24.dp)))
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(20.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF4CAF50).copy(alpha = likeAlpha),
            ) { Text("APPLY 🚀", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White) }
        }
        if (passAlpha > 0.05f) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFF4D6D).copy(alpha = passAlpha * 0.3f), RoundedCornerShape(24.dp)))
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(20.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFF4D6D).copy(alpha = passAlpha),
            ) { Text("SKIP 👋", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White) }
        }
    }
}

@Composable
fun ProjectSwipeCard(project: ProjectCard, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF16152A))
    ) {
        // Top accent stripe
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Brush.horizontalGradient(listOf(project.accentColor, project.accentColor.copy(alpha = 0.3f))))
        )

        // Header area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(top = 8.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(project.accentColor.copy(alpha = 0.2f), Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = project.field,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = project.accentColor,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = project.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFF0EFF8),
                    letterSpacing = (-0.5).sp
                )
            }

            // Match score
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0D0D14).copy(alpha = 0.8f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎯", fontSize = 12.sp)
                    Text(
                        text = "${project.matchScore}% fit",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            project.matchScore >= 90 -> Color(0xFF4CAF50)
                            project.matchScore >= 75 -> Color(0xFF7C6AF7)
                            else -> Color(0xFFFFD166)
                        }
                    )
                }
            }
        }

        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Owner row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(30.dp).background(project.ownerColor.copy(alpha = 0.3f), CircleShape)
                        .border(1.dp, project.ownerColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text(project.ownerEmoji, fontSize = 16.sp) }
                Text(
                    text = "by ${project.ownerName}",
                    fontSize = 13.sp,
                    color = Color(0xFF8B8AAC)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${project.slotsTotal - project.slotsFilled} slots left",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (project.slotsTotal - project.slotsFilled <= 1) Color(0xFFFF8C42) else Color(0xFF4CAF50)
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = project.description,
                fontSize = 13.sp,
                color = Color(0xFFB0AFC8),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 19.sp
            )

            Spacer(Modifier.height(12.dp))

            // Roles needed
            Text(
                text = "LOOKING FOR",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5A5A7A),
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(6.dp))
            project.rolesNeeded.forEach { slot ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(project.accentColor, CircleShape))
                        Text(slot.role, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD0CFF0))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        slot.skills.take(2).forEach { skill ->
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF2A2840)) {
                                Text(skill, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = Color(0xFF8B8AAC))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Meta info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetaTag("📅", project.timeline)
                MetaTag("🏠", project.workMode)
                MetaTag("🔥", project.commitment)
            }
        }
    }
}

@Composable
fun MetaTag(icon: String, label: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E1D2E),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 11.sp)
            Text(label, fontSize = 11.sp, color = Color(0xFF8B8AAC))
        }
    }
}

// ──────────────────────────────────────────────────────────────
//  Action Buttons
// ──────────────────────────────────────────────────────────────

@Composable
fun ActionButtons(
    onPass: () -> Unit,
    onSuperLike: () -> Unit,
    onLike: () -> Unit,
    superLikesLeft: Int,
    likesLeft: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pass button
        FloatingActionButton(
            onClick = onPass,
            modifier = Modifier.size(56.dp),
            containerColor = Color(0xFF1E1D2E),
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(0.dp),
        ) {
            Icon(Icons.Filled.Close, "Pass", tint = Color(0xFFFF4D6D), modifier = Modifier.size(26.dp))
        }

        // Super Like button (center, larger)
        FloatingActionButton(
            onClick = onSuperLike,
            modifier = Modifier.size(52.dp),
            containerColor = Color(0xFF2A2840),
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(0.dp),
        ) {
            Text(
                text = "⭐",
                fontSize = 22.sp,
                color = if (superLikesLeft > 0) Color(0xFFFFD166) else Color(0xFF3A3A5A)
            )
        }

        // Like button
        FloatingActionButton(
            onClick = onLike,
            modifier = Modifier.size(56.dp),
            containerColor = if (likesLeft > 0) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFF1E1D2E),
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(0.dp),
            contentColor = if (likesLeft > 0) Color(0xFF4CAF50) else Color(0xFF3A3A5A)
        ) {
            Icon(Icons.Filled.Favorite, "Like", modifier = Modifier.size(26.dp))
        }
    }
}

// ──────────────────────────────────────────────────────────────
//  Empty State
// ──────────────────────────────────────────────────────────────

@Composable
fun EmptyStackView(mode: CardMode) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(40.dp)
    ) {
        Text(if (mode == CardMode.PEOPLE) "🎉" else "🚀", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "You've seen everyone!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF0EFF8)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Check back later or update your profile to expand your pool.",
            fontSize = 14.sp,
            color = Color(0xFF6B6A8C),
            textAlign = TextAlign.Center
        )
    }
}

// ──────────────────────────────────────────────────────────────
//  Match Dialog
// ──────────────────────────────────────────────────────────────

@Composable
fun MatchDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF16152A),
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("🎊", fontSize = 48.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "It's a Match!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFF0EFF8)
                )
            }
        },
        text = {
            Text(
                "A new chat has been opened. Reach out within 24h!",
                fontSize = 14.sp,
                color = Color(0xFF8B8AAC),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C6AF7)),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Start Chatting →", fontWeight = FontWeight.Bold) }
        },
    )
}