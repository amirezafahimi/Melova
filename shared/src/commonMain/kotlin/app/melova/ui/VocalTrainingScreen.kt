package app.melova.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.melova.FeatureFlags
import app.melova.audio.scoring.ReRecordReason
import app.melova.data.model.MISTAKES_SHOWN_FIRST
import app.melova.data.model.TahrirComparison
import app.melova.data.model.VocalScoreResult
import app.melova.data.model.furthestFirst
import app.melova.data.model.worstFirst
import app.melova.platform.pad2
import app.melova.platform.persianDigits
import app.melova.platform.rememberMicPermission
import app.melova.presentation.LoginGate
import app.melova.presentation.PlaybackTarget
import app.melova.presentation.VocalTrainingMode
import app.melova.presentation.VocalTrainingUiState
import app.melova.presentation.VocalTrainingViewModel
import app.melova.data.model.PracticeLevels
import app.melova.ui.components.ErrorDetailSheet
import app.melova.ui.components.ForwardArrow
import app.melova.ui.components.MelovaBackground
import app.melova.ui.components.MelovaChip
import app.melova.ui.components.MelovaMediaIconButton
import app.melova.ui.components.MelovaProgressBar
import app.melova.ui.components.MelovaSkeleton
import app.melova.ui.components.MelovaSurfaceCard
import app.melova.ui.components.PastTakesSheet
import app.melova.ui.components.TopBarAction
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.EditNote
import app.melova.ui.components.MelovaTopBar
import app.melova.ui.components.MelovaWaveSkeleton
import app.melova.ui.components.ScoreMetricsList
import app.melova.ui.components.StudentRecordingWaveform
import app.melova.ui.components.VocalTrainingCorrectionRow
import app.melova.ui.components.VocalTrainingTahrirRow
import app.melova.ui.components.scoreColor
import app.melova.ui.layout.LtrContent
import app.melova.ui.layout.PagePadding
import app.melova.ui.layout.RtlContent
import app.melova.ui.theme.MelovaAccent
import app.melova.ui.theme.MelovaAccentSoft
import app.melova.ui.theme.MelovaAccentTint
import app.melova.ui.theme.MelovaBackgroundEnd
import app.melova.ui.theme.MelovaBackgroundStart
import app.melova.ui.theme.MelovaCard
import app.melova.ui.theme.MelovaCardElevated
import app.melova.ui.theme.MelovaDimens
import app.melova.ui.theme.MelovaError
import app.melova.ui.theme.MelovaGold
import app.melova.ui.theme.MelovaInfo
import app.melova.ui.theme.MelovaPillShape
import app.melova.ui.theme.MelovaSecondary
import app.melova.ui.theme.MelovaSecondaryTint
import app.melova.ui.theme.MelovaSmallShape
import app.melova.ui.theme.MelovaText
import app.melova.ui.theme.MelovaTextMuted
import app.melova.ui.theme.MelovaWarning
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun VocalTrainingScreen(
    onBack: () -> Unit,
    onOpenTeacherDetails: () -> Unit,
    /** Sending a take to the teacher takes a signed-in account. */
    loginGate: LoginGate = LoginGate.OPEN,
    onLogin: () -> Unit = {},
    onOpenChat: (chatId: String) -> Unit = {},
    /** The lesson's next ready section, offered once a take is scored; null on the last one. */
    nextSectionTitle: String? = null,
    onNextSection: (() -> Unit)? = null,
    /** Opens one of this section's earlier takes, read-only, on a page of its own. */
    onOpenPastTake: (takeId: String) -> Unit = {},
    /** Opens the lesson page, where its versions are bought, once the messages to the teacher run out. */
    onSeeVersions: () -> Unit = {},
    /** Changes when a purchase or sign-in changed what the student has, so messages left are read again. */
    accountRevision: Int = 0,
    viewModel: VocalTrainingViewModel = viewModel { VocalTrainingViewModel(createSavedStateHandle()) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var seenRevision by rememberSaveable { mutableStateOf(accountRevision) }
    LaunchedEffect(accountRevision) {
        if (accountRevision != seenRevision) {
            seenRevision = accountRevision
            viewModel.refreshOffer()
        }
    }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
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
                    .verticalScroll(scrollState)
                    .padding(PagePadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RtlContent {
                    MelovaTopBar(
                        title = uiState.lessonTitle.ifBlank { "تمرین" },
                        subtitle = uiState.oldTakeLabel ?: uiState.courseTitle,
                        onBack = onBack,
                        action = TopBarAction(
                            icon = Icons.Outlined.EditNote,
                            description = "تاریخچهٔ اجراها",
                            onClick = viewModel::openPastTakes
                        ).takeUnless { uiState.isSharedTake }
                    )

                    LessonCard(uiState, onRetry = viewModel::loadLesson)

                    if (!uiState.isLessonReady && !uiState.lessonLoadFailed) TeacherReferenceSkeleton()

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

                    // A shared take doesn't wait for the teacher's sample, and it may outlive the lesson.
                    val showTake = uiState.mode == VocalTrainingMode.RESULT || !uiState.lessonLoadFailed
                    if (if (uiState.isSharedTake) showTake else uiState.isLessonReady) {
                        StudentRecordingPanel(
                            uiState = uiState,
                            scoreResult = uiState.scoreResult,
                            onRecord = ::requestRecord,
                            onStopRecording = viewModel::onStopClicked,
                            onStopPlayback = viewModel::onStopClicked,
                            onRetryRecording = viewModel::retryRecording,
                            onSubmitRecording = viewModel::submitRecording,
                            onToggleUserPlayback = viewModel::toggleUserRecordingPlayback,
                            onPlayUserRecordingAt = viewModel::playUserRecordingAt,
                            onPlayCorrectionFromMarker = viewModel::playCorrectionFromMarker
                        )
                    }

                    if (!uiState.isSharedTake && uiState.scoreResult == null) HeadphoneHintBanner()

                    // A new result scrolls into view: it lands under the take, out of sight on a phone.
                    var resultTop by remember { mutableStateOf(-1) }
                    val resultGap = with(LocalDensity.current) { 16.dp.roundToPx() }
                    LaunchedEffect(uiState.scoreResult) {
                        if (uiState.scoreResult == null) return@LaunchedEffect
                        val top = snapshotFlow { resultTop }.first { it >= 0 }
                        scrollState.animateScrollTo((top - resultGap).coerceAtLeast(0))
                    }
                    AnimatedVisibility(
                        visible = uiState.scoreResult != null,
                        enter = fadeIn() + slideInVertically { it / 2 },
                        modifier = Modifier.onGloballyPositioned { resultTop = it.positionInParent().y.roundToInt() }
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
                                nextSectionTitle = nextSectionTitle,
                                onNextSection = onNextSection,
                                onDismiss = {
                                    viewModel.dismissResults()
                                    scope.launch { scrollState.animateScrollTo(0) }
                                },
                                isReadOnly = uiState.isSharedTake
                            )
                        }
                    }

                    uiState.errorDetail?.let { detail ->
                        ErrorDetailSheet(
                            detail = detail,
                            playing = uiState.detailPlaying,
                            positionOf = viewModel::detailPositionMs,
                            onToggle = viewModel::toggleDetailPlayback,
                            onScrubStart = viewModel::startDetailScrub,
                            onSeek = viewModel::seekDetail,
                            onDismiss = viewModel::hideErrorDetail
                        )
                    }

                    uiState.scoreResult?.takeUnless { uiState.isSharedTake }?.let { result ->
                        AskTeacherCard(
                            state = uiState.teacherAsk,
                            referralReasons = result.teacherReferral?.reasons.orEmpty(),
                            onSend = viewModel::sendToTeacher,
                            loginGate = loginGate,
                            onLogin = onLogin,
                            onOpenChat = onOpenChat,
                            offer = uiState.lessonOffer,
                            isFreeLesson = uiState.isFreeLesson,
                            onSeeVersions = onSeeVersions
                        )
                    }

                    if (uiState.arePastTakesOpen) {
                        PastTakesSheet(
                            takes = uiState.pastTakes,
                            onOpenTake = { take ->
                                viewModel.closePastTakes()
                                if (uiState.playbackTarget != PlaybackTarget.NONE) viewModel.onStopClicked()
                                onOpenPastTake(take.id)
                            },
                            onDismiss = viewModel::closePastTakes
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
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // The teacher's name and length sit on the sample card below, so they aren't repeated here.
            if (uiState.lessonDescription.isNotBlank() || uiState.bestScore != null) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = uiState.lessonDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MelovaText.copy(alpha = 0.85f),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )
                    uiState.bestScore?.let { best ->
                        MelovaChip(text = persianDigits("بهترین نمره: $best"), color = MelovaGold)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MelovaSmallShape)
                    .background(MelovaSecondary.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent)
                ) {
                    Text("تلاش دوباره")
                }
            }
        }
    }
}

/**
 * The teacher's sample card before its audio is here: the same card with its wave, its name line
 * and its button standing in place, so the student sees what is coming instead of a blank page.
 */
@Composable
private fun TeacherReferenceSkeleton() {
    MelovaSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MelovaSkeleton(Modifier.size(MelovaDimens.Icon), CircleShape)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MelovaSkeleton(Modifier.width(96.dp).height(14.dp))
                    MelovaSkeleton(Modifier.width(140.dp).height(10.dp))
                }
                MelovaSkeleton(Modifier.size(MelovaDimens.CompactButtonHeight))
            }
            MelovaWaveSkeleton(height = 72.dp)
            MelovaSkeleton(Modifier.fillMaxWidth().height(MelovaDimens.CompactButtonHeight))
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
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
                    modifier = Modifier.size(20.dp)
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
                        text = "$teacherName • " + persianDigits("${durationSeconds.coerceAtLeast(1)} ثانیه"),
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isPlaybackActive) {
                        MelovaMediaIconButton(
                            onClick = onStopPlayback,
                            icon = Icons.Default.Stop,
                            contentDescription = "توقف پخش استاد",
                            containerColor = MelovaSecondaryTint,
                            contentColor = MelovaSecondary
                        )
                    } else {
                        // A tint, like the detail page's: the card reads as one piece, and the
                        // student's own record button stays the only strong colour on the page.
                        MelovaMediaIconButton(
                            onClick = onTogglePlayback,
                            enabled = enabled,
                            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "توقف موقت" else "پخش نمونه استاد",
                            containerColor = MelovaSecondaryTint,
                            contentColor = MelovaSecondary
                        )
                    }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "روی موج بزنید تا از همان لحظه پخش شود",
                        style = MaterialTheme.typography.labelSmall,
                        color = MelovaText.copy(alpha = 0.5f),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )
                    PlaybackClock(currentMs = currentMs, totalMs = totalMs)
                }
            }

            Button(
                onClick = onOpenDetails,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MelovaDimens.CompactButtonHeight),
                shape = MelovaSmallShape,
                contentPadding = PaddingValues(horizontal = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MelovaSecondaryTint,
                    contentColor = MelovaSecondary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.GraphicEq,
                    contentDescription = null,
                    modifier = Modifier.size(MelovaDimens.IconSmall)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "جزئیات اجرای استاد (تحریر و ویبراتو)",
                    style = MaterialTheme.typography.labelLarge
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
    onSubmitRecording: () -> Unit,
    onToggleUserPlayback: () -> Unit,
    onPlayUserRecordingAt: (Long) -> Unit,
    onPlayCorrectionFromMarker: (app.melova.audio.scoring.VocalScorer.PitchCorrection) -> Unit
) {
    val isRecording = uiState.mode == VocalTrainingMode.RECORDING
    val isAnalyzing = uiState.mode == VocalTrainingMode.ANALYZING
    val isReviewing = uiState.mode == VocalTrainingMode.REVIEW
    val showResultRecording = uiState.mode == VocalTrainingMode.RESULT &&
        (scoreResult != null || uiState.isSharedTake)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MelovaCard),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                isRecording -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RecordingDot()
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
                        LtrContent {
                            Text(
                                text = formatPlaybackClock(uiState.recordingElapsedMs),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MelovaAccentSoft
                            )
                        }
                    }
                    LiveLevelWave(levels = uiState.liveLevels)
                    MelovaProgressBar(
                        progress = uiState.progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MelovaAccent.copy(alpha = 0.7f),
                        trackColor = MelovaText.copy(alpha = 0.10f)
                    )
                    Button(
                        onClick = onStopRecording,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(MelovaDimens.PrimaryButtonHeight),
                        shape = MelovaSmallShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("پایان ضبط", style = MaterialTheme.typography.titleSmall)
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

                isReviewing || showResultRecording -> {
                    UserRecordingSection(
                        durationSeconds = uiState.userRecordingDurationSeconds,
                        playbackPositionMs = uiState.userPlaybackPositionMs,
                        playbackProgress = if (uiState.playbackTarget == PlaybackTarget.USER) {
                            uiState.progress
                        } else {
                            0f
                        },
                        waveformPeaks = uiState.waveformPeaks,
                        corrections = scoreResult?.corrections.orEmpty(),
                        selectedCorrectionStartMs = uiState.selectedCorrectionStartMs,
                        isPlaybackActive = uiState.playbackTarget == PlaybackTarget.USER,
                        isPlaying = uiState.isPlayingUserRecording,
                        onTogglePlayback = onToggleUserPlayback,
                        onStopPlayback = onStopPlayback,
                        onPlayAtMs = onPlayUserRecordingAt,
                        onPlayCorrectionFromMarker = onPlayCorrectionFromMarker,
                        isBoxed = isReviewing
                    )
                    if (isReviewing) {
                        // Send and retry side by side: one glance, no scrolling for the second choice.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onSubmitRecording,
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(MelovaDimens.PrimaryButtonHeight),
                                shape = MelovaSmallShape,
                                colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent)
                            ) {
                                Text("ارسال برای تحلیل", style = MaterialTheme.typography.titleSmall)
                            }
                            OutlinedButton(
                                onClick = onRetryRecording,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(MelovaDimens.PrimaryButtonHeight),
                                shape = MelovaSmallShape,
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = MelovaAccentSoft,
                                    modifier = Modifier.size(MelovaDimens.Icon)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ضبط دوباره", color = MelovaText)
                            }
                        }
                    }
                }

                uiState.isSharedTake -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MelovaAccent,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "در حال باز کردن اجرا…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MelovaText,
                            textAlign = TextAlign.Start
                        )
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
                            modifier = Modifier.size(20.dp)
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
                            .height(MelovaDimens.PrimaryButtonHeight),
                        shape = MelovaSmallShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("شروع ضبط", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

private val PracticeSteps = listOf(
    "نمونه استاد را گوش دهید و تمرین کنید.",
    "ضبط را بزنید و همان ملودی را بخوانید؛ اکتاو بالاتر یا پایین‌تر هم قبول است و لازم نیست سرعتتان دقیقاً مثل استاد باشد.",
    "اجرایتان را گوش دهید و برای تحلیل بفرستید.",
    "روی هر خطا بزنید تا اجرای خودتان و استاد را مقایسه کنید."
)

/** One line until opened, so the steps don't push the practice down the page. */
@Composable
private fun HeadphoneHintBanner() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MelovaSmallShape)
            .background(MelovaCardElevated)
            .clickable { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Headphones,
                contentDescription = null,
                tint = MelovaGold,
                modifier = Modifier.size(MelovaDimens.Icon)
            )
            Text(
                text = "نحوه تمرین",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MelovaText,
                textAlign = TextAlign.Start
            )
            Text(
                text = "گوش دهید، بخوانید، بفرستید",
                style = MaterialTheme.typography.bodySmall,
                color = MelovaText.copy(alpha = 0.6f),
                textAlign = TextAlign.Start,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "بستن راهنما" else "باز کردن راهنما",
                tint = MelovaText.copy(alpha = 0.6f),
                modifier = Modifier.size(MelovaDimens.Icon)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PracticeSteps.forEachIndexed { index, step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(MelovaPillShape)
                                .background(MelovaGold.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = persianDigits("${index + 1}"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MelovaGold
                            )
                        }
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall,
                            color = MelovaText.copy(alpha = 0.85f),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/** A soft pulsing red dot, the universal "this is recording" sign. */
@Composable
private fun RecordingDot() {
    val pulse = rememberInfiniteTransition()
    val alpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse)
    )
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(MelovaPillShape)
            .background(MelovaError.copy(alpha = alpha))
    )
}

/**
 * The microphone as it hears you: one rounded bar per poll, newest on the right,
 * so the student sees the voice land while singing.
 */
@Composable
private fun LiveLevelWave(levels: List<Float>) {
    val accent = MelovaAccent
    val rest = MelovaText.copy(alpha = 0.14f)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        val count = VocalTrainingViewModel.LIVE_LEVEL_COUNT
        val step = size.width / count
        val stroke = step * 0.6f
        val mid = size.height / 2f
        val minHalf = 1.5.dp.toPx()
        val offset = count - levels.size
        for (i in 0 until count) {
            val level = levels.getOrNull(i - offset)
            val x = step * i + step / 2f
            // Square root lifts soft singing so it still shows; full voice fills the height.
            val half = if (level == null) minHalf else maxOf(minHalf, sqrt(level.coerceIn(0f, 1f)) * mid)
            drawLine(
                color = if (level == null) rest else accent.copy(alpha = 0.45f + 0.55f * (i + 1f) / count),
                start = Offset(x, mid - half),
                end = Offset(x, mid + half),
                strokeWidth = stroke,
                cap = StrokeCap.Round
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
    onDismiss: () -> Unit,
    isReadOnly: Boolean = false,
    nextSectionTitle: String? = null,
    onNextSection: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MelovaCard),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (result.requiresReRecord) {
                ReRecordResult(result = result, isReadOnly = isReadOnly, onRetryRecording = onRetryRecording, onDismiss = onDismiss)
                return@Column
            }

            ScoreHero(result = result, isNewBest = isNewBest, onDismiss = onDismiss.takeUnless { isReadOnly })

            // What to do next sits right under the score, before the list of mistakes.
            if (!isReadOnly) ResultActions(nextSectionTitle = nextSectionTitle, onNextSection = onNextSection, onRetryRecording = onRetryRecording)

            // The detail buttons blink until the student has opened one of them.
            var hasOpenedDetail by remember(result) { mutableStateOf(false) }

            if (result.corrections.isNotEmpty()) {
                MistakeList(
                    title = "خطاهای نت",
                    mistakes = remember(result) { result.corrections.worstFirst() },
                    key = { it.startMs }
                ) { correction ->
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
                        onShowDetails = {
                            hasOpenedDetail = true
                            onShowCorrectionDetail(correction)
                        },
                        invitesDetails = !hasOpenedDetail
                    )
                }
            }

            if (FeatureFlags.SHOW_TAHRIR_FEEDBACK && result.tahrirComparisons.isNotEmpty()) {
                MistakeList(
                    title = "تحریر و ویبراتو",
                    mistakes = remember(result) { result.tahrirComparisons.furthestFirst() },
                    key = { it.key }
                ) { comparison ->
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
                        onShowDetails = {
                            hasOpenedDetail = true
                            onShowTahrirDetail(comparison)
                        },
                        invitesDetails = !hasOpenedDetail
                    )
                }
            }

            if (result.weakPoints.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "نکته‌های کلی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MelovaText,
                        textAlign = TextAlign.Start
                    )
                    result.weakPoints.forEach { point ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MelovaAccentSoft)
                            )
                            Text(
                                text = point,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MelovaText.copy(alpha = 0.85f),
                                textAlign = TextAlign.Start,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The score as a filling ring beside a word for it, the new-best mark and the summary under them. */
@Composable
private fun ScoreHero(result: VocalScoreResult, isNewBest: Boolean, onDismiss: (() -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScoreRing(score = result.overall)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "نتیجه‌ی اجرای شما",
                    style = MaterialTheme.typography.labelLarge,
                    color = MelovaTextMuted,
                    textAlign = TextAlign.Start
                )
                Text(
                    text = scoreVerdict(result.overall),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MelovaText,
                    textAlign = TextAlign.Start
                )
                // Only a clear gain is called a record: the scorer moves a few points on changes nobody hears.
                val previous = result.previousBest
                if (isNewBest && previous != null && PracticeLevels.isRealNewBest(result.overall, previous)) {
                    MelovaChip(text = persianDigits("رکورد تازه در این بخش، قبلی $previous"), color = MelovaGold)
                }
            }
            if (onDismiss != null) {
                MelovaMediaIconButton(
                    onClick = onDismiss,
                    icon = Icons.Default.Close,
                    contentDescription = "بستن نتیجه",
                    containerColor = MelovaText.copy(alpha = 0.06f),
                    contentColor = MelovaTextMuted,
                    modifier = Modifier.align(Alignment.Top)
                )
            }
        }
        ScoreMetricsList(metrics = result.scoreMetrics)
        if (result.summary.isNotBlank()) {
            Text(
                text = result.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MelovaText.copy(alpha = 0.85f),
                textAlign = TextAlign.Start
            )
        }
    }
}

private fun scoreVerdict(score: Int): String = when {
    score >= 90 -> "عالی خواندید"
    score >= 75 -> "خوب خواندید"
    score >= 60 -> "نزدیک شدید"
    else -> "باز هم تمرین کنید"
}

/** The overall score, its ring filling to it once when the result comes in. */
@Composable
private fun ScoreRing(score: Int) {
    val color = scoreColor(score)
    val track = MelovaText.copy(alpha = 0.08f)
    val fill = remember(score) { Animatable(0f) }
    LaunchedEffect(score) { fill.animateTo(score.coerceIn(0, 100) / 100f, tween(900, easing = FastOutSlowInEasing)) }
    Box(modifier = Modifier.size(88.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke))
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * fill.value,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = persianDigits((fill.value * 100).roundToInt().toString()),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = persianDigits("از ۱۰۰"),
                style = MaterialTheme.typography.labelSmall,
                color = MelovaTextMuted
            )
        }
    }
}

/** Sing again and, when there is one, go on to the next section: the way on is the solid one. */
@Composable
private fun ResultActions(nextSectionTitle: String?, onNextSection: (() -> Unit)?, onRetryRecording: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Tinted, not solid: the page's one solid button is «بخش بعدی» or, without it, «ارسال برای استاد».
        Button(
            onClick = onRetryRecording,
            modifier = Modifier
                .weight(1f)
                .height(MelovaDimens.PrimaryButtonHeight),
            shape = MelovaSmallShape,
            contentPadding = PaddingValues(horizontal = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MelovaAccentTint, contentColor = MelovaAccentSoft)
        ) {
            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(MelovaDimens.Icon))
            Spacer(modifier = Modifier.width(8.dp))
            Text("دوباره بخوانم", style = MaterialTheme.typography.titleSmall, maxLines = 1)
        }
        if (onNextSection != null) {
            Button(
                onClick = onNextSection,
                modifier = Modifier
                    .weight(1.3f)
                    .height(MelovaDimens.PrimaryButtonHeight),
                shape = MelovaSmallShape,
                contentPadding = PaddingValues(horizontal = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent)
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text("بخش بعدی", style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    if (!nextSectionTitle.isNullOrBlank()) {
                        Text(
                            text = nextSectionTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = LocalContentColor.current.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                ForwardArrow(tint = LocalContentColor.current)
            }
        }
    }
}

/**
 * A kind of mistake, worst first: the first [MISTAKES_SHOWN_FIRST] show, and the rest wait behind
 * a button so the most important ones aren't lost among small ones.
 */
@Composable
private fun <T> MistakeList(
    title: String,
    mistakes: List<T>,
    key: (T) -> Any,
    row: @Composable (T) -> Unit
) {
    var showsAll by remember(mistakes) { mutableStateOf(false) }
    val hidden = mistakes.size - MISTAKES_SHOWN_FIRST
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MelovaText,
                textAlign = TextAlign.Start
            )
            MelovaChip(text = persianDigits(mistakes.size.toString()), color = MelovaAccentSoft)
            Spacer(modifier = Modifier.weight(1f))
            if (mistakes.size > 1) {
                Text(
                    text = "مهم‌ترها اول",
                    style = MaterialTheme.typography.labelMedium,
                    color = MelovaTextMuted
                )
            }
        }
        val shown = if (showsAll) mistakes else mistakes.take(MISTAKES_SHOWN_FIRST)
        shown.forEach { mistake -> key(key(mistake)) { row(mistake) } }
        if (hidden > 0) {
            OutlinedButton(
                onClick = { showsAll = !showsAll },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MelovaDimens.ButtonHeight),
                shape = MelovaSmallShape
            ) {
                Text(
                    text = if (showsAll) "فقط دو مورد مهم‌تر" else persianDigits("نمایش $hidden مورد دیگر"),
                    style = MaterialTheme.typography.titleSmall,
                    color = MelovaText
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (showsAll) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MelovaTextMuted,
                    modifier = Modifier.size(MelovaDimens.Icon)
                )
            }
        }
    }
}

/** The take didn't match the lesson: say why and offer to sing it again. */
@Composable
private fun ReRecordResult(
    result: VocalScoreResult,
    isReadOnly: Boolean,
    onRetryRecording: () -> Unit,
    onDismiss: () -> Unit
) {
    val reRecordTitle = result.reRecordTitle ?: when (result.reRecordReason) {
        ReRecordReason.UNRELATED_MELODY -> "ملودی خوانده‌شده مرتبط نیست"
        ReRecordReason.UNRELATED_LYRICS -> "متن خوانده‌شده مرتبط نیست"
        null -> "اجرای شما با این درس مرتبط نیست"
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        // Older results carry the same text in both; show it once.
        if (result.technicalAnalysis.isNotBlank() && result.technicalAnalysis != result.summary) {
            Text(
                text = result.technicalAnalysis,
                style = MaterialTheme.typography.bodyMedium,
                color = MelovaText,
                textAlign = TextAlign.Start
            )
        }
        if (!isReadOnly) {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onRetryRecording,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MelovaDimens.PrimaryButtonHeight),
                shape = MelovaSmallShape,
                colors = ButtonDefaults.buttonColors(containerColor = MelovaAccent)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ضبط مجدد", style = MaterialTheme.typography.titleSmall)
            }
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MelovaDimens.ButtonHeight),
                shape = MelovaSmallShape
            ) {
                Text("بستن", color = MelovaText)
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
    /** Boxed while the take waits to be sent; once scored it sits straight in the panel's card. */
    isBoxed: Boolean
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isBoxed) {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MelovaText.copy(alpha = 0.06f))
                        .padding(12.dp)
                } else {
                    Modifier
                }
            ),
        verticalArrangement = Arrangement.spacedBy(if (isBoxed) 10.dp else 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.GraphicEq,
                contentDescription = null,
                tint = MelovaAccentSoft,
                modifier = Modifier.size(20.dp)
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
                    text = persianDigits("نسخه ضبط‌شده • ${durationSeconds.coerceAtLeast(1)} ثانیه"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MelovaText.copy(alpha = 0.65f),
                    textAlign = TextAlign.Start
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isPlaybackActive) {
                    MelovaMediaIconButton(
                        onClick = onStopPlayback,
                        icon = Icons.Default.Stop,
                        contentDescription = "توقف پخش",
                        containerColor = MelovaAccentTint,
                        contentColor = MelovaAccentSoft
                    )
                } else {
                    MelovaMediaIconButton(
                        onClick = onTogglePlayback,
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "توقف موقت" else "پخش اجرای من",
                        containerColor = MelovaAccentTint,
                        contentColor = MelovaAccentSoft
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
            if (corrections.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LegendDot(color = MelovaWarning, label = "بالاتر")
                    LegendDot(color = MelovaInfo, label = "پایین‌تر")
                    LegendDot(color = MelovaTextMuted, label = "نت نخوانده")
                }
            }
        }
    }
}

/** «current / total» in one quiet line under a wave. */
@Composable
private fun PlaybackClock(currentMs: Long, totalMs: Long) {
    LtrContent {
        Text(
            text = "${formatPlaybackClock(currentMs)} / ${formatPlaybackClock(totalMs)}",
            style = MaterialTheme.typography.labelMedium,
            color = MelovaText.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
    return persianDigits("$minutes:${seconds.pad2()}")
}

