package app.melova.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import app.melova.ui.theme.MelovaTheme
import app.melova.ui.theme.ThemeMode
import app.melova.ui.theme.ThemePreferences

/**
 * The whole app with its saved theme, shared by the Android activity and the web page.
 * [onDarkThemeChange] lets the host follow the theme (Android's system bar icons).
 */
@Composable
fun MelovaRoot(themePreferences: ThemePreferences, onDarkThemeChange: @Composable (Boolean) -> Unit = {}) {
    var themeMode by remember { mutableStateOf(themePreferences.mode) }
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    onDarkThemeChange(darkTheme)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        MelovaTheme(darkTheme = darkTheme) {
            MelovaApp(
                themeMode = themeMode,
                onThemeModeChange = { mode ->
                    themeMode = mode
                    themePreferences.mode = mode
                }
            )
        }
    }
}
