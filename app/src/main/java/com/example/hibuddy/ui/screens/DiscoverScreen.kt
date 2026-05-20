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
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.data.remote.dto.*
import com.example.hibuddy.ui.screens.discover.DiscoverViewModel
import com.example.hibuddy.ui.theme.HiBuddyColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

enum class CardMode { PEOPLE, PROJECTS }

private enum class SwipeIntent { Pass, Like, SuperLike }

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel = viewModel(factory = DiscoverViewModel.Factory),
    onCreateProject: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        viewModel.loadCards()
    }

    val cards = if (uiState.mode == "CONTRIBUTOR") uiState.projectCards else uiState.userCards
    val cardIndex = uiState.currentCardIndex
    val topCards = cards.drop(cardIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
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
                CircularProgressIndicator(color = colorScheme.primary)
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
    val colorScheme = MaterialTheme.colorScheme

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
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Create Project", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Spacer(Modifier.width(1.dp))
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colorScheme.surfaceVariant,
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
                    color = if (isPeopleMode) HiBuddyColors.warning else HiBuddyColors.success,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Swap Mode",
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─── Swipeable User Card (API DTO) ────────────────────────────────────────

@Composable
private fun SwipeableCardFrame(
    cardKey: String,
    modifier: Modifier = Modifier,
    positiveLabel: String,
    negativeLabel: String,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSuperLike: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val latestOnSwipeLeft by rememberUpdatedState(onSwipeLeft)
    val latestOnSwipeRight by rememberUpdatedState(onSwipeRight)
    val latestOnSuperLike by rememberUpdatedState(onSuperLike)

    var cardOffset by remember(cardKey) { mutableStateOf(Offset.Zero) }
    var isDragging by remember(cardKey) { mutableStateOf(false) }
    var isSettling by remember(cardKey) { mutableStateOf(false) }
    var hasDispatched by remember(cardKey) { mutableStateOf(false) }
    var velocityTracker by remember(cardKey) { mutableStateOf(VelocityTracker()) }
    var animationJob by remember(cardKey) { mutableStateOf<Job?>(null) }

    DisposableEffect(cardKey) {
        onDispose { animationJob?.cancel() }
    }

    BoxWithConstraints(modifier = modifier) {
        val fallbackWidthPx = with(density) { 360.dp.toPx() }
        val fallbackHeightPx = with(density) { 620.dp.toPx() }
        val measuredWidthPx = with(density) { maxWidth.toPx() }
        val measuredHeightPx = with(density) { maxHeight.toPx() }
        val widthPx = measuredWidthPx.takeIf { it.isFinite() && it > 0f } ?: fallbackWidthPx
        val heightPx = measuredHeightPx.takeIf { it.isFinite() && it > 0f } ?: fallbackHeightPx
        val horizontalThreshold = (widthPx * 0.28f)
            .coerceAtLeast(with(density) { 96.dp.toPx() })
            .coerceAtMost(with(density) { 156.dp.toPx() })
        val verticalThreshold = (heightPx * 0.18f)
            .coerceAtLeast(with(density) { 92.dp.toPx() })
            .coerceAtMost(with(density) { 150.dp.toPx() })
        val velocityThreshold = with(density) { 850.dp.toPx() }
        val exitPadding = with(density) { 280.dp.toPx() }
        val maxAnimationVelocity = with(density) { 2600.dp.toPx() }

        fun launchSettleAnimation(
            targetOffset: Offset,
            initialVelocity: Offset,
            intent: SwipeIntent?,
        ) {
            animationJob?.cancel()
            if (intent != null) hasDispatched = true
            animationJob = scope.launch {
                isSettling = true
                val animatable = Animatable(cardOffset, Offset.VectorConverter)
                val animationSpec: AnimationSpec<Offset> = if (intent == null) {
                    spring(
                        dampingRatio = 0.78f,
                        stiffness = Spring.StiffnessMediumLow,
                    )
                } else {
                    spring(
                        dampingRatio = 0.9f,
                        stiffness = Spring.StiffnessMedium,
                    )
                }

                animatable.animateTo(
                    targetValue = targetOffset,
                    animationSpec = animationSpec,
                    initialVelocity = initialVelocity,
                ) {
                    cardOffset = value
                }
                cardOffset = targetOffset
                isSettling = false

                when (intent) {
                    SwipeIntent.Pass -> latestOnSwipeLeft()
                    SwipeIntent.Like -> latestOnSwipeRight()
                    SwipeIntent.SuperLike -> latestOnSuperLike()
                    null -> Unit
                }
            }
        }

        fun settleCard(velocityX: Float, velocityY: Float) {
            val projectedOffset = Offset(
                x = cardOffset.x + velocityX * 0.16f,
                y = cardOffset.y + velocityY * 0.16f,
            )
            val horizontalScore =
                abs(projectedOffset.x) / horizontalThreshold +
                    abs(velocityX) / velocityThreshold * 0.3f
            val superLikeScore =
                (-projectedOffset.y).coerceAtLeast(0f) / verticalThreshold +
                    (-velocityY).coerceAtLeast(0f) / velocityThreshold * 0.3f

            val intent = when {
                horizontalScore >= 1f && horizontalScore >= superLikeScore ->
                    if (projectedOffset.x >= 0f) SwipeIntent.Like else SwipeIntent.Pass
                superLikeScore >= 1f -> SwipeIntent.SuperLike
                else -> null
            }

            val cappedVelocity = Offset(
                x = velocityX.coerceIn(-maxAnimationVelocity, maxAnimationVelocity),
                y = velocityY.coerceIn(-maxAnimationVelocity, maxAnimationVelocity),
            )

            val targetOffset = when (intent) {
                SwipeIntent.Like -> Offset(
                    x = widthPx + exitPadding,
                    y = (cardOffset.y + velocityY * 0.12f).coerceIn(-heightPx * 0.7f, heightPx * 0.7f),
                )
                SwipeIntent.Pass -> Offset(
                    x = -widthPx - exitPadding,
                    y = (cardOffset.y + velocityY * 0.12f).coerceIn(-heightPx * 0.7f, heightPx * 0.7f),
                )
                SwipeIntent.SuperLike -> Offset(
                    x = (cardOffset.x + velocityX * 0.08f).coerceIn(-widthPx * 0.35f, widthPx * 0.35f),
                    y = -heightPx - exitPadding,
                )
                null -> Offset.Zero
            }

            launchSettleAnimation(
                targetOffset = targetOffset,
                initialVelocity = cappedVelocity,
                intent = intent,
            )
        }

        val rotation = (cardOffset.x / widthPx * 14f).coerceIn(-16f, 16f)
        val likeAlpha = (cardOffset.x / horizontalThreshold).coerceIn(0f, 1f)
        val passAlpha = (-cardOffset.x / horizontalThreshold).coerceIn(0f, 1f)
        val superLikeAlpha = (-cardOffset.y / verticalThreshold).coerceIn(0f, 1f)
        val colorScheme = MaterialTheme.colorScheme

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        cardOffset.x.roundToInt(),
                        cardOffset.y.roundToInt(),
                    )
                }
                .graphicsLayer {
                    rotationZ = rotation
                    transformOrigin = TransformOrigin(0.5f, 0.88f)
                }
                .pointerInput(cardKey, horizontalThreshold, verticalThreshold, widthPx, heightPx) {
                    detectDragGestures(
                        onDragStart = {
                            if (!hasDispatched) {
                                animationJob?.cancel()
                                isDragging = true
                                isSettling = false
                                velocityTracker = VelocityTracker()
                            }
                        },
                        onDragEnd = {
                            if (isDragging && !hasDispatched) {
                                isDragging = false
                                val velocity = velocityTracker.calculateVelocity()
                                settleCard(velocity.x, velocity.y)
                            }
                        },
                        onDragCancel = {
                            if (!hasDispatched) {
                                isDragging = false
                                launchSettleAnimation(
                                    targetOffset = Offset.Zero,
                                    initialVelocity = Offset.Zero,
                                    intent = null,
                                )
                            }
                        },
                        onDrag = { change, drag ->
                            if (hasDispatched || isSettling) {
                                change.consume()
                            } else {
                                change.consume()
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                val dampedVerticalDrag = if (cardOffset.y > 0f && drag.y > 0f) {
                                    drag.y * 0.35f
                                } else {
                                    drag.y
                                }
                                cardOffset += Offset(drag.x, dampedVerticalDrag)
                            }
                        },
                    )
                },
        ) {
            content()

            if (likeAlpha > 0.05f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(HiBuddyColors.success.copy(alpha = likeAlpha * 0.22f), RoundedCornerShape(24.dp)),
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(20.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = HiBuddyColors.successContainer.copy(alpha = likeAlpha),
                ) {
                    Text(
                        positiveLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = HiBuddyColors.onSuccessContainer,
                    )
                }
            }
            if (passAlpha > 0.05f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorScheme.error.copy(alpha = passAlpha * 0.22f), RoundedCornerShape(24.dp)),
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(20.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.errorContainer.copy(alpha = passAlpha),
                ) {
                    Text(
                        negativeLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = colorScheme.onErrorContainer,
                    )
                }
            }
            if (superLikeAlpha > 0.05f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(HiBuddyColors.warning.copy(alpha = superLikeAlpha * 0.22f), RoundedCornerShape(24.dp)),
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = HiBuddyColors.warningContainer.copy(alpha = superLikeAlpha),
                ) {
                    Text(
                        "SUPER",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = HiBuddyColors.onWarningContainer,
                    )
                }
            }
        }
    }
}

@Composable
fun SwipeableUserCard(
    card: UserCardResponse,
    modifier: Modifier = Modifier,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSuperLike: () -> Unit,
) {
    SwipeableCardFrame(
        cardKey = card.userId,
        modifier = modifier
            .fillMaxSize(),
        positiveLabel = "LIKE",
        negativeLabel = "PASS",
        onSwipeLeft = onSwipeLeft,
        onSwipeRight = onSwipeRight,
        onSuperLike = onSuperLike,
    ) {
        UserSwipeCardStatic(card = card, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun UserSwipeCardStatic(card: UserCardResponse, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val avatarColor = remember(card.userId) {
        val colors = listOf(Color(0xFF5B4FCF), Color(0xFFE03055), Color(0xFF06B6D4), Color(0xFF7C6AF7), Color(0xFF059669), Color(0xFFFF8C42))
        colors[kotlin.math.abs(card.userId.hashCode()) % colors.size]
    }
    val avatarTextColor = if (avatarColor.luminance() > 0.5f) Color(0xFF15161F) else Color.White
    Box(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(colorScheme.surface)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(260.dp).background(
                Brush.radialGradient(colors = listOf(avatarColor.copy(alpha = 0.45f), colorScheme.surface), radius = 400f)
            ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(100.dp).background(avatarColor, CircleShape).border(2.dp, colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = card.displayName.firstOrNull()?.uppercase() ?: "?", fontSize = 48.sp, color = avatarTextColor)
            }
            Surface(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp), shape = RoundedCornerShape(20.dp), color = colorScheme.surface.copy(alpha = 0.88f)) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🎯", fontSize = 12.sp)
                    Text("${card.matchScore.toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = when {
                        card.matchScore >= 90 -> HiBuddyColors.success
                        card.matchScore >= 75 -> colorScheme.primary
                        else -> HiBuddyColors.warning
                    })
                }
            }
            if (card.verifiedStudent) {
                Surface(modifier = Modifier.align(Alignment.TopStart).padding(16.dp), shape = RoundedCornerShape(20.dp), color = colorScheme.primary) {
                    Text("🎓 Verified", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colorScheme.onPrimary)
                }
            }
        }

        Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(card.displayName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
            }
            Text(
                "${card.university ?: ""}  ·  ${card.location ?: ""}",
                fontSize = 12.sp, color = colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Text(
                card.bio ?: "",
                fontSize = 13.sp, color = colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                card.roles.take(3).forEach { role ->
                    Surface(shape = RoundedCornerShape(8.dp), color = colorScheme.primaryContainer, border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.4f))) {
                        Text(role.roleName, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), fontSize = 11.sp, color = colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                card.skills.take(3).forEach { skill ->
                    val skillColor = when (skill.level) {
                        "BEGINNER" -> HiBuddyColors.info
                        "INTERMEDIATE" -> colorScheme.primary
                        "ADVANCED" -> colorScheme.error
                        else -> colorScheme.primary
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
    val colorScheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 12.sp)
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
        }
        Text(text = label, fontSize = 10.sp, color = colorScheme.onSurfaceVariant)
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
    SwipeableCardFrame(
        cardKey = card.projectId,
        modifier = modifier
            .fillMaxSize(),
        positiveLabel = "APPLY",
        negativeLabel = "SKIP",
        onSwipeLeft = onSwipeLeft,
        onSwipeRight = onSwipeRight,
        onSuperLike = onSuperLike,
    ) {
        ProjectSwipeCardStatic(card = card, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun ProjectSwipeCardStatic(card: ProjectCardResponse, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val accentColor = remember(card.projectId) {
        val colors = listOf(Color(0xFF7C6AF7), Color(0xFF059669), Color(0xFFE03055), Color(0xFF06B6D4), Color(0xFFFF8C42))
        colors[card.projectId.hashCode().let { kotlin.math.abs(it) % colors.size }]
    }
    val avatarTextColor = if (accentColor.luminance() > 0.5f) Color(0xFF15161F) else Color.White
    Box(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Brush.horizontalGradient(listOf(accentColor, accentColor.copy(alpha = 0.3f)))))
        Box(
            modifier = Modifier.fillMaxWidth().height(220.dp).padding(top = 8.dp).background(Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.2f), Color.Transparent))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(card.field, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Text(card.title, fontSize = 24.sp, fontWeight = FontWeight.Black, color = colorScheme.onSurface)
            }
            Surface(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp), shape = RoundedCornerShape(20.dp), color = colorScheme.surface.copy(alpha = 0.88f)) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🎯", fontSize = 12.sp)
                    Text("${card.matchScore.toInt()}% fit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = when {
                        card.matchScore >= 90 -> HiBuddyColors.success
                        card.matchScore >= 75 -> colorScheme.primary
                        else -> HiBuddyColors.warning
                    })
                }
            }
        }

        Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(30.dp).background(accentColor, CircleShape).border(1.dp, colorScheme.surface, CircleShape), contentAlignment = Alignment.Center) {
                    Text(card.ownerName.firstOrNull()?.uppercase() ?: "?", fontSize = 14.sp, color = avatarTextColor)
                }
                Text("by ${card.ownerName}", fontSize = 13.sp, color = colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                val slotsLeft = card.totalSlots - card.filledSlots
                Text("$slotsLeft slots left", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (slotsLeft <= 1) HiBuddyColors.warning else HiBuddyColors.success)
            }
            Spacer(Modifier.height(10.dp))
            Text(card.description, fontSize = 13.sp, color = colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp)
            Spacer(Modifier.height(12.dp))
            Text("LOOKING FOR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(6.dp))
            card.roleSlots.take(4).forEach { slot ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(accentColor, CircleShape))
                        Text(slot.roleName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${slot.filled}/${slot.count}", fontSize = 10.sp, color = colorScheme.onSurfaceVariant)
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
    val colorScheme = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(8.dp), color = colorScheme.surfaceVariant) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 11.sp)
            Text(label, fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
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
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FloatingActionButton(
            onClick = { if (enabled) onPass() },
            modifier = Modifier.size(56.dp),
            containerColor = colorScheme.surfaceVariant,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(0.dp),
            contentColor = colorScheme.error
        ) {
            Icon(Icons.Filled.Close, "Pass", modifier = Modifier.size(26.dp))
        }

        FloatingActionButton(
            onClick = { if (enabled) onSuperLike() },
            modifier = Modifier.size(52.dp),
            containerColor = colorScheme.surfaceVariant,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(0.dp),
            contentColor = if (superLikesLeft > 0) HiBuddyColors.warning else HiBuddyColors.disabledContent
        ) {
            Text("⭐", fontSize = 22.sp)
        }

        FloatingActionButton(
            onClick = { if (enabled) onLike() },
            modifier = Modifier.size(56.dp),
            containerColor = if (likesLeft > 0) HiBuddyColors.successContainer else colorScheme.surfaceVariant,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(0.dp),
            contentColor = if (likesLeft > 0) HiBuddyColors.onSuccessContainer else HiBuddyColors.disabledContent
        ) {
            Icon(Icons.Filled.Favorite, "Like", modifier = Modifier.size(26.dp))
        }
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────

@Composable
fun EmptyStackView(isPeopleMode: Boolean, onCreateProject: () -> Unit = {}) {
    val colorScheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(40.dp)) {
        Text(if (isPeopleMode) "🎉" else "🚀", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text("You've seen everyone!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text("Check back later or update your profile to expand your pool.", fontSize = 14.sp, color = colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (isPeopleMode) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onCreateProject,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Create a New Project", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Match Dialog ─────────────────────────────────────────────────────────

@Composable
fun MatchDialog(onDismiss: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("🎊", fontSize = 48.sp)
                Spacer(Modifier.height(8.dp))
                Text("It's a Match!", fontSize = 24.sp, fontWeight = FontWeight.Black, color = colorScheme.onSurface)
            }
        },
        text = {
            Text("A new chat has been opened. Go to Matches to start chatting!", fontSize = 14.sp, color = colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Got it!", fontWeight = FontWeight.Bold) }
        },
    )
}
