package app.melova.ui

import app.melova.platform.rememberMicPermission
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.melova.FeatureFlags
import app.melova.audio.scoring.VocalAnalysisFeedback
import app.melova.presentation.PlaybackTarget
import app.melova.data.model.TahrirComparison
import app.melova.data.model.VocalScoreResult
import app.melova.presentation.VocalTrainingMode
import app.melova.presentation.VocalTrainingUiState
import app.melova.presentation.VocalTrainingViewModel
import app.melova.audio.scoring.ReRecordReason
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaChip
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import app.melova.ui.components.MelovaMediaIconButton
import app.melova.ui.components.MelovaTopBar
import app.melova.ui.components.scoreColor
import app.melova.ui.components.MelovaProgressBar
import app.melova.ui.components.ScoreMetricsList
import app.melova.ui.components.ErrorDetailSheet
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
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaCardElevated
import app.melova.ui.theme.MelovaWarning
import app.melova.ui.theme.MelovaInfo
import app.melova.ui.theme.MelovaTextMuted
import app.melova.ui.theme.MelovaText
import app.melova.platform.pad2

@Composable
fun VocalTrainingScreen(
    onBack: () -> Unit,
    onOpenTeacherDetails: () -> Unit,
    viewModel: VocalTrainingViewModel = viewModel { VocalTrainingViewModel(createSavedStateHandle()) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val micPermission = rememberMicPermission { granted ->
        if (granted) viewModel.onRecordClicked()
        else viewModel.onRecordPermissionDenied()
    }

    fun requestRecord() {
        if (micPermission.isGranted()) viewModel.onRecordClicked()
        else micPermission.request()
    }

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
                        title = uiState.lessonTitle.ifBlank { "تمرین" },
                        subtitle = uiState.courseTitle,
                        onBack = onBack
                    )

                    LessonCard(uiState, onRetry = viewModel::loadLesson)

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
                            onStopPlayback = viewModel::onStopClicked,
                            onOpenDetails = {
                                if (uiState.playbackTarget != PlaybackTarget.NONE) viewModel.onStopClicked()
                                onOpenTeacherDetails()
                            }
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
                                isNewBest = uiState.isNewBest,
                                selectedCorrectionStartMs = uiState.selectedCorrectionStartMs,
                                playingSnippetStartMs = uiState.playingSnippetStartMs,
                                playingSnippetTarget = uiState.playingSnippetTarget,
                                isPlayingUserRecording = uiState.isPlayingUserRecording,
                                isPlayingTeacherSample = uiState.isPlayingTeacherSample,
                                onPlayCorrectionStudentSnippet = viewModel::playCorrectionSnippet,
                                onPlayCorrectionTeacherSnippet = viewModel::playCorrectionTeacherSnippet,
                                onPlayTahrirStudentSnippet = viewModel::playTahrirStudentSnippet,
                                onPlayTahrirTeacherSnippet = viewModel::playTahrirTeacherSnippet,
                                onShowCorrectionDetail = viewModel::showCorrectionDetail,
                                onShowTahrirDetail = viewModel::showTahrirDetail,
                                onRetryRecording = viewModel::retryRecording,
                                onDismiss = viewModel::dismissResults
                            )
                        }
                    }

                    uiState.errorDetail?.let { detail ->
                        val correction = detail.correction
                        val comparison = detail.comparison
                        ErrorDetailSheet(
                            detail = detail,
                            playingTarget = uiState.playingSnippetTarget.takeIf {
                                uiState.playingSnippetStartMs == detail.key && uiState.isPlaying
                            },
                            teacherPositionMs = uiState.teacherPlaybackPositionMs,
                            studentPositionMs = uiState.userPlaybackPositionMs,
                            onPlayTeacher = {
                                if (correction != null) viewModel.playCorrectionTeacherSnippet(correction)
                                else if (comparison != null) viewModel.playTahrirTeacherSnippet(comparison)
                            },
                            onPlayStudent = {
                                if (correction != null) viewModel.playCorrectionSnippet(correction)
                                else if (comparison != null) viewModel.playTahrirStudentSnippet(comparison)
                            },
                            onDismiss = viewModel::hideErrorDetail
                        )
                    }

                    uiState.scoreResult?.let { result ->
                        AskTeacherCard(
                            state = uiState.teacherAsk,
                            referralReasons = result.teacherReferral?.reasons.orEmpty(),
                            onSend = viewModel::sendToTeacher
                        )
                    }
                }
            }
    }
}

@Composable
private fun LessonCard(uiState: VocalTrainingUiState, onRetry: () -> Unit) {
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
                if (uiState.durationSeconds > 0) {
                    Text(
                        text = "• ${uiState.durationSeconds} ثانیه",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MelovaText.copy(alpha = 0.6f),
                        textAlign = TextAlign.Start
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                uiState.bestScore?.let { best ->
                    MelovaChip(text = "بهترین نمره: $best", color = MelovaGold)
                }
            }

            Text(
                text = uiState.lessonDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MelovaText.copy(alpha = 0.85f),
                textAlign = TextAlign.Start
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MelovaSecondary.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (uiState.isLessonLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MelovaAccent,
                        strokeWidth = 2.dp
                    )
                }
                Text(
                    text = uiState.statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaText.copy(alpha = 0.9f),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
            }

            if (uiState.lessonLoadFailed) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent)
                ) {
                    Text("تلاش دوباره")
                }
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
    onStopPlayback: () -> Unit,
    onOpenDetails: () -> Unit
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
                        color = MelovaText,
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
                        color = MelovaText.copy(alpha = 0.65f),
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
                        color = MelovaText.copy(alpha = 0.55f)
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
                    color = MelovaText.copy(alpha = 0.45f),
                    textAlign = TextAlign.Start
                )
            }

            OutlinedButton(
                onClick = onOpenDetails,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MelovaSecondary.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Outlined.GraphicEq,
                    contentDescription = null,
                    tint = MelovaSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "جزئیات اجرای استاد (تحریر و ویبراتو)",
                    color = MelovaSecondary
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
                                color = MelovaText,
                                textAlign = TextAlign.Start
                            )
                            Text(
                                text = "همان ملودی استاد را بخوانید",
                                style = MaterialTheme.typography.bodySmall,
                                color = MelovaText.copy(alpha = 0.65f),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                    MelovaProgressBar(
                        progress = uiState.progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MelovaAccent,
                        trackColor = MelovaText.copy(alpha = 0.15f)
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
                        color = MelovaText,
                        textAlign = TextAlign.Start
                    )
                    MelovaProgressBar(
                        progress = uiState.progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MelovaAccentSoft,
                        trackColor = MelovaText.copy(alpha = 0.15f)
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
                            color = MelovaText.copy(alpha = 0.7f),
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
                                color = MelovaText,
                                textAlign = TextAlign.Start
                            )
                            Text(
                                text = "پس از شنیدن استاد، ضبط را شروع کنید",
                                style = MaterialTheme.typography.bodySmall,
                                color = MelovaText.copy(alpha = 0.65f),
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
            .background(MelovaCardElevated)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Headphones,
            contentDescription = null,
            tint = MelovaGold,
            modifier = Modifier.size(22.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "نحوه تمرین",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MelovaText,
                textAlign = TextAlign.Start
            )
            Text(
                text = "۱) نمونه استاد را با هدفون گوش دهید.  ۲) ضبط را بزنید و همان ملودی را بخوانید؛ " +
                    "اکتاو بالاتر یا پایین‌تر هم قبول است و لازم نیست سرعتتان دقیقاً مثل استاد باشد.  " +
                    "۳) نتیجه را ببینید و روی هر خطا بزنید تا اجرای خودتان و استاد را مقایسه کنید.",
                style = MaterialTheme.typography.bodySmall,
                color = MelovaText.copy(alpha = 0.8f),
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
            .background(MelovaText.copy(alpha = 0.06f))
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
                tint = MelovaText.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "محدودیت‌های تحلیل",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MelovaText.copy(alpha = 0.75f),
                textAlign = TextAlign.Start
            )
        }
        VocalAnalysisFeedback.analysisLimitations.forEach { limitation ->
            Text(
                text = "• $limitation",
                style = MaterialTheme.typography.bodySmall,
                color = MelovaText.copy(alpha = 0.55f),
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun HolisticScoreResultCard(
    result: VocalScoreResult,
    isNewBest: Boolean,
    selectedCorrectionStartMs: Long?,
    playingSnippetStartMs: Long?,
    playingSnippetTarget: PlaybackTarget?,
    isPlayingUserRecording: Boolean,
    isPlayingTeacherSample: Boolean,
    onPlayCorrectionStudentSnippet: (app.melova.audio.scoring.VocalScorer.PitchCorrection) -> Unit,
    onPlayCorrectionTeacherSnippet: (app.melova.audio.scoring.VocalScorer.PitchCorrection) -> Unit,
    onPlayTahrirStudentSnippet: (TahrirComparison) -> Unit,
    onPlayTahrirTeacherSnippet: (TahrirComparison) -> Unit,
    onShowCorrectionDetail: (app.melova.audio.scoring.VocalScorer.PitchCorrection) -> Unit,
    onShowTahrirDetail: (TahrirComparison) -> Unit,
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
                    color = MelovaWarning,
                    textAlign = TextAlign.Start
                )
                Text(
                    text = result.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MelovaText.copy(alpha = 0.9f),
                    textAlign = TextAlign.Start
                )
                Text(
                    text = result.technicalAnalysis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaText.copy(alpha = 0.65f),
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
                    Text("بستن", color = MelovaText)
                }
                return@Column
            }

            Text(
                text = "نتیجه‌ی اجرای شما",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MelovaText,
                textAlign = TextAlign.Start
            )

            if (isNewBest) {
                MelovaChip(text = "رکورد جدید! بهترین نمره‌ی شما در این بخش", color = MelovaGold)
            }

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
                            color = scoreColor(result.overall)
                        )
                    }
                    Text(
                        text = "امتیاز کلی",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MelovaText.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            ScoreMetricsList(metrics = result.scoreMetrics)

            Text(
                text = result.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MelovaText.copy(alpha = 0.85f),
                textAlign = TextAlign.Start
            )

            Text(
                text = "تحلیل فنی",
                style = MaterialTheme.typography.titleMedium,
                color = MelovaText,
                textAlign = TextAlign.Start
            )
            Text(
                text = result.technicalAnalysis,
                style = MaterialTheme.typography.bodySmall,
                color = MelovaText.copy(alpha = 0.75f),
                textAlign = TextAlign.Start
            )

            AnalysisLimitationsNote()

            if (result.corrections.isNotEmpty()) {
                Text(
                    text = "خطاهای دقت نت",
                    style = MaterialTheme.typography.titleMedium,
                    color = MelovaText,
                    textAlign = TextAlign.Start
                )
                Text(
                    text = "هر خطا: ۲ ثانیه پخش (۱ ثانیه قبل و ۱ ثانیه بعد) — اجرای شما یا نمونه استاد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaText.copy(alpha = 0.55f),
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
                        onPlayTeacherSnippet = { onPlayCorrectionTeacherSnippet(correction) },
                        onShowDetails = { onShowCorrectionDetail(correction) }
                    )
                }
            }

            if (FeatureFlags.SHOW_TAHRIR_FEEDBACK && result.tahrirComparisons.isNotEmpty()) {
                Text(
                    text = "مقایسه تحریر و ویبراتو",
                    style = MaterialTheme.typography.titleMedium,
                    color = MelovaText,
                    textAlign = TextAlign.Start
                )
                Text(
                    text = "تحریر و ویبراتوی استاد و همان بخش از اجرای شما را جداگانه بشنوید و مقایسه کنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaText.copy(alpha = 0.55f),
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
                        onPlayTeacherSnippet = { onPlayTahrirTeacherSnippet(comparison) },
                        onShowDetails = { onShowTahrirDetail(comparison) }
                    )
                }
            }

            if (result.weakPoints.isNotEmpty()) {
                Text(
                    text = "نقاط ضعف کلی",
                    style = MaterialTheme.typography.titleMedium,
                    color = MelovaText,
                    textAlign = TextAlign.Start
                )
                result.weakPoints.forEach { point ->
                    Text(
                        text = "• $point",
                        style = MaterialTheme.typography.bodySmall,
                        color = MelovaText.copy(alpha = 0.7f),
                        textAlign = TextAlign.Start
                    )
                }
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("بستن نتیجه", color = MelovaText)
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
                        .background(MelovaText.copy(alpha = 0.06f))
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
                    color = MelovaText,
                    textAlign = TextAlign.Start
                )
                Text(
                    text = "نسخه ضبط‌شده • ${durationSeconds.coerceAtLeast(1)} ثانیه",
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaText.copy(alpha = 0.65f),
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
                    color = MelovaText.copy(alpha = 0.55f)
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
                LegendDot(color = MelovaWarning, label = "بم")
                LegendDot(color = MelovaInfo, label = "زیر")
                LegendDot(color = MelovaTextMuted, label = "نت نخوانده")
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
            color = MelovaText.copy(alpha = 0.6f)
        )
    }
}

private fun formatPlaybackClock(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.pad2()}"
}

