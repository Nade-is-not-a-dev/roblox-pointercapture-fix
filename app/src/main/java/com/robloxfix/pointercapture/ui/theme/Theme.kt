package com.robloxfix.pointercapture.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextPrimary,
    surface = Bg,
    onSurface = TextPrimary,
    background = Bg,
    onBackground = TextPrimary,
    surfaceVariant = Card,
    onSurfaceVariant = TextSecondary,
)

@Composable
public fun AppTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}
