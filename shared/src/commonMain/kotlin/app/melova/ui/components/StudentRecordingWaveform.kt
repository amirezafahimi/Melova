package app.melova.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.melova.audio.scoring.VocalScorer
import kotlin.math.abs
import kotlin.math.hypot
import app.melova.ui.theme.LocalMelovaPalette
import app.melova.ui.theme.MelovaPalette

@Composable
fun StudentRecordingWaveform(
    peaks: List<Float>,
    durationMs: Long,
    playheadMs: Long,
    corrections: List<VocalScorer.PitchCorrection>,
    selectedCorrectionStartMs: Long?,
    onSeekToMs: (Long) -> Unit,
    onCorrectionClick: (VocalScorer.PitchCorrection) -> Unit,
    modifier: Modifier = Modifier,
    markerStartMs: (VocalScorer.PitchCorrection) -> Long = { it.startMs },
    selectedMarkerStartMs: Long? = selectedCorrectionStartMs
) {
    val density = LocalDensity.current
    val markerHitRadiusPx = with(density) { 18.dp.toPx() }
    val totalMs = durationMs.coerceAtLeast(1L)
    val palette = LocalMelovaPalette.current

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .pointerInput(peaks, corrections, durationMs) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    val height = size.height.toFloat()
                    val tappedMs = ((offset.x / width) * totalMs).toLong().coerceIn(0L, totalMs)

                    val markerLayouts = layoutCorrectionMarkers(
                        corrections = corrections,
                        totalMs = totalMs,
                        width = width,
                        height = height,
                        peaks = peaks,
                        markerStartMs = markerStartMs
                    )
                    val nearest = markerLayouts.minByOrNull { layout ->
                        hypot(layout.position.x - offset.x, layout.position.y - offset.y)
                    }
                    if (nearest != null &&
                        hypot(
                            nearest.position.x - offset.x,
                            nearest.position.y - offset.y
                        ) <= markerHitRadiusPx
                    ) {
                        onCorrectionClick(nearest.correction)
                        return@detectTapGestures
                    }
                    onSeekToMs(tappedMs)
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val barCount = peaks.size.coerceAtLeast(1)
        val barWidth = width / barCount
        val playheadX = (playheadMs.toFloat() / totalMs) * width

        drawRoundRect(
            color = palette.text.copy(alpha = 0.04f),
            size = Size(width, height),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )

        peaks.forEachIndexed { index, peak ->
            val barHeight = (peak.coerceIn(0f, 1f) * height * 0.72f).coerceAtLeast(4f)
            val x = index * barWidth + barWidth * 0.15f
            val barW = barWidth * 0.7f
            drawRoundRect(
                color = palette.text.copy(alpha = 0.28f),
                topLeft = Offset(x, centerY - barHeight / 2f),
                size = Size(barW, barHeight),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
            )
        }

        if (playheadMs > 0L) {
            drawLine(
                color = palette.warning,
                start = Offset(playheadX, 8f),
                end = Offset(playheadX, height - 8f),
                strokeWidth = 3f
            )
        }

        layoutCorrectionMarkers(
            corrections = corrections,
            totalMs = totalMs,
            width = width,
            height = height,
            peaks = peaks,
            markerStartMs = markerStartMs
        ).forEach { layout ->
            val correction = layout.correction
            val marker = layout.position
            val isSelected = selectedMarkerStartMs == markerStartMs(correction)
            val color = correctionMarkerColor(correction, palette)
            val radius = if (isSelected) 10f else 7f

            drawLine(
                color = color.copy(alpha = 0.55f),
                start = Offset(marker.x, marker.y),
                end = Offset(marker.x, centerY),
                strokeWidth = 2f
            )
            drawCircle(
                color = color,
                radius = radius,
                center = marker
            )
            drawCircle(
                color = palette.card.copy(alpha = 0.95f),
                radius = radius,
                center = marker,
                style = Stroke(width = if (isSelected) 2.5f else 1.5f)
            )
        }
    }
}

private data class CorrectionMarkerLayout(
    val correction: VocalScorer.PitchCorrection,
    val position: Offset
)

private fun layoutCorrectionMarkers(
    corrections: List<VocalScorer.PitchCorrection>,
    totalMs: Long,
    width: Float,
    height: Float,
    peaks: List<Float>,
    markerStartMs: (VocalScorer.PitchCorrection) -> Long
): List<CorrectionMarkerLayout> {
    if (corrections.isEmpty() || width <= 0f || height <= 0f) return emptyList()

    val centerY = height / 2f
    val barCount = peaks.size.coerceAtLeast(1)
    val minGapPx = 12f
    val layerStepPx = 13f
    val duration = totalMs.coerceAtLeast(1L)
    val layouts = mutableListOf<CorrectionMarkerLayout>()

    corrections.sortedBy { markerStartMs(it) }.forEach { correction ->
        val baseX = ((markerStartMs(correction).toFloat() / duration) * width)
            .coerceIn(6f, width - 6f)
        val barIndex = ((baseX / width) * barCount).toInt().coerceIn(0, barCount - 1)
        val peak = peaks.getOrElse(barIndex) { 0f }
        val barHeight = (peak.coerceIn(0f, 1f) * height * 0.72f).coerceAtLeast(4f)
        val baseY = (centerY - barHeight / 2f - 6f).coerceAtLeast(10f)

        // Past the top edge there's no room left, so a dense cluster's last markers overlap.
        val topLayer = ((baseY - 8f) / layerStepPx).toInt()
        var layer = 0
        var position = Offset(baseX, baseY)
        while (layer < topLayer && layouts.any { placed ->
                hypot(placed.position.x - position.x, placed.position.y - position.y) < minGapPx
            }
        ) {
            layer++
            position = Offset(baseX, baseY - layer * layerStepPx)
        }

        layouts += CorrectionMarkerLayout(correction, position)
    }

    return layouts
}

private fun correctionMarkerColor(
    correction: VocalScorer.PitchCorrection,
    palette: MelovaPalette
): Color = when {
    correction.sungNote == "—" -> palette.textMuted
    correction.direction == VocalScorer.CorrectionDirection.SHARP -> palette.warning
    correction.direction == VocalScorer.CorrectionDirection.FLAT -> palette.info
    else -> palette.success
}
