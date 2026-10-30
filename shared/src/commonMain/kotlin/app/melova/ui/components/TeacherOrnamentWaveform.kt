package app.melova.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.melova.data.model.TeacherOrnament
import app.melova.data.model.TeacherOrnamentKind
import app.melova.ui.theme.LocalMelovaPalette
import app.melova.ui.theme.MelovaPalette
import kotlin.math.abs

/**
 * موج صدای استاد با بازه‌های تحریر (T)، ویبراتو (V) و گذر (G). هر بازه دقیقاً از ابتدای
 * مصوت تا انتهای آن کشیده می‌شود. زدن روی یک بازه همان بازه را پخش می‌کند و
 * زدن بیرون از بازه‌ها پخش را از همان لحظه شروع می‌کند. با [zoom] بزرگ‌تر از ۱
 * موج به‌صورت افقی پیمایش می‌شود و نشانگر پخش در دید می‌ماند.
 */
@Composable
fun TeacherOrnamentWaveform(
    peaks: List<Float>,
    durationMs: Long,
    playheadMs: Long,
    ornaments: List<TeacherOrnament>,
    selectedIndex: Int?,
    zoom: Int,
    onSeekToMs: (Long) -> Unit,
    onOrnamentClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMelovaPalette.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val totalMs = durationMs.coerceAtLeast(1L)
    val badgeStyle = remember(palette) {
        TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
    val rulerStyle = remember(palette) {
        TextStyle(color = palette.text.copy(alpha = 0.5f), fontSize = 9.sp)
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val viewportWidth = maxWidth
        val contentWidth = viewportWidth * zoom.coerceAtLeast(1)
        val viewportPx = with(density) { viewportWidth.toPx() }
        val contentPx = with(density) { contentWidth.toPx() }
        val scrollState = rememberScrollState()

        LaunchedEffect(playheadMs, zoom, contentPx) {
            if (zoom <= 1) return@LaunchedEffect
            val playheadPx = playheadMs.toFloat() / totalMs * contentPx
            val left = scrollState.value.toFloat()
            if (playheadPx < left || playheadPx > left + viewportPx * 0.85f) {
                scrollState.scrollTo((playheadPx - viewportPx * 0.15f).toInt().coerceAtLeast(0))
            }
        }

        Box(modifier = Modifier.horizontalScroll(scrollState)) {
            Canvas(
                modifier = Modifier
                    .width(contentWidth)
                    .height(170.dp)
                    .pointerInput(scrollState) {
                        // horizontalScroll follows touch only; in the web app a mouse drags it too.
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (down.type != PointerType.Mouse) return@awaitEachGesture
                            var moved = 0f
                            while (true) {
                                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                val dx = change.position.x - change.previousPosition.x
                                moved += abs(dx)
                                if (moved > viewConfiguration.touchSlop) {
                                    scrollState.dispatchRawDelta(-dx)
                                    change.consume()
                                }
                            }
                        }
                    }
                    .pointerInput(ornaments, durationMs, zoom) {
                        detectTapGestures { offset ->
                            val width = size.width.toFloat().coerceAtLeast(1f)
                            val tappedMs = ((offset.x / width) * totalMs).toLong().coerceIn(0L, totalMs)
                            val hit = ornaments.indexOfFirst { tappedMs in it.startMs..it.endMs }
                            if (hit >= 0) onOrnamentClick(hit) else onSeekToMs(tappedMs)
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val badgeArea = 16.dp.toPx()
                val rulerArea = 16.dp.toPx()
                val waveTop = badgeArea
                val waveHeight = height - badgeArea - rulerArea
                val centerY = waveTop + waveHeight / 2f
                fun xOf(ms: Long) = ms.toFloat() / totalMs * width

                drawRoundRect(
                    color = palette.text.copy(alpha = 0.04f),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )

                ornaments.forEachIndexed { index, ornament ->
                    val color = ornamentColor(ornament.kind, palette)
                    val startX = xOf(ornament.startMs)
                    val endX = xOf(ornament.endMs)
                    val selected = index == selectedIndex
                    drawRect(
                        color = color.copy(alpha = if (selected) 0.30f else 0.14f),
                        topLeft = Offset(startX, waveTop),
                        size = Size((endX - startX).coerceAtLeast(1f), waveHeight)
                    )
                    val edgeWidth = if (selected) 2.dp.toPx() else 1.dp.toPx()
                    drawLine(
                        color = color.copy(alpha = 0.9f),
                        start = Offset(endX, waveTop),
                        end = Offset(endX, waveTop + waveHeight),
                        strokeWidth = edgeWidth
                    )

                    drawLine(
                        color = color.copy(alpha = 0.9f),
                        start = Offset(startX, waveTop),
                        end = Offset(startX, waveTop + waveHeight),
                        strokeWidth = edgeWidth
                    )

                    // Just the letter, in the ornament's colour, over the band.
                    val label = textMeasurer.measure(
                        ornament.kind.letter,
                        badgeStyle.copy(color = if (selected) color else color.copy(alpha = 0.8f))
                    )
                    val labelLeft = ((startX + endX) / 2f - label.size.width / 2f)
                        .coerceIn(0f, (width - label.size.width).coerceAtLeast(0f))
                    drawText(
                        textLayoutResult = label,
                        topLeft = Offset(labelLeft, (badgeArea - label.size.height) / 2f)
                    )
                }

                if (peaks.isNotEmpty()) {
                    val barWidth = width / peaks.size
                    val stroke = (barWidth * 0.7f).coerceAtLeast(1f)
                    var segment = 0
                    peaks.forEachIndexed { index, peak ->
                        val barMs = (index + 0.5f) / peaks.size * totalMs
                        while (segment < ornaments.size && ornaments[segment].endMs < barMs) segment++
                        val inside = ornaments.getOrNull(segment)?.takeIf { barMs >= it.startMs }
                        val barHeight = (peak.coerceIn(0f, 1f) * waveHeight * 0.92f).coerceAtLeast(1.5f)
                        val x = index * barWidth + barWidth / 2f
                        drawLine(
                            color = inside?.let { ornamentColor(it.kind, palette) }
                                ?: palette.text.copy(alpha = 0.32f),
                            start = Offset(x, centerY - barHeight / 2f),
                            end = Offset(x, centerY + barHeight / 2f),
                            strokeWidth = stroke
                        )
                    }
                }

                // A tick each second; with zoom, every tenth of a second too.
                val tickMs = if (width / (totalMs / 100f) >= 6.dp.toPx()) 100L else 1_000L
                var tick = 0L
                while (tick <= totalMs) {
                    val x = xOf(tick)
                    val isSecond = tick % 1_000L == 0L
                    drawLine(
                        color = palette.text.copy(alpha = if (isSecond) 0.35f else 0.15f),
                        start = Offset(x, height - rulerArea),
                        end = Offset(x, height - rulerArea + if (isSecond) 6.dp.toPx() else 3.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                    if (isSecond && tick > 0L) {
                        val text = textMeasurer.measure("${tick / 1_000}s", rulerStyle)
                        drawText(
                            textLayoutResult = text,
                            topLeft = Offset(
                                (x - text.size.width / 2f).coerceIn(0f, width - text.size.width),
                                height - text.size.height - 1.dp.toPx()
                            )
                        )
                    }
                    tick += tickMs
                }

                val playheadX = xOf(playheadMs.coerceIn(0L, totalMs))
                drawLine(
                    color = palette.warning,
                    start = Offset(playheadX, waveTop - 2.dp.toPx()),
                    end = Offset(playheadX, waveTop + waveHeight),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

fun ornamentColor(kind: TeacherOrnamentKind, palette: MelovaPalette): Color = when (kind) {
    TeacherOrnamentKind.TAHRIR -> palette.gold
    TeacherOrnamentKind.VIBRATO -> palette.info
    TeacherOrnamentKind.GLIDE -> palette.secondary
}
