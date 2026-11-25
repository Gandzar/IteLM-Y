package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val YMusicLightColorScheme = lightColorScheme(
    primary = YtOrange,
    onPrimary = Color.White,
    primaryContainer = YtOrangeSoft,
    onPrimaryContainer = YtOrangeDark,
    secondary = AccentGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7F7EC),
    onSecondaryContainer = Color(0xFF0A4720),
    tertiary = SpatialGreenDeep,
    onTertiary = Color.White,
    background = YtBg,
    onBackground = YtTextPrimary,
    surface = YtCard,
    onSurface = YtTextPrimary,
    surfaceVariant = YtElevated,
    onSurfaceVariant = YtTextSecondary,
    surfaceContainerLowest = YtCard,
    surfaceContainerLow = YtBg,
    surfaceContainer = YtElevated,
    surfaceContainerHigh = YtChipBg,
    outline = YtDivider,
    outlineVariant = YtDivider,
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
        colorScheme = YMusicLightColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
