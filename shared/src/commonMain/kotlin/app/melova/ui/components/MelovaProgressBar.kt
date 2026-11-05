package app.melova.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.melova.ui.layout.LtrContent

@Composable
fun MelovaProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color,
    trackColor: Color,
    /** False keeps the timeline-style left-to-right fill; true fills from the start side (RTL in Persian). */
    followLayoutDirection: Boolean = false,
    thickness: Dp = 8.dp
) {
    val fraction = progress.coerceIn(0f, 1f)
    val bar: @Composable () -> Unit = {
        Box(
            modifier = modifier
                .height(thickness)
                .clip(RoundedCornerShape(999.dp))
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(999.dp))
                    .background(color)
            )
        }
    }
    if (followLayoutDirection) bar() else LtrContent(bar)
}
