package app.melova.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.melova.ui.theme.LocalMelovaPalette

private const val MARK_PATH =
    "M28 150 C 38 92, 48 52, 64 52 S 88 120, 100 120 S 120 52, 136 52 S 160 98, 166 150"
private const val MARK_GRID = 200f

/** The Melova mark: a melody line shaped like the letter M, ending in a note. */
@Composable
fun MelovaMark(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    lineColor: Color = LocalMelovaPalette.current.accent,
    noteColor: Color = LocalMelovaPalette.current.logoDot
) {
    val path = remember { PathParser().parsePathString(MARK_PATH).toPath() }
    Canvas(modifier = modifier.size(size)) {
        scale(this.size.minDimension / MARK_GRID, pivot = Offset.Zero) {
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 20f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawCircle(color = noteColor, radius = 16f, center = Offset(166f, 150f))
        }
    }
}
