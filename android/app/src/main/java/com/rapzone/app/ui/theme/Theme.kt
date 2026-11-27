package com.rapzone.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF0000),
    secondary = Color(0xFFFF3333),
    tertiary = Color(0xFFFF6666),
    background = Color(0xFF000000),
    surface = Color(0xFF121212),
    surfaceVariant = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0B0B0),
    error = Color(0xFFCF6679),
    errorContainer = Color(0xFF93000A),
    onError = Color.White,
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF767676),
    outlineVariant = Color(0xFF4A4A4A),
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,
    inversePrimary = Color(0xFFFF6666),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFCC0000),
    secondary = Color(0xFFE63946),
    tertiary = Color(0xFFFF6B6B),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFFE0E0E0),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF121212),
    onSurface = Color(0xFF121212),
    onSurfaceVariant = Color(0xFF666666),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onError = Color.White,
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFF767676),
    outlineVariant = Color(0xFFC6C6C6),
    inverseSurface = Color(0xFF121212),
    inverseOnSurface = Color.White,
    inversePrimary = Color(0xFFFF6B6B),
)

@Composable
fun RapZoneTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}