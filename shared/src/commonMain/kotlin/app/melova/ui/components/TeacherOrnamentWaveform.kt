package app.melova.ui.components

import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.melova.data.model.TeacherOrnament
import app.melova.data.model.TeacherOrnamentKind
import app.melova.platform.persianDigits
import app.melova.ui.theme.LocalMelovaPalette
import app.melova.ui.theme.MelovaPalette
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot

/**
 * The teacher's wave with its tahrir (T), vibrato (V) and glide (G) segments, each spanning
 * exactly its vowel. Tapping a segment plays it; tapping outside plays from that moment. With
 * [zoom] above 1 the wave moves sideways and keeps the playhead in view.
 *
 * The canvas always fills the view and only its drawing changes, so a pinch updates the zoom and
 * the position together in one frame and the point under the fingers stays there.
 */
@Composable
fun TeacherOrnamentWaveform(
    peaks: List<Float>,
    durationMs: Long,
    playheadMs: Long,
    ornaments: List<TeacherOrnament>,
    selectedIndex: Int?,
    zoom: Float,
    maxZoom: Float,
    onZoomChange: (Float) -> Unit,
    onSeekToMs: (Long) -> Unit,
    onOrnamentClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMelovaPalette.current
    val textMeasurer = rememberTextMeasurer()
    val totalMs = durationMs.coerceAtLeast(1L)
    val badgeStyle = remember(palette) {
        // On the pill: the dark theme's ornament colours are light, the light theme's are deep.
        TextStyle(
            color = if (palette.isDark) palette.backgroundStart else Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
    val rulerStyle = remember(palette) {
        TextStyle(color = palette.text.copy(alpha = 0.55f), fontSize = 10.sp)
    }
    val levels = remember(peaks) { peakLevels(peaks) }
    val view = remember { WaveView(zoom.coerceAtLeast(1f)) }
    val scope = rememberCoroutineScope()

    val currentPlayheadMs by rememberUpdatedState(playheadMs)
    val currentTotalMs by rememberUpdatedState(totalMs)
    val currentOrnaments by rememberUpdatedState(ornaments)
    val currentOnZoomChange by rememberUpdatedState(onZoomChange)
    val currentOnSeekToMs by rememberUpdatedState(onSeekToMs)
    val currentOnOrnamentClick by rememberUpdatedState(onOrnamentClick)
    val currentMaxZoom by rememberUpdatedState(maxZoom)

    // The zoom buttons: a pinch has already moved the view to this zoom.
    LaunchedEffect(zoom) {
        if (view.isTouched || abs(zoom - view.zoom) < 0.01f) return@LaunchedEffect
        animate(view.zoom, zoom.coerceAtLeast(1f), animationSpec = tween(ARRIVE_MS)) { value, _ ->
            view.show(value, wavePoint = view.left + 0.5f / view.zoom, viewPoint = 0.5f)
        }
    }

    LaunchedEffect(view) {
        // Conflated, so ticks that arrive during a glide are dropped rather than queued.
        snapshotFlow { currentPlayheadMs }
            .conflate()
            .collect { playhead ->
                if (view.isTouched || view.zoom <= 1f) return@collect
                val point = playhead.toFloat() / currentTotalMs
                val onScreen = (point - view.left) * view.zoom
                val followLeft = point - FOLLOW_FROM / view.zoom
                val glide = when {
                    onScreen in 0f..FOLLOW_FROM -> return@collect
                    // Past the follow line the wave slides under the pointer instead of jumping a page.
                    onScreen in 0f..1f && onScreen - FOLLOW_FROM < MAX_STEP -> {
                        view.moveTo(followLeft)
                        return@collect
                    }
                    onScreen in 0f..1f -> followLeft
                    else -> point - ARRIVE_AT / view.zoom
                }
                animate(view.left, glide, animationSpec = tween(ARRIVE_MS)) { value, _ ->
                    if (!view.isTouched) view.moveTo(value)
                }
            }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .pointerInput(view) {
                var fling: Job? = null
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fling?.cancel()
                    view.isTouched = true
                    val velocity = VelocityTracker()
                    velocity.addPosition(down.uptimeMillis, down.position)
                    var mode = TouchMode.UNDECIDED
                    var travel = Offset.Zero
                    var lastPosition = down.position
                    // Measured from where the fingers landed, so small errors never add up.
                    var startSpan = 0f
                    var startZoom = 1f
                    var wavePoint = 0f
                    while (true) {
                        val event = awaitPointerEvent()
                        val fingers = event.changes.filter { it.pressed }
                        if (fingers.isEmpty()) {
                            event.changes.firstOrNull()?.let { lastPosition = it.position }
                            break
                        }
                        if (fingers.size >= 2) {
                            val (first, second) = fingers
                            val span = hypot(first.position.x - second.position.x, first.position.y - second.position.y)
                            val viewPoint = (first.position.x + second.position.x) / 2f / size.width
                            if (mode != TouchMode.PINCH) {
                                mode = TouchMode.PINCH
                                startSpan = span.coerceAtLeast(1f)
                                startZoom = view.zoom
                                wavePoint = view.left + viewPoint / view.zoom
                            }
                            val next = (startZoom * span / startSpan).coerceIn(1f, currentMaxZoom)
                            view.show(next, wavePoint, viewPoint)
                            currentOnZoomChange(next)
                            event.changes.forEach { it.consume() }
                            continue
                        }
                        val finger = fingers.first()
                        velocity.addPosition(finger.uptimeMillis, finger.position)
                        val moved = finger.position - finger.previousPosition
                        when (mode) {
                            TouchMode.UNDECIDED -> {
                                travel += moved
                                if (finger.isConsumed) {
                                    mode = TouchMode.LET_GO
                                } else if (travel.getDistance() > viewConfiguration.touchSlop) {
                                    val sideways = abs(travel.x) > abs(travel.y)
                                    mode = if (sideways && view.zoom > 1f) TouchMode.DRAG else TouchMode.LET_GO
                                }
                            }
                            TouchMode.DRAG -> {
                                view.moveTo(view.left - moved.x / size.width / view.zoom)
                                finger.consume()
                            }
                            // One finger left after a pinch stays still, so lifting doesn't shift the wave.
                            TouchMode.PINCH, TouchMode.LET_GO -> mode = TouchMode.LET_GO
                        }
                    }
                    view.isTouched = false
                    when (mode) {
                        TouchMode.DRAG -> {
                            val startLeft = view.left
                            val perPx = 1f / size.width / view.zoom
                            fling = scope.launch {
                                animateDecay(0f, velocity.calculateVelocity().x, FloatExponentialDecaySpec()) { value, _ ->
                                    view.moveTo(startLeft - value * perPx)
                                }
                            }
                        }
                        TouchMode.UNDECIDED -> {
                            val point = view.left + lastPosition.x / size.width / view.zoom
                            val tappedMs = (point * currentTotalMs).toLong().coerceIn(0L, currentTotalMs)
                            val hit = currentOrnaments.indexOfFirst { tappedMs in it.startMs..it.endMs }
                            if (hit >= 0) currentOnOrnamentClick(hit) else currentOnSeekToMs(tappedMs)
                        }
                        else -> Unit
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val badgeArea = 20.dp.toPx()
        val rulerArea = 16.dp.toPx()
        val waveTop = badgeArea
        val waveHeight = height - badgeArea - rulerArea
        val centerY = waveTop + waveHeight / 2f
        val waveWidth = width * view.zoom
        val left = view.left
        fun xOf(point: Float) = (point - left) * waveWidth
        fun xOf(ms: Long) = xOf(ms.toFloat() / totalMs)

        drawRoundRect(
            color = palette.text.copy(alpha = 0.04f),
            size = Size(width, height),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )

        clipRect {
            ornaments.forEachIndexed { index, ornament ->
                val startX = xOf(ornament.startMs)
                val endX = xOf(ornament.endMs)
                if (endX < 0f || startX > width) return@forEachIndexed
                val color = ornamentColor(ornament.kind, palette)
                val selected = index == selectedIndex
                val bandWidth = (endX - startX).coerceAtLeast(2.dp.toPx())
                // A soft rounded band; the bars inside already carry the colour, so no hard edges.
                drawRoundRect(
                    color = color.copy(alpha = if (selected) 0.22f else 0.09f),
                    topLeft = Offset(startX, waveTop),
                    size = Size(bandWidth, waveHeight),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
                drawRoundRect(
                    color = color.copy(alpha = if (selected) 1f else 0.7f),
                    topLeft = Offset(startX, waveTop),
                    size = Size(bandWidth, 3.dp.toPx()),
                    cornerRadius = CornerRadius(1.5.dp.toPx())
                )

                // The letter in a small pill of the ornament's colour, over the band.
                val label = textMeasurer.measure(ornament.kind.letter, badgeStyle)
                val pillWidth = label.size.width + 10.dp.toPx()
                val pillHeight = label.size.height.toFloat()
                val pillLeft = ((startX + endX) / 2f - pillWidth / 2f)
                    .coerceIn(0f, (width - pillWidth).coerceAtLeast(0f))
                val pillTop = ((badgeArea - pillHeight) / 2f - 1.dp.toPx()).coerceAtLeast(0f)
                drawRoundRect(
                    color = color.copy(alpha = if (selected) 1f else 0.85f),
                    topLeft = Offset(pillLeft, pillTop),
                    size = Size(pillWidth, pillHeight),
                    cornerRadius = CornerRadius(pillHeight / 2f)
                )
                drawText(
                    textLayoutResult = label,
                    topLeft = Offset(pillLeft + 5.dp.toPx(), pillTop)
                )
            }

            if (levels.isNotEmpty()) {
                // Bars keep their place on the wave while the zoom changes, so nothing shimmers.
                val bars = levels.firstOrNull { waveWidth / it.size >= MIN_BAR_DP.dp.toPx() } ?: levels.last()
                val barWidth = waveWidth / bars.size
                val stroke = (barWidth * 0.62f).coerceAtLeast(1f)
                val playedPoint = playheadMs.toFloat() / totalMs
                val first = floor(left * bars.size).toInt().coerceAtLeast(0)
                val last = ceil((left + 1f / view.zoom) * bars.size).toInt().coerceAtMost(bars.size)
                var segment = 0
                for (index in first until last) {
                    val point = (index + 0.5f) / bars.size
                    val barMs = point * totalMs
                    while (segment < ornaments.size && ornaments[segment].endMs < barMs) segment++
                    val inside = ornaments.getOrNull(segment)?.takeIf { barMs >= it.startMs }
                    val barHeight = (bars[index].coerceIn(0f, 1f) * waveHeight * 0.92f).coerceAtLeast(1.5f)
                    val x = xOf(point)
                    val played = playheadMs > 0L && point <= playedPoint
                    drawLine(
                        color = when {
                            inside != null -> ornamentColor(inside.kind, palette).copy(alpha = if (played) 1f else 0.8f)
                            played -> palette.accent
                            else -> palette.text.copy(alpha = 0.26f)
                        },
                        start = Offset(x, centerY - barHeight / 2f),
                        end = Offset(x, centerY + barHeight / 2f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }

            // A tick each second; with zoom, every tenth of a second too.
            val tickMs = if (waveWidth / (totalMs / 100f) >= 6.dp.toPx()) 100L else 1_000L
            var tick = (left * totalMs).toLong() / tickMs * tickMs
            while (tick <= totalMs) {
                val x = xOf(tick)
                if (x > width) break
                val isSecond = tick % 1_000L == 0L
                drawLine(
                    color = palette.text.copy(alpha = if (isSecond) 0.35f else 0.15f),
                    start = Offset(x, height - rulerArea),
                    end = Offset(x, height - rulerArea + if (isSecond) 6.dp.toPx() else 3.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
                if (isSecond && tick > 0L) {
                    val text = textMeasurer.measure("${persianDigits((tick / 1_000).toString())}s", rulerStyle)
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
                color = palette.text.copy(alpha = 0.9f),
                start = Offset(playheadX, waveTop),
                end = Offset(playheadX, waveTop + waveHeight),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(color = palette.text, radius = 3.5.dp.toPx(), center = Offset(playheadX, waveTop))
        }
    }
}

/** What the wave shows: [left] is the share of it hidden past the view's left edge. */
private class WaveView(zoom: Float) {
    var zoom by mutableFloatStateOf(zoom)
        private set
    var left by mutableFloatStateOf(0f)
        private set

    /** The fingers are on the wave, so nothing else may move it. */
    var isTouched by mutableStateOf(false)

    fun moveTo(value: Float) {
        left = value.coerceIn(0f, (1f - 1f / zoom).coerceAtLeast(0f))
    }

    /** At [newZoom], with [wavePoint] (a share of the wave) at [viewPoint] (a share of the view). */
    fun show(newZoom: Float, wavePoint: Float, viewPoint: Float) {
        zoom = newZoom
        moveTo(wavePoint - viewPoint / newZoom)
    }
}

private enum class TouchMode { UNDECIDED, DRAG, PINCH, LET_GO }

/** The peaks, then each list after it halved by keeping the louder of every two bars. */
private fun peakLevels(peaks: List<Float>): List<List<Float>> {
    if (peaks.isEmpty()) return emptyList()
    val levels = mutableListOf(peaks)
    while (levels.last().size > 1) {
        levels += levels.last().chunked(2) { it.max() }
    }
    return levels
}

/** Share of the view the playhead may cross before the wave starts to follow it. */
private const val FOLLOW_FROM = 0.7f

/** Beyond this share of the view past the follow line, the wave glides instead of stepping. */
private const val MAX_STEP = 0.05f

/** Where in the view the playhead lands after the wave moves to find it. */
private const val ARRIVE_AT = 0.2f
private const val ARRIVE_MS = 260

/** The narrowest a bar may be before neighbouring bars are merged. */
private const val MIN_BAR_DP = 2

fun ornamentColor(kind: TeacherOrnamentKind, palette: MelovaPalette): Color = when (kind) {
    TeacherOrnamentKind.TAHRIR -> palette.gold
    TeacherOrnamentKind.VIBRATO -> palette.info
    TeacherOrnamentKind.GLIDE -> palette.teal
}
