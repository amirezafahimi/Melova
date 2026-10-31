package app.melova.presentation

import app.melova.platform.SecurityException
import androidx.lifecycle.ViewModel
import app.melova.platform.appContext
import app.melova.platform.cacheDir
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.melova.audio.io.WavUtils
import app.melova.audio.io.WaveformPeaksExtractor
import app.melova.audio.pitch.PitchContour
import app.melova.audio.scoring.VocalScorer
import app.melova.data.model.ErrorDetail
import app.melova.data.model.ErrorDetailLane
import app.melova.data.model.ErrorMarkKind
import app.melova.data.model.PlaybackClip
import app.melova.data.model.TahrirComparison
import app.melova.data.model.VocalScoreResult
import app.melova.data.repository.ChatRepository
import app.melova.data.repository.RemoteVocalScoringRepository
import app.melova.data.local.PracticeProgressStore
import app.melova.data.repository.LessonRepository
import app.melova.data.repository.RemoteLessonRepository
import app.melova.data.repository.StudentRepository
import app.melova.data.repository.VocalScoringRepository
import app.melova.audio.playback.DetailVoicePlayer
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
import app.melova.platform.smoothSpeedChanges
import kotlin.concurrent.Volatile

class VocalTrainingViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val lessonId: String = checkNotNull(savedStateHandle[ARG_LESSON_ID])
    private val sectionId: String = checkNotNull(savedStateHandle[ARG_SECTION_ID])

    /** Set when this page shows a take sent to a teacher instead of a new practice. */
    private val reviewId: String? = savedStateHandle[ARG_REVIEW_ID]

    private val scoringRepository: VocalScoringRepository = RemoteVocalScoringRepository()
    private val lessonRepository: LessonRepository = RemoteLessonRepository()
    private val studentRepository = StudentRepository(appContext)
    private val chatRepository = ChatRepository(appContext)
    private val progressStore = PracticeProgressStore(appContext)
    private val player = ReferenceAudioPlayer(appContext)
    private val recorder = VocalRecorder(appContext)

    @Volatile
    private var lesson: TeacherVocalGenerator.TeacherLesson? = null

    @Volatile
    private var userRecordingFile =
        LocalFile(cacheDir(appContext), USER_RECORDING_FILE_NAME)

    @Volatile
    private var lastRecordingDurationMs: Long = 0L

    /** Both voices' samples for «دیدن جزییات», read once per lesson / take. */
    @Volatile
    private var teacherAudio: Pair<ShortArray, Int>? = null

    @Volatile
    private var studentAudio: Pair<ShortArray, Int>? = null

    private val _uiState = MutableStateFlow(
        VocalTrainingUiState(
            bestScore = if (reviewId == null) progressStore.bestScore(lessonId, sectionId) else null,
            isSharedTake = reviewId != null
        )
    )
    val uiState: StateFlow<VocalTrainingUiState> = _uiState.asStateFlow()

    private var sessionJob: Job? = null
    private var progressJob: Job? = null
    private var snippetStopJob: Job? = null
    private var detailJob: Job? = null
    private var detailPlaybackJob: Job? = null

    /** Made when the first detail sheet opens: most visits to this page never need them. */
    private var teacherVoice: DetailVoicePlayer? = null
    private var studentVoice: DetailVoicePlayer? = null
    private var resumeAfterScrub = false

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
            if (reviewId != null && _uiState.value.mode != VocalTrainingMode.RESULT) loadSharedTake(reviewId)
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
                // Singing in a free lesson adds it to the student's lessons.
                if (reviewId == null && remoteLesson.isFree && remoteLesson.isOwned == false) claimFreeLesson()
                Triple(remoteLesson.title, prepared, withContext(ioDispatcher) {
                    WaveformPeaksExtractor.fromWavFile(prepared.referenceFile)
                })
            }.onSuccess { (courseTitle, prepared, teacherPeaks) ->
                lesson = prepared
                teacherAudio = null
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
                        statusMessage = if (reviewId != null) {
                            it.statusMessage
                        } else {
                            PRACTICE_TIP
                        }
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

    /** The take and its result, so the rest of this page works just as after a recording. */
    private fun loadSharedTake(id: String) {
        viewModelScope.launch {
            runCatching {
                val take = chatRepository.take(id)
                val audio = chatRepository.takeAudio(id)
                val (pcm, peaks) = withContext(ioDispatcher) {
                    WavUtils.readPcm16Samples(audio) to WaveformPeaksExtractor.fromWavFile(audio)
                }
                userRecordingFile = audio
                studentAudio = pcm
                val recordingMs = pcm.durationMs
                _uiState.update {
                    it.copy(
                        mode = VocalTrainingMode.RESULT,
                        userRecordingDurationSeconds = ((recordingMs + 999) / 1000).toInt().coerceAtLeast(1),
                        waveformPeaks = peaks,
                        scoreResult = take.result,
                        sharedTakeStudent = take.studentName.takeIf { take.isTeacherViewing },
                        statusMessage = take.result?.summary?.ifBlank { null } ?: "تحلیل این اجرا ذخیره نشده است."
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(lessonLoadFailed = true, statusMessage = error.message ?: "این اجرا باز نشد.")
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
        if (!_uiState.value.hasTake) return
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
        if (!_uiState.value.hasTake) return
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
     * Plays exactly [startMs, endMs] of the teacher's or student's voice.
     * With [clip], speed eases down into the mistake and back up after it.
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
        // Without smooth speed changes the whole clip plays at the slow speed.
        val rampSpeed = clip?.takeIf { smoothSpeedChanges }
        clip?.let { player.setSpeed(if (rampSpeed != null) it.speedAt(clipStartMs) else it.slowSpeed) }

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
                rampSpeed?.let { player.setSpeed(it.speedAt(positionMs)) }
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
                errorDetail = null,
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
            }.onSuccess { chatId ->
                _uiState.update { it.copy(teacherAsk = TeacherAskState(isSent = true, chatId = chatId)) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(teacherAsk = TeacherAskState(error = error.message ?: "ارسال ممکن نشد."))
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

    fun showCorrectionDetail(correction: VocalScorer.PitchCorrection) {
        val teacherMs = correction.teacherClip?.focusCentreMs ?: correction.referenceStartMs
        val studentMs = correction.studentClip?.focusCentreMs ?: correction.startMs
        showErrorDetail(
            ErrorDetail(
                key = correction.startMs,
                markKind = ErrorMarkKind.MOMENT,
                teacher = null,
                student = null,
                correction = correction
            ),
            teacherMark = teacherMs..teacherMs,
            teacherClip = correction.teacherClip,
            studentMark = studentMs..studentMs,
            studentClip = correction.studentClip
        )
    }

    fun showTahrirDetail(comparison: TahrirComparison) {
        val studentStart = comparison.studentStartMs
        val studentEnd = comparison.studentEndMs
        showErrorDetail(
            ErrorDetail(
                key = comparison.key,
                markKind = ErrorMarkKind.SPAN,
                teacher = null,
                student = null,
                comparison = comparison
            ),
            teacherMark = comparison.refStartMs..comparison.refEndMs,
            teacherClip = comparison.teacherClip,
            studentMark = if (comparison.hasStudentRegion && studentStart != null && studentEnd != null) {
                studentStart..studentEnd
            } else {
                null
            },
            studentClip = comparison.studentClip
        )
    }

    fun hideErrorDetail() {
        detailJob?.cancel()
        pauseDetailPlayback()
        teacherVoice?.unload()
        studentVoice?.unload()
        _uiState.update { it.copy(errorDetail = null) }
    }

    private fun showErrorDetail(
        detail: ErrorDetail,
        teacherMark: LongRange,
        teacherClip: PlaybackClip?,
        studentMark: LongRange?,
        studentClip: PlaybackClip?
    ) {
        if (_uiState.value.scoreResult == null) return
        stopActivePlayback()
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            val (teacherPcm, studentPcm) = withContext(ioDispatcher) {
                runCatching { loadTeacherAudio() }.getOrNull() to
                    runCatching { loadStudentAudio() }.getOrNull()
            }
            val teacher = detailLane(teacherPcm, teacherMark, teacherClip)
            val student = studentMark?.let { detailLane(studentPcm, it, studentClip) }
            _uiState.update {
                if (it.scoreResult == null) it
                else it.copy(errorDetail = detail.copy(teacher = teacher, student = student))
            }
            if (teacher != null) lesson?.referenceFile?.let { file ->
                val voice = teacherVoice ?: DetailVoicePlayer(appContext).also { teacherVoice = it }
                voice.load(file, teacher.startMs, teacher.endMs, teacher.speed)
            }
            if (student != null) {
                val voice = studentVoice ?: DetailVoicePlayer(appContext).also { studentVoice = it }
                voice.load(userRecordingFile, student.startMs, student.endMs, student.speed)
            }
            val lanes = withContext(ioDispatcher) {
                val teacherLine = teacher?.let { lineOf(teacherPcm, it) }.orEmpty()
                val studentLine = student?.let {
                    PitchContour.inRegisterOf(teacherLine, lineOf(studentPcm, it))
                }.orEmpty()
                val guide = if (teacher != null && student != null) {
                    PitchContour.guide(teacherLine, studentLine, student.markStartMs..student.markEndMs) {
                        teacher.timeOn(student, it)
                    }
                } else {
                    emptyList()
                }
                detail.copy(
                    teacher = teacher?.copy(pitch = teacherLine),
                    student = student?.copy(pitch = studentLine, guide = guide),
                    pitchRange = PitchContour.rangeOf(listOf(teacherLine, studentLine, guide))
                )
            }
            _uiState.update { if (it.errorDetail == null) it else it.copy(errorDetail = lanes) }
        }
    }

    private suspend fun lineOf(pcm: Pair<ShortArray, Int>?, lane: ErrorDetailLane) =
        pcm?.let { (samples, sampleRate) ->
            PitchContour.of(samples, sampleRate, lane.startMs, lane.endMs)
        }.orEmpty()

    private fun loadTeacherAudio(): Pair<ShortArray, Int>? {
        teacherAudio?.let { return it }
        val file = lesson?.referenceFile?.takeIf { it.exists() } ?: return null
        return WavUtils.readPcm16Samples(file).also { teacherAudio = it }
    }

    private fun loadStudentAudio(): Pair<ShortArray, Int>? {
        studentAudio?.let { return it }
        if (!userRecordingFile.exists() || userRecordingFile.length() <= 44L) return null
        return WavUtils.readPcm16Samples(userRecordingFile).also { studentAudio = it }
    }

    /** The clip the student hears around the error, or a second each side of it. */
    private fun detailLane(pcm: Pair<ShortArray, Int>?, mark: LongRange, clip: PlaybackClip?): ErrorDetailLane? {
        val totalMs = pcm?.durationMs ?: return null
        if (totalMs <= 0L) return null
        val startMs = (clip?.startMs ?: (mark.first - DETAIL_CONTEXT_MS)).coerceIn(0L, totalMs)
        val endMs = (clip?.endMs ?: (mark.last + DETAIL_CONTEXT_MS)).coerceIn(startMs + 1L, totalMs.coerceAtLeast(startMs + 1L))
        return ErrorDetailLane(
            startMs = startMs,
            endMs = endMs,
            markStartMs = mark.first,
            markEndMs = mark.last,
            speed = clip?.slowSpeed ?: 1f
        )
    }

    private val Pair<ShortArray, Int>.durationMs: Long
        get() = if (second > 0) first.size * 1000L / second else 0L

    /** Plays or pauses one voice of the open sheet; the other one waits where it stopped. */
    fun toggleDetailPlayback(target: PlaybackTarget) {
        val isPlayingTarget = _uiState.value.detailPlaying == target
        pauseDetailPlayback()
        if (!isPlayingTarget) playDetail(target)
    }

    private fun playDetail(target: PlaybackTarget) {
        val voice = detailVoice(target) ?: return
        voice.play()
        _uiState.update { it.copy(detailPlaying = target) }
        detailPlaybackJob = viewModelScope.launch {
            while (isActive && !voice.hasFinished) delay(SPEED_TICK_MS)
            voice.pause()
            _uiState.update { it.copy(detailPlaying = PlaybackTarget.NONE) }
        }
    }

    private fun pauseDetailPlayback() {
        detailPlaybackJob?.cancel()
        detailVoice(_uiState.value.detailPlaying)?.pause()
        _uiState.update { it.copy(detailPlaying = PlaybackTarget.NONE) }
    }

    /** The head is being dragged: silence until it lands. */
    fun startDetailScrub(target: PlaybackTarget) {
        resumeAfterScrub = _uiState.value.detailPlaying == target
        pauseDetailPlayback()
    }

    fun seekDetail(target: PlaybackTarget, positionMs: Long) {
        detailVoice(target)?.seek(positionMs)
        if (resumeAfterScrub) playDetail(target)
        resumeAfterScrub = false
    }

    fun detailPositionMs(target: PlaybackTarget): Long = detailVoice(target)?.positionMs ?: 0L

    private fun detailVoice(target: PlaybackTarget): DetailVoicePlayer? = when (target) {
        PlaybackTarget.TEACHER -> teacherVoice
        PlaybackTarget.USER -> studentVoice
        PlaybackTarget.NONE -> null
    }

    private val PlaybackClip.focusCentreMs: Long
        get() = (slowStartMs + slowEndMs) / 2

    /** Back to the page as it first opened. */
    fun dismissResults() {
        sessionJob?.cancel()
        stopActivePlayback()
        studentAudio = null
        _uiState.update {
            it.copy(
                mode = VocalTrainingMode.IDLE,
                scoreResult = null,
                errorDetail = null,
                teacherAsk = TeacherAskState(),
                isNewBest = false,
                userRecordingDurationSeconds = 0,
                waveformPeaks = emptyList(),
                statusMessage = PRACTICE_TIP
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
        if (!_uiState.value.hasTake) return
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
                statusMessage = "در حال ضبط... در پایان، «پایان ضبط» را بزنید"
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
                        showTakeForReview(recordingFile)
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

    /** The student hears and sees the take first; nothing is uploaded until they send it. */
    private suspend fun showTakeForReview(recordingFile: LocalFile) {
        val peaks = withContext(ioDispatcher) { WaveformPeaksExtractor.fromWavFile(recordingFile) }
        _uiState.update {
            it.copy(
                mode = VocalTrainingMode.REVIEW,
                progress = 0f,
                userRecordingDurationSeconds = ((lastRecordingDurationMs + 999) / 1000).toInt().coerceAtLeast(1),
                waveformPeaks = peaks,
                userPlaybackPositionMs = 0L,
                statusMessage = "اجرایتان را گوش دهید؛ اگر راضی بودید برای تحلیل بفرستید."
            )
        }
    }

    fun submitRecording() {
        val currentLesson = lesson ?: return
        if (_uiState.value.mode != VocalTrainingMode.REVIEW) return
        sessionJob?.cancel()
        stopActivePlayback()
        sessionJob = viewModelScope.launch { analyzeRecording(currentLesson.referenceFile, userRecordingFile) }
    }

    private suspend fun analyzeRecording(referenceFile: LocalFile, recordingFile: LocalFile) {
        _uiState.update {
            it.copy(
                mode = VocalTrainingMode.ANALYZING,
                progress = 0f,
                scoreResult = null,
                errorDetail = null,
                teacherAsk = TeacherAskState(),
                statusMessage = "تحلیل اجرا توسط برنامه..."
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
            studentAudio = null

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
                    errorDetail = null,
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
                    // The take stays, so it can be sent again without singing it again.
                    mode = VocalTrainingMode.REVIEW,
                    progress = 0f,
                    scoreResult = null,
                    errorDetail = null,
                    teacherAsk = TeacherAskState(),
                    statusMessage = "تحلیل انجام نشد. ${e.message.orEmpty()}".trim()
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
                _uiState.value.hasTake &&
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
        teacherVoice?.release()
        studentVoice?.release()
        super.onCleared()
    }

    companion object {
        const val ARG_LESSON_ID = "lessonId"
        const val ARG_SECTION_ID = "sectionId"
        const val ARG_REVIEW_ID = "reviewId"
        private const val USER_RECORDING_FILE_NAME = "user_vocal.wav"
        private const val PRACTICE_TIP = "برای تحلیل بهتر، جمله‌های استاد را عیناً بخوانید؛ بدون تکرار یا اصلاح."
        private const val CORRECTION_CLIP_PADDING_MS = 1_000L
        /** How often the speed updates, so that its ramp sounds smooth. */
        private const val SPEED_TICK_MS = 20L
        private const val DETAIL_CONTEXT_MS = 1_000L
    }
}

enum class VocalTrainingMode { IDLE, RECORDING, REVIEW, ANALYZING, RESULT }

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
    val teacherAsk: TeacherAskState = TeacherAskState(),
    /** The open «دیدن جزییات» sheet. */
    val errorDetail: ErrorDetail? = null,
    /** The voice sounding in the detail sheet. */
    val detailPlaying: PlaybackTarget = PlaybackTarget.NONE,
    /** A take opened from a chat: everything can be played, nothing can be recorded. */
    val isSharedTake: Boolean = false,
    /** Whose take it is, when the teacher is the one looking. */
    val sharedTakeStudent: String? = null
) {
    /** A take is on the page, waiting to be sent or already scored. */
    val hasTake: Boolean
        get() = mode == VocalTrainingMode.REVIEW || mode == VocalTrainingMode.RESULT

    val isPlayingTeacherSample: Boolean
        get() = playbackTarget == PlaybackTarget.TEACHER && isPlaying

    val isPlayingUserRecording: Boolean
        get() = hasTake &&
            playbackTarget == PlaybackTarget.USER &&
            isPlaying
}

/** Sending the latest take to the teacher. */
data class TeacherAskState(
    val isSending: Boolean = false,
    val isSent: Boolean = false,
    /** The chat the take went into. */
    val chatId: String? = null,
    val error: String? = null
)
