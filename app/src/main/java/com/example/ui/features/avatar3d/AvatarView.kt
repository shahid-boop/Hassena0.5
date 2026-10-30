package com.example.ui.features.avatar3d

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AvatarView: Unified, modular Jetpack Compose 3D component hosting the HASASENA A1
 * female virtual assistant. Supports multiple presentation modes (Full Workstation,
 * Focused Portrait, Card Preview, and Floating Audio-Reactive PIP Bubble).
 */
@Composable
fun AvatarView(
    modifier: Modifier = Modifier,
    state: AvatarWorkstationState = AvatarWorkstationState(),
    mode: AvatarViewMode = AvatarViewMode.WORKSTATION,
    showStatusBadge: Boolean = true,
    onAvatarReady: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    // Map effective camera preset according to view mode
    val effectivePreset = remember(mode, state.cameraPreset) {
        when (mode) {
            AvatarViewMode.PORTRAIT, AvatarViewMode.PIP_BUBBLE -> CameraPreset.CLOSEUP_PORTRAIT
            AvatarViewMode.CARD -> CameraPreset.WORKSTATION
            AvatarViewMode.WORKSTATION -> state.cameraPreset
        }
    }

    val effectiveState = remember(state, effectivePreset) {
        state.copy(cameraPreset = effectivePreset)
    }

    // Audio-reactive border pulse for PIP / Card modes
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val isSpeakingOrListening = state.activity == AvatarActivity.SPEAKING ||
            state.activity == AvatarActivity.LISTENING ||
            state.audioLevel > 0.05f

    when (mode) {
        AvatarViewMode.WORKSTATION -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .testTag("avatar_view_workstation")
            ) {
                Avatar3DWorkstationView(
                    state = effectiveState,
                    onReady = { onAvatarReady?.invoke() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        AvatarViewMode.PORTRAIT -> {
            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF00F0FF).copy(alpha = 0.35f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .background(Color(0xFF050914))
                    .testTag("avatar_view_portrait")
            ) {
                Avatar3DWorkstationView(
                    state = effectiveState,
                    onReady = { onAvatarReady?.invoke() },
                    modifier = Modifier.fillMaxSize()
                )

                if (showStatusBadge) {
                    AvatarStatusBadge(
                        activity = state.activity,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    )
                }
            }
        }

        AvatarViewMode.CARD -> {
            val shape = RoundedCornerShape(16.dp)
            Box(
                modifier = modifier
                    .clip(shape)
                    .shadow(elevation = 8.dp, shape = shape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0A1428), Color(0xFF050B17))
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFF00F0FF).copy(alpha = if (isSpeakingOrListening) 0.8f else 0.4f),
                                Color(0xFFC752FF).copy(alpha = 0.3f)
                            )
                        ),
                        shape = shape
                    )
                    .then(
                        if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
                    )
                    .testTag("avatar_view_card")
            ) {
                Avatar3DWorkstationView(
                    state = effectiveState,
                    onReady = { onAvatarReady?.invoke() },
                    modifier = Modifier.fillMaxSize()
                )

                if (showStatusBadge) {
                    AvatarStatusBadge(
                        activity = state.activity,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    )
                }
            }
        }

        AvatarViewMode.PIP_BUBBLE -> {
            val bubbleShape = CircleShape
            val glowColor = if (isSpeakingOrListening) {
                Color(0xFF00F0FF).copy(alpha = (0.5f * pulseScale).coerceIn(0.3f, 0.9f))
            } else {
                Color(0xFF00F0FF).copy(alpha = 0.3f)
            }

            Box(
                modifier = modifier
                    .size(96.dp)
                    .clip(bubbleShape)
                    .shadow(elevation = 10.dp, shape = bubbleShape)
                    .border(
                        width = if (isSpeakingOrListening) 2.5.dp else 1.5.dp,
                        color = glowColor,
                        shape = bubbleShape
                    )
                    .background(Color(0xFF050914))
                    .then(
                        if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
                    )
                    .testTag("avatar_view_pip")
            ) {
                Avatar3DWorkstationView(
                    state = effectiveState,
                    onReady = { onAvatarReady?.invoke() },
                    modifier = Modifier.fillMaxSize()
                )

                if (isSpeakingOrListening) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                            .background(Color(0xDD081428), RoundedCornerShape(6.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Active voice",
                            tint = Color(0xFF00F0FF),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Convenient overload accepting individual avatar parameters directly.
 */
@Composable
fun AvatarView(
    activity: AvatarActivity,
    audioLevel: Float = 0f,
    currentTaskTitle: String = "Ready for Command",
    currentTaskProgress: Float = 0f,
    activeCodeSnippet: String = "",
    cameraPreset: CameraPreset = CameraPreset.WORKSTATION,
    avatarStyle: AvatarStyle = AvatarStyle.EXECUTIVE_NAVY,
    mode: AvatarViewMode = AvatarViewMode.WORKSTATION,
    showStatusBadge: Boolean = true,
    onAvatarReady: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AvatarView(
        modifier = modifier,
        state = AvatarWorkstationState(
            activity = activity,
            audioLevel = audioLevel,
            currentTaskTitle = currentTaskTitle,
            currentTaskProgress = currentTaskProgress,
            activeCodeSnippet = activeCodeSnippet,
            cameraPreset = cameraPreset,
            avatarStyle = avatarStyle
        ),
        mode = mode,
        showStatusBadge = showStatusBadge,
        onAvatarReady = onAvatarReady,
        onClick = onClick
    )
}

@Composable
private fun AvatarStatusBadge(
    activity: AvatarActivity,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color(0xCC051020), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = null,
            tint = Color(0xFF00F0FF),
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = activity.displayLabel,
            color = Color(0xFFE0F4FF),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
