package app.melova.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import app.melova.resources.Res
import app.melova.resources.iran_sans_fa_num
import app.melova.resources.iran_sans_fa_num_bold
import app.melova.resources.iran_sans_fa_num_light
import app.melova.resources.iran_sans_fa_num_medium
import org.jetbrains.compose.resources.Font

/** IRANSans with Persian digits; SemiBold falls back to the closest file (Medium/Bold). */
@Composable
fun iranSans(): FontFamily = FontFamily(
    Font(Res.font.iran_sans_fa_num_light, FontWeight.Light),
    Font(Res.font.iran_sans_fa_num, FontWeight.Normal),
    Font(Res.font.iran_sans_fa_num_medium, FontWeight.Medium),
    Font(Res.font.iran_sans_fa_num_bold, FontWeight.Bold)
)

private fun FontFamily.style(
    size: TextUnit,
    lineHeight: TextUnit,
    weight: FontWeight = FontWeight.Normal
) = TextStyle(
    fontFamily = this,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = 0.sp
)

/** The fonts are resources, loaded in composition (and, on the web, fetched once). */
@Composable
fun melovaTypography(): Typography {
    val family = iranSans()
    return remember(family) { family.typography() }
}

private fun FontFamily.typography() = Typography(
    displayLarge = style(52.sp, 64.sp, FontWeight.Bold),
    displayMedium = style(42.sp, 54.sp, FontWeight.Bold),
    displaySmall = style(34.sp, 46.sp, FontWeight.Bold),
    headlineLarge = style(28.sp, 40.sp, FontWeight.Bold),
    headlineMedium = style(24.sp, 36.sp, FontWeight.Bold),
    headlineSmall = style(21.sp, 32.sp, FontWeight.Bold),
    titleLarge = style(19.sp, 30.sp, FontWeight.Bold),
    titleMedium = style(16.sp, 26.sp, FontWeight.Medium),
    titleSmall = style(14.sp, 22.sp, FontWeight.Medium),
    bodyLarge = style(16.sp, 28.sp),
    bodyMedium = style(14.sp, 25.sp),
    bodySmall = style(12.5.sp, 22.sp),
    labelLarge = style(14.sp, 20.sp, FontWeight.Medium),
    labelMedium = style(12.sp, 18.sp, FontWeight.Medium),
    labelSmall = style(11.sp, 16.sp, FontWeight.Medium)
)
