package app.melova.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import app.melova.R

/** IRANSans with Persian digits; SemiBold falls back to the closest file (Medium/Bold). */
val IranSans = FontFamily(
    Font(R.font.iran_sans_fa_num_light, FontWeight.Light),
    Font(R.font.iran_sans_fa_num, FontWeight.Normal),
    Font(R.font.iran_sans_fa_num_medium, FontWeight.Medium),
    Font(R.font.iran_sans_fa_num_bold, FontWeight.Bold)
)

private fun style(
    size: TextUnit,
    lineHeight: TextUnit,
    weight: FontWeight = FontWeight.Normal
) = TextStyle(
    fontFamily = IranSans,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = 0.sp
)

val Typography = Typography(
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
