package app.melova.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.melova.data.model.TeacherOrnament
import app.melova.data.model.TeacherOrnamentKind
import app.melova.presentation.TeacherPerformanceUiState
import app.melova.presentation.TeacherPerformanceViewModel
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaMediaIconButton
import app.melova.ui.components.MelovaTopBar
import app.melova.ui.components.TeacherOrnamentWaveform
import app.melova.ui.components.ornamentColor
import app.melova.ui.layout.LtrContent
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.LocalMelovaPalette
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaAccentTint
import app.melova.ui.theme.MelovaCard
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaText
import app.melova.platform.formatFixed
import app.melova.platform.pad2

/** جزئیات اجرای استاد: موج صدا با بازه‌های T و V و پخش آهسته‌ی هر بازه. */
@Composable
fun TeacherPerformanceScreen(
    onBack: () -> Unit,
    viewModel: TeacherPerformanceViewModel = viewModel { TeacherPerformanceViewModel(createSavedStateHandle()) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MelovaBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RtlContent {
                MelovaTopBar(
                    title = "جزئیات اجرای استاد",
                    subtitle = listOf(uiState.title, uiState.teacherName)
                        .filter { it.isNotBlank() }
                        .joinToString(" • "),
                    onBack = {
                        viewModel.stopPlayback()
                        onBack()
                    }
                )

                when {
                    uiState.isLoading -> LoadingCard("در حال آماده‌سازی اجرای استاد…")
                    uiState.errorMessage != null -> MessageCard(
                        message = uiState.errorMessage.orEmpty(),
                        onRetry = viewModel::load
                    )
                    uiState.isReady -> {
                        WaveformCard(uiState, viewModel)
                        PlaybackControlsCard(uiState, viewModel)
                        OrnamentListCard(uiState, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun WaveformCard(uiState: TeacherPerformanceUiState, viewModel: TeacherPerformanceViewModel) {
    SectionCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MelovaMediaIconButton(
                onClick = viewModel::togglePlayback,
                icon = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (uiState.isPlaying) "توقف موقت" else "پخش اجرای استاد",
                containerColor = MelovaSecondary
            )
            LtrContent {
                Text(
                    text = "${formatPreciseClock(uiState.positionMs)} / ${formatPreciseClock(uiState.durationMs)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MelovaText.copy(alpha = 0.75f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            MelovaMediaIconButton(
                onClick = viewModel::zoomOut,
                enabled = uiState.zoom > 1f,
                icon = Icons.Default.ZoomOut,
                contentDescription = "کوچک‌نمایی موج",
                containerColor = MelovaAccentTint,
                contentColor = MelovaAccentSoft,
                buttonSize = 36.dp,
                iconSize = 20.dp
            )
            MelovaMediaIconButton(
                onClick = viewModel::zoomIn,
                enabled = uiState.zoom < TeacherPerformanceViewModel.MAX_ZOOM,
                icon = Icons.Default.ZoomIn,
                contentDescription = "بزرگ‌نمایی موج",
                containerColor = MelovaAccentTint,
                contentColor = MelovaAccentSoft,
                buttonSize = 36.dp,
                iconSize = 20.dp
            )
        }

        LtrContent {
            TeacherOrnamentWaveform(
                peaks = uiState.peaks,
                durationMs = uiState.durationMs,
                playheadMs = uiState.positionMs,
                ornaments = uiState.ornaments,
                selectedIndex = uiState.playingIndex ?: uiState.selectedIndex,
                zoom = uiState.zoom,
                onZoomChange = viewModel::setZoom,
                onSeekToMs = viewModel::playAt,
                onOrnamentClick = viewModel::playSegment
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val palette = LocalMelovaPalette.current
            TeacherOrnamentKind.entries.forEach { kind ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LetterTag(kind.letter, ornamentColor(kind, palette))
                    Text(
                        text = kind.legend,
                        style = MaterialTheme.typography.labelMedium,
                        color = MelovaText.copy(alpha = 0.75f)
                    )
                }
            }
            if (uiState.zoom > 1f) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "بزرگ‌نمایی ${formatFixed(uiState.zoom, 1)}×",
                    style = MaterialTheme.typography.labelSmall,
                    color = MelovaText.copy(alpha = 0.5f)
                )
            }
        }
        Text(
            text = "روی هر بازه بزنید تا فقط همان مصوت پخش شود؛ بیرون از بازه‌ها، پخش از همان لحظه شروع می‌شود. " +
                "با دو انگشت موج را بزرگ یا کوچک کنید.",
            style = MaterialTheme.typography.labelSmall,
            color = MelovaText.copy(alpha = 0.45f),
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun PlaybackControlsCard(uiState: TeacherPerformanceUiState, viewModel: TeacherPerformanceViewModel) {
    SectionCard {
        Text(
            text = "سرعت پخش",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MelovaText
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TeacherPerformanceViewModel.SPEEDS.forEach { speed ->
                ChoicePill(
                    text = formatSpeed(speed),
                    selected = uiState.speed == speed,
                    onClick = { viewModel.setSpeed(speed) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        ChoicePill(
            text = if (uiState.loopSegment) "تکرار بازه: روشن" else "تکرار بازه: خاموش",
            selected = uiState.loopSegment,
            onClick = viewModel::toggleLoop,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = true
        )
    }
}

@Composable
private fun OrnamentListCard(uiState: TeacherPerformanceUiState, viewModel: TeacherPerformanceViewModel) {
    SectionCard {
        Text(
            text = "تحریرها، ویبراتوها و گذرهای استاد",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MelovaText
        )
        when {
            uiState.isOrnamentsLoading -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MelovaAccent, strokeWidth = 2.dp)
                Text(
                    text = "در حال تحلیل اجرای استاد… (بار اول کمی طول می‌کشد)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaText.copy(alpha = 0.8f)
                )
            }
            uiState.ornamentsError != null -> {
                Text(
                    text = uiState.ornamentsError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaText.copy(alpha = 0.8f),
                    textAlign = TextAlign.Start
                )
                RetryButton(onClick = viewModel::loadOrnaments)
            }
            uiState.ornaments.isEmpty() -> Text(
                text = "در این اجرا تحریر، ویبراتو یا گذری پیدا نشد.",
                style = MaterialTheme.typography.bodySmall,
                color = MelovaText.copy(alpha = 0.7f)
            )
            else -> {
                val numbers = ornamentNumbers(uiState.ornaments)
                uiState.ornaments.forEachIndexed { index, ornament ->
                    OrnamentRow(
                        ornament = ornament,
                        number = numbers[index],
                        isSelected = index == (uiState.playingIndex ?: uiState.selectedIndex),
                        isPlaying = index == uiState.playingIndex && uiState.isPlaying,
                        onClick = {
                            if (index == uiState.playingIndex && uiState.isPlaying) {
                                viewModel.stopPlayback()
                            } else {
                                viewModel.playSegment(index)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OrnamentRow(
    ornament: TeacherOrnament,
    number: Int,
    isSelected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val color = ornamentColor(ornament.kind, LocalMelovaPalette.current)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) color.copy(alpha = 0.16f) else MelovaText.copy(alpha = 0.04f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${ornament.kind.title} $number",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MelovaText
            )
            LtrContent {
                Text(
                    text = "${formatPreciseClock(ornament.startMs)} → ${formatPreciseClock(ornament.endMs)}" +
                        "  (${formatFixed(ornament.durationMs / 1000f, 2)}s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MelovaText.copy(alpha = 0.6f)
                )
            }
        }
        MelovaMediaIconButton(
            onClick = onClick,
            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "توقف" else "پخش این بازه",
            containerColor = color,
            buttonSize = 34.dp,
            iconSize = 18.dp
        )
    }
}

/** The T/V mark: just the letter, bold, in the ornament's colour. */
@Composable
private fun LetterTag(letter: String, color: Color) {
    Text(
        text = letter,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Black,
        color = color
    )
}


@Composable
private fun ChoicePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MelovaAccent else MelovaAccentTint)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon) {
            Icon(
                imageVector = Icons.Default.Repeat,
                contentDescription = null,
                tint = if (selected) Color.White else MelovaAccentSoft,
                modifier = Modifier.size(18.dp).padding(end = 4.dp)
            )
        }
        LtrContent {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else MelovaAccentSoft
            )
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MelovaCard),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun LoadingCard(message: String) {
    SectionCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MelovaAccent, strokeWidth = 2.dp)
            Text(text = message, style = MaterialTheme.typography.bodySmall, color = MelovaText)
        }
    }
}

@Composable
private fun MessageCard(message: String, onRetry: () -> Unit) {
    SectionCard {
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MelovaText)
        RetryButton(onClick = onRetry)
    }
}

@Composable
private fun RetryButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent)
    ) {
        Text("تلاش دوباره")
    }
}

/** Numbers each kind on its own: تحریر ۱، تحریر ۲، ویبراتو ۱، گذر ۱… */
private fun ornamentNumbers(ornaments: List<TeacherOrnament>): List<Int> {
    val counts = mutableMapOf<TeacherOrnamentKind, Int>()
    return ornaments.map { ornament ->
        val next = (counts[ornament.kind] ?: 0) + 1
        counts[ornament.kind] = next
        next
    }
}

private fun formatSpeed(speed: Float): String =
    if (speed == speed.toInt().toFloat()) "${speed.toInt()}×" else "${speed}×"

/** m:ss.cc — hundredths, so a vowel's edges can be read exactly. */
private fun formatPreciseClock(timeMs: Long): String {
    val clamped = timeMs.coerceAtLeast(0L)
    val minutes = clamped / 60_000
    val seconds = (clamped / 1000) % 60
    val hundredths = (clamped % 1000) / 10
    return "$minutes:${seconds.pad2()}.${hundredths.pad2()}"
}
