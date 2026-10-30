package com.example.ui.widgets.orb

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.models.OrbConfig
import com.example.models.OrbState
import com.example.ui.theme.AmberThinking
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.EmeraldReady
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HasseenaOrb(
    orbState: OrbState,
    orbConfig: OrbConfig,
    audioLevel: Float,
    onTapOrb: () -> Unit,
    onLongPressOrb: () -> Unit,
    modifier: Modifier = Modifier
) {
    val speedMultiplier = orbConfig.animationSpeed.coerceIn(0.4f, 2.5f)
    val stateSpeedFactor = when (orbState) {
        OrbState.IDLE -> 1.0f
        OrbState.LISTENING -> 1.6f
        OrbState.THINKING -> 2.4f
        OrbState.SPEAKING -> 1.8f
        OrbState.EXECUTING -> 2.8f
        OrbState.ERROR -> 0.8f
    } * speedMultiplier

    val targetPrimaryColor = when (orbState) {
        OrbState.IDLE -> orbConfig.colorPreset.primaryColor
        OrbState.LISTENING -> EmeraldReady
        OrbState.THINKING -> AmberThinking
        OrbState.SPEAKING -> orbConfig.colorPreset.primaryColor
        OrbState.EXECUTING -> orbConfig.colorPreset.accentColor
        OrbState.ERROR -> CoralAlert
    }

    val targetSecondaryColor = when (orbState) {
        OrbState.IDLE -> orbConfig.colorPreset.secondaryColor
        OrbState.LISTENING -> orbConfig.colorPreset.primaryColor
        OrbState.THINKING -> orbConfig.colorPreset.secondaryColor
        OrbState.SPEAKING -> orbConfig.colorPreset.accentColor
        OrbState.EXECUTING -> EmeraldReady
        OrbState.ERROR -> Color(0xFFFF8A80)
    }

    val primaryColor by animateColorAsState(
        targetValue = targetPrimaryColor,
        animationSpec = tween(durationMillis = 260),
        label = "orb_primary_color"
    )
    val secondaryColor by animateColorAsState(
        targetValue = targetSecondaryColor,
        animationSpec = tween(durationMillis = 260),
        label = "orb_secondary_color"
    )

    val smoothedAudio by animateFloatAsState(
        targetValue = audioLevel.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 80),
        label = "orb_audio_level"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "orb_infinite_transition")

    val baseRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (6500 / speedMultiplier).toInt().coerceAtLeast(1200),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rotation"
    )

    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (4800 / speedMultiplier).toInt().coerceAtLeast(900),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_counter_rotation"
    )

    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (1400 / stateSpeedFactor.coerceAtLeast(0.5f)).toInt().coerceAtLeast(320),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_wave_phase"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(orbConfig.sizeDp.dp)
            .testTag("jarvis_orb")
            .semantics {
                contentDescription = "JARVIS AI Orb (${orbState.label}). ${orbState.statusSubtitle}"
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTapOrb,
                onLongClick = onLongPressOrb
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val canvasMin = min(size.width, size.height)
            val center = Offset(size.width / 2f, size.height / 2f)
            val dynamicScale = pulsePhase + (smoothedAudio * 0.16f)
            val baseRadius = (canvasMin * 0.25f) * dynamicScale

            // 1. Deep Atmospheric Outer Halo Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.34f + smoothedAudio * 0.22f),
                        secondaryColor.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = (canvasMin * 0.49f).coerceAtLeast(10f)
                ),
                radius = canvasMin * 0.49f,
                center = center
            )

            // 2. Rotating Orbital Energy Rings
            if (orbConfig.showRings) {
                val ringRadius1 = baseRadius * 1.48f
                val ringRadius2 = baseRadius * 1.28f

                rotate(degrees = baseRotation * stateSpeedFactor * 0.5f, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                primaryColor.copy(alpha = 0.85f),
                                secondaryColor.copy(alpha = 0.9f),
                                Color.Transparent
                            ),
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(center.x - ringRadius1, center.y - ringRadius1 * 0.78f),
                        size = Size(ringRadius1 * 2f, ringRadius1 * 1.56f),
                        style = Stroke(width = canvasMin * 0.014f, cap = StrokeCap.Round)
                    )
                }

                rotate(degrees = counterRotation * stateSpeedFactor * 0.6f, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                secondaryColor.copy(alpha = 0.85f),
                                Color.Transparent,
                                primaryColor.copy(alpha = 0.85f),
                                Color.Transparent
                            ),
                            center = center
                        ),
                        startAngle = 45f,
                        sweepAngle = 240f,
                        useCenter = false,
                        topLeft = Offset(center.x - ringRadius2 * 0.82f, center.y - ringRadius2),
                        size = Size(ringRadius2 * 1.64f, ringRadius2 * 2f),
                        style = Stroke(width = canvasMin * 0.012f, cap = StrokeCap.Round)
                    )
                }

                // Third precision ring during THINKING or EXECUTING
                if (orbState == OrbState.THINKING || orbState == OrbState.EXECUTING) {
                    val execRingRadius = baseRadius * 1.62f
                    rotate(degrees = baseRotation * 2.2f, pivot = center) {
                        for (seg in 0 until 8) {
                            drawArc(
                                color = primaryColor.copy(alpha = 0.75f),
                                startAngle = seg * 45f,
                                sweepAngle = 22f,
                                useCenter = false,
                                topLeft = Offset(center.x - execRingRadius, center.y - execRingRadius),
                                size = Size(execRingRadius * 2f, execRingRadius * 2f),
                                style = Stroke(width = canvasMin * 0.01f, cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }

            // 3. Audio-Reactive Waveform Bars (Active during LISTENING or SPEAKING)
            if (orbState == OrbState.LISTENING || orbState == OrbState.SPEAKING || smoothedAudio > 0.08f) {
                val barCount = 32
                val innerWaveRadius = baseRadius * 1.12f
                val activeAmplitude = if (orbState == OrbState.SPEAKING) {
                    0.45f + 0.35f * sin(wavePhase * 2f)
                } else {
                    smoothedAudio.coerceAtLeast(0.18f)
                }

                for (i in 0 until barCount) {
                    val angleRad = (i.toFloat() / barCount) * 2f * PI.toFloat() + (wavePhase * 0.3f)
                    val harmonic = kotlin.math.abs(sin(angleRad * 3f + wavePhase))
                    val barLen = canvasMin * (0.025f + 0.095f * activeAmplitude * harmonic)
                    val startX = center.x + cos(angleRad) * innerWaveRadius
                    val startY = center.y + sin(angleRad) * innerWaveRadius
                    val endX = center.x + cos(angleRad) * (innerWaveRadius + barLen)
                    val endY = center.y + sin(angleRad) * (innerWaveRadius + barLen)

                    drawLine(
                        color = primaryColor.copy(alpha = 0.78f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = canvasMin * 0.011f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 4. Orbiting Quantum Particles
            if (orbConfig.showParticles) {
                val particleCount = 14
                for (i in 0 until particleCount) {
                    val angleDeg = (i * (360f / particleCount)) + (baseRotation * (if (i % 2 == 0) 1.2f else -0.9f))
                    val angleRad = angleDeg * (PI.toFloat() / 180f)
                    val orbitDist = baseRadius * (1.18f + (i % 4) * 0.14f)
                    val px = center.x + cos(angleRad) * orbitDist
                    val py = center.y + sin(angleRad) * orbitDist
                    val pRadius = canvasMin * (if (i % 3 == 0) 0.015f else 0.009f)

                    drawCircle(
                        color = if (i % 2 == 0) primaryColor else secondaryColor,
                        radius = pRadius,
                        center = Offset(px, py)
                    )
                }
            }

            // 5. Core Luminous Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        primaryColor,
                        secondaryColor.copy(alpha = 0.9f),
                        Color(0xFF050814)
                    ),
                    center = Offset(center.x - baseRadius * 0.22f, center.y - baseRadius * 0.22f),
                    radius = (baseRadius * 1.15f).coerceAtLeast(10f)
                ),
                radius = baseRadius,
                center = center
            )

            // 6. Specular Inner Highlight Rim
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = baseRadius * 0.92f,
                center = center,
                style = Stroke(width = canvasMin * 0.007f)
            )

            // 7. Offset glass highlight and lower hemisphere shade create stronger depth cues.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.72f),
                        Color(0xFFB8F8FF).copy(alpha = 0.24f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - baseRadius * 0.34f, center.y - baseRadius * 0.42f),
                    radius = (baseRadius * 0.62f).coerceAtLeast(8f)
                ),
                radius = baseRadius * 0.62f,
                center = Offset(center.x - baseRadius * 0.20f, center.y - baseRadius * 0.24f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color(0xFF020611).copy(alpha = 0.62f)),
                    center = Offset(center.x + baseRadius * 0.28f, center.y + baseRadius * 0.36f),
                    radius = (baseRadius * 1.1f).coerceAtLeast(10f)
                ),
                radius = baseRadius * 0.98f,
                center = center
            )
        }
    }
}
