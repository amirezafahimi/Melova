package app.melova.presentation

import androidx.lifecycle.ViewModel
import app.melova.platform.appContext
import app.melova.platform.cacheDir
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.melova.audio.io.WavUtils
import app.melova.audio.io.WaveformPeaksExtractor
import app.melova.audio.playback.ReferenceAudioPlayer
import app.melova.audio.playback.TeacherVocalGenerator
import app.melova.data.model.Lesson
import app.melova.data.model.LessonSection
import app.melova.data.model.TeacherOrnament
import app.melova.data.repository.LessonRepository
import app.melova.data.repository.RemoteLessonRepository
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

/**
 * The teacher's performance in detail: the wave, its tahrir (T) and vibrato (V) segments,
 * and exact playback of each at any speed.
 */
class TeacherPerformanceViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val lessonId: String = checkNotNull(savedStateHandle[ARG_LESSON_ID])
    private val sectionId: String = checkNotNull(savedStateHandle[ARG_SECTION_ID])

    private val lessonRepository: LessonRepository = RemoteLessonRepository()
    private val player = ReferenceAudioPlayer(appContext)

    private val _uiState = MutableStateFlow(TeacherPerformanceUiState())
    val uiState: StateFlow<TeacherPerformanceUiState> = _uiState.asStateFlow()

    private var lesson: Lesson? = null
    private var section: LessonSection? = null
    private var progressJob: Job? = null
    private var playJob: Job? = null

    private var slowJob: Job? = null

    /** What the player reads from: the teacher's file, or a slower copy once one is here. */
    private var voice: Voice? = null
    private val voices = mutableMapOf<Float, Voice>()

    /**
     * A segment plays from its own cut of the audio, so it ends where the
     * vowel ends. Pausing at the end position instead lets the audio already
     * queued for the output (tens of ms on a speaker, far more on Bluetooth)
     * run into the next syllable.
     */
    private val segmentFile = LocalFile(cacheDir(appContext), SEGMENT_FILE_NAME)

    /** Where in the teacher's file the loaded segment starts; null while the whole file is loaded. */
    private var loadedClipStartMs: Long? = null

    init {
        load()
    }

    fun load() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val remoteLesson = lessonRepository.lessons(appContext)
                    .firstOrNull { it.id == lessonId }
                    ?: error("این درس پیدا نشد.")
                val remoteSection = remoteLesson.section(sectionId) ?: error("این بخش پیدا نشد.")
                val file = lessonRepository.sectionAudioFile(appContext, remoteLesson, remoteSection)
                val prepared = withContext(ioDispatcher) {
                    TeacherVocalGenerator.lessonFromFile(
                        referenceFile = file,
                        title = remoteSection.title,
                        teacherName = remoteLesson.teacher,
                        description = remoteSection.description
                    )
                }
                val barCount = (prepared.durationMs / MS_PER_BAR).toInt().coerceIn(1, MAX_BARS)
                val peaks = withContext(ioDispatcher) {
                    WaveformPeaksExtractor.fromWavFile(file, barCount)
                }
                val (samples, rate) = withContext(ioDispatcher) { WavUtils.readPcm16Samples(file) }
                lesson = remoteLesson
                section = remoteSection
                voice = Voice(file, samples, rate, speed = 1f).also { voices[1f] = it }
                loadWholeFile()
                _uiState.update {
                    it.copy(
                        title = prepared.title,
                        teacherName = prepared.teacherName,
                        durationMs = prepared.durationMs,
                        peaks = peaks,
                        isLoading = false,
                        isReady = true
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "خطا در بارگذاری اجرای استاد")
                }
            }
            if (_uiState.value.isReady) loadOrnaments()
        }
    }

    fun loadOrnaments() {
        val currentLesson = lesson ?: return
        val currentSection = section ?: return
        if (_uiState.value.isOrnamentsLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isOrnamentsLoading = true, ornamentsError = null) }
            runCatching { lessonRepository.sectionOrnaments(currentLesson, currentSection) }
                .onSuccess { ornaments ->
                    _uiState.update { it.copy(ornaments = ornaments, isOrnamentsLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isOrnamentsLoading = false,
                            ornamentsError = error.message ?: "دریافت تحریرهای استاد ممکن نشد."
                        )
                    }
                }
        }
    }

    fun togglePlayback() {
        if (!_uiState.value.isReady) return
        if (player.isPlaying) {
            player.pause()
            stopProgressPolling()
            _uiState.update { it.copy(isPlaying = false, positionMs = playheadMs()) }
            return
        }
        val state = _uiState.value
        if (state.playingIndex != null && player.canResume) {
            player.play()
            _uiState.update { it.copy(isPlaying = true) }
            startProgressPolling()
            return
        }
        // After a segment ends, «play» continues the whole performance from there.
        val fromMs = if (state.positionMs >= state.durationMs) 0L else state.positionMs
        playWholeFrom(fromMs)
    }

    /** Plays from the tapped moment of the wave to the end. */
    fun playAt(positionMs: Long) {
        if (!_uiState.value.isReady) return
        playWholeFrom(positionMs)
    }

    /** Plays just this segment, from the start of its vowel (or [fromMs] in it) to the end. */
    fun playSegment(index: Int) = playSegment(index, fromMs = null)

    private fun playSegment(index: Int, fromMs: Long?) {
        if (!_uiState.value.isReady) return
        val ornament = _uiState.value.ornaments.getOrNull(index) ?: return
        val startMs = (fromMs ?: ornament.startMs).coerceIn(ornament.startMs, ornament.endMs)
        stopProgressPolling()
        // Released before the segment file is rewritten under it; marked as
        // not holding the whole file, so playing the whole file reloads it.
        player.stop()
        loadedClipStartMs = ornament.startMs
        _uiState.update {
            it.copy(selectedIndex = index, playingIndex = index, isPlaying = true, positionMs = startMs)
        }
        playJob?.cancel()
        playJob = viewModelScope.launch {
            // A slower copy on its way would take over mid-vowel and start it again; a short
            // tahrir is better heard once, from the clean copy, than stretched by the player first.
            slowJob?.join()
            withContext(ioDispatcher) { writeSegment(ornament.startMs, ornament.endMs) }
            player.prepare(segmentFile, onFinished = ::onPlaybackFinished)
            player.setLooping(_uiState.value.loopSegment)
            applySpeed()
            player.playFrom(((startMs - ornament.startMs) / (voice?.speed ?: 1f)).toLong())
            startProgressPolling()
        }
    }

    /** The player's own stretching covers the wait; the better copy takes over when it arrives. */
    fun setSpeed(speed: Float) {
        _uiState.update { it.copy(speed = speed) }
        applySpeed()
        slowJob?.cancel()
        slowJob = viewModelScope.launch {
            val wanted = voiceAt(speed) ?: return@launch
            if (wanted !== voice) switchTo(wanted)
        }
    }

    /** Null when it can't be had, and the slowing stays with the player. */
    private suspend fun voiceAt(speed: Float): Voice? {
        voices[speed]?.let { return it }
        val currentLesson = lesson ?: return null
        val currentSection = section ?: return null
        return runCatching {
            val file = lessonRepository.sectionSlowAudioFile(appContext, currentLesson, currentSection, speed)
            val (samples, rate) = withContext(ioDispatcher) { WavUtils.readPcm16Samples(file) }
            Voice(file, samples, rate, speed).also { voices[speed] = it }
        }.getOrNull()
    }

    private fun switchTo(next: Voice) {
        val wasPlaying = player.isPlaying
        val positionMs = playheadMs()
        val segment = _uiState.value.playingIndex
        // A segment waiting for this copy picks it up itself once it is here.
        val segmentWaiting = segment != null && playJob?.isActive == true
        voice = next
        when {
            segmentWaiting -> Unit
            // Carries on from the same moment instead of going back to the vowel's start.
            segment != null && wasPlaying -> playSegment(segment, fromMs = positionMs)
            wasPlaying -> {
                loadWholeFile()
                player.playFrom((positionMs / next.speed).toLong())
            }
            else -> {
                loadWholeFile()
                _uiState.update { it.copy(playingIndex = null) }
            }
        }
    }

    private fun applySpeed() {
        player.setSpeed(_uiState.value.speed / (voice?.speed ?: 1f))
    }

    fun toggleLoop() {
        _uiState.update { it.copy(loopSegment = !it.loopSegment) }
        if (_uiState.value.playingIndex != null) player.setLooping(_uiState.value.loopSegment)
    }

    fun zoomIn() {
        setZoom(_uiState.value.zoom * 2)
    }

    fun zoomOut() {
        setZoom(_uiState.value.zoom / 2)
    }

    fun setZoom(zoom: Float) {
        _uiState.update { it.copy(zoom = zoom.coerceIn(1f, MAX_ZOOM)) }
    }

    fun stopPlayback() {
        if (player.isPlaying) player.pause()
        stopProgressPolling()
        _uiState.update { it.copy(isPlaying = false, playingIndex = null) }
    }

    private fun playWholeFrom(positionMs: Long) {
        val totalMs = _uiState.value.durationMs.coerceAtLeast(1L)
        val startMs = positionMs.coerceIn(0L, totalMs)
        playJob?.cancel()
        if (loadedClipStartMs != null) loadWholeFile()
        applySpeed()
        player.playFrom((startMs / (voice?.speed ?: 1f)).toLong())
        _uiState.update {
            it.copy(isPlaying = true, positionMs = startMs, playingIndex = null)
        }
        startProgressPolling()
    }

    private fun loadWholeFile() {
        val file = voice?.file ?: return
        player.prepare(file, onFinished = ::onPlaybackFinished)
        applySpeed()
        loadedClipStartMs = null
    }

    /** Where in the teacher's own time the player is; a slow copy runs longer than that. */
    private fun playheadMs(): Long =
        (loadedClipStartMs ?: 0L) + (player.currentPositionMs() * (voice?.speed ?: 1f)).toLong()

    /** The segment's own samples, with a few ms fade at each edge so the cut does not click. */
    private fun writeSegment(startMs: Long, endMs: Long) {
        val source = voice ?: return
        val samplesPerMs = source.sampleRate / source.speed / 1000f
        val from = (startMs * samplesPerMs).toInt().coerceIn(0, source.samples.size)
        val to = (endMs * samplesPerMs).toInt().coerceIn(from, source.samples.size)
        val clip = source.samples.copyOfRange(from, to)
        val fade = minOf(clip.size / 4, source.sampleRate * EDGE_FADE_MS / 1000)
        for (index in 0 until fade) {
            val gain = index.toFloat() / fade
            clip[index] = (clip[index] * gain).toInt().toShort()
            val tail = clip.size - 1 - index
            clip[tail] = (clip[tail] * gain).toInt().toShort()
        }
        WavUtils.writePcm16Wav(segmentFile, clip, source.sampleRate)
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                _uiState.update { it.copy(positionMs = playheadMs()) }
                delay(PROGRESS_TICK_MS)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun onPlaybackFinished() {
        stopProgressPolling()
        _uiState.update { state ->
            val segment = state.playingIndex?.let { state.ornaments.getOrNull(it) }
            state.copy(isPlaying = false, playingIndex = null, positionMs = segment?.endMs ?: state.durationMs)
        }
    }

    override fun onCleared() {
        stopProgressPolling()
        player.release()
        super.onCleared()
    }

    companion object {
        const val ARG_LESSON_ID = "lessonId"
        const val ARG_SECTION_ID = "sectionId"
        val SPEEDS = listOf(0.25f, 0.5f, 0.75f, 1f)
        const val MAX_ZOOM = 16f
        /** One waveform bar per this many milliseconds, so zooming in stays sharp. */
        private const val MS_PER_BAR = 10L
        private const val MAX_BARS = 12_000
        private const val PROGRESS_TICK_MS = 33L
        private const val EDGE_FADE_MS = 4
        private const val SEGMENT_FILE_NAME = "teacher_segment.wav"
    }
}

/** The teacher's voice at [speed], which is 1 for their own file. */
private class Voice(val file: LocalFile, val samples: ShortArray, val sampleRate: Int, val speed: Float)

data class TeacherPerformanceUiState(
    val title: String = "",
    val teacherName: String = "",
    val durationMs: Long = 0L,
    val peaks: List<Float> = emptyList(),
    val ornaments: List<TeacherOrnament> = emptyList(),
    val isLoading: Boolean = false,
    val isReady: Boolean = false,
    val errorMessage: String? = null,
    val isOrnamentsLoading: Boolean = false,
    val ornamentsError: String? = null,
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val speed: Float = 1f,
    /** The segment last picked, kept highlighted after it stops. */
    val selectedIndex: Int? = null,
    /** The segment playing now; playback stops (or loops) at its end. */
    val playingIndex: Int? = null,
    val loopSegment: Boolean = false,
    val zoom: Float = 1f
)
