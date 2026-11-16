package com.aibuilt.ghostbank.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColorLightPrimaryContainer = Color(0xFFB8EFDD)
private val ColorLightTertiary = Color(0xFFB07A00)
private val ColorLightError = Color(0xFFB3261E)
private val ColorLightOnSurfaceVariant = Color(0xFF3F5249)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = GhostMintDim,
    onPrimaryContainer = GhostNight,
    secondary = DarkSecondary,
    onSecondary = GhostNight,
    tertiary = GhostAmber,
    onTertiary = GhostNight,
    error = GhostRed,
    onError = GhostNight,
    background = DarkSurface,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVar,
    onSurfaceVariant = GhostGhost,
    outline = DarkOutline
)

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = ColorLightPrimaryContainer,
    onPrimaryContainer = LightOnSurface,
    secondary = LightSecondary,
    onSecondary = LightOnPrimary,
    tertiary = ColorLightTertiary,
    onTertiary = LightOnSurface,
    error = ColorLightError,
    onError = LightOnPrimary,
    background = LightSurface,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVar,
    onSurfaceVariant = ColorLightOnSurfaceVariant,
    outline = LightOutline
)

@Composable
fun GhostBankTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = GhostTypography,
        content = content
    )
}
