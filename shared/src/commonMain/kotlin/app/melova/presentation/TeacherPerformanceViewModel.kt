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
import kotlin.math.roundToInt

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
    /** Plays the segments from [reel], so whatever sounds after a stop is silence, not the next consonant. */
    private val segmentPlayer = ReferenceAudioPlayer(appContext)

    private val _uiState = MutableStateFlow(TeacherPerformanceUiState())
    val uiState: StateFlow<TeacherPerformanceUiState> = _uiState.asStateFlow()

    private var lesson: Lesson? = null
    private var section: LessonSection? = null
    private var progressJob: Job? = null

    private var slowJob: Job? = null

    /** What the player reads from: the teacher's file, or a slower copy once one is here. */
    private var voice: Voice? = null
    private val voices = mutableMapOf<Float, Voice>()

    /** Every segment of a voice cut out ahead of time, so a tap only has to seek. */
    private var reel: Reel? = null
    private val reels = mutableMapOf<Float, Reel>()
    private var reelJob: Job? = null

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
                lesson = remoteLesson
                section = remoteSection
                voice = Voice(file, speed = 1f).also { voices[1f] = it }
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
                    reels.clear()
                    cutReel()
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

    private fun cutReel() {
        val current = voice ?: return
        reelJob?.cancel()
        reelJob = viewModelScope.launch { reelFor(current)?.let(::loadReel) }
    }

    private suspend fun reelFor(source: Voice): Reel? {
        reels[source.speed]?.let { return it }
        val ornaments = _uiState.value.ornaments.takeIf { it.isNotEmpty() } ?: return null
        return runCatching { withContext(ioDispatcher) { writeReel(source, ornaments) } }
            .getOrNull()
            ?.also { reels[source.speed] = it }
    }

    private fun loadReel(next: Reel) {
        reel = next
        segmentPlayer.prepare(next.file, onFinished = ::onPlaybackFinished)
        applySpeed()
    }

    /**
     * The segments' own samples one after another, [REEL_GAP_MS] of silence after each and a few
     * ms fade at each edge so the cut does not click. A stop always sounds a little late (what the
     * speaker already has keeps playing), and here that late bit falls in the silence.
     */
    private fun writeReel(source: Voice, ornaments: List<TeacherOrnament>): Reel {
        val (samples, sampleRate) = WavUtils.readPcm16Samples(source.file)
        val samplesPerMs = sampleRate / source.speed / 1000f
        val gap = sampleRate * REEL_GAP_MS / 1000
        val clips = ornaments.map { ornament ->
            val from = (ornament.startMs * samplesPerMs).toInt().coerceIn(0, samples.size)
            val to = (ornament.endMs * samplesPerMs).toInt().coerceIn(from, samples.size)
            val clip = samples.copyOfRange(from, to)
            val fade = minOf(clip.size / 4, sampleRate * EDGE_FADE_MS / 1000)
            for (index in 0 until fade) {
                val gain = index.toFloat() / fade
                clip[index] = (clip[index] * gain).toInt().toShort()
                val tail = clip.size - 1 - index
                clip[tail] = (clip[tail] * gain).toInt().toShort()
            }
            clip
        }
        val all = ShortArray(clips.sumOf { it.size + gap })
        val startsMs = mutableListOf<Long>()
        var at = 0
        for (clip in clips) {
            startsMs += at * 1000L / sampleRate
            clip.copyInto(all, at)
            at += clip.size + gap
        }
        val file = LocalFile(cacheDir(appContext), "teacher_segments_${(source.speed * 100).roundToInt()}.wav")
        WavUtils.writePcm16Wav(file, all, sampleRate)
        return Reel(file, source.speed, ornaments, startsMs)
    }

    fun togglePlayback() {
        if (!_uiState.value.isReady) return
        if (player.isPlaying || segmentPlayer.isPlaying) {
            player.pause()
            segmentPlayer.pause()
            stopProgressPolling()
            _uiState.update { it.copy(isPlaying = false, positionMs = playheadMs()) }
            return
        }
        val state = _uiState.value
        val segment = state.playingIndex?.let { state.ornaments.getOrNull(it) }
        if (segment != null && segmentPlayer.canResume) {
            segmentPlayer.play()
            _uiState.update { it.copy(isPlaying = true) }
            // Still a segment: it must stop at its own vowel's end, not run into the next one.
            watchSegment(segment)
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
        _uiState.update { it.copy(selectedIndex = index) }
        viewModelScope.launch {
            // Cut as soon as the segments arrive, so this waits only when tapped right away.
            if (reel == null) reelJob?.join()
            val current = reel ?: return@launch
            if (_uiState.value.selectedIndex != index) return@launch
            player.pause()
            applySpeed()
            segmentPlayer.playFrom(current.reelMs(index, startMs))
            _uiState.update { it.copy(playingIndex = index, isPlaying = true, positionMs = startMs) }
            watchSegment(ornament)
        }
    }

    /**
     * Keeps a segment inside its own vowel: the playhead is read often enough to stop within a
     * few milliseconds of its end, and to take it back to the start while the loop is on.
     */
    private fun watchSegment(ornament: TeacherOrnament) {
        stopProgressPolling()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val position = playheadMs()
                _uiState.update { it.copy(positionMs = position.coerceAtMost(ornament.endMs)) }
                if (position >= ornament.endMs) {
                    val index = _uiState.value.playingIndex
                    val current = reel
                    if (_uiState.value.loopSegment && index != null && current != null) {
                        segmentPlayer.playFrom(current.reelMs(index, ornament.startMs))
                    } else {
                        segmentPlayer.pause()
                        progressJob = null
                        _uiState.update {
                            it.copy(isPlaying = false, playingIndex = null, positionMs = ornament.endMs)
                        }
                        return@launch
                    }
                }
                delay(SEGMENT_TICK_MS)
            }
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
            Voice(file, speed).also { voices[speed] = it }
        }.getOrNull()
    }

    private suspend fun switchTo(next: Voice) {
        val nextReel = reelFor(next)
        val wasPlaying = player.isPlaying || segmentPlayer.isPlaying
        val positionMs = playheadMs()
        val segment = _uiState.value.playingIndex
        segmentPlayer.pause()
        voice = next
        loadWholeFile()
        if (nextReel != null) loadReel(nextReel)
        when {
            // Carries on from the same moment instead of going back to the vowel's start.
            segment != null && wasPlaying -> playSegment(segment, fromMs = positionMs)
            wasPlaying -> player.playFrom((positionMs / next.speed).toLong())
            else -> _uiState.update { it.copy(playingIndex = null) }
        }
    }

    private fun applySpeed() {
        val speed = _uiState.value.speed
        player.setSpeed(speed / (voice?.speed ?: 1f))
        reel?.let { segmentPlayer.setSpeed(speed / it.speed) }
    }

    fun toggleLoop() {
        _uiState.update { it.copy(loopSegment = !it.loopSegment) }
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
        if (segmentPlayer.isPlaying) segmentPlayer.pause()
        stopProgressPolling()
        _uiState.update { it.copy(isPlaying = false, playingIndex = null) }
    }

    private fun playWholeFrom(positionMs: Long) {
        val totalMs = _uiState.value.durationMs.coerceAtLeast(1L)
        val startMs = positionMs.coerceIn(0L, totalMs)
        segmentPlayer.pause()
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
    }

    /** Where in the teacher's own time the player is; a slow copy runs longer than that. */
    private fun playheadMs(): Long {
        val segment = _uiState.value.playingIndex
        val current = reel
        if (segment != null && current != null) return current.teacherMs(segment, segmentPlayer.currentPositionMs())
        return (player.currentPositionMs() * (voice?.speed ?: 1f)).toLong()
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
        segmentPlayer.release()
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
        /** A segment's end is a moment in the voice, so its playhead is read more often. */
        private const val SEGMENT_TICK_MS = 8L
        private const val EDGE_FADE_MS = 4
        /** Longer than any output delay, Bluetooth too, so a late stop never reaches the next segment. */
        private const val REEL_GAP_MS = 1_000
    }
}

/** The teacher's voice at [speed], which is 1 for their own file. */
private class Voice(val file: LocalFile, val speed: Float)

/** The segments of a voice at [speed] in one [file], segment i starting at [startsMs]`[i]` of it. */
private class Reel(
    val file: LocalFile,
    val speed: Float,
    private val ornaments: List<TeacherOrnament>,
    private val startsMs: List<Long>
) {
    fun reelMs(index: Int, teacherMs: Long): Long =
        startsMs[index] + ((teacherMs - ornaments[index].startMs) / speed).toLong()

    fun teacherMs(index: Int, reelMs: Long): Long =
        ornaments[index].startMs + ((reelMs - startsMs[index]) * speed).toLong()
}

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
