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
import app.melova.resources.iran_sans
import app.melova.resources.iran_sans_bold
import app.melova.resources.iran_sans_light
import app.melova.resources.iran_sans_medium
import org.jetbrains.compose.resources.Font

/**
 * IRANSans, drawing each digit as it was written: what people type stays as typed, and the app's
 * own numbers go through persianDigits. SemiBold falls back to the closest file (Medium/Bold).
 */
@Composable
fun iranSans(): FontFamily = FontFamily(
    Font(Res.font.iran_sans_light, FontWeight.Light),
    Font(Res.font.iran_sans, FontWeight.Normal),
    Font(Res.font.iran_sans_medium, FontWeight.Medium),
    Font(Res.font.iran_sans_bold, FontWeight.Bold)
)

private fun FontFamily.style(
    size: TextUnit,
    lineHeight: TextUnit,
    weight: FontWeight = FontWeight.Normal,
    fontFeatures: String? = null
) = TextStyle(
    fontFamily = this,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = 0.sp,
    fontFeatureSettings = fontFeatures
)

/**
 * The fonts are resources, loaded in composition (and, on the web, fetched once). Text laid out
 * before a font that fills in missing glyphs arrives keeps its boxes until it's laid out again,
 * so [lateFontsLoaded] gives every style a feature that's already on: nothing looks different,
 * but the styles no longer match and all text is laid out again, keeping the state of the page.
 */
@Composable
fun melovaTypography(lateFontsLoaded: Boolean = false): Typography {
    val family = iranSans()
    return remember(family, lateFontsLoaded) { family.typography(if (lateFontsLoaded) "kern" else null) }
}

private fun FontFamily.typography(fontFeatures: String?): Typography {
    fun style(size: TextUnit, lineHeight: TextUnit, weight: FontWeight = FontWeight.Normal) =
        style(size, lineHeight, weight, fontFeatures)
    return Typography(
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
}
