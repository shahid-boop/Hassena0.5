package com.example.ui.features.voice

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.OrbConfig
import com.example.models.OrbState
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanDim
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.ObsidianSurfaceGlass
import com.example.ui.theme.QuantumViolet
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.widgets.orb.HasseenaOrb
import kotlin.math.sin

@Composable
fun DedicatedVoiceScreen(
    orbState: OrbState,
    orbConfig: OrbConfig,
    audioLevel: Float,
    partialTranscript: String,
    liveHeadline: String,
    onTapOrb: () -> Unit,
    onInterrupt: () -> Unit,
    onNavigateBack: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(16.dp)
            .testTag("dedicated_voice_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(ObsidianSurfaceGlass, CircleShape)
                    .border(1.dp, BorderSubtle, CircleShape)
                    .testTag("voice_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(CyberCyan.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "JARVIS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Voice Assistant",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(40.dp)
                    .background(ObsidianSurfaceGlass, CircleShape)
                    .border(1.dp, BorderSubtle, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs: "Voice" vs "Text"
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = ObsidianSurface,
            contentColor = CyberCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CyberCyan
                )
            },
            modifier = Modifier
                .width(220.dp)
                .clip(RoundedCornerShape(50))
                .border(1.dp, BorderSubtle, RoundedCornerShape(50))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Voice", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    selectedTab = 1
                    onNavigateToChat()
                },
                text = { Text("Text", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Large Glowing Arc Reactor Pulse Visualizer (Matching Screen 4)
        Box(
            modifier = Modifier
                .size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            // Concentric glowing rings
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension / 2f

                // Outer faint ring
                drawCircle(
                    color = CyberCyan.copy(alpha = 0.15f * pulseScale),
                    radius = baseRadius * 0.95f,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Middle glowing ring
                drawCircle(
                    color = CyberCyan.copy(alpha = 0.35f * pulseScale),
                    radius = baseRadius * 0.78f,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Inner core ring
                drawCircle(
                    color = CyberCyan.copy(alpha = 0.6f),
                    radius = baseRadius * 0.60f,
                    style = Stroke(width = 4.dp.toPx())
                )

                // High-frequency radial tick marks
                val ticks = 36
                for (i in 0 until ticks) {
                    val angle = Math.toRadians((i * (360.0 / ticks)))
                    val r1 = baseRadius * 0.68f
                    val r2 = baseRadius * 0.74f
                    val start = Offset(center.x + (r1 * sin(angle)).toFloat(), center.y + (r1 * kotlin.math.cos(angle)).toFloat())
                    val end = Offset(center.x + (r2 * sin(angle)).toFloat(), center.y + (r2 * kotlin.math.cos(angle)).toFloat())
                    drawLine(
                        color = CyberCyan.copy(alpha = 0.4f),
                        start = start,
                        end = end,
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            // Core Orb
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                CyberCyan,
                                CyberCyanDim,
                                Color(0xFF4A000E)
                            )
                        )
                    )
                    .border(2.dp, CyberCyan, CircleShape)
                    .clickable { onTapOrb() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Microphone",
                    tint = Color.White,
                    modifier = Modifier.size(46.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // State indicator text
        val statusTitle = when (orbState) {
            OrbState.LISTENING -> "Listening..."
            OrbState.THINKING -> "Processing..."
            OrbState.SPEAKING -> "Speaking..."
            OrbState.EXECUTING -> "Executing Action..."
            OrbState.ERROR -> "Action Blocked"
            else -> "Ready"
        }
        val statusSubtitle = when (orbState) {
            OrbState.LISTENING -> "Speak now or say \"Hey JARVIS\""
            OrbState.THINKING -> "Routing intelligence..."
            OrbState.SPEAKING -> "Tap microphone or speak to interrupt"
            OrbState.EXECUTING -> "Applying system changes..."
            else -> "Tap microphone to begin"
        }

        Text(
            text = statusTitle,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = statusSubtitle,
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Audio Waveform Visualizer
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val bars = 24
                val barWidth = 4.dp.toPx()
                val spacing = (size.width - (bars * barWidth)) / (bars - 1)

                for (i in 0 until bars) {
                    val progress = i.toFloat() / bars
                    val multiplier = if (orbState == OrbState.LISTENING || orbState == OrbState.SPEAKING) {
                        (0.3f + 0.7f * sin(progress * Math.PI).toFloat()) * (0.4f + audioLevel * 1.5f).coerceIn(0.2f, 1f)
                    } else {
                        0.15f
                    }
                    val barHeight = size.height * multiplier
                    val x = i * (barWidth + spacing)
                    val y = (size.height - barHeight) / 2f

                    drawRoundRect(
                        color = CyberCyan.copy(alpha = 0.85f),
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live transcript or spoken headline
        if (partialTranscript.isNotBlank() || liveHeadline.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (partialTranscript.isNotBlank()) {
                        Text(
                            text = "“$partialTranscript”",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldReady
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (liveHeadline.isNotBlank()) {
                        Text(
                            text = liveHeadline,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            maxLines = 4
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action Buttons Row: Cancel, Interrupt
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onInterrupt,
                shape = RoundedCornerShape(50),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                modifier = Modifier.height(44.dp)
            ) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = CoralAlert)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = onTapOrb,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = Color.Black
                ),
                modifier = Modifier.height(44.dp)
            ) {
                Icon(Icons.Default.Mic, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (orbState == OrbState.LISTENING) "Stop" else "Listen", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
