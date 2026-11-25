package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = AccentGreen,
    onPrimary = Color(0xFF041409),
    primaryContainer = Color(0xFF143824),
    onPrimaryContainer = AccentGreenBright,
    secondary = SpatialCyan,
    onSecondary = Color(0xFF041A22),
    secondaryContainer = SpatialPanelAlt,
    onSecondaryContainer = SpatialGreen,
    tertiary = SpatialGreen,
    onTertiary = Color(0xFF06150F),
    background = BgPrimary,
    onBackground = TextPrimary,
    surface = BgCard,
    onSurface = TextPrimary,
    surfaceVariant = BgElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = SpatialBg,
    surfaceContainerLow = BgCard,
    surfaceContainer = BgElevated,
    surfaceContainerHigh = BgHover,
    outline = BorderSubtle,
    outlineVariant = SpatialBorder,
    error = DangerRed,
    onError = Color.White,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun YMusicLiteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
