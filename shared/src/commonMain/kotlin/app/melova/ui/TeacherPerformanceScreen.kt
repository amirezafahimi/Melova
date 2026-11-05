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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import app.melova.platform.persianDigits
import app.melova.presentation.TeacherPerformanceUiState
import app.melova.presentation.TeacherPerformanceViewModel
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import app.melova.ui.components.MelovaRowSkeleton
import app.melova.ui.components.MelovaSegmentedControl
import app.melova.ui.components.MelovaShimmerVeil
import app.melova.ui.components.MelovaSkeleton
import app.melova.ui.components.MelovaWaveSkeleton
import app.melova.ui.theme.MelovaDimens
import app.melova.ui.theme.MelovaOutline
import app.melova.ui.theme.MelovaSmallShape
import app.melova.ui.theme.MelovaTextMuted
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.LetterTag
import app.melova.ui.components.MelovaMediaIconButton
import app.melova.ui.components.OrnamentRow
import app.melova.ui.components.formatPreciseClock
import app.melova.ui.components.ornamentNumbers
import app.melova.ui.components.MelovaTopBar
import app.melova.ui.components.TeacherOrnamentWaveform
import app.melova.ui.components.ornamentColor
import app.melova.ui.layout.LtrContent
import app.melova.ui.layout.PagePadding
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.LocalMelovaPalette
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaAccentTint
import app.melova.ui.theme.MelovaText
import app.melova.platform.formatFixed
import app.melova.platform.pad2

/** The teacher's performance in detail: the wave with its T and V segments, each playable slowly. */
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
                .padding(PagePadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    uiState.isLoading -> LoadingSkeleton()
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
                containerColor = MelovaAccentTint,
                contentColor = MelovaAccentSoft,
                buttonSize = MelovaDimens.ButtonHeight
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
                buttonSize = 40.dp,
                iconSize = 20.dp
            )
            MelovaMediaIconButton(
                onClick = viewModel::zoomIn,
                enabled = uiState.zoom < TeacherPerformanceViewModel.MAX_ZOOM,
                icon = Icons.Default.ZoomIn,
                contentDescription = "بزرگ‌نمایی موج",
                containerColor = MelovaAccentTint,
                contentColor = MelovaAccentSoft,
                buttonSize = 40.dp,
                iconSize = 20.dp
            )
        }

        // The wave arrives before its ornaments do. It plays from the first moment, and the light
        // over it says the tahrirs and vibratos are still being read, instead of a bare wave.
        Box {
            LtrContent {
                TeacherOrnamentWaveform(
                    peaks = uiState.peaks,
                    durationMs = uiState.durationMs,
                    playheadMs = uiState.positionMs,
                    ornaments = uiState.ornaments,
                    selectedIndex = uiState.playingIndex ?: uiState.selectedIndex,
                    zoom = uiState.zoom,
                    maxZoom = TeacherPerformanceViewModel.MAX_ZOOM,
                    onZoomChange = viewModel::setZoom,
                    onSeekToMs = viewModel::playAt,
                    onOrnamentClick = viewModel::playSegment
                )
            }
            if (uiState.isOrnamentsLoading) MelovaShimmerVeil(Modifier.matchParentSize())
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
                    text = persianDigits("بزرگ‌نمایی ${formatFixed(uiState.zoom, 1)}×"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MelovaText.copy(alpha = 0.5f)
                )
            }
        }
        Text(
            text = if (uiState.isOrnamentsLoading) {
                "اجرا آماده‌ی شنیدن است؛ تحریرها و ویبراتوها همین حالا روی موج می‌آیند."
            } else {
                "روی هر بازه بزنید تا همان مصوت پخش شود؛ با دو انگشت بزرگ‌نمایی کنید."
            },
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
        val speeds = TeacherPerformanceViewModel.SPEEDS
        MelovaSegmentedControl(
            options = speeds.map(::formatSpeed),
            selectedIndex = speeds.indexOfFirst { it == uiState.speed }.coerceAtLeast(0),
            onSelect = { viewModel.setSpeed(speeds[it]) }
        )
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
            // The rows take their places while the analysis runs, so the wait has the page's own shape.
            uiState.isOrnamentsLoading -> {
                Text(
                    text = "در حال تحلیل اجرای استاد… (بار اول کمی طول می‌کشد)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaTextMuted
                )
                repeat(3) { MelovaRowSkeleton(modifier = Modifier.padding(vertical = 6.dp)) }
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
private fun ChoicePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: Boolean = false
) {
    // A tint marks the choice and an outline marks the rest: a solid fill here shouted over the wave.
    Row(
        modifier = modifier
            .clip(MelovaSmallShape)
            .background(if (selected) MelovaAccentTint else Color.Transparent)
            .border(1.dp, MelovaOutline, MelovaSmallShape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val content = if (selected) MelovaAccentSoft else MelovaTextMuted
        if (leadingIcon) {
            Icon(
                imageVector = Icons.Default.Repeat,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(18.dp).padding(end = 4.dp)
            )
        }
        LtrContent {
            Text(text = text, style = MaterialTheme.typography.labelLarge, color = content)
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    MelovaSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

/**
 * The page while the teacher's audio is on its way: its own cards, with the wave, the speeds and
 * the rows standing in their places, so nothing jumps when the real performance arrives.
 */
@Composable
private fun LoadingSkeleton() {
    SectionCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MelovaSkeleton(Modifier.size(MelovaDimens.ButtonHeight))
            MelovaSkeleton(Modifier.width(80.dp).height(14.dp))
            Spacer(modifier = Modifier.weight(1f))
            MelovaSkeleton(Modifier.size(MelovaDimens.CompactButtonHeight))
            MelovaSkeleton(Modifier.size(MelovaDimens.CompactButtonHeight))
        }
        MelovaWaveSkeleton()
        Text(
            text = "در حال آماده‌سازی اجرای استاد…",
            style = MaterialTheme.typography.bodySmall,
            color = MelovaTextMuted
        )
    }
    SectionCard {
        MelovaSkeleton(Modifier.width(72.dp).height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) { MelovaSkeleton(Modifier.weight(1f).height(MelovaDimens.CompactButtonHeight)) }
        }
    }
    SectionCard {
        MelovaSkeleton(Modifier.width(160.dp).height(14.dp))
        repeat(3) { MelovaRowSkeleton(modifier = Modifier.padding(vertical = 6.dp)) }
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
        modifier = Modifier.fillMaxWidth().height(MelovaDimens.ButtonHeight),
        shape = MelovaSmallShape,
        colors = ButtonDefaults.buttonColors(containerColor = MelovaAccentTint, contentColor = MelovaAccentSoft)
    ) {
        Text("تلاش دوباره")
    }
}

private fun formatSpeed(speed: Float): String =
    persianDigits(if (speed == speed.toInt().toFloat()) "${speed.toInt()}×" else "${speed}×")
