package app.melova.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.melova.audio.io.WavUtils
import app.melova.audio.io.WaveformPeaksExtractor
import app.melova.audio.playback.ReferenceAudioPlayer
import app.melova.data.model.TeacherOrnament
import app.melova.data.repository.TeacherRepository
import app.melova.platform.LocalFile
import app.melova.platform.appContext
import app.melova.platform.cacheDir
import app.melova.platform.ioDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A section's take before it is confirmed: sent to the server, which reads its tahrirs, vibratos
 * and glides the way the lesson page will, and shown over its wave so the teacher can hear each
 * one. The take stays on the server under the name it will keep, so confirming it sends nothing
 * again; a take left behind is deleted here, or by the server a day later.
 */
class TakePreviewViewModel : ViewModel() {
    private val repository = TeacherRepository(appContext)
    private val player = ReferenceAudioPlayer(appContext)

    private val _uiState = MutableStateFlow(TakePreviewUiState())
    val uiState: StateFlow<TakePreviewUiState> = _uiState.asStateFlow()

    private var lessonId: String? = null
    private var sectionId: String? = null
    private var audio: LocalFile? = null
    private var uploadJob: Job? = null
    private var progressJob: Job? = null
    private var playJob: Job? = null

    /** The take as 16-bit mono samples, for cutting each ornament out on its own. */
    private var samples: ShortArray = ShortArray(0)
    private var sampleRate: Int = 0
    private var wholeFile: LocalFile? = null
    private var playingClip = false
    private val segmentFile = LocalFile(cacheDir(appContext), SEGMENT_FILE_NAME)

    /** Sends [take] unless it is the one already sent; [sectionId] for a new take of that section. */
    fun preview(lessonId: String, sectionId: String?, take: LocalFile) {
        if (take == audio && _uiState.value.phase != TakePreviewPhase.FAILED) return
        discard()
        this.lessonId = lessonId
        this.sectionId = sectionId
        audio = take
        _uiState.value = TakePreviewUiState(phase = TakePreviewPhase.UPLOADING)
        uploadJob = viewModelScope.launch {
            runCatching {
                val uploaded = repository.uploadTake(lessonId, sectionId, take) { sent ->
                    _uiState.update {
                        if (it.phase != TakePreviewPhase.UPLOADING) it
                        else it.copy(uploadProgress = sent, phase = if (sent >= 1f) TakePreviewPhase.ANALYZING else it.phase)
                    }
                }
                _uiState.update { it.copy(takeId = uploaded.id, phase = TakePreviewPhase.ANALYZING) }
                val file = readable(take) ?: repository.takeAudio(lessonId, uploaded.id)
                loadWave(file)
                uploaded
            }.onSuccess { uploaded ->
                _uiState.update {
                    it.copy(
                        phase = TakePreviewPhase.READY,
                        ornaments = uploaded.ornaments,
                        analysisNote = uploaded.failureReason
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    // A take the server already has can still be confirmed, even without its wave here.
                    if (it.takeId != null) {
                        it.copy(phase = TakePreviewPhase.READY, analysisNote = "نمایش موج این اجرا ممکن نشد.")
                    } else {
                        it.copy(phase = TakePreviewPhase.FAILED, errorMessage = error.message ?: "بارگذاری اجرا ممکن نشد.")
                    }
                }
            }
        }
    }

    fun retry() {
        val lesson = lessonId ?: return
        val take = audio ?: return
        audio = null
        preview(lesson, sectionId, take)
    }

    /** The take is no longer wanted: recorded again, removed, or the page left without it. */
    fun discard() {
        uploadJob?.cancel()
        stopPlayback()
        val lesson = lessonId
        val takeId = _uiState.value.takeId
        if (lesson != null && takeId != null) {
            // Outlives this screen; the server keeps any take a section already uses.
            leftovers.launch { runCatching { repository.discardTake(lesson, takeId) } }
        }
        audio = null
        samples = ShortArray(0)
        wholeFile = null
        _uiState.value = TakePreviewUiState()
    }

    /** Confirmed as a section: the take is the section's now, so it must stay. */
    fun confirmed() {
        uploadJob?.cancel()
        stopPlayback()
        audio = null
        _uiState.value = TakePreviewUiState()
    }

    fun togglePlayback() {
        val state = _uiState.value
        if (wholeFile == null) return
        if (player.isPlaying) {
            stopPlayback()
            return
        }
        playWholeFrom(if (state.positionMs >= state.durationMs) 0L else state.positionMs)
    }

    fun playAt(positionMs: Long) {
        if (wholeFile == null) return
        playWholeFrom(positionMs)
    }

    /** Just this ornament, from its own cut, so it ends where its vowel ends. */
    fun toggleSegment(index: Int) {
        val state = _uiState.value
        if (state.playingIndex == index && state.isPlaying) {
            stopPlayback()
            return
        }
        val ornament = state.ornaments.getOrNull(index) ?: return
        if (samples.isEmpty()) return
        stopPolling()
        player.stop()
        playingClip = true
        _uiState.update { it.copy(selectedIndex = index, playingIndex = index, isPlaying = true, positionMs = ornament.startMs) }
        playJob?.cancel()
        playJob = viewModelScope.launch {
            withContext(ioDispatcher) { writeSegment(ornament) }
            player.prepare(segmentFile, onFinished = ::onFinished)
            player.playFrom(0L)
            startPolling(clipStartMs = ornament.startMs)
        }
    }

    fun setZoom(zoom: Float) {
        _uiState.update { it.copy(zoom = zoom.coerceIn(1f, MAX_ZOOM)) }
    }

    fun stopPlayback() {
        playJob?.cancel()
        if (player.isPlaying) player.pause()
        stopPolling()
        _uiState.update { it.copy(isPlaying = false, playingIndex = null) }
    }

    /** Our own recording, or a picked WAV this device can read as it is; null for anything else. */
    private suspend fun readable(take: LocalFile): LocalFile? = withContext(ioDispatcher) {
        runCatching { take.takeIf { isMonoPcm16(it.readBytes()) } }.getOrNull()
    }

    private suspend fun loadWave(file: LocalFile) {
        val (pcm, rate) = withContext(ioDispatcher) { WavUtils.readPcm16Samples(file) }
        val durationMs = if (rate > 0) pcm.size * 1000L / rate else 0L
        val bars = (durationMs / MS_PER_BAR).toInt().coerceIn(1, MAX_BARS)
        val peaks = withContext(ioDispatcher) { WaveformPeaksExtractor.fromWavFile(file, bars) }
        samples = pcm
        sampleRate = rate
        wholeFile = file
        _uiState.update { it.copy(peaks = peaks, durationMs = durationMs) }
    }

    private fun playWholeFrom(positionMs: Long) {
        val file = wholeFile ?: return
        val startMs = positionMs.coerceIn(0L, _uiState.value.durationMs.coerceAtLeast(0L))
        playJob?.cancel()
        if (playingClip || !player.canResume) {
            player.prepare(file, onFinished = ::onFinished)
            playingClip = false
        }
        player.playFrom(startMs)
        _uiState.update { it.copy(isPlaying = true, playingIndex = null, positionMs = startMs) }
        startPolling(clipStartMs = 0L)
    }

    private fun writeSegment(ornament: TeacherOrnament) {
        val perMs = sampleRate / 1000f
        val from = (ornament.startMs * perMs).toInt().coerceIn(0, samples.size)
        val to = (ornament.endMs * perMs).toInt().coerceIn(from, samples.size)
        val clip = samples.copyOfRange(from, to)
        // A few ms of fade at each edge, so the cut doesn't click.
        val fade = minOf(clip.size / 4, sampleRate * EDGE_FADE_MS / 1000)
        for (index in 0 until fade) {
            val gain = index.toFloat() / fade
            clip[index] = (clip[index] * gain).toInt().toShort()
            val tail = clip.size - 1 - index
            clip[tail] = (clip[tail] * gain).toInt().toShort()
        }
        WavUtils.writePcm16Wav(segmentFile, clip, sampleRate)
    }

    private fun startPolling(clipStartMs: Long) {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                _uiState.update { it.copy(positionMs = clipStartMs + player.currentPositionMs()) }
                delay(PROGRESS_TICK_MS)
            }
        }
    }

    private fun stopPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun onFinished() {
        stopPolling()
        _uiState.update { state ->
            val ornament = state.playingIndex?.let { state.ornaments.getOrNull(it) }
            state.copy(isPlaying = false, playingIndex = null, positionMs = ornament?.endMs ?: state.durationMs)
        }
    }

    override fun onCleared() {
        if (_uiState.value.takeId != null) discard()
        stopPolling()
        player.release()
        super.onCleared()
    }

    companion object {
        const val MAX_ZOOM = 16f
        /** One waveform bar per this many milliseconds, so zooming in stays sharp. */
        private const val MS_PER_BAR = 10L
        private const val MAX_BARS = 12_000
        private const val PROGRESS_TICK_MS = 33L
        private const val EDGE_FADE_MS = 4
        private const val SEGMENT_FILE_NAME = "take_preview_segment.wav"

        /** Discarding a take is sent after its screen is gone. */
        private val leftovers = CoroutineScope(SupervisorJob() + ioDispatcher)

        /** Format 1 (PCM), one channel, 16 bits: what [WavUtils.readPcm16Samples] reads correctly. */
        internal fun isMonoPcm16(bytes: ByteArray): Boolean =
            bytes.size > 44 &&
                bytes.decodeToString(0, 4) == "RIFF" &&
                bytes.decodeToString(8, 12) == "WAVE" &&
                WavUtils.readLeShort(bytes, 20).toInt() == 1 &&
                WavUtils.readLeShort(bytes, 22).toInt() == 1 &&
                WavUtils.readLeShort(bytes, 34).toInt() == 16
    }
}

enum class TakePreviewPhase { IDLE, UPLOADING, ANALYZING, READY, FAILED }

data class TakePreviewUiState(
    val phase: TakePreviewPhase = TakePreviewPhase.IDLE,
    /** How much of the take has been sent, 0 to 1. */
    val uploadProgress: Float = 0f,
    /** Set once the server has the take; what confirming it sends. */
    val takeId: String? = null,
    val ornaments: List<TeacherOrnament> = emptyList(),
    /** Why the ornaments, or the wave, couldn't be shown; the take can still be confirmed. */
    val analysisNote: String? = null,
    /** Why the take didn't reach the server. */
    val errorMessage: String? = null,
    val peaks: List<Float> = emptyList(),
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val selectedIndex: Int? = null,
    val playingIndex: Int? = null,
    val zoom: Float = 1f
) {
    val canConfirm: Boolean
        get() = phase == TakePreviewPhase.READY && takeId != null
}
