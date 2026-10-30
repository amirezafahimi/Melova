package app.melova.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.melova.audio.scoring.VocalScorer
import app.melova.data.model.ErrorDetail
import app.melova.data.model.ErrorDetailLane
import app.melova.data.model.ErrorMarkKind
import app.melova.data.model.TahrirKind
import app.melova.platform.pad2
import app.melova.presentation.PlaybackTarget
import app.melova.ui.layout.LtrContent
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaCard
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaInfo
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaSuccess
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaWarning
import kotlin.math.abs
import kotlin.math.roundToInt

/** A wrong note's dot also lights the bars this close to it. */
private const val MOMENT_GLOW_MS = 60L

/**
 * «دیدن جزییات» یک خطا: موج استاد و هنرجو زیر هم، هر کدام در بازه‌ای که پخش
 * می‌شود. خطای نت یک نقطه است و تحریر یا ویبراتو یک بازه‌ی رنگی؛ زدن روی هر موج
 * همان تکه را آهسته پخش می‌کند و خط پخش روی موج حرکت می‌کند.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorDetailSheet(
    detail: ErrorDetail,
    playingTarget: PlaybackTarget?,
    teacherPositionMs: Long,
    studentPositionMs: Long,
    onPlayTeacher: () -> Unit,
    onPlayStudent: () -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    DetailPill(text = "تطابق ${comparison.score}٪", color = markColor)
                }

                DetailLaneCard(
                    label = "استاد",
                    icon = Icons.Outlined.Hearing,
                    laneColor = MelovaAccent,
                    markColor = markColor,
                    markKind = detail.markKind,
                    lane = detail.teacher,
                    missingText = "موج استاد در دسترس نیست.",
                    isPlaying = playingTarget == PlaybackTarget.TEACHER,
                    positionMs = teacherPositionMs,
                    onPlay = onPlayTeacher
                )
                DetailLaneCard(
                    label = "شما",
                    icon = Icons.Outlined.Mic,
                    laneColor = MelovaSecondary,
                    markColor = markColor,
                    markKind = detail.markKind,
                    lane = detail.student,
                    missingText = "این بخش در اجرای شما پیدا نشد.",
                    isPlaying = playingTarget == PlaybackTarget.USER,
                    positionMs = studentPositionMs,
                    onPlay = onPlayStudent
                )

                MarkLegend(markKind = detail.markKind, markColor = markColor, kindLabel = kindLabel)

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
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
        if (way != null && cents > 0) DetailPill(text = "$cents سنت $way", color = markColor)
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
            .padding(horizontal = 10.dp, vertical = 4.dp)
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
    missingText: String,
    isPlaying: Boolean,
    positionMs: Long,
    onPlay: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
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
                    onClick = onPlay,
                    shape = RoundedCornerShape(50),
                    color = laneColor.copy(alpha = if (isPlaying) 0.9f else 0.3f),
                    contentColor = MelovaText
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
                            text = "پخش آهسته",
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

        DetailWaveform(
            lane = lane,
            laneColor = laneColor,
            markColor = markColor,
            markKind = markKind,
            playheadMs = positionMs.takeIf { isPlaying },
            onTap = onPlay
        )
        // Time runs left to right whatever the text direction.
        LtrContent {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = formatDetailClock(lane.startMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MelovaText.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatDetailClock(lane.endMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MelovaText.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun DetailWaveform(
    lane: ErrorDetailLane,
    laneColor: Color,
    markColor: Color,
    markKind: ErrorMarkKind,
    playheadMs: Long?,
    onTap: () -> Unit
) {
    val spanMs = (lane.endMs - lane.startMs).coerceAtLeast(1L).toFloat()
    val playhead = MelovaText
    val background = MelovaText.copy(alpha = 0.04f)
    val ring = MelovaCard
    val markStart = lane.markStartMs - if (markKind == ErrorMarkKind.MOMENT) MOMENT_GLOW_MS else 0L
    val markEnd = lane.markEndMs + if (markKind == ErrorMarkKind.MOMENT) MOMENT_GLOW_MS else 0L

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .pointerInput(lane) { detectTapGestures { onTap() } }
    ) {
        val width = size.width
        val height = size.height
        fun xOf(ms: Long) = ((ms - lane.startMs) / spanMs * width).coerceIn(0f, width)
        val dotY = 10.dp.toPx()
        val waveTop = dotY + 10.dp.toPx()
        val centerY = (waveTop + height) / 2f
        val maxBar = height - waveTop - 4.dp.toPx()

        drawRoundRect(
            color = background,
            size = Size(width, height),
            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
        )

        if (markKind == ErrorMarkKind.SPAN) {
            val left = xOf(lane.markStartMs)
            val right = xOf(lane.markEndMs).coerceAtLeast(left + 2f)
            drawRect(
                color = markColor.copy(alpha = 0.16f),
                topLeft = Offset(left, 0f),
                size = Size(right - left, height)
            )
            listOf(left, right).forEach { x ->
                drawLine(
                    color = markColor.copy(alpha = 0.85f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
            // A bracket across the top names the stretch without any text.
            drawLine(
                color = markColor,
                start = Offset(left, dotY),
                end = Offset(right, dotY),
                strokeWidth = 3.dp.toPx()
            )
        }

        val barCount = lane.peaks.size.coerceAtLeast(1)
        val slot = width / barCount
        lane.peaks.forEachIndexed { index, peak ->
            val barMs = lane.startMs + ((index + 0.5f) / barCount * spanMs).toLong()
            val inMark = barMs in markStart..markEnd
            val barHeight = (peak * maxBar).coerceAtLeast(2.dp.toPx())
            val barWidth = (slot * 0.62f).coerceAtLeast(1f)
            drawRoundRect(
                color = if (inMark) markColor else laneColor.copy(alpha = 0.55f),
                topLeft = Offset(index * slot + (slot - barWidth) / 2f, centerY - barHeight / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }

        if (markKind == ErrorMarkKind.MOMENT) {
            val x = xOf(lane.markStartMs)
            drawLine(
                color = markColor.copy(alpha = 0.8f),
                start = Offset(x, dotY),
                end = Offset(x, height),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
            )
            drawCircle(color = markColor.copy(alpha = 0.25f), radius = 10.dp.toPx(), center = Offset(x, dotY))
            drawCircle(color = markColor, radius = 6.dp.toPx(), center = Offset(x, dotY))
            drawCircle(
                color = ring,
                radius = 6.dp.toPx(),
                center = Offset(x, dotY),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        if (playheadMs != null && playheadMs in lane.startMs..lane.endMs) {
            val x = xOf(playheadMs)
            drawLine(
                color = playhead.copy(alpha = 0.9f),
                start = Offset(x, waveTop - 4.dp.toPx()),
                end = Offset(x, height),
                strokeWidth = 2.dp.toPx()
            )
        }
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
                "نقطه: لحظه‌ای که نت شما از نت استاد فاصله گرفته. روی هر موج بزنید تا آهسته پخش شود."
            } else {
                "بازه‌ی رنگی: جای $kindLabel در صدای استاد و شما. روی هر موج بزنید تا آهسته پخش شود."
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
    return "${seconds / 60L}:${(seconds % 60L).pad2()}.${(safe % 1000L) / 100L}"
}

/** The «دیدن جزییات» pill on a correction or tahrir row. */
@Composable
fun ShowDetailsButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MelovaText.copy(alpha = 0.08f),
        contentColor = MelovaText
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.GraphicEq, contentDescription = null, modifier = Modifier.size(15.dp))
            Text(
                text = "دیدن جزییات",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
