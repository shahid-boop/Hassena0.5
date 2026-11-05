package com.example.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D LIQUID GLASS DESIGN SYSTEM
 *
 * Provides realistic translucent glass surfaces, specular light refractions,
 * glowing multi-spectrum borders, floating glass cards, and fluid 3D animated backdrops.
 */
object LiquidGlassTokens {

    val CardCornerRadius: Dp = 20.dp
    val DialogCornerRadius: Dp = 24.dp
    val ButtonCornerRadius: Dp = 14.dp
    val PillCornerRadius: Dp = 50.dp

    // Multi-layered glass surface gradient (translucent depth)
    val GlassSurfaceBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0x73111C35),
            Color(0x4D0B1220),
            Color(0x59080F1E)
        )
    )

    // Elevated glass panel gradient (for dialogs, focused cards)
    val GlassElevatedBrush: Brush = Brush.linearGradient(
        colors = listOf(
            Color(0x8C162545),
            Color(0x660F1A30),
            Color(0x8013203D)
        ),
        start = Offset(0f, 0f),
        end = Offset(400f, 600f)
    )

    // Specular light sheen gradient (simulating top-down directional lighting)
    val SpecularSheenBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0x40FFFFFF),
            Color(0x10FFFFFF),
            Color(0x02FFFFFF)
        )
    )

    // Glowing liquid rim brush (Electric blue into violet neon)
    val GlassRimBrush: Brush = Brush.linearGradient(
        colors = listOf(
            Color(0x9938BDF8),
            Color(0x40FFFFFF),
            Color(0x80A855F7),
            Color(0x6638BDF8)
        )
    )

    // Subtle glass border
    val GlassSubtleBorderBrush: Brush = Brush.linearGradient(
        colors = listOf(
            Color(0x5538BDF8),
            Color(0x22FFFFFF),
            Color(0x448B5CF6)
        )
    )

    // Interactive button glass gradient
    val GlassButtonBrush: Brush = Brush.linearGradient(
        colors = listOf(
            Color(0x4D00F0FF),
            Color(0x2638BDF8),
            Color(0x33A855F7)
        )
    )

    // Accent glow brush (Electric Cyan into Deep Violet)
    val AccentElectricGradient: Brush = Brush.horizontalGradient(
        colors = listOf(
            CyberCyan,
            QuantumViolet
        )
    )
}

/**
 * Modifier extension to apply the 3D Liquid Glass effect to any component:
 * - 3D Ambient shadow
 * - Translucent glass background with light refraction
 * - Specular highlight sheen
 * - Angled glowing rim
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(LiquidGlassTokens.CardCornerRadius),
    cornerRadius: Dp? = null,
    accentGlow: Color? = null,
    borderAlpha: Float = 0.35f,
    elevation: Dp = 10.dp,
    borderBrush: Brush? = null,
    borderWidth: Dp = 1.dp,
    surfaceBrush: Brush = LiquidGlassTokens.GlassSurfaceBrush,
    spotGlowColor: Color = CyberCyan.copy(alpha = 0.35f)
): Modifier {
    val resolvedShape = if (cornerRadius != null) RoundedCornerShape(cornerRadius) else shape
    val resolvedBorder = borderBrush ?: if (accentGlow != null) {
        Brush.linearGradient(
            listOf(
                accentGlow.copy(alpha = borderAlpha),
                accentGlow.copy(alpha = (borderAlpha * 0.4f).coerceAtLeast(0.08f)),
                Color(0x33A855F7)
            )
        )
    } else {
        LiquidGlassTokens.GlassRimBrush
    }
    val resolvedSpot = if (accentGlow != null) accentGlow.copy(alpha = 0.35f) else spotGlowColor
    return this
        .shadow(
            elevation = elevation,
            shape = resolvedShape,
            ambientColor = Color(0x3300F0FF),
            spotColor = resolvedSpot
        )
        .clip(resolvedShape)
        .background(surfaceBrush)
        .drawWithContent {
            drawContent()
            // Draw specular liquid highlight on top border area
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height * 0.35f
                )
            )
        }
        .border(
            width = borderWidth,
            brush = resolvedBorder,
            shape = resolvedShape
        )
}

/**
 * Reusable 3D Liquid Glass Card
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    cornerRadius: Dp = LiquidGlassTokens.CardCornerRadius,
    accentColor: Color? = null,
    elevation: Dp = 8.dp,
    borderBrush: Brush? = null,
    borderWidth: Dp = 1.dp,
    surfaceBrush: Brush = LiquidGlassTokens.GlassSurfaceBrush,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val resolvedShape = shape ?: RoundedCornerShape(cornerRadius)
    val resolvedBorder = borderBrush ?: if (accentColor != null) {
        Brush.linearGradient(
            listOf(
                accentColor.copy(alpha = 0.65f),
                accentColor.copy(alpha = 0.25f),
                Color(0x33A855F7)
            )
        )
    } else {
        LiquidGlassTokens.GlassSubtleBorderBrush
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1.0f,
        animationSpec = spring(),
        label = "glass_card_press_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .liquidGlass(
                shape = resolvedShape,
                elevation = if (isPressed) elevation / 2 else elevation,
                borderBrush = resolvedBorder,
                borderWidth = borderWidth,
                surfaceBrush = surfaceBrush,
                spotGlowColor = accentColor?.copy(alpha = 0.35f) ?: CyberCyan.copy(alpha = 0.3f)
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        content = content
    )
}

/**
 * Reusable 3D Liquid Glass Interactive Button
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(LiquidGlassTokens.ButtonCornerRadius),
    surfaceBrush: Brush = LiquidGlassTokens.GlassButtonBrush,
    borderBrush: Brush = LiquidGlassTokens.GlassRimBrush,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1.0f,
        animationSpec = spring(),
        label = "glass_button_press_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 4.dp else 8.dp,
                shape = shape,
                ambientColor = CyberCyan.copy(alpha = 0.4f),
                spotColor = QuantumViolet.copy(alpha = 0.5f)
            )
            .clip(shape)
            .then(
                if (enabled) Modifier.background(surfaceBrush)
                else Modifier.background(Color(0x331E293B))
            )
            .drawWithContent {
                drawContent()
                if (enabled) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.5f
                        )
                    )
                }
            }
            .border(
                width = 1.2.dp,
                brush = if (enabled) borderBrush else Brush.linearGradient(listOf(Color(0x3364748B), Color(0x3364748B))),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/**
 * Reusable Liquid Glass Status / Filter Pill
 */
@Composable
fun LiquidGlassPill(
    text: String = "",
    label: String = text,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    leadingDotColor: Color? = null,
    icon: ImageVector? = null,
    accentColor: Color? = null,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(LiquidGlassTokens.PillCornerRadius)
    val pillText = if (label.isNotBlank()) label else text
    val activeAccent = accentColor ?: CyberCyan
    val backgroundBrush = if (isSelected) {
        Brush.horizontalGradient(
            listOf(activeAccent.copy(alpha = 0.30f), QuantumViolet.copy(alpha = 0.30f))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0x40162545), Color(0x2A0B1325))
        )
    }
    val borderBrush = if (isSelected) {
        Brush.linearGradient(
            listOf(activeAccent.copy(alpha = 0.8f), QuantumViolet.copy(alpha = 0.6f))
        )
    } else {
        Brush.linearGradient(listOf(Color(0x3338BDF8), Color(0x22A855F7)))
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isSelected) 6.dp else 2.dp,
                shape = shape,
                spotColor = if (isSelected) activeAccent.copy(alpha = 0.4f) else Color.Transparent
            )
            .clip(shape)
            .background(backgroundBrush)
            .border(1.dp, borderBrush, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) activeAccent else TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            } else if (leadingDotColor != null) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(leadingDotColor, CircleShape)
                        .shadow(4.dp, CircleShape, spotColor = leadingDotColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = pillText,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) activeAccent else TextSecondary
            )
        }
    }
}

/**
 * 3D ANIMATED LIQUID GLASS BACKDROP
 *
 * Renders an animated futuristic deep space environment with orbiting
 * chromatic liquid orbs (Electric Cyan, Violet, Neon Azure) that pass behind
 * the translucent glass panes, creating liquid refraction, caustics, and real depth.
 */
@Composable
fun LiquidGlassBackdrop(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_glass_backdrop_transition")

    // Harmonic orbit phase animations
    val orb1Phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "liquid_orb1_phase"
    )

    val orb2Phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 32000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "liquid_orb2_phase"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liquid_pulse_glow"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Deep Space Void Base
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF030712),
                    Color(0xFF080F20),
                    Color(0xFF050914)
                )
            )
        )

        // 2. Liquid Chromatic Orb 1 — Electric Cyan (Upper Floating Caustic)
        val orb1X = (w * 0.25f) + (cos(orb1Phase) * w * 0.18f)
        val orb1Y = (h * 0.22f) + (sin(orb1Phase) * h * 0.10f)
        val orb1Radius = (w * 0.65f) * pulseGlow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x3800F0FF),
                    Color(0x180284C7),
                    Color.Transparent
                ),
                center = Offset(orb1X, orb1Y),
                radius = orb1Radius
            ),
            center = Offset(orb1X, orb1Y),
            radius = orb1Radius
        )

        // 3. Liquid Chromatic Orb 2 — Deep Quantum Violet (Lower Center Caustic)
        val orb2X = (w * 0.78f) - (cos(orb2Phase) * w * 0.20f)
        val orb2Y = (h * 0.70f) + (sin(orb2Phase) * h * 0.12f)
        val orb2Radius = (w * 0.72f) * (2f - pulseGlow)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x33A855F7),
                    Color(0x1A4F46E5),
                    Color.Transparent
                ),
                center = Offset(orb2X, orb2Y),
                radius = orb2Radius
            ),
            center = Offset(orb2X, orb2Y),
            radius = orb2Radius
        )

        // 4. Subtle Liquid Caustic Orb 3 — Neon Ice (Center-Right Specular Glow)
        val orb3X = w * 0.85f
        val orb3Y = h * 0.35f
        val orb3Radius = w * 0.45f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x2238BDF8),
                    Color.Transparent
                ),
                center = Offset(orb3X, orb3Y),
                radius = orb3Radius
            ),
            center = Offset(orb3X, orb3Y),
            radius = orb3Radius
        )

        // 5. 3D Perspective Depth Grid (Subtle luminous cyan grid lines)
        val gridColor = Color(0x1238BDF8)
        val gridGap = 36.dp.toPx()
        val horizonY = h * 0.58f

        // Horizontal perspective lines with distance compression
        var currentY = horizonY
        var step = 10.dp.toPx()
        while (currentY <= h) {
            drawLine(
                color = gridColor,
                start = Offset(0f, currentY),
                end = Offset(w, currentY),
                strokeWidth = 0.8.dp.toPx()
            )
            currentY += step
            step *= 1.15f
        }

        // Perspective vertical rays radiating towards the horizon vanishing point
        val vp = Offset(w * 0.5f, horizonY)
        var x = 0f
        while (x <= w) {
            drawLine(
                color = gridColor,
                start = vp,
                end = Offset(x, h),
                strokeWidth = 0.8.dp.toPx()
            )
            x += gridGap
        }

        // 6. Holographic Orbit Rings with liquid glass refraction caustics
        val orbitCenter = Offset(w * 0.86f, h * 0.12f)
        listOf(48f, 68f, 92f).forEachIndexed { index, radiusDp ->
            val ringColor = when (index) {
                0 -> Color(0x3300F0FF)
                1 -> Color(0x24A855F7)
                else -> Color(0x1A38BDF8)
            }
            drawCircle(
                color = ringColor,
                radius = radiusDp.dp.toPx(),
                center = orbitCenter,
                style = Stroke(
                    width = if (index == 0) 1.2.dp.toPx() else 0.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
        drawCircle(
            color = Color(0x9900F0FF),
            radius = 2.5.dp.toPx(),
            center = orbitCenter
        )
    }
}
