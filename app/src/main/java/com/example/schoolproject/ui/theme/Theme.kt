package com.example.schoolproject.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CalmaGreen,
    onPrimary = Color.White,
    primaryContainer = CalmaGreenDark,
    onPrimaryContainer = Color.White,
    secondary = CalmaAmber,
    background = Color(0xFF181F1B),
    surface = Color(0xFF222B26),
    onBackground = Color(0xFFE2ECE6),
    onSurface = Color(0xFFE2ECE6)
)

private val LightColorScheme = lightColorScheme(
    primary = CalmaGreen,
    onPrimary = Color.White,
    primaryContainer = CalmaGreenContainer,
    onPrimaryContainer = CalmaGreenDark,
    secondary = CalmaAmber,
    onSecondary = Color.White,
    secondaryContainer = CalmaAmberLight,
    background = CalmaBackground,
    surface = CalmaCardSurface,
    onBackground = CalmaTextDark,
    onSurface = CalmaTextDark,
    onSurfaceVariant = CalmaTextMuted,
    outline = CalmaBorder
)

@Composable
fun SchoolProjectTheme(
    darkTheme: Boolean = false,
    // Keep consistent light design as per Figma specs
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
