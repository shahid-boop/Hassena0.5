package com.example.ui.features.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.services.memory.ChatMessageEntity
import com.example.ui.theme.AmberThinking
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.QuantumViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ChatStreamScreen(
    messages: List<ChatMessageEntity>,
    isGenerating: Boolean,
    onSendPrompt: (String) -> Unit,
    onVoiceMicTap: () -> Unit,
    onInterrupt: () -> Unit,
    onSpeakMessage: (String) -> Unit,
    onCopyMessage: (String) -> Unit,
    onShareMessage: (String) -> Unit,
    onClearHistory: () -> Unit,
    onNavigateBackHome: () -> Unit
) {
    BackHandler { onNavigateBackHome() }

    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Smooth scroll down when a new message is added or user submits a prompt
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            runCatching {
                listState.animateScrollToItem(messages.lastIndex)
            }
        }
    }

    // While streaming tokens, keep pinned to bottom if user is already near bottom without competing animations
    LaunchedEffect(messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty() && isGenerating) {
            val isAtBottom = !listState.canScrollForward ||
                (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= messages.lastIndex - 1
            if (isAtBottom) {
                runCatching {
                    listState.scrollToItem(messages.lastIndex)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Streaming Intelligence Log",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Real-time SSE token stream & local tool execution telemetry",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            if (messages.isNotEmpty()) {
                IconButton(
                    onClick = onClearHistory,
                    modifier = Modifier.testTag("clear_chat_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear conversation history",
                        tint = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Empty stream",
                        tint = CyberCyan,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Conversations Yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try a fast device command ('Turn on flashlight', 'Show battery', 'Open YouTube') or ask a complex question to see real-time streaming.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("chatlog")
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("chat_messages_list"),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageCard(
                            message = msg,
                            onSpeak = { onSpeakMessage(msg.content) },
                            onCopy = { onCopyMessage(msg.content) },
                            onShare = { onShareMessage(msg.content) },
                            onRetry = {
                                val lastUser = messages.lastOrNull { it.role == "user" }?.content
                                if (!lastUser.isNullOrBlank()) {
                                    onSendPrompt(lastUser)
                                }
                            }
                        )
                    }
                }

                // Quick Floating Scroll-To-Bottom FAB when user scrolls up
                androidx.compose.animation.AnimatedVisibility(
                    visible = listState.canScrollForward,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 8.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                runCatching {
                                    listState.animateScrollToItem(messages.lastIndex)
                                }
                            }
                        },
                        containerColor = ObsidianSurfaceElevated,
                        contentColor = CyberCyan,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                        modifier = Modifier
                            .size(40.dp)
                            .border(1.dp, CyberCyan.copy(alpha = 0.5f), CircleShape)
                            .testTag("scroll_to_bottom_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Scroll to bottom",
                            tint = CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        if (isGenerating) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Streaming tokens live...",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmberThinking
                )
                Button(
                    onClick = onInterrupt,
                    colors = ButtonDefaults.buttonColors(containerColor = CoralAlert),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.StopCircle,
                        contentDescription = "Cancel request",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop Stream", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Bottom input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text("Message JARVIS...", color = TextMuted)
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = BorderSubtle,
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank()) {
                            onSendPrompt(inputText)
                            inputText = ""
                        }
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_prompt_input")
            )

            IconButton(
                onClick = onVoiceMicTap,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ObsidianSurfaceElevated)
                    .border(1.dp, BorderSubtle, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice input",
                    tint = CyberCyan
                )
            }

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendPrompt(inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CyberCyan)
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send prompt",
                    tint = Color.Black
                )
            }
        }
    }
}

@Composable
private fun ChatMessageCard(
    message: ChatMessageEntity,
    onSpeak: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onRetry: () -> Unit
) {
    val isUser = message.role == "user"
    val containerColor = when {
        message.isError -> CoralAlert.copy(alpha = 0.12f)
        isUser -> QuantumViolet.copy(alpha = 0.22f)
        else -> ObsidianSurface
    }
    val borderColor = when {
        message.isError -> CoralAlert.copy(alpha = 0.6f)
        isUser -> QuantumViolet.copy(alpha = 0.5f)
        message.isStreaming -> CyberCyan
        else -> BorderSubtle
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = containerColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            modifier = Modifier.fillMaxWidth(if (isUser) 0.86f else 0.96f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Route & Latency header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "YOU • ${message.routeCategory}" else "${message.routeCategory} • ${message.modelName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) TextSecondary else CyberCyan,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (!isUser && message.latencyMs > 0L) {
                        Text(
                            text = "${message.latencyMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldReady
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )

                if (!message.citationsSummary.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianSurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "VERIFIED SOURCES",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message.citationsSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }

                if (message.isError && !message.recoveryHint.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Recovery hint",
                                tint = AmberThinking,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = message.recoveryHint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AmberThinking
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Retry", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                if (!isUser) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onSpeak, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak message",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy message",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share message",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
