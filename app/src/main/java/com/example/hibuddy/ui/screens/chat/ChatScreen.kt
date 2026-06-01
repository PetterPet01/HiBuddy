package com.example.hibuddy.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hibuddy.data.remote.dto.InvitationRoleSlotResponse
import com.example.hibuddy.data.remote.dto.ProjectInvitationResponse
import com.example.hibuddy.ui.theme.HiBuddyColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    matchId: String,
    userName: String,
    targetUserId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel(
        key = "chat_$matchId",
        factory = ChatViewModel.factory(matchId, userName, targetUserId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    var inputText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    var blockReason by remember { mutableStateOf("") }
    var inviteMessage by remember { mutableStateOf("") }
    var selectedRoleSlotId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose { viewModel.cleanup() }
    }

    LaunchedEffect(uiState.messages.lastOrNull()?.id) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    LaunchedEffect(uiState.error) {
        val error = uiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
        viewModel.clearError()
    }

    LaunchedEffect(uiState.actionMessage) {
        val message = uiState.actionMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearActionMessage()
        if (message.contains("blocked", ignoreCase = true)) {
            onBack()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatPalette.Background),
        containerColor = ChatPalette.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ChatTopBar(
                userName = userName,
                isTyping = uiState.isTyping,
                otherUserIsOnline = uiState.otherUserIsOnline,
                otherUserLastSeenAt = uiState.otherUserLastSeenAt,
                showMenu = showMenu,
                onShowMenuChange = { showMenu = it },
                onBack = {
                    viewModel.cleanup()
                    onBack()
                },
                onRefresh = { viewModel.refreshMessages() },
                canInvite = uiState.invitationOptions?.canInvite == true,
                onInvite = {
                    showMenu = false
                    val slots = uiState.invitationOptions?.openRoleSlots.orEmpty()
                    selectedRoleSlotId = slots.firstOrNull()?.id
                    showInviteDialog = true
                },
                onReport = {
                    showMenu = false
                    showReportDialog = true
                },
                onBlock = {
                    showMenu = false
                    showBlockDialog = true
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ChatPalette.Background)
        ) {
            AnimatedVisibility(visible = uiState.connectionState == ChatConnectionState.RECONNECTING) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = ChatPalette.Accent,
                    trackColor = ChatPalette.Surface
                )
            }

            ChatTimeline(
                uiState = uiState,
                listState = listState,
                onLoadOlder = { viewModel.loadOlderMessages() },
                onRetryMessage = { viewModel.retryMessage(it) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            ProjectInvitationPanel(
                invitations = uiState.projectInvitations,
                isLoading = uiState.isInvitationActionLoading,
                onAccept = { viewModel.acceptProjectInvitation(it) },
                onDecline = { viewModel.declineProjectInvitation(it) }
            )

            ChatComposer(
                value = inputText,
                onValueChange = {
                    inputText = it
                    viewModel.sendTyping()
                },
                onSend = {
                    val message = inputText.trim()
                    if (message.isNotEmpty()) {
                        viewModel.sendMessage(message)
                        inputText = ""
                    }
                },
                connectionState = uiState.connectionState
            )
        }
    }

    if (showInviteDialog) {
        InviteProjectDialog(
            projectTitle = uiState.invitationOptions?.projectTitle.orEmpty(),
            roleSlots = uiState.invitationOptions?.openRoleSlots.orEmpty(),
            selectedRoleSlotId = selectedRoleSlotId,
            message = inviteMessage,
            isLoading = uiState.isInvitationActionLoading,
            onSelectRoleSlot = { selectedRoleSlotId = it },
            onMessageChange = { inviteMessage = it },
            onDismiss = { showInviteDialog = false },
            onConfirm = {
                selectedRoleSlotId?.let { roleSlotId ->
                    viewModel.createProjectInvitation(roleSlotId, inviteMessage)
                    inviteMessage = ""
                    showInviteDialog = false
                }
            }
        )
    }

    if (showBlockDialog) {
        BlockUserDialog(
            isLoading = uiState.isModerationActionInProgress,
            reason = blockReason,
            onReasonChange = { blockReason = it },
            onDismiss = { showBlockDialog = false },
            onConfirm = {
                viewModel.blockUser(uiState.targetUserId, blockReason.takeIf { it.isNotBlank() })
                showBlockDialog = false
            }
        )
    }

    if (showReportDialog) {
        ReportUserDialog(
            isLoading = uiState.isModerationActionInProgress,
            reason = reportReason,
            onReasonChange = { reportReason = it },
            onDismiss = { showReportDialog = false },
            onConfirm = {
                viewModel.reportUser(uiState.targetUserId, reportReason, "Reported from chat")
                reportReason = ""
                showReportDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    userName: String,
    isTyping: Boolean,
    otherUserIsOnline: Boolean,
    otherUserLastSeenAt: String?,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    canInvite: Boolean,
    onInvite: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit
) {
    TopAppBar(
        modifier = Modifier.statusBarsPadding(),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(name = userName, size = 42.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName.ifBlank { "Conversation" },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ChatPalette.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PresenceDot(otherUserIsOnline, isTyping)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = presenceStatusLabel(otherUserIsOnline, otherUserLastSeenAt, isTyping),
                            fontSize = 12.sp,
                            color = presenceStatusColor(otherUserIsOnline, isTyping),
                            maxLines = 1
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChatPalette.TextPrimary)
            }
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = ChatPalette.TextSecondary)
            }
            Box {
                IconButton(onClick = { onShowMenuChange(true) }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More options", tint = ChatPalette.TextPrimary)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { onShowMenuChange(false) },
                    modifier = Modifier.background(ChatPalette.SurfaceElevated)
                ) {
                    if (canInvite) {
                        DropdownMenuItem(
                            text = { Text("Invite to Project", color = ChatPalette.TextPrimary) },
                            onClick = onInvite,
                            leadingIcon = {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = ChatPalette.TextSecondary)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Report user", color = ChatPalette.TextPrimary) },
                        onClick = onReport,
                        leadingIcon = {
                            Icon(Icons.Filled.Report, contentDescription = null, tint = ChatPalette.TextSecondary)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Block user", color = ChatPalette.Danger) },
                        onClick = onBlock,
                        leadingIcon = {
                            Icon(Icons.Filled.Block, contentDescription = null, tint = ChatPalette.Danger)
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = ChatPalette.TopBar)
    )
}

@Composable
private fun ChatTimeline(
    uiState: ChatUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onLoadOlder: () -> Unit,
    onRetryMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading && uiState.messages.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ChatPalette.Accent)
        }
        return
    }

    if (uiState.messages.isEmpty()) {
        EmptyConversation(modifier = modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (uiState.canLoadMore) {
            item(key = "load_older") {
                LoadEarlierRow(
                    isLoading = uiState.isLoadingOlder,
                    onClick = onLoadOlder,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        itemsIndexed(
            items = uiState.messages,
            key = { _, message -> message.id }
        ) { index, message ->
            val previous = uiState.messages.getOrNull(index - 1)
            val next = uiState.messages.getOrNull(index + 1)
            val showDate = previous == null || dateKey(previous.createdAt) != dateKey(message.createdAt)
            val beginsCluster = previous == null ||
                previous.senderId != message.senderId ||
                showDate ||
                abs(message.localSortTime - previous.localSortTime) > MESSAGE_CLUSTER_WINDOW_MS
            val endsCluster = next == null ||
                next.senderId != message.senderId ||
                dateKey(next.createdAt) != dateKey(message.createdAt) ||
                abs(next.localSortTime - message.localSortTime) > MESSAGE_CLUSTER_WINDOW_MS

            if (showDate) {
                DateDivider(label = formatDayLabel(message.createdAt))
            }

            ChatBubble(
                message = message,
                showSender = beginsCluster && !message.isFromMe,
                showTail = endsCluster,
                onRetry = { onRetryMessage(message.id) }
            )
        }
    }
}

@Composable
private fun EmptyConversation(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = ChatPalette.SurfaceElevated,
                border = BorderStroke(1.dp, ChatPalette.Border)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = ChatPalette.Accent, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Start the conversation",
                color = ChatPalette.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Messages sync in real time and stay available on this device.",
                color = ChatPalette.TextTertiary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun ProjectInvitationPanel(
    invitations: List<ProjectInvitationResponse>,
    isLoading: Boolean,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit
) {
    val visibleInvitations = invitations
        .filter { it.status == "PENDING" || it.status == "ACCEPTED" || it.status == "DECLINED" }
        .take(2)
    if (visibleInvitations.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChatPalette.TopBar)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        visibleInvitations.forEach { invitation ->
            ProjectInvitationCard(
                invitation = invitation,
                isLoading = isLoading,
                onAccept = { onAccept(invitation.id) },
                onDecline = { onDecline(invitation.id) }
            )
        }
    }
}

@Composable
private fun ProjectInvitationCard(
    invitation: ProjectInvitationResponse,
    isLoading: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val statusColor = when (invitation.status) {
        "ACCEPTED" -> ChatPalette.Online
        "DECLINED" -> ChatPalette.Danger
        else -> ChatPalette.Warning
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = ChatPalette.Surface,
        border = BorderStroke(1.dp, ChatPalette.Border)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    color = ChatPalette.Accent.copy(alpha = 0.16f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Work, contentDescription = null, tint = ChatPalette.Accent, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(invitation.projectTitle, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ChatPalette.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Role: ${invitation.role}", fontSize = 12.sp, color = ChatPalette.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Surface(shape = RoundedCornerShape(999.dp), color = statusColor.copy(alpha = 0.16f)) {
                    Text(
                        invitation.status.lowercase().replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            invitation.message?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 12.sp, color = ChatPalette.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            when {
                invitation.isIncoming && invitation.status == "PENDING" -> {
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = onDecline, enabled = !isLoading) {
                            Text("Decline", color = ChatPalette.TextSecondary)
                        }
                        TextButton(onClick = onAccept, enabled = !isLoading) {
                            Text("Accept", color = ChatPalette.Accent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                invitation.isOutgoing && invitation.status == "PENDING" -> {
                    Text("Waiting for ${invitation.inviteeName.ifBlank { "this user" }} to respond.", fontSize = 12.sp, color = ChatPalette.TextTertiary)
                }
            }
        }
    }
}

@Composable
private fun InviteProjectDialog(
    projectTitle: String,
    roleSlots: List<InvitationRoleSlotResponse>,
    selectedRoleSlotId: String?,
    message: String,
    isLoading: Boolean,
    onSelectRoleSlot: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite to Project", color = ChatPalette.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(projectTitle.ifBlank { "Project" }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ChatPalette.TextPrimary)
                if (roleSlots.isEmpty()) {
                    Text("No open role slots remaining.", fontSize = 13.sp, color = ChatPalette.Warning)
                } else {
                    roleSlots.forEach { slot ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !isLoading) { onSelectRoleSlot(slot.id) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRoleSlotId == slot.id,
                                onClick = { onSelectRoleSlot(slot.id) },
                                enabled = !isLoading
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(slot.roleName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ChatPalette.TextPrimary)
                                Text("${slot.filled}/${slot.count} filled", fontSize = 12.sp, color = ChatPalette.TextSecondary)
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = message,
                    onValueChange = onMessageChange,
                    label = { Text("Message optional") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isLoading && selectedRoleSlotId != null && roleSlots.isNotEmpty()) {
                Text("Send Invite", color = ChatPalette.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel", color = ChatPalette.TextSecondary)
            }
        },
        containerColor = ChatPalette.TopBar
    )
}

@Composable
private fun LoadEarlierRow(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.padding(bottom = 10.dp), contentAlignment = Alignment.Center) {
        TextButton(onClick = onClick, enabled = !isLoading) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ChatPalette.Accent)
                Spacer(Modifier.width(8.dp))
            }
            Text("Load earlier", color = ChatPalette.Accent)
        }
    }
}

@Composable
private fun DateDivider(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = ChatPalette.Border)
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = ChatPalette.Surface,
            border = BorderStroke(1.dp, ChatPalette.Border)
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                fontSize = 11.sp,
                color = ChatPalette.TextTertiary,
                fontWeight = FontWeight.Medium
            )
        }
        HorizontalDivider(modifier = Modifier.weight(1f), color = ChatPalette.Border)
    }
}

@Composable
private fun ChatBubble(
    message: MessageItem,
    showSender: Boolean,
    showTail: Boolean,
    onRetry: () -> Unit
) {
    val bubbleColor = if (message.isFromMe) ChatPalette.OutgoingBubble else ChatPalette.IncomingBubble
    val textColor = if (message.isFromMe) ChatPalette.OnOutgoingBubble else ChatPalette.TextPrimary
    val alignment = if (message.isFromMe) Alignment.End else Alignment.Start
    val shape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (!message.isFromMe && showTail) 6.dp else 20.dp,
        bottomEnd = if (message.isFromMe && showTail) 6.dp else 20.dp
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (showSender && message.senderName.isNotBlank()) {
            Text(
                text = message.senderName,
                fontSize = 11.sp,
                color = ChatPalette.TextTertiary,
                modifier = Modifier.padding(start = 14.dp, bottom = 3.dp)
            )
        }

        Surface(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clickable(enabled = message.deliveryState == MessageDeliveryState.FAILED) { onRetry() },
            shape = shape,
            color = bubbleColor,
            border = if (message.deliveryState == MessageDeliveryState.FAILED) {
                BorderStroke(1.dp, ChatPalette.Danger.copy(alpha = 0.55f))
            } else {
                null
            }
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                SelectionContainer {
                    Text(
                        text = message.content,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        color = textColor
                    )
                }
                Spacer(Modifier.height(5.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.deliveryState == MessageDeliveryState.FAILED) {
                        Icon(
                            Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = ChatPalette.Danger,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Tap to retry", fontSize = 10.sp, color = ChatPalette.Danger)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = formatMessageTime(message.createdAt),
                        fontSize = 10.sp,
                        color = if (message.isFromMe) ChatPalette.OnOutgoingBubble.copy(alpha = 0.72f) else ChatPalette.TextTertiary
                    )
                    if (message.isFromMe) {
                        Spacer(Modifier.width(5.dp))
                        DeliveryIcon(message.deliveryState)
                    }
                }
            }
        }
        Spacer(Modifier.height(if (showTail) 6.dp else 2.dp))
    }
}

@Composable
private fun DeliveryIcon(state: MessageDeliveryState) {
    val (icon, tint) = when (state) {
        MessageDeliveryState.SENDING -> Icons.Filled.Schedule to ChatPalette.OnOutgoingBubble.copy(alpha = 0.62f)
        MessageDeliveryState.SENT -> Icons.Filled.Check to ChatPalette.OnOutgoingBubble.copy(alpha = 0.72f)
        MessageDeliveryState.READ -> Icons.Filled.DoneAll to ChatPalette.ReadReceipt
        MessageDeliveryState.FAILED -> Icons.Filled.ErrorOutline to ChatPalette.Danger
    }
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
}

@Composable
private fun ChatComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    connectionState: ChatConnectionState
) {
    val canSend = value.trim().isNotEmpty()

    Surface(
        color = ChatPalette.TopBar,
        border = BorderStroke(1.dp, ChatPalette.Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (connectionState != ChatConnectionState.CONNECTED) {
                Row(
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.WifiOff, contentDescription = null, tint = ChatPalette.Warning, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = connectionStatusLabel(connectionState, isTyping = false),
                        color = ChatPalette.Warning,
                        fontSize = 12.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message", color = ChatPalette.TextTertiary) },
                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = ChatPalette.TextPrimary),
                    minLines = 1,
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChatPalette.Accent,
                        unfocusedBorderColor = ChatPalette.Border,
                        focusedContainerColor = ChatPalette.Surface,
                        unfocusedContainerColor = ChatPalette.Surface,
                        cursorColor = ChatPalette.Accent
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                FilledIconButton(
                    onClick = onSend,
                    enabled = canSend,
                    modifier = Modifier
                        .size(50.dp)
                        .offset(y = (-1).dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = ChatPalette.Accent,
                        contentColor = ChatPalette.OnAccent,
                        disabledContainerColor = ChatPalette.SurfaceElevated,
                        disabledContentColor = ChatPalette.TextTertiary
                    ),
                    shape = CircleShape
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun BlockUserDialog(
    isLoading: Boolean,
    reason: String,
    onReasonChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Block user", color = ChatPalette.TextPrimary) },
        text = {
            Column {
                Text(
                    text = "You will stop receiving messages from this person.",
                    color = ChatPalette.TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = { Text("Reason optional") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isLoading) {
                Text("Block", color = ChatPalette.Danger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel", color = ChatPalette.TextSecondary)
            }
        },
        containerColor = ChatPalette.TopBar
    )
}

@Composable
private fun ReportUserDialog(
    isLoading: Boolean,
    reason: String,
    onReasonChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report user", color = ChatPalette.TextPrimary) },
        text = {
            Column {
                Text(
                    text = "Share what happened so the moderation team can review it.",
                    color = ChatPalette.TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = reason.isNotBlank() && !isLoading) {
                Text(
                    text = "Submit",
                    color = if (reason.isNotBlank()) ChatPalette.Danger else ChatPalette.TextTertiary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel", color = ChatPalette.TextSecondary)
            }
        },
        containerColor = ChatPalette.TopBar
    )
}

@Composable
private fun InitialsAvatar(name: String, size: androidx.compose.ui.unit.Dp) {
    val initials = remember(name) {
        name.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifBlank { "?" }
    }
    val colors = listOf(ChatPalette.AvatarBlue, ChatPalette.AvatarGreen, ChatPalette.AvatarRose, ChatPalette.AvatarGold)
    val color = colors[abs(name.hashCode()) % colors.size]

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = color,
            border = BorderStroke(1.dp, ChatPalette.Border)
        ) {}
        Text(initials, color = ChatPalette.OnAvatar, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PresenceDot(isOnline: Boolean, isTyping: Boolean) {
    val color = presenceStatusColor(isOnline, isTyping)
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

private fun presenceStatusLabel(isOnline: Boolean, lastSeenAt: String?, isTyping: Boolean): String {
    if (isTyping && isOnline) return "typing..."
    if (isOnline) return "Online"
    return lastSeenAt?.let { "Last seen ${formatPresenceRelativeTime(it)}" } ?: "Offline"
}

@Composable
private fun presenceStatusColor(isOnline: Boolean, isTyping: Boolean): Color {
    return when {
        isTyping && isOnline -> ChatPalette.Accent
        isOnline -> ChatPalette.Online
        else -> ChatPalette.TextTertiary
    }
}

private fun connectionStatusLabel(connectionState: ChatConnectionState, isTyping: Boolean): String {
    if (isTyping && connectionState == ChatConnectionState.CONNECTED) return "typing..."
    return when (connectionState) {
        ChatConnectionState.CONNECTED -> "Connected"
        ChatConnectionState.CONNECTING -> "Connecting"
        ChatConnectionState.RECONNECTING -> "Reconnecting"
        ChatConnectionState.DISCONNECTED -> "Disconnected"
    }
}

private fun formatMessageTime(isoTime: String): String {
    return try {
        val date = parseIsoDate(isoTime) ?: return ""
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    } catch (_: Exception) {
        ""
    }
}

private fun formatPresenceRelativeTime(isoTime: String): String {
    val seenAt = parseIsoDate(isoTime)?.time ?: return "recently"
    val diff = (System.currentTimeMillis() - seenAt).coerceAtLeast(0L)
    return when {
        diff < 60_000L -> "just now"
        diff < 3_600_000L -> "${diff / 60_000L}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000L}h ago"
        else -> "${diff / 86_400_000L}d ago"
    }
}

private fun formatDayLabel(isoTime: String): String {
    val date = parseIsoDate(isoTime) ?: return ""
    val messageCal = Calendar.getInstance().apply { time = date }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    return when {
        messageCal.isSameDay(today) -> "Today"
        messageCal.isSameDay(yesterday) -> "Yesterday"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
    }
}

private fun dateKey(isoTime: String): String {
    return isoTime.take(10)
}

private fun parseIsoDate(isoTime: String): Date? {
    return try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(isoTime.take(19))
    } catch (_: Exception) {
        null
    }
}

private fun Calendar.isSameDay(other: Calendar): Boolean {
    return get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
        get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
}

private object ChatPalette {
    val Background: Color
        @Composable get() = MaterialTheme.colorScheme.background

    val TopBar: Color
        @Composable get() = MaterialTheme.colorScheme.surface

    val Surface: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant

    val SurfaceElevated: Color
        @Composable get() = MaterialTheme.colorScheme.surface

    val IncomingBubble: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant

    val OutgoingBubble: Color
        @Composable get() = MaterialTheme.colorScheme.primary

    val OnOutgoingBubble: Color
        @Composable get() = MaterialTheme.colorScheme.onPrimary

    val Border: Color
        @Composable get() = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    val Accent: Color
        @Composable get() = MaterialTheme.colorScheme.primary

    val OnAccent: Color
        @Composable get() = MaterialTheme.colorScheme.onPrimary

    val Online: Color
        @Composable get() = HiBuddyColors.success

    val Warning: Color
        @Composable get() = HiBuddyColors.warning

    val Danger: Color
        @Composable get() = MaterialTheme.colorScheme.error

    val ReadReceipt: Color
        @Composable get() = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)

    val TextPrimary: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface

    val TextSecondary: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

    val TextTertiary: Color
        @Composable get() = HiBuddyColors.mutedContent

    val OnAvatar = Color.White
    val AvatarBlue = Color(0xFF2F5FDB)
    val AvatarGreen = Color(0xFF047857)
    val AvatarRose = Color(0xFFBE2B5B)
    val AvatarGold = Color(0xFF8A5A00)
}

private const val MESSAGE_CLUSTER_WINDOW_MS = 5 * 60 * 1000L
