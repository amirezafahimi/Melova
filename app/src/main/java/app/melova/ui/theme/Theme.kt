package app.melova.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private fun MelovaPalette.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = Color.White,
        primaryContainer = accent.copy(alpha = 0.18f),
        onPrimaryContainer = accentSoft,
        secondary = secondary,
        onSecondary = Color.White,
        tertiary = gold,
        onTertiary = backgroundStart,
        background = backgroundStart,
        onBackground = text,
        surface = card,
        onSurface = text,
        surfaceVariant = cardElevated,
        onSurfaceVariant = textMuted,
        surfaceContainer = card,
        surfaceContainerHigh = cardElevated,
        outline = outline,
        outlineVariant = outline,
        error = error
    )
}

@Composable
fun MelovaTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    CompositionLocalProvider(LocalMelovaPalette provides palette) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(),
            typography = Typography,
            content = content
        )
    }
}
