package app.melova.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LongState
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.melova.audio.pitch.PitchContour
import app.melova.audio.pitch.PitchPoint
import app.melova.audio.scoring.VocalScorer
import app.melova.data.model.ErrorDetail
import app.melova.data.model.ErrorDetailLane
import app.melova.data.model.ErrorMarkKind
import app.melova.data.model.TahrirKind
import app.melova.platform.PlatformBackHandler
import app.melova.platform.pad2
import app.melova.platform.persianDigits
import app.melova.presentation.PlaybackTarget
import app.melova.ui.layout.LtrContent
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaAccentTint
import app.melova.ui.theme.MelovaCard
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaInfo
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaSuccess
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaWarning
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private const val LINE_REVEAL_MS = 600

/** Keeps the head's knob whole at either end of the lane. */
private val HEAD_INSET = 9.dp

/**
 * «دیدن جزییات» for one mistake: the teacher's and student's pitch lines one above the other,
 * each over the stretch that plays, with the teacher's line faint under the student's. A note
 * mistake is a point, a tahrir or vibrato a coloured span. Each voice has its own play and pause
 * and a head that stays where it stopped and can be dragged.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorDetailSheet(
    detail: ErrorDetail,
    playing: PlaybackTarget,
    positionOf: (PlaybackTarget) -> Long,
    onToggle: (PlaybackTarget) -> Unit,
    onScrubStart: (PlaybackTarget) -> Unit,
    onSeek: (PlaybackTarget, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val correction = detail.correction
    val comparison = detail.comparison
    val markColor = when {
        comparison != null -> MelovaGold
        correction?.direction == VocalScorer.CorrectionDirection.SHARP -> MelovaWarning
        correction?.direction == VocalScorer.CorrectionDirection.FLAT -> MelovaInfo
        else -> MelovaSuccess
    }
    val kindLabel = when {
        comparison == null -> "خطای نت"
        comparison.kind == TahrirKind.VIBRATO -> "ویبراتو"
        else -> "تحریر"
    }

    PlatformBackHandler(onBack = onDismiss)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MelovaCard
    ) {
        RtlContent {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = kindLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MelovaText,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start
                    )
                    val timeMs = comparison?.refStartMs ?: correction?.startMs ?: 0L
                    DetailPill(text = "زمان ‎${formatDetailClock(timeMs)}‏", color = MelovaText)
                }

                wordLabel(comparison?.word ?: correction?.word)?.let { word ->
                    DetailPill(text = word, color = MelovaAccent)
                }

                Text(
                    text = comparison?.message ?: correction?.message.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = markColor,
                    textAlign = TextAlign.Start
                )

                if (correction != null) {
                    NoteFacts(correction, markColor)
                } else if (comparison != null) {
                    DetailPill(text = persianDigits("تطابق ${comparison.score}٪"), color = markColor)
                }

                DetailLaneCard(
                    label = "استاد",
                    icon = Icons.Outlined.Hearing,
                    laneColor = MelovaAccent,
                    markColor = markColor,
                    markKind = detail.markKind,
                    lane = detail.teacher,
                    pitchRange = detail.pitchRange,
                    missingText = "صدای استاد در دسترس نیست.",
                    isPlaying = playing == PlaybackTarget.TEACHER,
                    position = { positionOf(PlaybackTarget.TEACHER) },
                    onToggle = { onToggle(PlaybackTarget.TEACHER) },
                    onScrubStart = { onScrubStart(PlaybackTarget.TEACHER) },
                    onSeek = { onSeek(PlaybackTarget.TEACHER, it) }
                )
                DetailLaneCard(
                    label = "شما",
                    icon = Icons.Outlined.Mic,
                    laneColor = MelovaSecondary,
                    markColor = markColor,
                    markKind = detail.markKind,
                    lane = detail.student,
                    pitchRange = detail.pitchRange,
                    missingText = "این بخش در اجرای شما پیدا نشد.",
                    isPlaying = playing == PlaybackTarget.USER,
                    position = { positionOf(PlaybackTarget.USER) },
                    onToggle = { onToggle(PlaybackTarget.USER) },
                    onScrubStart = { onScrubStart(PlaybackTarget.USER) },
                    onSeek = { onSeek(PlaybackTarget.USER, it) }
                )

                MarkLegend(markKind = detail.markKind, markColor = markColor, kindLabel = kindLabel)

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("بستن", color = MelovaText)
                }
            }
        }
    }
}

@Composable
private fun NoteFacts(correction: VocalScorer.PitchCorrection, markColor: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailPill(text = "نت استاد: ${correction.expectedNote}", color = MelovaAccent)
        if (correction.sungNote != "—") {
            DetailPill(text = "شما: ${correction.sungNote}", color = markColor)
        }
        val cents = abs(correction.centsOff).roundToInt()
        val way = when (correction.direction) {
            VocalScorer.CorrectionDirection.SHARP -> "بالاتر"
            VocalScorer.CorrectionDirection.FLAT -> "پایین‌تر"
            VocalScorer.CorrectionDirection.GOOD -> null
        }
        if (way != null && cents > 0) DetailPill(text = persianDigits("$cents سنت $way"), color = markColor)
    }
}

@Composable
private fun DetailPill(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Composable
private fun DetailLaneCard(
    label: String,
    icon: ImageVector,
    laneColor: Color,
    markColor: Color,
    markKind: ErrorMarkKind,
    lane: ErrorDetailLane?,
    pitchRange: ClosedFloatingPointRange<Float>,
    missingText: String,
    isPlaying: Boolean,
    position: () -> Long,
    onToggle: () -> Unit,
    onScrubStart: () -> Unit,
    onSeek: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(laneColor.copy(alpha = 0.08f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = laneColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MelovaText,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )
            if (lane != null) {
                Surface(
                    onClick = onToggle,
                    shape = RoundedCornerShape(50),
                    color = laneColor.copy(alpha = if (isPlaying) 0.9f else 0.2f),
                    contentColor = if (isPlaying) Color.White else MelovaAccentSoft
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = when {
                                isPlaying -> "مکث"
                                lane.speed < 1f -> "پخش آهسته"
                                else -> "پخش"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }

        if (lane == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = missingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaText.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
            return@Column
        }

        val headMs = remember(lane.startMs, lane.endMs) { mutableLongStateOf(lane.startMs) }
        var isScrubbing by remember { mutableStateOf(false) }
        val currentPosition by rememberUpdatedState(position)
        // Read on every frame, so the head glides with the sound.
        LaunchedEffect(lane.startMs, lane.endMs, isPlaying) {
            if (!isScrubbing) headMs.longValue = currentPosition()
            while (isPlaying) withFrameMillis { if (!isScrubbing) headMs.longValue = currentPosition() }
        }

        PitchLane(
            lane = lane,
            pitchRange = pitchRange,
            laneColor = laneColor,
            markColor = markColor,
            markKind = markKind,
            headMs = headMs,
            isScrubbing = isScrubbing,
            onScrubStart = {
                isScrubbing = true
                onScrubStart()
            },
            onScrubEnd = {
                isScrubbing = false
                onSeek(headMs.longValue)
            },
            onTap = { ms ->
                headMs.longValue = ms
                onSeek(ms)
            }
        )
        // Time runs left to right whatever the text direction.
        LtrContent {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDetailClock(lane.startMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MelovaText.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                )
                HeadClock(headMs)
                Text(
                    text = formatDetailClock(lane.endMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MelovaText.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun HeadClock(headMs: LongState) {
    val clock by remember { derivedStateOf { formatDetailClock(headMs.longValue) } }
    Text(
        text = clock,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MelovaText
    )
}

/**
 * A voice's notes as a line over a faint semitone grid: higher on the card is a higher note, the
 * same in both lanes. What has played is bright, and the head can be dragged or tapped anywhere.
 */
@Composable
private fun PitchLane(
    lane: ErrorDetailLane,
    pitchRange: ClosedFloatingPointRange<Float>,
    laneColor: Color,
    markColor: Color,
    markKind: ErrorMarkKind,
    headMs: MutableLongState,
    isScrubbing: Boolean,
    onScrubStart: () -> Unit,
    onScrubEnd: () -> Unit,
    onTap: (Long) -> Unit
) {
    val spanMs = (lane.endMs - lane.startMs).coerceAtLeast(1L).toFloat()
    val headColor = MelovaText
    val background = MelovaText.copy(alpha = 0.04f)
    val gridColor = MelovaText.copy(alpha = 0.05f)
    val guideColor = MelovaText.copy(alpha = 0.2f)
    val ring = MelovaCard
    val currentOnScrubStart by rememberUpdatedState(onScrubStart)
    val currentOnScrubEnd by rememberUpdatedState(onScrubEnd)
    val currentOnTap by rememberUpdatedState(onTap)
    val reveal by animateFloatAsState(
        targetValue = if (lane.pitch.isEmpty()) 0f else 1f,
        animationSpec = tween(LINE_REVEAL_MS)
    )
    val points = remember(lane.pitch) { lane.pitch.flatten() }
    val guidePoints = remember(lane.guide) { lane.guide.flatten() }
    val inset = with(LocalDensity.current) { HEAD_INSET.toPx() }
    fun msAt(x: Float, width: Int) =
        lane.startMs + (((x - inset) / (width - 2 * inset)).coerceIn(0f, 1f) * spanMs).toLong()

    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(136.dp)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(lane.startMs, lane.endMs) {
                detectTapGestures { offset -> currentOnTap(msAt(offset.x, size.width)) }
            }
            .pointerInput(lane.startMs, lane.endMs) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        headMs.longValue = msAt(offset.x, size.width)
                        currentOnScrubStart()
                    },
                    onDragEnd = { currentOnScrubEnd() },
                    onDragCancel = { currentOnScrubEnd() },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        headMs.longValue = msAt(change.position.x, size.width)
                    }
                )
            }
            .drawWithCache {
                val top = 18.dp.toPx()
                val bottom = size.height - 12.dp.toPx()
                val lowest = pitchRange.start
                val noteSpan = (pitchRange.endInclusive - lowest).coerceAtLeast(1f)
                fun xOf(ms: Long) = inset + (ms - lane.startMs) / spanMs * (size.width - 2 * inset)
                fun yOf(note: Float) = bottom - (note - lowest) / noteSpan * (bottom - top)
                fun offsets(line: List<List<PitchPoint>>) =
                    line.map { run -> run.map { Offset(xOf(it.timeMs), yOf(it.semitone)) } }

                val linePath = pitchPath(offsets(lane.pitch))
                val guidePath = pitchPath(offsets(lane.guide))
                val offStretch = if (markKind == ErrorMarkKind.MOMENT) {
                    PitchContour.offStretch(lane.pitch, guidePoints, lane.markStartMs)
                } else {
                    emptyList()
                }
                // The gap between what was sung and what the teacher sang, around the mistake.
                val gapPath = Path().apply {
                    offStretch.forEachIndexed { index, (point, _) ->
                        val at = Offset(xOf(point.timeMs), yOf(point.semitone))
                        if (index == 0) moveTo(at.x, at.y) else lineTo(at.x, at.y)
                    }
                    offStretch.asReversed().forEach { (point, guideNote) ->
                        lineTo(xOf(point.timeMs), yOf(guideNote))
                    }
                    close()
                }
                val markRange = when {
                    markKind == ErrorMarkKind.SPAN -> lane.markStartMs..lane.markEndMs
                    offStretch.isEmpty() -> null
                    else -> offStretch.first().first.timeMs..offStretch.last().first.timeMs
                }
                val lineStroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                val nearGlow = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                val farGlow = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                val guideStroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                val markStroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

                onDrawBehind {
                    drawRect(color = background)
                    val firstLine = ceil(lowest).toInt()
                    val lastLine = floor(lowest + noteSpan).toInt()
                    for (note in firstLine..lastLine) {
                        val y = yOf(note.toFloat())
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    }

                    if (markKind == ErrorMarkKind.SPAN) {
                        val left = xOf(lane.markStartMs)
                        val right = xOf(lane.markEndMs).coerceAtLeast(left + 2f)
                        drawRect(
                            color = markColor.copy(alpha = 0.13f),
                            topLeft = Offset(left, 0f),
                            size = Size(right - left, size.height)
                        )
                        // A bracket across the top names the stretch without any text.
                        drawLine(
                            color = markColor,
                            start = Offset(left, 1.5.dp.toPx()),
                            end = Offset(right, 1.5.dp.toPx()),
                            strokeWidth = 3.dp.toPx()
                        )
                    }

                    val headX = xOf(headMs.longValue)
                    clipRect(right = size.width * reveal) {
                        drawPath(guidePath, color = guideColor, style = guideStroke)
                        drawPath(gapPath, color = markColor.copy(alpha = 0.28f))
                        clipRect(left = headX) {
                            drawPath(linePath, color = laneColor, alpha = 0.75f, style = lineStroke)
                        }
                        clipRect(right = headX) {
                            drawPath(linePath, color = laneColor, alpha = 0.1f, style = farGlow)
                            drawPath(linePath, color = laneColor, alpha = 0.22f, style = nearGlow)
                            drawPath(linePath, color = laneColor, style = lineStroke)
                        }
                        if (markRange != null) {
                            clipRect(left = xOf(markRange.first), right = xOf(markRange.last).coerceAtLeast(xOf(markRange.first) + 2f)) {
                                drawPath(linePath, color = markColor, style = markStroke)
                            }
                        }
                    }

                    if (markKind == ErrorMarkKind.MOMENT) {
                        val x = xOf(lane.markStartMs)
                        val y = PitchContour.noteNear(points, lane.markStartMs)?.let(::yOf)
                        val guideY = PitchContour.noteNear(guidePoints, lane.markStartMs)?.let(::yOf)
                        drawLine(
                            color = markColor.copy(alpha = 0.5f),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
                        )
                        val dotY = y ?: top
                        if (guideY != null) {
                            drawLine(
                                color = markColor,
                                start = Offset(x, guideY),
                                end = Offset(x, dotY),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                        val dot = Offset(x, dotY)
                        drawCircle(color = markColor.copy(alpha = 0.25f), radius = 10.dp.toPx(), center = dot)
                        drawCircle(color = markColor, radius = 5.5.dp.toPx(), center = dot)
                        drawCircle(color = ring, radius = 5.5.dp.toPx(), center = dot, style = Stroke(1.5.dp.toPx()))
                    }

                    drawLine(
                        color = headColor.copy(alpha = 0.85f),
                        start = Offset(headX, 0f),
                        end = Offset(headX, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                    PitchContour.noteNear(points, headMs.longValue)?.let { note ->
                        val bead = Offset(headX, yOf(note))
                        drawCircle(color = laneColor, radius = 4.5.dp.toPx(), center = bead)
                        drawCircle(color = headColor, radius = 4.5.dp.toPx(), center = bead, style = Stroke(1.5.dp.toPx()))
                    }
                    val knob = Offset(headX, 8.dp.toPx())
                    if (isScrubbing) drawCircle(color = headColor.copy(alpha = 0.2f), radius = 13.dp.toPx(), center = knob)
                    drawCircle(color = headColor, radius = (if (isScrubbing) 8.dp else 6.dp).toPx(), center = knob)
                    drawCircle(color = ring, radius = 2.dp.toPx(), center = knob)
                }
            }
    )
}

/** A soft line through [runs]. */
private fun pitchPath(runs: List<List<Offset>>): Path = Path().apply {
    runs.filter { it.isNotEmpty() }.forEach { run ->
        moveTo(run.first().x, run.first().y)
        for (index in 1 until run.size) {
            val previous = run[index - 1]
            val middle = (previous + run[index]) / 2f
            quadraticTo(previous.x, previous.y, middle.x, middle.y)
        }
        lineTo(run.last().x, run.last().y)
    }
}

@Composable
private fun MarkLegend(markKind: ErrorMarkKind, markColor: Color, kindLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(width = 22.dp, height = 14.dp)) {
            if (markKind == ErrorMarkKind.MOMENT) {
                drawCircle(color = markColor, radius = 5.dp.toPx(), center = center)
            } else {
                drawRect(color = markColor.copy(alpha = 0.25f), size = size)
                drawLine(markColor, Offset(0f, 1.dp.toPx()), Offset(size.width, 1.dp.toPx()), 3.dp.toPx())
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (markKind == ErrorMarkKind.MOMENT) {
                "نقطه: لحظه‌ای که نت شما از نت استاد فاصله گرفته. خط کم‌رنگ زیر خط شما، نت‌های استاد است. نشانگر را بکشید تا به هر لحظه بروید."
            } else {
                "بازه‌ی رنگی: جای $kindLabel در صدای استاد و شما. خط کم‌رنگ زیر خط شما، نت‌های استاد است. نشانگر را بکشید تا به هر لحظه بروید."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MelovaText.copy(alpha = 0.65f),
            textAlign = TextAlign.Start
        )
    }
}

private fun formatDetailClock(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val seconds = safe / 1000L
    return persianDigits("${seconds / 60L}:${(seconds % 60L).pad2()}.${(safe % 1000L) / 100L}")
}

/** The «دیدن جزییات» pill on a correction or tahrir row. */
@Composable
fun ShowDetailsButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MelovaAccentTint,
        contentColor = MelovaAccentSoft
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
                text = "دیدن جزییات",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
