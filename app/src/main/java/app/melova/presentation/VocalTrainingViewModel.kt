package app.melova.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.melova.audio.io.WaveformPeaksExtractor
import app.melova.audio.scoring.VocalScorer
import app.melova.data.model.VocalScoreResult
import app.melova.data.repository.RemoteVocalScoringRepository
import app.melova.data.repository.RemoteTeacherReferenceRepository
import app.melova.data.repository.TeacherReferenceRepository
import app.melova.data.repository.VocalScoringRepository
import app.melova.audio.playback.ReferenceAudioPlayer
import app.melova.audio.playback.TeacherVocalGenerator
import app.melova.audio.capture.DeviceAudioHints
import app.melova.audio.capture.VocalRecorder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class VocalTrainingViewModel(application: Application) : AndroidViewModel(application) {
    private val scoringRepository: VocalScoringRepository = RemoteVocalScoringRepository()
    private val teacherReferenceRepository: TeacherReferenceRepository = RemoteTeacherReferenceRepository()
    private val player = ReferenceAudioPlayer(application)
    private val recorder = VocalRecorder(application)

    @Volatile
    private var lesson: TeacherVocalGenerator.TeacherLesson? = null

    private val userRecordingFile =
        File(getApplication<Application>().cacheDir, USER_RECORDING_FILE_NAME)

    @Volatile
    private var lastRecordingDurationMs: Long = 0L

    private val _uiState = MutableStateFlow(VocalTrainingUiState())
    val uiState: StateFlow<VocalTrainingUiState> = _uiState.asStateFlow()

    private var sessionJob: Job? = null
    private var progressJob: Job? = null
    private var snippetStopJob: Job? = null

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(statusMessage = "در حال دریافت نمونه استاد از سرور…")
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    teacherReferenceRepository.ensureCached(getApplication())
                    TeacherVocalGenerator.prepareLesson(getApplication())
                }
            }.onSuccess { prepared ->
                lesson = prepared
                val teacherPeaks = withContext(Dispatchers.IO) {
                    WaveformPeaksExtractor.fromWavFile(prepared.referenceFile)
                }
                _uiState.update {
                    it.copy(
                        lessonTitle = prepared.title,
                        teacherName = prepared.teacherName,
                        lessonDescription = prepared.description,
                        durationSeconds = ((prepared.durationMs + 999) / 1000).toInt(),
                        teacherSourceLabel = prepared.sourceLabel,
                        teacherAudioFormatLabel = prepared.audioFormatLabel,
                        teacherWaveformPeaks = teacherPeaks,
                        isLessonReady = true,
                        statusMessage = "نمونه استاد آماده است — پخش کنید و سپس ضبط کنید"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLessonReady = false,
                        statusMessage = error.message ?: "خطا در بارگذاری نمونه استاد"
                    )
                }
            }
        }
    }

    fun onListenClicked() = playTeacherFrom(0L)

    fun toggleTeacherPlayback() {
        if (!canPlayTeacher()) return

        cancelSnippetStop()
        if (_uiState.value.playbackTarget == PlaybackTarget.TEACHER &&
            (player.isPlaying || player.canResume)
        ) {
            player.togglePlayPause()
            _uiState.update { it.copy(isPlaying = player.isPlaying) }
            return
        }
        playTeacherFrom(0L)
    }

    fun playTeacherAt(positionMs: Long) {
        if (!canPlayTeacher()) return
        cancelSnippetStop()
        playTeacherFrom(positionMs.coerceAtLeast(0L))
    }

    private fun canPlayTeacher(): Boolean {
        val mode = _uiState.value.mode
        return lesson != null &&
            _uiState.value.isLessonReady &&
            mode != VocalTrainingMode.RECORDING &&
            mode != VocalTrainingMode.ANALYZING
    }

    private fun playTeacherFrom(positionMs: Long) {
        val currentLesson = lesson ?: return
        val totalMs = (currentLesson.durationMs).coerceAtLeast(1L)
        val seekMs = positionMs.coerceIn(0L, totalMs)
        val canSeekInPlace =
            _uiState.value.playbackTarget == PlaybackTarget.TEACHER && player.durationMs() > 0L

        sessionJob?.cancel()
        _uiState.update {
            it.copy(
                playbackTarget = PlaybackTarget.TEACHER,
                isPlaying = true,
                teacherPlaybackPositionMs = seekMs,
                progress = seekMs.toFloat() / totalMs,
                playingSnippetStartMs = null,
                playingSnippetTarget = null,
                selectedCorrectionStartMs = null
            )
        }

        if (canSeekInPlace) {
            stopProgressPolling()
            player.playFrom(seekMs)
            startTeacherPlaybackProgressPolling()
            return
        }

        stopProgressPolling()
        player.stop()
        sessionJob = viewModelScope.launch {
            player.prepare(
                file = currentLesson.referenceFile,
                onFinished = {
                    stopProgressPolling()
                    _uiState.update {
                        it.copy(
                            isPlaying = false,
                            playbackTarget = PlaybackTarget.NONE,
                            teacherPlaybackPositionMs = 0L,
                            progress = 0f
                        )
                    }
                },
                onReady = {
                    player.playFrom(seekMs)
                    startTeacherPlaybackProgressPolling()
                }
            )
        }
    }

    fun toggleUserRecordingPlayback() {
        if (_uiState.value.mode != VocalTrainingMode.RESULT) return
        if (!userRecordingFile.exists() || userRecordingFile.length() <= 44L) return

        cancelSnippetStop()
        if (_uiState.value.playbackTarget == PlaybackTarget.USER &&
            (player.isPlaying || player.canResume)
        ) {
            player.togglePlayPause()
            _uiState.update {
                it.copy(
                    isPlaying = player.isPlaying,
                    playingSnippetStartMs = if (player.isPlaying) null else it.playingSnippetStartMs,
                    playingSnippetTarget = if (player.isPlaying) null else it.playingSnippetTarget
                )
            }
            return
        }
        _uiState.update { it.copy(playingSnippetStartMs = null, playingSnippetTarget = null) }
        startUserRecordingPlayback(startAtMs = 0L, highlightCorrectionMs = null)
    }

    fun playUserRecordingAt(positionMs: Long) {
        if (_uiState.value.mode != VocalTrainingMode.RESULT) return
        cancelSnippetStop()
        _uiState.update { it.copy(playingSnippetStartMs = null, playingSnippetTarget = null) }
        playUserRecordingFrom(positionMs.coerceAtLeast(0L), highlightCorrectionMs = null)
    }

    fun playCorrectionFromMarker(correction: VocalScorer.PitchCorrection) {
        if (_uiState.value.mode != VocalTrainingMode.RESULT) return
        cancelSnippetStop()
        _uiState.update { it.copy(playingSnippetStartMs = null, playingSnippetTarget = null) }
        playUserRecordingFrom(
            positionMs = correction.startMs.coerceAtLeast(0L),
            highlightCorrectionMs = correction.startMs
        )
    }

    fun playCorrectionSnippet(correction: VocalScorer.PitchCorrection) {
        playCorrectionSnippet(correction, PlaybackTarget.USER)
    }

    fun playCorrectionTeacherSnippet(correction: VocalScorer.PitchCorrection) {
        playCorrectionSnippet(correction, PlaybackTarget.TEACHER)
    }

    private fun playCorrectionSnippet(
        correction: VocalScorer.PitchCorrection,
        target: PlaybackTarget
    ) {
        if (_uiState.value.mode != VocalTrainingMode.RESULT) return
        if (target == PlaybackTarget.NONE) return

        if (target == PlaybackTarget.USER) {
            if (!userRecordingFile.exists() || userRecordingFile.length() <= 44L) return
        } else if (!canPlayTeacher()) return

        cancelSnippetStop()

        val anchorMs = when (target) {
            PlaybackTarget.USER -> correction.startMs
            PlaybackTarget.TEACHER -> correction.referenceStartMs
            PlaybackTarget.NONE -> return
        }
        val totalMs = when (target) {
            PlaybackTarget.USER -> (_uiState.value.userRecordingDurationSeconds * 1000L).coerceAtLeast(1L)
            PlaybackTarget.TEACHER -> (lesson?.durationMs ?: 0L).coerceAtLeast(1L)
            PlaybackTarget.NONE -> return
        }

        val clipStartMs = anchorMs.coerceAtLeast(0L)
        val clipEndMs = (anchorMs + CORRECTION_CLIP_PADDING_MS).coerceAtMost(totalMs)

        when (target) {
            PlaybackTarget.USER -> playUserRecordingFrom(clipStartMs, correction.startMs)
            PlaybackTarget.TEACHER -> playTeacherFrom(clipStartMs)
            PlaybackTarget.NONE -> return
        }

        _uiState.update {
            it.copy(
                playingSnippetStartMs = correction.startMs,
                playingSnippetTarget = target,
                selectedCorrectionStartMs = if (target == PlaybackTarget.USER) correction.startMs else null
            )
        }

        snippetStopJob = viewModelScope.launch {
            while (isActive && _uiState.value.playbackTarget == target) {
                if (player.currentPositionMs() >= clipEndMs - 40L) {
                    player.pause()
                    val endMs = clipEndMs.coerceAtMost(totalMs)
                    _uiState.update {
                        it.copy(
                            isPlaying = false,
                            progress = endMs.toFloat() / totalMs,
                            playingSnippetStartMs = null,
                            playingSnippetTarget = null,
                            userPlaybackPositionMs = if (target == PlaybackTarget.USER) {
                                endMs
                            } else {
                                it.userPlaybackPositionMs
                            },
                            teacherPlaybackPositionMs = if (target == PlaybackTarget.TEACHER) {
                                endMs
                            } else {
                                it.teacherPlaybackPositionMs
                            }
                        )
                    }
                    break
                }
                delay(40)
            }
        }
    }

    fun retryRecording() {
        sessionJob?.cancel()
        player.stop()
        stopProgressPolling()
        _uiState.update {
            it.copy(
                mode = VocalTrainingMode.IDLE,
                scoreResult = null,
                progress = 0f,
                isPlaying = false,
                playbackTarget = PlaybackTarget.NONE,
                waveformPeaks = emptyList(),
                selectedCorrectionStartMs = null,
                playingSnippetStartMs = null,
                playingSnippetTarget = null,
                statusMessage = "ضبط مجدد — همان ملودی را بخوانید"
            )
        }
        startRecording()
    }

    fun onRecordClicked() {
        if (lesson == null) {
            _uiState.update {
                it.copy(statusMessage = "لطفاً صبر کنید، درس هنوز آماده نشده است...")
            }
            return
        }
        startRecording()
    }

    fun onRecordPermissionDenied() {
        _uiState.update {
            it.copy(statusMessage = "بدون دسترسی میکروفون، ضبط ممکن نیست.")
        }
    }

    fun onStopClicked() {
        when (_uiState.value.mode) {
            VocalTrainingMode.RECORDING -> recorder.requestStop()
            else -> {
                if (_uiState.value.playbackTarget != PlaybackTarget.NONE) {
                    stopActivePlayback()
                } else {
                    sessionJob?.cancel()
                    _uiState.update {
                        it.copy(mode = VocalTrainingMode.IDLE, isPlaying = false, progress = 0f)
                    }
                }
            }
        }
    }

    fun dismissResults() {
        sessionJob?.cancel()
        stopActivePlayback()
        _uiState.update {
            it.copy(
                mode = VocalTrainingMode.IDLE,
                scoreResult = null,
                progress = 0f,
                isPlaying = false,
                playbackTarget = PlaybackTarget.NONE,
                waveformPeaks = emptyList(),
                selectedCorrectionStartMs = null,
                playingSnippetStartMs = null,
                playingSnippetTarget = null
            )
        }
    }

    private fun startUserRecordingPlayback(
        startAtMs: Long = 0L,
        highlightCorrectionMs: Long? = null
    ) {
        playUserRecordingFrom(startAtMs, highlightCorrectionMs)
    }

    private fun playUserRecordingFrom(positionMs: Long, highlightCorrectionMs: Long?) {
        if (_uiState.value.mode != VocalTrainingMode.RESULT) return
        if (!userRecordingFile.exists() || userRecordingFile.length() <= 44L) return

        cancelSnippetStop()
        val totalMs = (_uiState.value.userRecordingDurationSeconds * 1000L).coerceAtLeast(1L)
        val seekMs = positionMs.coerceIn(0L, totalMs)
        val canSeekInPlace =
            _uiState.value.playbackTarget == PlaybackTarget.USER && player.durationMs() > 0L

        sessionJob?.cancel()
        _uiState.update {
            it.copy(
                playbackTarget = PlaybackTarget.USER,
                isPlaying = true,
                progress = seekMs.toFloat() / totalMs,
                selectedCorrectionStartMs = highlightCorrectionMs,
                userPlaybackPositionMs = seekMs
            )
        }

        if (canSeekInPlace) {
            stopProgressPolling()
            player.playFrom(seekMs)
            startUserPlaybackProgressPolling()
            return
        }

        stopProgressPolling()
        player.stop()
        sessionJob = viewModelScope.launch {
            var started = false
            player.prepare(
                file = userRecordingFile,
                onFinished = {
                    stopProgressPolling()
                    _uiState.update {
                        it.copy(
                            isPlaying = false,
                            progress = 0f,
                            playbackTarget = PlaybackTarget.NONE,
                            userPlaybackPositionMs = 0L,
                            selectedCorrectionStartMs = null,
                            playingSnippetStartMs = null,
                            playingSnippetTarget = null
                        )
                    }
                },
                onReady = {
                    if (started) return@prepare
                    started = true
                    player.playFrom(seekMs)
                    startUserPlaybackProgressPolling()
                }
            )
        }
    }

    private fun stopActivePlayback() {
        cancelSnippetStop()
        stopProgressPolling()
        player.stop()
        _uiState.update {
            it.copy(
                isPlaying = false,
                progress = 0f,
                playbackTarget = PlaybackTarget.NONE,
                teacherPlaybackPositionMs = 0L,
                userPlaybackPositionMs = 0L,
                selectedCorrectionStartMs = null,
                playingSnippetStartMs = null,
                playingSnippetTarget = null
            )
        }
    }

    private fun cancelSnippetStop() {
        snippetStopJob?.cancel()
        snippetStopJob = null
    }

    private fun startRecording() {
        val currentLesson = lesson ?: return
        sessionJob?.cancel()
        stopActivePlayback()

        val recordingFile = userRecordingFile
        val maxRecordingMs = (currentLesson.durationMs + 8_000L).coerceIn(10_000L, 45_000L)
        val minRequiredMs = (currentLesson.durationMs * 0.25).toLong().coerceIn(3_000L, currentLesson.durationMs)

        _uiState.update {
            it.copy(
                mode = VocalTrainingMode.RECORDING,
                progress = 0f,
                statusMessage = "در حال ضبط... پس از اتمام «توقف» را بزنید"
            )
        }

        sessionJob = viewModelScope.launch {
            try {
                startRecordingProgressPolling(maxRecordingMs)
                val recordedMs = withContext(Dispatchers.IO) {
                    recorder.recordFreeform(recordingFile, maxRecordingMs)
                }
                stopProgressPolling()

                when {
                    recordedMs < 500L -> {
                        _uiState.update {
                            it.copy(
                                mode = VocalTrainingMode.IDLE,
                                progress = 0f,
                                statusMessage = if (DeviceAudioHints.isEmulator) {
                                    "صدایی ضبط نشد. در امولیتور: ⋮ → Microphone → Host audio input را روشن کنید."
                                } else {
                                    "صدایی ضبط نشد. دسترسی میکروفون و اتصال هدفون را بررسی کنید."
                                }
                            )
                        }
                    }
                    recordedMs < minRequiredMs -> {
                        _uiState.update {
                            it.copy(
                                mode = VocalTrainingMode.IDLE,
                                progress = 0f,
                                statusMessage = "ضبط کوتاه بود (${recordedMs / 1000} ثانیه). " +
                                    "حداقل ${minRequiredMs / 1000} ثانیه بخوانید و «توقف» بزنید."
                            )
                        }
                    }
                    else -> {
                        lastRecordingDurationMs = recordedMs
                        analyzeRecording(currentLesson.referenceFile, recordingFile)
                    }
                }
            } catch (_: CancellationException) {
                stopProgressPolling()
                recorder.cancel()
                if (_uiState.value.mode != VocalTrainingMode.RESULT) {
                    _uiState.update {
                        it.copy(mode = VocalTrainingMode.IDLE, progress = 0f, statusMessage = "ضبط لغو شد")
                    }
                }
            } catch (e: SecurityException) {
                stopProgressPolling()
                _uiState.update {
                    it.copy(
                        mode = VocalTrainingMode.IDLE,
                        progress = 0f,
                        statusMessage = "دسترسی میکروفون داده نشده. از تنظیمات اپ اجازه دهید."
                    )
                }
            } catch (e: IllegalStateException) {
                stopProgressPolling()
                _uiState.update {
                    it.copy(
                        mode = VocalTrainingMode.IDLE,
                        progress = 0f,
                        statusMessage = e.message ?: "خطا در ضبط"
                    )
                }
            } catch (e: Exception) {
                stopProgressPolling()
                _uiState.update {
                    it.copy(
                        mode = VocalTrainingMode.IDLE,
                        progress = 0f,
                        statusMessage = "خطا در ضبط: ${e.message ?: "میکروفون در دسترس نیست"}"
                    )
                }
            }
        }
    }

    private suspend fun analyzeRecording(referenceFile: File, recordingFile: File) {
        _uiState.update {
            it.copy(
                mode = VocalTrainingMode.ANALYZING,
                progress = 0f,
                scoreResult = null,
                statusMessage = "تحلیل اجرا روی سرور..."
            )
        }
        startAnalyzingProgressPolling()

        try {
            val segments = lesson?.expectedSegments
            val score = withContext(Dispatchers.IO) {
                scoringRepository.score(
                    referenceFile = referenceFile,
                    studentFile = recordingFile,
                    expectedSegments = segments
                )
            }

            val peaks = withContext(Dispatchers.IO) {
                WaveformPeaksExtractor.fromWavFile(recordingFile)
            }

            stopProgressPolling()
            val recordingSeconds = ((lastRecordingDurationMs + 999) / 1000).toInt().coerceAtLeast(1)
            _uiState.update {
                it.copy(
                    mode = VocalTrainingMode.RESULT,
                    progress = 0f,
                    isPlaying = false,
                    playbackTarget = PlaybackTarget.NONE,
                    userRecordingDurationSeconds = recordingSeconds,
                    waveformPeaks = peaks,
                    userPlaybackPositionMs = 0L,
                    selectedCorrectionStartMs = null,
                    playingSnippetStartMs = null,
                    playingSnippetTarget = null,
                    scoreResult = score,
                    statusMessage = score.summary.ifBlank {
                        if (score.overall > 0) "نمره: ${score.overall}" else "تحلیل انجام شد"
                    }
                )
            }
        } catch (e: Exception) {
            stopProgressPolling()
            _uiState.update {
                it.copy(
                    mode = VocalTrainingMode.IDLE,
                    progress = 0f,
                    scoreResult = null,
                    statusMessage = "خطا در ارتباط با سرور: ${e.message ?: "نامشخص"}"
                )
            }
        }
    }

    private fun startAnalyzingProgressPolling() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            var progress = 0.05f
            while (isActive && _uiState.value.mode == VocalTrainingMode.ANALYZING) {
                progress = (progress + 0.03f).coerceAtMost(0.92f)
                _uiState.update { it.copy(progress = progress) }
                delay(120)
            }
        }
    }

    private fun startTeacherPlaybackProgressPolling() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive && _uiState.value.playbackTarget == PlaybackTarget.TEACHER) {
                _uiState.update {
                    it.copy(
                        progress = player.currentProgress(),
                        isPlaying = player.isPlaying,
                        teacherPlaybackPositionMs = player.currentPositionMs()
                    )
                }
                delay(50)
            }
        }
    }

    private fun startUserPlaybackProgressPolling() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive &&
                _uiState.value.mode == VocalTrainingMode.RESULT &&
                _uiState.value.playbackTarget == PlaybackTarget.USER
            ) {
                _uiState.update {
                    it.copy(
                        progress = player.currentProgress(),
                        isPlaying = player.isPlaying,
                        userPlaybackPositionMs = player.currentPositionMs()
                    )
                }
                delay(50)
            }
        }
    }

    private fun startRecordingProgressPolling(maxRecordingMs: Long) {
        val startedAt = System.currentTimeMillis()
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive && _uiState.value.mode == VocalTrainingMode.RECORDING) {
                val elapsed = System.currentTimeMillis() - startedAt
                val progress = (elapsed.toFloat() / maxRecordingMs).coerceIn(0f, 1f)
                _uiState.update { it.copy(progress = progress) }
                delay(50)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun onCleared() {
        sessionJob?.cancel()
        stopProgressPolling()
        recorder.cancel()
        player.release()
        super.onCleared()
    }

    companion object {
        private const val USER_RECORDING_FILE_NAME = "user_vocal.wav"
        private const val CORRECTION_CLIP_PADDING_MS = 1_000L
    }
}

enum class VocalTrainingMode { IDLE, RECORDING, ANALYZING, RESULT }

enum class PlaybackTarget { NONE, TEACHER, USER }

data class VocalTrainingUiState(
    val lessonTitle: String = "",
    val teacherName: String = "",
    val lessonDescription: String = "",
    val durationSeconds: Int = 0,
    val teacherSourceLabel: String = "",
    val teacherAudioFormatLabel: String = "",
    val teacherWaveformPeaks: List<Float> = emptyList(),
    val teacherPlaybackPositionMs: Long = 0L,
    val isLessonReady: Boolean = false,
    val mode: VocalTrainingMode = VocalTrainingMode.IDLE,
    val progress: Float = 0f,
    val isPlaying: Boolean = false,
    val playbackTarget: PlaybackTarget = PlaybackTarget.NONE,
    val userRecordingDurationSeconds: Int = 0,
    val userPlaybackPositionMs: Long = 0L,
    val waveformPeaks: List<Float> = emptyList(),
    val selectedCorrectionStartMs: Long? = null,
    val playingSnippetStartMs: Long? = null,
    val playingSnippetTarget: PlaybackTarget? = null,
    val statusMessage: String = "",
    val scoreResult: VocalScoreResult? = null
) {
    val isPlayingTeacherSample: Boolean
        get() = playbackTarget == PlaybackTarget.TEACHER && isPlaying

    val isPlayingUserRecording: Boolean
        get() = mode == VocalTrainingMode.RESULT &&
            playbackTarget == PlaybackTarget.USER &&
            isPlaying
}
