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
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

enum class CardMode { PEOPLE, PROJECTS }

internal enum class SwipeIntent { Pass, Like, SuperLike, Queue }

internal data class SwipeDecisionThresholds(
    val horizontalThreshold: Float,
    val verticalThreshold: Float,
    val flingVelocityThreshold: Float,
)

internal fun resolveSwipeIntent(
    offset: Offset,
    velocity: Offset,
    thresholds: SwipeDecisionThresholds,
): SwipeIntent? {
    val horizontal = offset.x
    val upward = (-offset.y).coerceAtLeast(0f)
    val absHorizontal = abs(horizontal)
    val horizontalVelocity = velocity.x
    val upwardVelocity = (-velocity.y).coerceAtLeast(0f)
    val absHorizontalVelocity = abs(horizontalVelocity)
    val absVerticalVelocity = abs(velocity.y)

    val nearCenter = absHorizontal < thresholds.horizontalThreshold * 0.62f &&
        upward < thresholds.verticalThreshold * 0.62f
    val notFlinging = absHorizontalVelocity < thresholds.flingVelocityThreshold * 0.92f &&
        upwardVelocity < thresholds.flingVelocityThreshold * 0.92f
    if (nearCenter && notFlinging) return null

    val intentionalQueueDrag = horizontal >= thresholds.horizontalThreshold * 0.92f &&
        upward >= thresholds.verticalThreshold * 0.88f
    val intentionalQueueFling = horizontal >= thresholds.horizontalThreshold * 0.45f &&
        upward >= thresholds.verticalThreshold * 0.45f &&
        horizontalVelocity >= thresholds.flingVelocityThreshold * 0.7f &&
        upwardVelocity >= thresholds.flingVelocityThreshold * 0.7f
    if (intentionalQueueDrag || intentionalQueueFling) return SwipeIntent.Queue

    val intentionalSuperLikeDrag = upward >= thresholds.verticalThreshold * 1.05f &&
        upward >= absHorizontal * 0.72f
    val intentionalSuperLikeFling = upward >= thresholds.verticalThreshold * 0.35f &&
        upwardVelocity >= thresholds.flingVelocityThreshold &&
        upwardVelocity >= absHorizontalVelocity * 0.82f
    if (intentionalSuperLikeDrag || intentionalSuperLikeFling) return SwipeIntent.SuperLike

    val intentionalHorizontalDrag = absHorizontal >= thresholds.horizontalThreshold &&
        absHorizontal >= upward * 0.78f
    val intentionalHorizontalFling = absHorizontal >= thresholds.horizontalThreshold * 0.35f &&
        absHorizontalVelocity >= thresholds.flingVelocityThreshold &&
        absHorizontalVelocity >= absVerticalVelocity * 0.86f
    if (intentionalHorizontalDrag || intentionalHorizontalFling) {
        return if (horizontal >= 0f || horizontalVelocity > 0f) SwipeIntent.Like else SwipeIntent.Pass
    }

    return null
}

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel = viewModel(factory = DiscoverViewModel.Factory),
    onCreateProject: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadCards()
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    val cards = if (uiState.mode == "CONTRIBUTOR") uiState.projectCards else uiState.userCards
    val cardIndex = uiState.currentCardIndex
    val topCards = cards.drop(cardIndex)

    val topCardKey = topCards.firstOrNull()?.let { card ->
        if (uiState.mode == "OWNER") {
            (card as UserCardResponse).userId
        } else {
            (card as ProjectCardResponse).projectId
        }
    }
    var requestedSwipeIntent by remember(topCardKey) { mutableStateOf<SwipeIntent?>(null) }
    var cardInteractionBusy by remember(topCardKey) { mutableStateOf(false) }
    val interactionBlocked = uiState.isSwiping || cardInteractionBusy

    fun requestSwipeIntent(intent: SwipeIntent) {
        if (!interactionBlocked && topCards.isNotEmpty()) {
            requestedSwipeIntent = intent
        }
    }

    fun reportBlockedSwipe(intent: SwipeIntent) {
        when (intent) {
            SwipeIntent.Like -> viewModel.swipe("LIKE")
            SwipeIntent.SuperLike -> viewModel.swipe("SUPER_LIKE")
            SwipeIntent.Queue -> viewModel.queueCurrentCard()
            SwipeIntent.Pass -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
        ) {
            DiscoverHeader(
                isPeopleMode = uiState.mode == "OWNER",
                onToggle = {
                    if (!interactionBlocked) {
                        val newMode = if (uiState.mode == "CONTRIBUTOR") "OWNER" else "CONTRIBUTOR"
                        viewModel.switchMode(newMode)
                    }
                },
                onCreateProject = onCreateProject,
            )

            if (uiState.mode == "OWNER" && uiState.ownerProjects.isNotEmpty()) {
                OwnerProjectSelector(
                    projects = uiState.ownerProjects,
                    selectedId = uiState.selectedOwnerProjectId,
                    onSelected = viewModel::selectOwnerProject
                )
            }

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
                            enabled = !uiState.isSwiping,
                            requestedSwipeIntent = requestedSwipeIntent,
                            onRequestedSwipeIntentConsumed = { requestedSwipeIntent = null },
                            onInteractionBusyChanged = { cardInteractionBusy = it },
                            canStartSwipe = { intent ->
                                when (intent) {
                                    SwipeIntent.Like -> uiState.dailyLikesRemaining > 0
                                    SwipeIntent.SuperLike -> uiState.dailyLikesRemaining > 0 && uiState.dailySuperlikesRemaining > 0
                                    SwipeIntent.Queue -> uiState.queuedUserCount < 3
                                    SwipeIntent.Pass -> true
                                }
                            },
                            onBlockedSwipe = ::reportBlockedSwipe,
                            onSwipeLeft = { viewModel.swipe("PASS") },
                            onSwipeRight = { viewModel.swipe("LIKE") },
                            onSuperLike = { viewModel.swipe("SUPER_LIKE") },
                            onQueueDrop = { viewModel.queueCurrentCard() }
                        )
                    } else {
                        SwipeableProjectCard(
                            card = first as ProjectCardResponse,
                            modifier = Modifier.fillMaxSize(),
                            enabled = !uiState.isSwiping,
                            requestedSwipeIntent = requestedSwipeIntent,
                            onRequestedSwipeIntentConsumed = { requestedSwipeIntent = null },
                            onInteractionBusyChanged = { cardInteractionBusy = it },
                            canStartSwipe = { intent ->
                                when (intent) {
                                    SwipeIntent.Like -> uiState.dailyLikesRemaining > 0
                                    SwipeIntent.SuperLike -> uiState.dailyLikesRemaining > 0 && uiState.dailySuperlikesRemaining > 0
                                    SwipeIntent.Queue -> uiState.queuedProjectCount < 3
                                    SwipeIntent.Pass -> true
                                }
                            },
                            onBlockedSwipe = ::reportBlockedSwipe,
                            onSwipeLeft = { viewModel.swipe("PASS") },
                            onSwipeRight = { viewModel.swipe("LIKE") },
                            onSuperLike = { viewModel.swipe("SUPER_LIKE") },
                            onQueueDrop = { viewModel.queueCurrentCard() }
                        )
                    }
                }
            }

            ActionButtons(
                onPass = { requestSwipeIntent(SwipeIntent.Pass) },
                onSuperLike = { requestSwipeIntent(SwipeIntent.SuperLike) },
                onLike = { requestSwipeIntent(SwipeIntent.Like) },
                superLikesLeft = uiState.dailySuperlikesRemaining,
                likesLeft = uiState.dailyLikesRemaining,
                enabled = topCards.isNotEmpty() && !interactionBlocked
            )

            Spacer(Modifier.height(4.dp))
        }

        QueueCornerButton(
            count = uiState.queuedUserCount + uiState.queuedProjectCount,
            onClick = onOpenQueue,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 58.dp, end = 16.dp)
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 84.dp)
        )
    }

    if (uiState.matchedProjectId != null) {
        MatchDialog(onDismiss = { viewModel.clearMatch() })
    }
}

@Composable
private fun OwnerProjectSelector(
    projects: List<ProjectResponse>,
    selectedId: String?,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = projects.firstOrNull { it.id == selectedId }
    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Filled.Work, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                selected?.title ?: "Select recruiting project",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            projects.forEach { project ->
                DropdownMenuItem(
                    text = { Text(project.title) },
                    onClick = {
                        expanded = false
                        onSelected(project.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun QueueCornerButton(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(modifier = modifier) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(52.dp),
            containerColor = colorScheme.primaryContainer,
            contentColor = colorScheme.onPrimaryContainer,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(2.dp)
        ) {
            Icon(Icons.Filled.Bookmark, contentDescription = "Open queue", modifier = Modifier.size(24.dp))
        }
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp),
            shape = CircleShape,
            color = if (count >= 6) MaterialTheme.colorScheme.error else HiBuddyColors.success
        ) {
            Text(
                count.toString(),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
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
    enabled: Boolean = true,
    requestedSwipeIntent: SwipeIntent? = null,
    onRequestedSwipeIntentConsumed: () -> Unit = {},
    onInteractionBusyChanged: (Boolean) -> Unit = {},
    canStartSwipe: (SwipeIntent) -> Boolean = { true },
    onBlockedSwipe: (SwipeIntent) -> Unit = {},
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSuperLike: () -> Unit,
    onQueueDrop: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val latestOnSwipeLeft by rememberUpdatedState(onSwipeLeft)
    val latestOnSwipeRight by rememberUpdatedState(onSwipeRight)
    val latestOnSuperLike by rememberUpdatedState(onSuperLike)
    val latestOnQueueDrop by rememberUpdatedState(onQueueDrop)
    val latestOnRequestedSwipeIntentConsumed by rememberUpdatedState(onRequestedSwipeIntentConsumed)
    val latestOnInteractionBusyChanged by rememberUpdatedState(onInteractionBusyChanged)
    val latestCanStartSwipe by rememberUpdatedState(canStartSwipe)
    val latestOnBlockedSwipe by rememberUpdatedState(onBlockedSwipe)

    var cardOffset by remember(cardKey) { mutableStateOf(Offset.Zero) }
    var isDragging by remember(cardKey) { mutableStateOf(false) }
    var isSettling by remember(cardKey) { mutableStateOf(false) }
    var hasDispatched by remember(cardKey) { mutableStateOf(false) }
    var velocityTracker by remember(cardKey) { mutableStateOf(VelocityTracker()) }
    var animationJob by remember(cardKey) { mutableStateOf<Job?>(null) }

    DisposableEffect(cardKey) {
        onDispose {
            animationJob?.cancel()
            onInteractionBusyChanged(false)
        }
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
        val thresholds = SwipeDecisionThresholds(
            horizontalThreshold = horizontalThreshold,
            verticalThreshold = verticalThreshold,
            flingVelocityThreshold = velocityThreshold,
        )

        fun targetOffsetFor(intent: SwipeIntent, velocity: Offset): Offset = when (intent) {
            SwipeIntent.Like -> Offset(
                x = widthPx + exitPadding,
                y = (cardOffset.y + velocity.y * 0.12f).coerceIn(-heightPx * 0.7f, heightPx * 0.7f),
            )
            SwipeIntent.Pass -> Offset(
                x = -widthPx - exitPadding,
                y = (cardOffset.y + velocity.y * 0.12f).coerceIn(-heightPx * 0.7f, heightPx * 0.7f),
            )
            SwipeIntent.SuperLike -> Offset(
                x = (cardOffset.x + velocity.x * 0.08f).coerceIn(-widthPx * 0.35f, widthPx * 0.35f),
                y = -heightPx - exitPadding,
            )
            SwipeIntent.Queue -> Offset(
                x = widthPx * 0.46f,
                y = -heightPx * 0.48f,
            )
        }

        fun capVelocity(velocity: Offset) = Offset(
            x = velocity.x.coerceIn(-maxAnimationVelocity, maxAnimationVelocity),
            y = velocity.y.coerceIn(-maxAnimationVelocity, maxAnimationVelocity),
        )

        fun launchSettleAnimation(
            targetOffset: Offset,
            initialVelocity: Offset,
            intent: SwipeIntent?,
        ) {
            animationJob?.cancel()
            if (intent != null) hasDispatched = true
            animationJob = scope.launch {
                isSettling = true
                latestOnInteractionBusyChanged(true)
                val animatable = Animatable(cardOffset, Offset.VectorConverter)
                val animationSpec: AnimationSpec<Offset> = if (intent == null) {
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    )
                } else {
                    spring(
                        dampingRatio = 0.92f,
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
                    SwipeIntent.Queue -> latestOnQueueDrop()
                    null -> latestOnInteractionBusyChanged(false)
                }
            }
        }

        fun animateIntent(intent: SwipeIntent, velocity: Offset = Offset.Zero) {
            if (hasDispatched || isSettling) return
            if (!latestCanStartSwipe(intent)) {
                latestOnBlockedSwipe(intent)
                launchSettleAnimation(
                    targetOffset = Offset.Zero,
                    initialVelocity = Offset.Zero,
                    intent = null,
                )
                return
            }
            val cappedVelocity = capVelocity(velocity)
            launchSettleAnimation(
                targetOffset = targetOffsetFor(intent, cappedVelocity),
                initialVelocity = cappedVelocity,
                intent = intent,
            )
        }

        fun settleCard(velocityX: Float, velocityY: Float) {
            val rawVelocity = Offset(velocityX, velocityY)
            val cappedVelocity = capVelocity(rawVelocity)
            val intent = resolveSwipeIntent(
                offset = cardOffset,
                velocity = cappedVelocity,
                thresholds = thresholds,
            )

            if (intent == null) {
                launchSettleAnimation(
                    targetOffset = Offset.Zero,
                    initialVelocity = cappedVelocity,
                    intent = null,
                )
            } else {
                animateIntent(intent, cappedVelocity)
            }
        }

        LaunchedEffect(requestedSwipeIntent, enabled, widthPx, heightPx) {
            val intent = requestedSwipeIntent ?: return@LaunchedEffect
            latestOnRequestedSwipeIntentConsumed()
            if (!enabled || hasDispatched || isSettling) return@LaunchedEffect
            val buttonVelocity = when (intent) {
                SwipeIntent.Pass -> Offset(-velocityThreshold * 0.85f, 0f)
                SwipeIntent.Like -> Offset(velocityThreshold * 0.85f, 0f)
                SwipeIntent.SuperLike -> Offset(0f, -velocityThreshold * 0.9f)
                SwipeIntent.Queue -> Offset(velocityThreshold * 0.72f, -velocityThreshold * 0.72f)
            }
            animateIntent(intent, buttonVelocity)
        }

        val rotation = (cardOffset.x / widthPx * 14f).coerceIn(-16f, 16f)
        val likeAlpha = (cardOffset.x / horizontalThreshold).coerceIn(0f, 1f)
        val passAlpha = (-cardOffset.x / horizontalThreshold).coerceIn(0f, 1f)
        val superLikeAlpha = (-cardOffset.y / verticalThreshold).coerceIn(0f, 1f)
        val queueAlpha = (
            cardOffset.x.coerceAtLeast(0f) / horizontalThreshold * 0.45f +
                (-cardOffset.y).coerceAtLeast(0f) / verticalThreshold * 0.65f
            ).coerceIn(0f, 1f)
        val colorScheme = MaterialTheme.colorScheme

        val dragModifier = if (enabled) {
            Modifier.pointerInput(cardKey, horizontalThreshold, verticalThreshold, widthPx, heightPx) {
                detectDragGestures(
                    onDragStart = {
                        if (!hasDispatched) {
                            animationJob?.cancel()
                            isDragging = true
                            latestOnInteractionBusyChanged(true)
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
                            velocityTracker.addPosition(change.uptimeMillis, change.previousPosition)
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
            }
        } else {
            Modifier
        }

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
                .then(dragModifier),
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
            if (queueAlpha > 0.08f && cardOffset.x > 0f && cardOffset.y < 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorScheme.primary.copy(alpha = queueAlpha * 0.16f), RoundedCornerShape(24.dp)),
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(20.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.primaryContainer.copy(alpha = queueAlpha),
                ) {
                    Text(
                        "QUEUE",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeableUserCard(
    card: UserCardResponse,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    requestedSwipeIntent: SwipeIntent? = null,
    onRequestedSwipeIntentConsumed: () -> Unit = {},
    onInteractionBusyChanged: (Boolean) -> Unit = {},
    canStartSwipe: (SwipeIntent) -> Boolean = { true },
    onBlockedSwipe: (SwipeIntent) -> Unit = {},
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSuperLike: () -> Unit,
    onQueueDrop: () -> Unit,
) {
    SwipeableCardFrame(
        cardKey = card.userId,
        modifier = modifier
            .fillMaxSize(),
        positiveLabel = "LIKE",
        negativeLabel = "PASS",
        enabled = enabled,
        requestedSwipeIntent = requestedSwipeIntent,
        onRequestedSwipeIntentConsumed = onRequestedSwipeIntentConsumed,
        onInteractionBusyChanged = onInteractionBusyChanged,
        canStartSwipe = canStartSwipe,
        onBlockedSwipe = onBlockedSwipe,
        onSwipeLeft = onSwipeLeft,
        onSwipeRight = onSwipeRight,
        onSuperLike = onSuperLike,
        onQueueDrop = onQueueDrop,
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
                modifier = Modifier.size(100.dp).clip(CircleShape).background(avatarColor).border(2.dp, colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!card.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = card.avatarUrl,
                        contentDescription = "${card.displayName} avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(text = card.displayName.firstOrNull()?.uppercase() ?: "?", fontSize = 48.sp, color = avatarTextColor)
                }
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
private fun SwipeableProjectCard(
    card: ProjectCardResponse,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    requestedSwipeIntent: SwipeIntent? = null,
    onRequestedSwipeIntentConsumed: () -> Unit = {},
    onInteractionBusyChanged: (Boolean) -> Unit = {},
    canStartSwipe: (SwipeIntent) -> Boolean = { true },
    onBlockedSwipe: (SwipeIntent) -> Unit = {},
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSuperLike: () -> Unit,
    onQueueDrop: () -> Unit,
) {
    SwipeableCardFrame(
        cardKey = card.projectId,
        modifier = modifier
            .fillMaxSize(),
        positiveLabel = "APPLY",
        negativeLabel = "SKIP",
        enabled = enabled,
        requestedSwipeIntent = requestedSwipeIntent,
        onRequestedSwipeIntentConsumed = onRequestedSwipeIntentConsumed,
        onInteractionBusyChanged = onInteractionBusyChanged,
        canStartSwipe = canStartSwipe,
        onBlockedSwipe = onBlockedSwipe,
        onSwipeLeft = onSwipeLeft,
        onSwipeRight = onSwipeRight,
        onSuperLike = onSuperLike,
        onQueueDrop = onQueueDrop,
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
                Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(accentColor).border(1.dp, colorScheme.surface, CircleShape), contentAlignment = Alignment.Center) {
                    if (!card.ownerAvatar.isNullOrBlank()) {
                        AsyncImage(
                            model = card.ownerAvatar,
                            contentDescription = "${card.ownerName} avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(card.ownerName.firstOrNull()?.uppercase() ?: "?", fontSize = 14.sp, color = avatarTextColor)
                    }
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
