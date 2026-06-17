package com.javis.ai.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JavisDarkColorScheme = darkColorScheme(
    primary = JavisBlue,
    onPrimary = JavisDeepBlue,
    primaryContainer = JavisCard,
    onPrimaryContainer = JavisTextPrimary,
    secondary = JavisCyan,
    onSecondary = JavisDeepBlue,
    secondaryContainer = JavisCardElevated,
    onSecondaryContainer = JavisTextPrimary,
    tertiary = JavisPurple,
    background = JavisDeepBlue,
    onBackground = JavisTextPrimary,
    surface = JavisDarkSurface,
    onSurface = JavisTextPrimary,
    surfaceVariant = JavisCard,
    onSurfaceVariant = JavisTextSecondary,
    error = JavisError,
    onError = Color.White,
    outline = JavisDivider,
    outlineVariant = JavisDivider.copy(alpha = 0.5f)
)

@Composable
fun JavisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JavisDarkColorScheme,
        typography = JavisTypography,
        content = content
    )
}
