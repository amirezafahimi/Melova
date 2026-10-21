package app.melova.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.melova.audio.scoring.VocalAnalysisFeedback
import app.melova.presentation.PlaybackTarget
import app.melova.data.model.TahrirComparison
import app.melova.data.model.VocalScoreResult
import app.melova.presentation.VocalTrainingMode
import app.melova.presentation.VocalTrainingUiState
import app.melova.presentation.VocalTrainingViewModel
import app.melova.audio.scoring.ReRecordReason
import app.melova.ui.components.MelovaMediaIconButton
import app.melova.ui.components.MelovaProgressBar
import app.melova.ui.components.ScoreMetricsList
import app.melova.ui.components.StudentRecordingWaveform
import app.melova.ui.components.VocalTrainingCorrectionRow
import app.melova.ui.components.VocalTrainingTahrirRow
import app.melova.ui.layout.LtrContent
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaBackgroundEnd
import app.melova.ui.theme.MelovaBackgroundStart
import app.melova.ui.theme.MelovaCard
import app.melova.ui.theme.MelovaSecondary

@Composable
fun VocalTrainingScreen(
    onBack: () -> Unit,
    viewModel: VocalTrainingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onRecordClicked()
        else viewModel.onRecordPermissionDenied()
    }

    fun requestRecord() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) viewModel.onRecordClicked()
        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MelovaBackgroundStart, MelovaBackgroundEnd)
                    )
                )
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RtlContent {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت",
                                tint = Color.White
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "آموزش آواز",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "مقایسه کل اجرا — نه ثانیه‌به‌ثانیه",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }

                    LessonCard(uiState)

                    if (uiState.isLessonReady) {
                        TeacherReferenceSection(
                            teacherName = uiState.teacherName,
                            durationSeconds = uiState.durationSeconds,
                            audioFormatLabel = uiState.teacherAudioFormatLabel,
                            waveformPeaks = uiState.teacherWaveformPeaks,
                            playbackPositionMs = uiState.teacherPlaybackPositionMs,
                            playbackProgress = if (uiState.playbackTarget == PlaybackTarget.TEACHER) {
                                uiState.progress
                            } else {
                                0f
                            },
                            isPlaybackActive = uiState.playbackTarget == PlaybackTarget.TEACHER,
                            isPlaying = uiState.isPlayingTeacherSample,
                            enabled = uiState.mode != VocalTrainingMode.RECORDING &&
                                uiState.mode != VocalTrainingMode.ANALYZING,
                            onTogglePlayback = viewModel::toggleTeacherPlayback,
                            onPlayAtMs = viewModel::playTeacherAt,
                            onStopPlayback = viewModel::onStopClicked
                        )
                    }

                    if (uiState.isLessonReady) {
                        StudentRecordingPanel(
                            uiState = uiState,
                            scoreResult = uiState.scoreResult,
                            onRecord = ::requestRecord,
                            onStopRecording = viewModel::onStopClicked,
                            onStopPlayback = viewModel::onStopClicked,
                            onRetryRecording = viewModel::retryRecording,
                            onToggleUserPlayback = viewModel::toggleUserRecordingPlayback,
                            onPlayUserRecordingAt = viewModel::playUserRecordingAt,
                            onPlayCorrectionFromMarker = viewModel::playCorrectionFromMarker
                        )
                    }

                    HeadphoneHintBanner()

                    AnimatedVisibility(
                        visible = uiState.scoreResult != null,
                        enter = fadeIn() + slideInVertically { it / 2 }
                    ) {
                        uiState.scoreResult?.let { result ->
                            HolisticScoreResultCard(
                                result = result,
                                selectedCorrectionStartMs = uiState.selectedCorrectionStartMs,
                                playingSnippetStartMs = uiState.playingSnippetStartMs,
                                playingSnippetTarget = uiState.playingSnippetTarget,
                                isPlayingUserRecording = uiState.isPlayingUserRecording,
                                isPlayingTeacherSample = uiState.isPlayingTeacherSample,
                                onPlayCorrectionStudentSnippet = viewModel::playCorrectionSnippet,
                                onPlayCorrectionTeacherSnippet = viewModel::playCorrectionTeacherSnippet,
                                onPlayTahrirStudentSnippet = viewModel::playTahrirStudentSnippet,
                                onPlayTahrirTeacherSnippet = viewModel::playTahrirTeacherSnippet,
                                onRetryRecording = viewModel::retryRecording,
                                onDismiss = viewModel::dismissResults
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonCard(uiState: VocalTrainingUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MelovaCard),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = uiState.lessonTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Start
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MelovaSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = uiState.teacherName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MelovaSecondary,
                    textAlign = TextAlign.Start
                )
                Text(
                    text = "• ${uiState.durationSeconds} ثانیه",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Start
                )
            }

            if (uiState.teacherAudioFormatLabel.isNotBlank()) {
                LtrContent {
                    Text(
                        text = uiState.teacherAudioFormatLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MelovaAccentSoft.copy(alpha = 0.85f),
                        textAlign = TextAlign.Start
                    )
                }
            }

            if (uiState.teacherSourceLabel.isNotBlank()) {
                Text(
                    text = uiState.teacherSourceLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Start
                )
            }

            Text(
                text = uiState.lessonDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f,
                textAlign = TextAlign.Start
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MelovaSecondary.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = uiState.statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
private fun TeacherReferenceSection(
    teacherName: String,
    durationSeconds: Int,
    audioFormatLabel: String,
    waveformPeaks: List<Float>,
    playbackPositionMs: Long,
    playbackProgress: Float,
    isPlaybackActive: Boolean,
    isPlaying: Boolean,
    enabled: Boolean,
    onTogglePlayback: () -> Unit,
    onPlayAtMs: (Long) -> Unit,
    onStopPlayback: () -> Unit
) {
    val totalMs = (durationSeconds.coerceAtLeast(1) * 1000L)
    val currentMs = if (isPlaybackActive) {
        playbackPositionMs.coerceIn(0L, totalMs)
    } else {
        0L
    }
    val playheadMs = if (isPlaybackActive) playbackPositionMs else 0L

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MelovaCard),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Hearing,
                    contentDescription = null,
                    tint = MelovaSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "نمونه استاد",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = buildString {
                            append(teacherName)
                            append(" • ")
                            append(durationSeconds.coerceAtLeast(1))
                            append(" ثانیه")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                        textAlign = TextAlign.Start
                    )
                    if (audioFormatLabel.isNotBlank()) {
                        LtrContent {
                            Text(
                                text = audioFormatLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MelovaAccentSoft.copy(alpha = 0.8f),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isPlaybackActive) {
                        MelovaMediaIconButton(
                            onClick = onStopPlayback,
                            icon = Icons.Default.Stop,
                            contentDescription = "توقف پخش استاد",
                            containerColor = MelovaAccent.copy(alpha = 0.28f),
                            contentColor = MelovaAccentSoft
                        )
                    } else {
                        MelovaMediaIconButton(
                            onClick = onTogglePlayback,
                            enabled = enabled,
                            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "توقف موقت" else "پخش نمونه استاد",
                            containerColor = MelovaSecondary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LtrContent {
                    Text(
                        text = formatPlaybackClock(totalMs),
                        style = MaterialTheme.typography.labelLarge,
                        color = MelovaSecondary
                    )
                }
                LtrContent {
                    Text(
                        text = formatPlaybackClock(currentMs),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
            }

            if (waveformPeaks.isNotEmpty()) {
                StudentRecordingWaveform(
                    peaks = waveformPeaks,
                    durationMs = totalMs,
                    playheadMs = playheadMs,
                    corrections = emptyList(),
                    selectedCorrectionStartMs = null,
                    onSeekToMs = { if (enabled) onPlayAtMs(it) },
                    onCorrectionClick = {}
                )
                Text(
                    text = "روی موج بزنید تا از همان لحظه پخش شود",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.45f),
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
private fun StudentRecordingPanel(
    uiState: VocalTrainingUiState,
    scoreResult: VocalScoreResult?,
    onRecord: () -> Unit,
    onStopRecording: () -> Unit,
    onStopPlayback: () -> Unit,
    onRetryRecording: () -> Unit,
    onToggleUserPlayback: () -> Unit,
    onPlayUserRecordingAt: (Long) -> Unit,
    onPlayCorrectionFromMarker: (app.melova.audio.scoring.VocalScorer.PitchCorrection) -> Unit
) {
    val isRecording = uiState.mode == VocalTrainingMode.RECORDING
    val isAnalyzing = uiState.mode == VocalTrainingMode.ANALYZING
    val showResultRecording = uiState.mode == VocalTrainingMode.RESULT && scoreResult != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MelovaCard),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when {
                isRecording -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = MelovaAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "در حال ضبط",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                textAlign = TextAlign.Start
                            )
                            Text(
                                text = "همان ملودی استاد را بخوانید",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.65f),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                    MelovaProgressBar(
                        progress = uiState.progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MelovaAccent,
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                    OutlinedButton(
                        onClick = onStopRecording,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, MelovaAccentSoft.copy(alpha = 0.75f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MelovaAccent.copy(alpha = 0.10f),
                            contentColor = MelovaAccentSoft
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("پایان ضبط")
                    }
                }

                isAnalyzing -> {
                    Text(
                        text = "تحلیل اجرای شما",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Start
                    )
                    MelovaProgressBar(
                        progress = uiState.progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MelovaAccentSoft,
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MelovaAccent,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "در حال استخراج نت‌ها و مقایسه با استاد...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Start
                        )
                    }
                }

                showResultRecording -> {
                    UserRecordingSection(
                        durationSeconds = uiState.userRecordingDurationSeconds,
                        playbackPositionMs = uiState.userPlaybackPositionMs,
                        playbackProgress = if (uiState.playbackTarget == PlaybackTarget.USER) {
                            uiState.progress
                        } else {
                            0f
                        },
                        waveformPeaks = uiState.waveformPeaks,
                        corrections = scoreResult!!.corrections,
                        selectedCorrectionStartMs = uiState.selectedCorrectionStartMs,
                        isPlaybackActive = uiState.playbackTarget == PlaybackTarget.USER,
                        isPlaying = uiState.isPlayingUserRecording,
                        onTogglePlayback = onToggleUserPlayback,
                        onStopPlayback = onStopPlayback,
                        onPlayAtMs = onPlayUserRecordingAt,
                        onPlayCorrectionFromMarker = onPlayCorrectionFromMarker
                    )
                    Button(
                        onClick = onRetryRecording,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ضبط مجدد")
                    }
                }

                else -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GraphicEq,
                            contentDescription = null,
                            tint = MelovaAccentSoft,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "اجرای شما",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                textAlign = TextAlign.Start
                            )
                            Text(
                                text = "پس از شنیدن استاد، ضبط را شروع کنید",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.65f),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                    Button(
                        onClick = onRecord,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("شروع ضبط")
                    }
                }
            }
        }
    }
}

@Composable
private fun HeadphoneHintBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF4A3F6B))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Headphones,
            contentDescription = null,
            tint = Color(0xFFFFD180),
            modifier = Modifier.size(22.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "نحوه تمرین",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Start
            )
            Text(
                text = "۱) نمونه WAV استاد را پخش کنید  ۲) همان ملودی را ضبط کنید (اکتاو بالاتر یا پایین‌تر هم درست است). " +
                    "تحلیل با RMVPE و DTW انجام می‌شود — سرعت آزاد؛ اختلاف کمتر از نصف ربع‌پرده عالی، تا ربع‌پرده قابل قبول.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3f,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun AnalysisLimitationsNote() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "محدودیت‌های تحلیل",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Start
            )
        }
        VocalAnalysisFeedback.analysisLimitations.forEach { limitation ->
            Text(
                text = "• $limitation",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.55f),
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.35f,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun HolisticScoreResultCard(
    result: VocalScoreResult,
    selectedCorrectionStartMs: Long?,
    playingSnippetStartMs: Long?,
    playingSnippetTarget: PlaybackTarget?,
    isPlayingUserRecording: Boolean,
    isPlayingTeacherSample: Boolean,
    onPlayCorrectionStudentSnippet: (app.melova.audio.scoring.VocalScorer.PitchCorrection) -> Unit,
    onPlayCorrectionTeacherSnippet: (app.melova.audio.scoring.VocalScorer.PitchCorrection) -> Unit,
    onPlayTahrirStudentSnippet: (TahrirComparison) -> Unit,
    onPlayTahrirTeacherSnippet: (TahrirComparison) -> Unit,
    onRetryRecording: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MelovaCard),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (result.requiresReRecord) {
                val reRecordTitle = when (result.reRecordReason) {
                    ReRecordReason.UNRELATED_MELODY -> "ملودی خوانده‌شده مرتبط نیست"
                    ReRecordReason.UNRELATED_LYRICS -> "متن خوانده‌شده مرتبط نیست"
                    null -> "اجرای شما با این درس مرتبط نیست"
                }
                Text(
                    text = reRecordTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFB74D),
                    textAlign = TextAlign.Start
                )
                Text(
                    text = result.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f,
                    textAlign = TextAlign.Start
                )
                Text(
                    text = result.technicalAnalysis,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Start
                )
                Button(
                    onClick = onRetryRecording,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ضبط مجدد")
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("بستن", color = Color.White)
                }
                return@Column
            }

            Text(
                text = "نتیجه مقایسه کل اجرا",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Start
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MelovaAccent.copy(alpha = 0.15f))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LtrContent {
                        Text(
                            text = "${result.overall}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MelovaAccentSoft
                        )
                    }
                    Text(
                        text = "امتیاز کلی",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            ScoreMetricsList(metrics = result.scoreMetrics)

            Text(
                text = result.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Start
            )

            Text(
                text = "تحلیل فنی",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Start
            )
            Text(
                text = result.technicalAnalysis,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f),
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.4f,
                textAlign = TextAlign.Start
            )

            AnalysisLimitationsNote()

            if (result.corrections.isNotEmpty()) {
                Text(
                    text = "تصحیح‌های پیشنهادی",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Start
                )
                Text(
                    text = "هر خطا: ۲ ثانیه پخش (۱ ثانیه قبل و ۱ ثانیه بعد) — اجرای شما یا نمونه استاد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.55f),
                    textAlign = TextAlign.Start
                )
                result.corrections.forEach { correction ->
                    VocalTrainingCorrectionRow(
                        correction = correction,
                        isSelected = selectedCorrectionStartMs == correction.startMs,
                        isStudentSnippetPlaying = playingSnippetStartMs == correction.startMs &&
                            playingSnippetTarget == PlaybackTarget.USER &&
                            isPlayingUserRecording,
                        isTeacherSnippetPlaying = playingSnippetStartMs == correction.startMs &&
                            playingSnippetTarget == PlaybackTarget.TEACHER &&
                            isPlayingTeacherSample,
                        onPlayStudentSnippet = { onPlayCorrectionStudentSnippet(correction) },
                        onPlayTeacherSnippet = { onPlayCorrectionTeacherSnippet(correction) }
                    )
                }
            }

            if (result.ornamentCorrections.isNotEmpty()) {
                Text(
                    text = "ظرافت‌های اجرا",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Start
                )
                Text(
                    text = "شیوه‌ی رسیدن به نت‌ها و اتصال آن‌ها در اجرای شما با استاد مقایسه شده؛ هر دو را بشنوید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.55f),
                    textAlign = TextAlign.Start
                )
                result.ornamentCorrections.forEach { ornament ->
                    VocalTrainingCorrectionRow(
                        correction = ornament,
                        isSelected = selectedCorrectionStartMs == ornament.startMs,
                        isStudentSnippetPlaying = playingSnippetStartMs == ornament.startMs &&
                            playingSnippetTarget == PlaybackTarget.USER &&
                            isPlayingUserRecording,
                        isTeacherSnippetPlaying = playingSnippetStartMs == ornament.startMs &&
                            playingSnippetTarget == PlaybackTarget.TEACHER &&
                            isPlayingTeacherSample,
                        onPlayStudentSnippet = { onPlayCorrectionStudentSnippet(ornament) },
                        onPlayTeacherSnippet = { onPlayCorrectionTeacherSnippet(ornament) },
                        header = "زمان \u200E${ornament.timeLabel}\u200F • رسیدن به نت",
                        accentColor = Color(0xFFCE93D8)
                    )
                }
            }

            if (result.tahrirComparisons.isNotEmpty()) {
                Text(
                    text = "مقایسه تحریرها",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Start
                )
                Text(
                    text = "تحریرهای استاد و همان بخش از اجرای شما را جداگانه بشنوید و مقایسه کنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.55f),
                    textAlign = TextAlign.Start
                )
                result.tahrirComparisons.forEach { comparison ->
                    VocalTrainingTahrirRow(
                        comparison = comparison,
                        isStudentSnippetPlaying = playingSnippetStartMs == comparison.key &&
                            playingSnippetTarget == PlaybackTarget.USER &&
                            isPlayingUserRecording,
                        isTeacherSnippetPlaying = playingSnippetStartMs == comparison.key &&
                            playingSnippetTarget == PlaybackTarget.TEACHER &&
                            isPlayingTeacherSample,
                        onPlayStudentSnippet = { onPlayTahrirStudentSnippet(comparison) },
                        onPlayTeacherSnippet = { onPlayTahrirTeacherSnippet(comparison) }
                    )
                }
            }

            if (result.weakPoints.isNotEmpty()) {
                Text(
                    text = "نقاط ضعف کلی",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Start
                )
                result.weakPoints.forEach { point ->
                    Text(
                        text = "• $point",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Start
                    )
                }
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("بستن نتیجه", color = Color.White)
            }
        }
    }
}

@Composable
private fun UserRecordingSection(
    durationSeconds: Int,
    playbackPositionMs: Long,
    playbackProgress: Float,
    waveformPeaks: List<Float>,
    corrections: List<app.melova.audio.scoring.VocalScorer.PitchCorrection>,
    selectedCorrectionStartMs: Long?,
    isPlaybackActive: Boolean,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onStopPlayback: () -> Unit,
    onPlayAtMs: (Long) -> Unit,
    onPlayCorrectionFromMarker: (app.melova.audio.scoring.VocalScorer.PitchCorrection) -> Unit,
    embedded: Boolean = false
) {
    val totalMs = (durationSeconds.coerceAtLeast(1) * 1000L)
    val currentMs = when {
        isPlaybackActive -> playbackPositionMs.coerceIn(0L, totalMs)
        selectedCorrectionStartMs != null -> selectedCorrectionStartMs.coerceIn(0L, totalMs)
        else -> 0L
    }
    val playheadMs = when {
        isPlaybackActive -> playbackPositionMs
        selectedCorrectionStartMs != null -> selectedCorrectionStartMs
        else -> 0L
    }
    val sectionSpacing = if (embedded) 8.dp else 10.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (embedded) {
                    Modifier
                } else {
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(12.dp)
                }
            ),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.GraphicEq,
                contentDescription = null,
                tint = MelovaAccentSoft,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "اجرای شما",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Start
                )
                Text(
                    text = "نسخه ضبط‌شده • ${durationSeconds.coerceAtLeast(1)} ثانیه",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Start
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isPlaybackActive) {
                    MelovaMediaIconButton(
                        onClick = onStopPlayback,
                        icon = Icons.Default.Stop,
                        contentDescription = "توقف پخش",
                        containerColor = MelovaAccent.copy(alpha = 0.28f),
                        contentColor = MelovaAccentSoft
                    )
                } else {
                    MelovaMediaIconButton(
                        onClick = onTogglePlayback,
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "توقف موقت" else "پخش اجرای من",
                        containerColor = MelovaSecondary
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LtrContent {
                Text(
                    text = formatPlaybackClock(totalMs),
                    style = MaterialTheme.typography.labelLarge,
                    color = MelovaAccentSoft
                )
            }
            LtrContent {
                Text(
                    text = formatPlaybackClock(currentMs),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
        }

        if (waveformPeaks.isNotEmpty()) {
            StudentRecordingWaveform(
                peaks = waveformPeaks,
                durationMs = totalMs,
                playheadMs = playheadMs,
                corrections = corrections,
                selectedCorrectionStartMs = selectedCorrectionStartMs,
                onSeekToMs = onPlayAtMs,
                onCorrectionClick = onPlayCorrectionFromMarker
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LegendDot(color = Color(0xFFFFB74D), label = "بم")
                LegendDot(color = Color(0xFF81D4FA), label = "زیر")
                LegendDot(color = Color(0xFFB0BEC5), label = "نت نخوانده")
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

private fun formatPlaybackClock(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

