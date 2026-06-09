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
import androidx.compose.ui.unit.dp
import app.melova.ui.layout.LtrContent

@Composable
fun MelovaProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color,
    trackColor: Color
) {
    val fraction = progress.coerceIn(0f, 1f)
    LtrContent {
        Box(
            modifier = modifier
                .height(8.dp)
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
}
