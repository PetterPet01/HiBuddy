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
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.data.remote.dto.*
import com.example.hibuddy.ui.screens.discover.DiscoverViewModel
import kotlin.math.abs

enum class CardMode { PEOPLE, PROJECTS }

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel = viewModel(factory = DiscoverViewModel.Factory),
    onCreateProject: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCards()
    }

    val cards = if (uiState.mode == "CONTRIBUTOR") uiState.projectCards else uiState.userCards
    val cardIndex = uiState.currentCardIndex
    val topCards = cards.drop(cardIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D14))
    ) {
        DiscoverHeader(
            isPeopleMode = uiState.mode == "OWNER",
            onToggle = {
                val newMode = if (uiState.mode == "CONTRIBUTOR") "OWNER" else "CONTRIBUTOR"
                viewModel.switchMode(newMode)
            },
            onCreateProject = onCreateProject,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading && topCards.isEmpty()) {
                CircularProgressIndicator(color = Color(0xFF7C6AF7))
            } else if (topCards.isEmpty()) {
                EmptyStackView(
                    isPeopleMode = uiState.mode == "OWNER",
                    onCreateProject = onCreateProject,
                )
            } else {
                val first = topCards[0]
                if (topCards.size >= 3 && uiState.mode == "OWNER") {
                    UserSwipeCardStatic(
                        card = topCards[2] as UserCardResponse,
                        modifier = Modifier.fillMaxSize().offset(y = 16.dp).scale(0.92f).alpha(0.5f)
                    )
                }
                if (topCards.size >= 2) {
                    if (uiState.mode == "OWNER") {
                        UserSwipeCardStatic(
                            card = topCards[1] as UserCardResponse,
                            modifier = Modifier.fillMaxSize().offset(y = 8.dp).scale(0.96f).alpha(0.75f)
                        )
                    } else if (topCards.size >= 2 && uiState.mode == "CONTRIBUTOR") {
                        ProjectSwipeCardStatic(
                            card = topCards[1] as ProjectCardResponse,
                            modifier = Modifier.fillMaxSize().offset(y = 8.dp).scale(0.96f).alpha(0.7f)
                        )
                    }
                }
                if (uiState.mode == "OWNER") {
                    SwipeableUserCard(
                        card = first as UserCardResponse,
                        modifier = Modifier.fillMaxSize(),
                        onSwipeLeft = { viewModel.swipe("PASS") },
                        onSwipeRight = { viewModel.swipe("LIKE") },
                        onSuperLike = { viewModel.swipe("SUPER_LIKE") }
                    )
                } else {
                    SwipeableProjectCard(
                        card = first as ProjectCardResponse,
                        modifier = Modifier.fillMaxSize(),
                        onSwipeLeft = { viewModel.swipe("PASS") },
                        onSwipeRight = { viewModel.swipe("LIKE") },
                        onSuperLike = { viewModel.swipe("SUPER_LIKE") }
                    )
                }
            }
        }

        ActionButtons(
            onPass = { viewModel.swipe("PASS") },
            onSuperLike = { viewModel.swipe("SUPER_LIKE") },
            onLike = { viewModel.swipe("LIKE") },
            superLikesLeft = uiState.dailySuperlikesRemaining,
            likesLeft = uiState.dailyLikesRemaining,
            enabled = topCards.isNotEmpty()
        )

        Spacer(Modifier.height(4.dp))
    }

    if (uiState.matchedProjectId != null) {
        MatchDialog(onDismiss = { viewModel.clearMatch() })
    }
}

@Composable
fun DiscoverHeader(
    isPeopleMode: Boolean,
    onToggle: () -> Unit,
    onCreateProject: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPeopleMode) {
            TextButton(
                onClick = onCreateProject,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF7C6AF7))
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Create Project", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C6AF7))
            }
        } else {
            Spacer(Modifier.width(1.dp))
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1D2E),
            modifier = Modifier.clickable { onToggle() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isPeopleMode) "👑 Owner Mode" else "🛠️ Contributor Mode",
                    fontSize = 13.sp,
                    color = if (isPeopleMode) Color(0xFFFFD166) else Color(0xFF4CAF50),
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

// ─── Swipeable User Card (API DTO) ────────────────────────────────────────

@Composable
fun SwipeableUserCard(
    card: UserCardResponse,
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
            .pointerInput(card.userId) {
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
        UserSwipeCardStatic(card = card, modifier = Modifier.fillMaxSize())

        if (likeAlpha > 0.05f) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF4CAF50).copy(alpha = likeAlpha * 0.3f), RoundedCornerShape(24.dp)))
            Surface(modifier = Modifier.align(Alignment.TopStart).padding(20.dp), shape = RoundedCornerShape(8.dp), color = Color(0xFF4CAF50).copy(alpha = likeAlpha)) {
                Text("LIKE", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
        if (passAlpha > 0.05f) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFF4D6D).copy(alpha = passAlpha * 0.3f), RoundedCornerShape(24.dp)))
            Surface(modifier = Modifier.align(Alignment.TopEnd).padding(20.dp), shape = RoundedCornerShape(8.dp), color = Color(0xFFFF4D6D).copy(alpha = passAlpha)) {
                Text("PASS", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
        if (superLikeAlpha > 0.05f) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFFD166).copy(alpha = superLikeAlpha * 0.3f), RoundedCornerShape(24.dp)))
            Surface(modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp), shape = RoundedCornerShape(8.dp), color = Color(0xFFFFD166).copy(alpha = superLikeAlpha)) {
                Text("SUPER", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF0D0D14))
            }
        }
    }
}

@Composable
fun UserSwipeCardStatic(card: UserCardResponse, modifier: Modifier = Modifier) {
    val avatarColor = remember(card.userId) {
        val colors = listOf(Color(0xFF5B4FCF), Color(0xFFE03055), Color(0xFF06B6D4), Color(0xFF7C6AF7), Color(0xFF059669), Color(0xFFFF8C42))
        colors[kotlin.math.abs(card.userId.hashCode()) % colors.size]
    }
    Box(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF16152A))
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(260.dp).background(
                Brush.radialGradient(colors = listOf(avatarColor.copy(alpha = 0.6f), Color(0xFF0D0D14)), radius = 400f)
            ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(100.dp).background(avatarColor.copy(alpha = 0.3f), CircleShape).border(2.dp, avatarColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = card.displayName.firstOrNull()?.uppercase() ?: "?", fontSize = 48.sp, color = Color.White)
            }
            Surface(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp), shape = RoundedCornerShape(20.dp), color = Color(0xFF0D0D14).copy(alpha = 0.8f)) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🎯", fontSize = 12.sp)
                    Text("${card.matchScore.toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = when {
                        card.matchScore >= 90 -> Color(0xFF4CAF50)
                        card.matchScore >= 75 -> Color(0xFF7C6AF7)
                        else -> Color(0xFFFFD166)
                    })
                }
            }
            if (card.verifiedStudent) {
                Surface(modifier = Modifier.align(Alignment.TopStart).padding(16.dp), shape = RoundedCornerShape(20.dp), color = Color(0xFF7C6AF7).copy(alpha = 0.9f)) {
                    Text("🎓 Verified", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(card.displayName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF0EFF8))
            }
            Text(
                "${card.university ?: ""}  ·  ${card.location ?: ""}",
                fontSize = 12.sp, color = Color(0xFF6B6A8C)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                card.bio ?: "",
                fontSize = 13.sp, color = Color(0xFFB0AFC8), maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                card.roles.take(3).forEach { role ->
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF7C6AF7).copy(alpha = 0.18f), border = BorderStroke(1.dp, Color(0xFF7C6AF7).copy(alpha = 0.4f))) {
                        Text(role.roleName, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), fontSize = 11.sp, color = Color(0xFF7C6AF7), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                card.skills.take(3).forEach { skill ->
                    val skillColor = when (skill.level) {
                        "BEGINNER" -> Color(0xFF4ECDC4)
                        "INTERMEDIATE" -> Color(0xFF7C6AF7)
                        "ADVANCED" -> Color(0xFFFF6B6B)
                        else -> Color(0xFF7C6AF7)
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = skillColor.copy(alpha = 0.15f)) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(skillColor, CircleShape))
                            Text(skill.skillName, fontSize = 11.sp, color = skillColor)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatChip(icon = "✅", value = "${card.projectsCompleted}", label = "Projects")
                StatChip(icon = "⭐", value = "${card.reputationScore}", label = "Rep Score")
                StatChip(icon = "📍", value = card.location?.split(" ")?.firstOrNull() ?: "—", label = "Location")
            }
        }
    }
}

@Composable
fun StatChip(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 12.sp)
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF0EFF8))
        }
        Text(text = label, fontSize = 10.sp, color = Color(0xFF6B6A8C))
    }
}

// ─── Swipeable Project Card (API DTO) ─────────────────────────────────────

@Composable
fun SwipeableProjectCard(
    card: ProjectCardResponse,
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
            .pointerInput(card.projectId) {
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
        ProjectSwipeCardStatic(card = card, modifier = Modifier.fillMaxSize())

        if (likeAlpha > 0.05f) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF4CAF50).copy(alpha = likeAlpha * 0.3f), RoundedCornerShape(24.dp)))
            Surface(modifier = Modifier.align(Alignment.TopStart).padding(20.dp), shape = RoundedCornerShape(8.dp), color = Color(0xFF4CAF50).copy(alpha = likeAlpha)) {
                Text("APPLY", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
        if (passAlpha > 0.05f) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFF4D6D).copy(alpha = passAlpha * 0.3f), RoundedCornerShape(24.dp)))
            Surface(modifier = Modifier.align(Alignment.TopEnd).padding(20.dp), shape = RoundedCornerShape(8.dp), color = Color(0xFFFF4D6D).copy(alpha = passAlpha)) {
                Text("SKIP", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    }
}

@Composable
fun ProjectSwipeCardStatic(card: ProjectCardResponse, modifier: Modifier = Modifier) {
    val accentColor = remember(card.projectId) {
        val colors = listOf(Color(0xFF7C6AF7), Color(0xFF059669), Color(0xFFE03055), Color(0xFF06B6D4), Color(0xFFFF8C42))
        colors[card.projectId.hashCode().let { kotlin.math.abs(it) % colors.size }]
    }
    Box(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF16152A))
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Brush.horizontalGradient(listOf(accentColor, accentColor.copy(alpha = 0.3f)))))
        Box(
            modifier = Modifier.fillMaxWidth().height(220.dp).padding(top = 8.dp).background(Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.2f), Color.Transparent))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(card.field, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Text(card.title, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFFF0EFF8), letterSpacing = (-0.5).sp)
            }
            Surface(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp), shape = RoundedCornerShape(20.dp), color = Color(0xFF0D0D14).copy(alpha = 0.8f)) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🎯", fontSize = 12.sp)
                    Text("${card.matchScore.toInt()}% fit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = when {
                        card.matchScore >= 90 -> Color(0xFF4CAF50)
                        card.matchScore >= 75 -> Color(0xFF7C6AF7)
                        else -> Color(0xFFFFD166)
                    })
                }
            }
        }

        Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(30.dp).background(accentColor.copy(alpha = 0.3f), CircleShape).border(1.dp, accentColor, CircleShape), contentAlignment = Alignment.Center) {
                    Text(card.ownerName.firstOrNull()?.uppercase() ?: "?", fontSize = 14.sp, color = Color.White)
                }
                Text("by ${card.ownerName}", fontSize = 13.sp, color = Color(0xFF8B8AAC))
                Spacer(Modifier.weight(1f))
                val slotsLeft = card.totalSlots - card.filledSlots
                Text("$slotsLeft slots left", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (slotsLeft <= 1) Color(0xFFFF8C42) else Color(0xFF4CAF50))
            }
            Spacer(Modifier.height(10.dp))
            Text(card.description, fontSize = 13.sp, color = Color(0xFFB0AFC8), maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp)
            Spacer(Modifier.height(12.dp))
            Text("LOOKING FOR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5A5A7A), letterSpacing = 1.5.sp)
            Spacer(Modifier.height(6.dp))
            card.roleSlots.take(4).forEach { slot ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(accentColor, CircleShape))
                        Text(slot.roleName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD0CFF0))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${slot.filled}/${slot.count}", fontSize = 10.sp, color = Color(0xFF8B8AAC))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetaTag("🏠", card.workMode)
                MetaTag("🔥", card.commitmentLevel)
                if (card.startDate != null) MetaTag("📅", card.startDate.take(10))
            }
        }
    }
}

@Composable
fun MetaTag(icon: String, label: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF1E1D2E)) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 11.sp)
            Text(label, fontSize = 11.sp, color = Color(0xFF8B8AAC))
        }
    }
}

// ─── Action Buttons ───────────────────────────────────────────────────────

@Composable
fun ActionButtons(
    onPass: () -> Unit,
    onSuperLike: () -> Unit,
    onLike: () -> Unit,
    superLikesLeft: Int,
    likesLeft: Int,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FloatingActionButton(
            onClick = { if (enabled) onPass() },
            modifier = Modifier.size(56.dp),
            containerColor = Color(0xFF1E1D2E),
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(0.dp),
        ) {
            Icon(Icons.Filled.Close, "Pass", tint = Color(0xFFFF4D6D), modifier = Modifier.size(26.dp))
        }

        FloatingActionButton(
            onClick = { if (enabled) onSuperLike() },
            modifier = Modifier.size(52.dp),
            containerColor = Color(0xFF2A2840),
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(0.dp),
        ) {
            Text("⭐", fontSize = 22.sp, color = if (superLikesLeft > 0) Color(0xFFFFD166) else Color(0xFF3A3A5A))
        }

        FloatingActionButton(
            onClick = { if (enabled) onLike() },
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

// ─── Empty State ──────────────────────────────────────────────────────────

@Composable
fun EmptyStackView(isPeopleMode: Boolean, onCreateProject: () -> Unit = {}) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(40.dp)) {
        Text(if (isPeopleMode) "🎉" else "🚀", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text("You've seen everyone!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF0EFF8))
        Spacer(Modifier.height(8.dp))
        Text("Check back later or update your profile to expand your pool.", fontSize = 14.sp, color = Color(0xFF6B6A8C), textAlign = TextAlign.Center)
        if (isPeopleMode) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onCreateProject,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C6AF7)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Create a New Project", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ─── Match Dialog ─────────────────────────────────────────────────────────

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
                Text("It's a Match!", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFFF0EFF8))
            }
        },
        text = {
            Text("A new chat has been opened. Go to Matches to start chatting!", fontSize = 14.sp, color = Color(0xFF8B8AAC), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C6AF7)),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Got it!", fontWeight = FontWeight.Bold) }
        },
    )
}
