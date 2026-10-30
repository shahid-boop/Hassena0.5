package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HasseenaDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF00262C),
    primaryContainer = Color(0xFF00424C),
    onPrimaryContainer = CyberCyan,
    secondary = QuantumViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF281A54),
    onSecondaryContainer = QuantumVioletLight,
    tertiary = EmeraldReady,
    onTertiary = Color(0xFF002912),
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = CoralAlert,
    onError = Color.White
)

@Composable
fun HasseenaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HasseenaDarkColorScheme,
        typography = Typography,
        content = content
    )
}
