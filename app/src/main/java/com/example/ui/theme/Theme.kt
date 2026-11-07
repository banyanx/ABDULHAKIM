package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LuxuryDarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = LuxuryBackground,
    primaryContainer = GoldDark,
    onPrimaryContainer = GoldLight,
    secondary = CyanTurbo,
    onSecondary = LuxuryBackground,
    secondaryContainer = LuxurySurfaceVariant,
    onSecondaryContainer = CyanTurbo,
    tertiary = GreenAccept,
    onTertiary = LuxuryBackground,
    error = RedReject,
    onError = TextPrimary,
    background = LuxuryBackground,
    onBackground = TextPrimary,
    surface = LuxurySurface,
    onSurface = TextPrimary,
    surfaceVariant = LuxurySurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = LuxuryCardBorder
)

@Composable
fun LocateAssistantTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LuxuryDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    LocateAssistantTheme(content = content)
}
