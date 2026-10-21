package app.melova.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Every color the UI uses; the light set keeps text and accents at readable contrast on white. */
@Immutable
data class MelovaPalette(
    val isDark: Boolean,
    val backgroundStart: Color,
    val backgroundEnd: Color,
    val card: Color,
    val cardElevated: Color,
    val outline: Color,
    val accent: Color,
    val accentSoft: Color,
    val secondary: Color,
    val gold: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
    val text: Color,
    val textMuted: Color,
    /** The note at the end of the logo's melody line. */
    val logoDot: Color
)

val DarkPalette = MelovaPalette(
    isDark = true,
    backgroundStart = Color(0xFF0E0A18),
    backgroundEnd = Color(0xFF1B1230),
    card = Color(0xFF1F1733),
    cardElevated = Color(0xFF2A2042),
    outline = Color(0xFF3A2E57),
    accent = Color(0xFF9B7BFF),
    accentSoft = Color(0xFFCDBBFF),
    secondary = Color(0xFF5E6CF2),
    gold = Color(0xFFF3C969),
    success = Color(0xFF5DD6A4),
    warning = Color(0xFFFFB74D),
    error = Color(0xFFFF7A8A),
    info = Color(0xFF81D4FA),
    text = Color(0xFFF4F0FF),
    textMuted = Color(0xFFB4ABC9),
    logoDot = Color(0xFFF3C969)
)

val LightPalette = MelovaPalette(
    isDark = false,
    backgroundStart = Color(0xFFF7F4FF),
    backgroundEnd = Color(0xFFEDE7FF),
    card = Color(0xFFFFFFFF),
    cardElevated = Color(0xFFF1ECFF),
    outline = Color(0xFFE2DAF7),
    accent = Color(0xFF6B4FE0),
    accentSoft = Color(0xFF5A3FD1),
    secondary = Color(0xFF4A55D6),
    gold = Color(0xFF9A6410),
    success = Color(0xFF187A56),
    warning = Color(0xFFB45A00),
    error = Color(0xFFC73A52),
    info = Color(0xFF1C6FA8),
    text = Color(0xFF1B1230),
    textMuted = Color(0xFF5F5675),
    logoDot = Color(0xFFE8A93A)
)

val LocalMelovaPalette = staticCompositionLocalOf { DarkPalette }

val MelovaBackgroundStart: Color @Composable @ReadOnlyComposable get() = LocalMelovaPalette.current.backgroundStart
val MelovaBackgroundEnd: Color @Composable @ReadOnlyComposable get() = LocalMelovaPalette.current.backgroundEnd
val MelovaCard: Color @Composable @ReadOnlyComposable get() = LocalMelovaPalette.current.card
val MelovaCardElevated: Color @Composable @ReadOnlyComposable get() = LocalMelovaPalette.current.cardElevated
val MelovaOutline: Color @Composable @ReadOnlyComposable get() = LocalMelovaPalette.current.outline
val MelovaAccent: Color @Composable @ReadOnlyComposable get() = LocalMelovaPalette.current.accent
val MelovaAccentSoft: Color @Composable @ReadOnlyComposable get() = LocalMelovaPalette.current.accentSoft
val MelovaSecondary: Color @Composable @ReadOnlyComposable get() = LocalMelovaPalette.current.secondary
val MelovaGold: Color @Composable @ReadOnlyComposable get() = LocalMelovaPalette.current.gold
val MelovaSuccess: Color @Composable @ReadOnlyComposable get() = LocalMelovaPalette.current.success
val MelovaWarning: Color @Composable @ReadOnlyComposable get() = LocalMelovaPalette.current.warning
val MelovaError: Color @Composable @ReadOnlyComposable get() = LocalMelovaPalette.current.error
val MelovaInfo: Color @Composable @ReadOnlyComposable get() = LocalMelovaPalette.current.info
val MelovaText: Color @Composable @ReadOnlyComposable get() = LocalMelovaPalette.current.text
val MelovaTextMuted: Color @Composable @ReadOnlyComposable get() = LocalMelovaPalette.current.textMuted
