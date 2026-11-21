package com.rcsolutions.rcosmobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AuroraColorScheme = darkColorScheme(
    primary = AuroraEmerald,
    onPrimary = AuroraOnEmerald,
    primaryContainer = AuroraCardSelected,
    onPrimaryContainer = AuroraEmeraldBright,
    secondary = AuroraCyan,
    onSecondary = AuroraOnEmerald,
    secondaryContainer = AuroraCardElevated,
    onSecondaryContainer = AuroraTextPrimary,
    background = AuroraObsidian,
    onBackground = AuroraTextPrimary,
    surface = AuroraCardSurface,
    onSurface = AuroraTextPrimary,
    surfaceVariant = AuroraCardElevated,
    onSurfaceVariant = AuroraTextSecondary,
    outline = AuroraBorder,
    outlineVariant = AuroraBorder,
    error = AuroraRed,
    onError = AuroraTextPrimary
)

private val AuroraShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AuroraOneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AuroraColorScheme,
        typography = AuroraTypography,
        shapes = AuroraShapes,
        content = content
    )
}
