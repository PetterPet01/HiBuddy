package com.example.hibuddy.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    matchId: String,
    userName: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel(
        key = "chat_$matchId",
        factory = ChatViewModel.factory(matchId, userName)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose { viewModel.cleanup() }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D14))
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(userName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF0EFF8))
                    if (uiState.isConnected) {
                        Text("Online", fontSize = 11.sp, color = Color(0xFF4CAF50))
                    }
                    if (uiState.isTyping) {
                        Text("typing...", fontSize = 11.sp, color = Color(0xFF7C6AF7))
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    viewModel.cleanup()
                    onBack()
                }) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = Color(0xFFF0EFF8))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF13131F))
        )

        if (uiState.isLoading && uiState.messages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF7C6AF7))
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            state = listState,
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (uiState.messages.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No messages yet. Say hello!", fontSize = 14.sp, color = Color(0xFF6B6A8C))
                    }
                }
            } else {
                items(uiState.messages) { message ->
                    ChatBubble(message = message)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF13131F))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    viewModel.sendTyping()
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...", color = Color(0xFF6B6A8C)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color(0xFF1E1D2E),
                    unfocusedContainerColor = Color(0xFF1E1D2E)
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText.trim())
                        inputText = ""
                    }
                },
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF7C6AF7)),
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Send, "Send", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }
}

@Composable
fun ChatBubble(message: MessageItem) {
    val alignment = if (message.isFromMe) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isFromMe) Color(0xFF7C6AF7) else Color(0xFF1E1D2E)
    val textColor = if (message.isFromMe) Color.White else Color(0xFFF0EFF8)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!message.isFromMe && message.senderName.isNotBlank()) {
            Text(
                message.senderName,
                fontSize = 11.sp,
                color = Color(0xFF8B8AAC),
                modifier = Modifier.padding(start = 16.dp, bottom = 2.dp)
            )
        }
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(message.content, fontSize = 15.sp, color = textColor, lineHeight = 20.sp)
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        formatTime(message.createdAt),
                        fontSize = 10.sp,
                        color = if (message.isFromMe) Color.White.copy(alpha = 0.6f) else Color(0xFF6B6A8C)
                    )
                }
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}

private fun formatTime(isoTime: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        val date = sdf.parse(isoTime.take(19)) ?: return ""
        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(date)
    } catch (e: Exception) {
        ""
    }
}
