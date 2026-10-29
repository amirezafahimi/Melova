package app.melova.presentation

import app.melova.platform.SecurityException
import androidx.lifecycle.ViewModel
import app.melova.platform.appContext
import app.melova.platform.cacheDir
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.melova.audio.io.WaveformPeaksExtractor
import app.melova.audio.scoring.VocalScorer
import app.melova.data.model.PlaybackClip
import app.melova.data.model.TahrirComparison
import app.melova.data.model.VocalScoreResult
import app.melova.data.repository.RemoteVocalScoringRepository
import app.melova.data.local.PracticeProgressStore
import app.melova.data.repository.LessonRepository
import app.melova.data.repository.RemoteLessonRepository
import app.melova.data.repository.StudentRepository
import app.melova.data.remote.ApiException
import app.melova.data.repository.VocalScoringRepository
import app.melova.audio.playback.ReferenceAudioPlayer
import app.melova.audio.playback.TeacherVocalGenerator
import app.melova.audio.capture.DeviceAudioHints
import app.melova.audio.capture.VocalRecorder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import app.melova.platform.LocalFile
import app.melova.platform.ioDispatcher
import app.melova.platform.currentTimeMillis
import kotlin.concurrent.Volatile

class VocalTrainingViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val lessonId: String = checkNotNull(savedStateHandle[ARG_LESSON_ID])
    private val sectionId: String = checkNotNull(savedStateHandle[ARG_SECTION_ID])

    private val scoringRepository: VocalScoringRepository = RemoteVocalScoringRepository()
    private val lessonRepository: LessonRepository = RemoteLessonRepository()
    private val studentRepository = StudentRepository(appContext)
    private val progressStore = PracticeProgressStore(appContext)
    private val player = ReferenceAudioPlayer(appContext)
    private val recorder = VocalRecorder(appContext)

    @Volatile
    private var lesson: TeacherVocalGenerator.TeacherLesson? = null

    private val userRecordingFile =
        LocalFile(cacheDir(appContext), USER_RECORDING_FILE_NAME)

    @Volatile
    private var lessonTeacherId: String = ""

    @Volatile
    private var lastRecordingDurationMs: Long = 0L

    private val _uiState = MutableStateFlow(
        VocalTrainingUiState(bestScore = progressStore.bestScore(lessonId, sectionId))
    )
    val uiState: StateFlow<VocalTrainingUiState> = _uiState.asStateFlow()

    private var sessionJob: Job? = null
    private var progressJob: Job? = null
    private var snippetStopJob: Job? = null

    init {
        loadLesson()
    }

    fun loadLesson() {
        if (_uiState.value.isLessonLoading) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLessonLoading = true,
                    lessonLoadFailed = false,
                    statusMessage = "در حال آماده‌سازی نمونه استاد…"
                )
            }
            runCatching {
                val remoteLesson = lessonRepository.lessons(appContext)
                    .firstOrNull { it.id == lessonId }
                    ?: error("این درس پیدا نشد.")
                val section = remoteLesson.section(sectionId) ?: error("این بخش پیدا نشد.")
                val referenceFile = lessonRepository.sectionAudioFile(appContext, remoteLesson, section)
                val prepared = withContext(ioDispatcher) {
                    TeacherVocalGenerator.lessonFromFile(
                        referenceFile = referenceFile,
                        title = section.title,
                        teacherName = remoteLesson.teacher,
                        description = section.description
                    )
                }
                lessonTeacherId = remoteLesson.teacherId
                // A free lesson is claimed once so its reviews are on the student's account.
                if (remoteLesson.isFree && remoteLesson.isOwned == false) claimFreeLesson()
                Triple(remoteLesson.title, prepared, withContext(ioDispatcher) {
                    WaveformPeaksExtractor.fromWavFile(prepared.referenceFile)
                })
            }.onSuccess { (courseTitle, prepared, teacherPeaks) ->
                lesson = prepared
                _uiState.update {
                    it.copy(
                        courseTitle = courseTitle,
                        lessonTitle = prepared.title,
                        teacherName = prepared.teacherName,
                        lessonDescription = prepared.description,
                        durationSeconds = ((prepared.durationMs + 999) / 1000).toInt(),
                        teacherSourceLabel = prepared.sourceLabel,
                        teacherAudioFormatLabel = prepared.audioFormatLabel,
                        teacherWaveformPeaks = teacherPeaks,
                        isLessonReady = true,
                        isLessonLoading = false,
                        statusMessage = "اول نمونه استاد را گوش دهید، بعد ضبط را شروع کنید."
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLessonReady = false,
                        isLessonLoading = false,
                        lessonLoadFailed = true,
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

        val clip = when (target) {
            PlaybackTarget.USER -> correction.studentClip
            PlaybackTarget.TEACHER -> correction.teacherClip
            PlaybackTarget.NONE -> null
        }
        if (clip != null) {
            playRegionSnippet(
                target = target,
                startMs = clip.startMs,
                endMs = clip.endMs,
                snippetKey = correction.startMs,
                clip = clip,
                highlightCorrectionMs = if (target == PlaybackTarget.USER) correction.startMs else null
            )
            return
        }

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

    fun playTahrirStudentSnippet(comparison: TahrirComparison) {
        comparison.studentClip?.let { clip ->
            playRegionSnippet(PlaybackTarget.USER, clip.startMs, clip.endMs, comparison.key, clip)
            return
        }
        val start = comparison.studentStartMs ?: return
        val end = comparison.studentEndMs ?: return
        playRegionSnippet(PlaybackTarget.USER, start, end, comparison.key)
    }

    fun playTahrirTeacherSnippet(comparison: TahrirComparison) {
        comparison.teacherClip?.let { clip ->
            playRegionSnippet(PlaybackTarget.TEACHER, clip.startMs, clip.endMs, comparison.key, clip)
            return
        }
        playRegionSnippet(PlaybackTarget.TEACHER, comparison.refStartMs, comparison.refEndMs, comparison.key)
    }

    /**
     * پخش گزیده‌ی یک بازه‌ی دقیق [startMs, endMs] از صدای استاد یا هنرجو.
     * با [clip]، سرعت پخش در محدوده‌ی خطا آرام‌آرام کم و بعد از آن آرام‌آرام زیاد می‌شود.
     */
    private fun playRegionSnippet(
        target: PlaybackTarget,
        startMs: Long,
        endMs: Long,
        snippetKey: Long,
        clip: PlaybackClip? = null,
        highlightCorrectionMs: Long? = null
    ) {
        if (_uiState.value.mode != VocalTrainingMode.RESULT) return
        if (target == PlaybackTarget.NONE) return

        if (target == PlaybackTarget.USER) {
            if (!userRecordingFile.exists() || userRecordingFile.length() <= 44L) return
        } else if (!canPlayTeacher()) return

        cancelSnippetStop()

        val totalMs = when (target) {
            PlaybackTarget.USER -> (_uiState.value.userRecordingDurationSeconds * 1000L).coerceAtLeast(1L)
            PlaybackTarget.TEACHER -> (lesson?.durationMs ?: 0L).coerceAtLeast(1L)
            PlaybackTarget.NONE -> return
        }

        val clipStartMs = startMs.coerceIn(0L, totalMs)
        val clipEndMs = endMs.coerceIn(clipStartMs + 1L, totalMs)

        when (target) {
            PlaybackTarget.USER -> playUserRecordingFrom(clipStartMs, highlightCorrectionMs)
            PlaybackTarget.TEACHER -> playTeacherFrom(clipStartMs)
            PlaybackTarget.NONE -> return
        }
        clip?.let { player.setSpeed(it.speedAt(clipStartMs)) }

        _uiState.update {
            it.copy(
                playingSnippetStartMs = snippetKey,
                playingSnippetTarget = target,
                selectedCorrectionStartMs = highlightCorrectionMs
            )
        }

        snippetStopJob = viewModelScope.launch {
            while (isActive && _uiState.value.playbackTarget == target) {
                val positionMs = player.currentPositionMs()
                clip?.let { player.setSpeed(it.speedAt(positionMs)) }
                if (positionMs >= clipEndMs - 40L) {
                    player.pause()
                    player.setSpeed(1f)
                    val endAtMs = clipEndMs.coerceAtMost(totalMs)
                    _uiState.update {
                        it.copy(
                            isPlaying = false,
                            progress = endAtMs.toFloat() / totalMs,
                            playingSnippetStartMs = null,
                            playingSnippetTarget = null,
                            userPlaybackPositionMs = if (target == PlaybackTarget.USER) {
                                endAtMs
                            } else {
                                it.userPlaybackPositionMs
                            },
                            teacherPlaybackPositionMs = if (target == PlaybackTarget.TEACHER) {
                                endAtMs
                            } else {
                                it.teacherPlaybackPositionMs
                            }
                        )
                    }
                    break
                }
                delay(if (clip != null) SPEED_TICK_MS else 40L)
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
                teacherAsk = TeacherAskState(),
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

    private fun claimFreeLesson() {
        viewModelScope.launch {
            runCatching {
                studentRepository.ensureAccount()
                studentRepository.claimFreeLesson(lessonId)
            }
        }
    }

    /** Sends the take just scored to the teacher, with the score summary and an optional note. */
    fun sendToTeacher(note: String) {
        val score = _uiState.value.scoreResult ?: return
        if (_uiState.value.teacherAsk.isSending || _uiState.value.teacherAsk.isSent) return
        if (!userRecordingFile.exists() || userRecordingFile.length() <= 44L) return
        viewModelScope.launch {
            _uiState.update { it.copy(teacherAsk = it.teacherAsk.copy(isSending = true, error = null)) }
            runCatching {
                studentRepository.ensureAccount()
                studentRepository.sendForReview(lessonId, sectionId, userRecordingFile, note, score)
            }.onSuccess { account ->
                _uiState.update {
                    it.copy(
                        teacherAsk = TeacherAskState(
                            isSent = true,
                            creditsLeft = account.credits(lessonTeacherId),
                            studentId = account.id
                        )
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        teacherAsk = TeacherAskState(
                            error = error.message ?: "ارسال ممکن نشد.",
                            needsCredit = (error as? ApiException)?.code == 402,
                            studentId = studentRepository.studentId
                        )
                    )
                }
            }
        }
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
                teacherAsk = TeacherAskState(),
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
        player.setSpeed(1f)
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
                val recordedMs = withContext(ioDispatcher) {
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

    private suspend fun analyzeRecording(referenceFile: LocalFile, recordingFile: LocalFile) {
        _uiState.update {
            it.copy(
                mode = VocalTrainingMode.ANALYZING,
                progress = 0f,
                scoreResult = null,
                teacherAsk = TeacherAskState(),
                statusMessage = "تحلیل اجرا روی سرور..."
            )
        }
        startAnalyzingProgressPolling()

        try {
            val segments = lesson?.expectedSegments
            val score = withContext(ioDispatcher) {
                scoringRepository.score(
                    referenceFile = referenceFile,
                    studentFile = recordingFile,
                    expectedSegments = segments,
                    lessonId = lessonId,
                    sectionId = sectionId
                )
            }
            val isNewBest = !score.requiresReRecord && score.overall > 0 &&
                progressStore.recordScore(lessonId, sectionId, score.overall)

            val peaks = withContext(ioDispatcher) {
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
                    teacherAsk = TeacherAskState(),
                    isNewBest = isNewBest,
                    bestScore = progressStore.bestScore(lessonId, sectionId),
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
                    teacherAsk = TeacherAskState(),
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
        val startedAt = currentTimeMillis()
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive && _uiState.value.mode == VocalTrainingMode.RECORDING) {
                val elapsed = currentTimeMillis() - startedAt
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
        const val ARG_LESSON_ID = "lessonId"
        const val ARG_SECTION_ID = "sectionId"
        private const val USER_RECORDING_FILE_NAME = "user_vocal.wav"
        private const val CORRECTION_CLIP_PADDING_MS = 1_000L
        /** هر چند میلی‌ثانیه سرعت پخش به‌روز شود تا کم و زیاد شدنش نرم باشد. */
        private const val SPEED_TICK_MS = 20L
    }
}

enum class VocalTrainingMode { IDLE, RECORDING, ANALYZING, RESULT }

enum class PlaybackTarget { NONE, TEACHER, USER }

data class VocalTrainingUiState(
    val courseTitle: String = "",
    val lessonTitle: String = "",
    val teacherName: String = "",
    val lessonDescription: String = "",
    val durationSeconds: Int = 0,
    val teacherSourceLabel: String = "",
    val teacherAudioFormatLabel: String = "",
    val teacherWaveformPeaks: List<Float> = emptyList(),
    val teacherPlaybackPositionMs: Long = 0L,
    val isLessonReady: Boolean = false,
    val isLessonLoading: Boolean = false,
    val lessonLoadFailed: Boolean = false,
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
    val scoreResult: VocalScoreResult? = null,
    /** Best score for this section on this device, including the latest attempt. */
    val bestScore: Int? = null,
    val isNewBest: Boolean = false,
    val teacherAsk: TeacherAskState = TeacherAskState()
) {
    val isPlayingTeacherSample: Boolean
        get() = playbackTarget == PlaybackTarget.TEACHER && isPlaying

    val isPlayingUserRecording: Boolean
        get() = mode == VocalTrainingMode.RESULT &&
            playbackTarget == PlaybackTarget.USER &&
            isPlaying
}

/** Sending the latest take to the teacher. */
data class TeacherAskState(
    val isSending: Boolean = false,
    val isSent: Boolean = false,
    val error: String? = null,
    /** The server said no credit or no lesson: the student pays the teacher, who adds it. */
    val needsCredit: Boolean = false,
    val creditsLeft: Int? = null,
    val studentId: String? = null
)
