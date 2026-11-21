package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SkipBuddyLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = EmeraldPrimary,
    onSecondary = EmeraldOnPrimary,
    secondaryContainer = SurfaceSubtle,
    onSecondaryContainer = TextPrimary,
    background = SurfaceBackground,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSubtle,
    onSurfaceVariant = TextSecondary,
    outline = BorderHairline,
    error = StatusOffRed,
    onError = EmeraldOnPrimary,
    errorContainer = StatusOffContainer,
    onErrorContainer = StatusOffRed
)

@Composable
fun SkipBuddyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SkipBuddyLightColorScheme,
        typography = SkipBuddyTypography,
        content = content
    )
}
