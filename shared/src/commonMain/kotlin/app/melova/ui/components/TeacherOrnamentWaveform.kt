package app.melova.ui.components

import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
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
import kotlinx.coroutines.flow.conflate
import kotlin.math.abs
import kotlin.math.hypot

/**
 * The teacher's wave with its tahrir (T), vibrato (V) and glide (G) segments, each spanning
 * exactly its vowel. Tapping a segment plays it; tapping outside plays from that moment. With
 * [zoom] above 1 the wave scrolls sideways and keeps the playhead in view.
 */
@Composable
fun TeacherOrnamentWaveform(
    peaks: List<Float>,
    durationMs: Long,
    playheadMs: Long,
    ornaments: List<TeacherOrnament>,
    selectedIndex: Int?,
    zoom: Float,
    onZoomChange: (Float) -> Unit,
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
        val contentWidth = viewportWidth * zoom.coerceAtLeast(1f)
        val viewportPx = with(density) { viewportWidth.toPx() }
        val contentPx = with(density) { contentWidth.toPx() }
        val scrollState = rememberScrollState()
        var pinch by remember { mutableStateOf<PinchFocus?>(null) }
        val currentZoom by rememberUpdatedState(zoom)
        val currentContentPx by rememberUpdatedState(contentPx)

        val currentPlayheadMs by rememberUpdatedState(playheadMs)
        val currentViewportPx by rememberUpdatedState(viewportPx)
        val currentTotalMs by rememberUpdatedState(totalMs)

        LaunchedEffect(scrollState) {
            // Conflated, so ticks that arrive during a smooth scroll are dropped rather than queued.
            snapshotFlow { Triple(currentPlayheadMs, currentZoom, currentContentPx) }
                .conflate()
                .collect { (playhead, zoomNow, content) ->
                    // During a pinch the wave stays put under the fingers, wherever the playhead is.
                    pinch?.let { focus ->
                        scrollState.scrollTo((focus.fraction * content - focus.viewportX).toInt().coerceAtLeast(0))
                        return@collect
                    }
                    if (zoomNow <= 1f || scrollState.isScrollInProgress) return@collect
                    val width = currentViewportPx
                    val playheadPx = playhead.toFloat() / currentTotalMs * content
                    val onScreen = playheadPx - scrollState.value
                    when {
                        onScreen in 0f..width * FOLLOW_FROM -> Unit
                        // Past the follow line the wave slides under the pointer instead of jumping a page.
                        onScreen in 0f..width -> scrollState.scrollTo((playheadPx - width * FOLLOW_FROM).toInt())
                        else -> scrollState.animateScrollTo(
                            value = (playheadPx - width * ARRIVE_AT).toInt().coerceAtLeast(0),
                            animationSpec = tween(ARRIVE_MS)
                        )
                    }
                }
        }

        Box(
            modifier = Modifier
                .pointerInput(scrollState) {
                    // Runs before the scrolling and the taps, and takes the event from them for two fingers.
                    awaitEachGesture {
                        // Measured from where the fingers landed: several moves can arrive in one frame,
                        // before the new zoom is back here, and adding them up would lose all but the last.
                        var startSpan = 0f
                        var startZoom = 1f
                        var fraction = 0f
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val fingers = event.changes.filter { it.pressed }
                            if (fingers.isEmpty()) break
                            if (fingers.size < 2) {
                                startSpan = 0f
                                continue
                            }
                            val (first, second) = fingers
                            val span = hypot(first.position.x - second.position.x, first.position.y - second.position.y)
                            val viewportX = (first.position.x + second.position.x) / 2f
                            if (startSpan <= 0f) {
                                startSpan = span
                                startZoom = currentZoom
                                fraction = (scrollState.value + viewportX) / currentContentPx
                            } else {
                                pinch = PinchFocus(fraction = fraction, viewportX = viewportX)
                                onZoomChange(startZoom * span / startSpan)
                            }
                            event.changes.forEach { it.consume() }
                        }
                        pinch = null
                    }
                }
                .horizontalScroll(scrollState)
        ) {
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
                    .pointerInput(ornaments, durationMs) {
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

/** Share of the view the playhead may cross before the wave starts to follow it. */
private const val FOLLOW_FROM = 0.7f

/** Where in the view the playhead lands after the wave moves to find it. */
private const val ARRIVE_AT = 0.2f
private const val ARRIVE_MS = 260

/** Where the fingers are: this share of the wave stays at this x on screen. */
private class PinchFocus(val fraction: Float, val viewportX: Float)

fun ornamentColor(kind: TeacherOrnamentKind, palette: MelovaPalette): Color = when (kind) {
    TeacherOrnamentKind.TAHRIR -> palette.gold
    TeacherOrnamentKind.VIBRATO -> palette.info
    TeacherOrnamentKind.GLIDE -> palette.secondary
}
