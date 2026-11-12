package app.melova.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaAccentTint
import app.melova.ui.theme.MelovaCard

/**
 * The teacher's «آفرین», drawn like the ink stamp a teacher presses into a pupil's notebook: a
 * round seal with a double ring, set a little askew. It is the one bold mark of the practice
 * features, so it appears only where a teacher really gave it.
 *
 * [pressed] plays the stamp coming down once, for the moment it is given; a stamp that was
 * already there when the page opened just sits.
 */
@Composable
fun AfarinStamp(modifier: Modifier = Modifier, size: Dp = 56.dp, pressed: Boolean = false) {
    val scale = remember { Animatable(if (pressed) 1.5f else 1f) }
    val alpha = remember { Animatable(if (pressed) 0f else 1f) }
    LaunchedEffect(pressed) {
        if (pressed) {
            alpha.animateTo(1f, tween(120))
            scale.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
        }
    }
    val ink = MelovaAccentSoft
    // Filled with the card's own colour first, so the stamp stays readable over whatever it overlaps.
    val paper = MelovaCard
    val wash = MelovaAccentTint
    Box(
        modifier = modifier
            .size(size)
            .rotate(-12f)
            .scale(scale.value)
            .alpha(alpha.value),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outer = this.size.minDimension / 2
            drawCircle(paper, radius = outer)
            drawCircle(wash, radius = outer)
            drawCircle(ink, radius = outer - 1.5.dp.toPx(), style = Stroke(width = 2.dp.toPx()))
            drawCircle(ink, radius = outer - 5.dp.toPx(), style = Stroke(width = 1.dp.toPx()))
        }
        Text(
            text = "آفرین",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = ink
        )
    }
}
