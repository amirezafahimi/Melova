package app.melova.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.melova.audio.playback.ReferenceAudioPlayer
import app.melova.data.model.Lesson
import app.melova.data.model.LessonSection
import app.melova.data.repository.RemoteLessonRepository
import app.melova.platform.LocalFile
import app.melova.platform.appContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Plays a lesson's own audio to its teacher in the panel, one thing at a time: the intro or a
 * section, downloaded the way students get it. Starting another stops the one playing.
 */
class TeacherListenViewModel : ViewModel() {
    private val repository = RemoteLessonRepository()
    private val player = ReferenceAudioPlayer(appContext)

    private val _uiState = MutableStateFlow(ListenUiState())
    val uiState: StateFlow<ListenUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var progressJob: Job? = null

    fun toggleIntro(lesson: Lesson) = toggle(introKey(lesson)) { repository.introFile(appContext, lesson) }

    fun toggleSection(lesson: Lesson, section: LessonSection) =
        toggle(sectionKey(section)) { repository.sectionAudioFile(appContext, lesson, section) }

    fun seekTo(fraction: Float) {
        val state = _uiState.value
        if (!state.isReady || state.durationMs <= 0L) return
        val positionMs = (state.durationMs * fraction.coerceIn(0f, 1f)).toLong()
        player.seekTo(positionMs)
        _uiState.update { it.copy(positionMs = positionMs) }
    }

    fun skipBy(deltaMs: Long) {
        val state = _uiState.value
        if (!state.isReady || state.durationMs <= 0L) return
        seekTo((state.positionMs + deltaMs).toFloat() / state.durationMs)
    }

    /** Leaving the page, or recording over it. */
    fun pause() {
        if (!player.isPlaying) return
        player.pause()
        _uiState.update { it.copy(isPlaying = false) }
    }

    private fun toggle(key: String, file: suspend () -> LocalFile) {
        val state = _uiState.value
        if (state.key == key) {
            when {
                state.isReady -> {
                    player.togglePlayPause()
                    _uiState.update { it.copy(isPlaying = player.isPlaying) }
                }
                state.errorMessage != null -> load(key, file)
            }
            return
        }
        load(key, file)
    }

    private fun load(key: String, file: suspend () -> LocalFile) {
        loadJob?.cancel()
        progressJob?.cancel()
        player.pause()
        _uiState.value = ListenUiState(key = key, isLoading = true)
        loadJob = viewModelScope.launch {
            runCatching { file() }
                .onSuccess { audio ->
                    player.prepare(
                        file = audio,
                        onFinished = {
                            player.pause()
                            player.seekTo(0L)
                            _uiState.update { if (it.key == key) it.copy(isPlaying = false, positionMs = 0L) else it }
                        },
                        onReady = {
                            if (_uiState.value.key != key || _uiState.value.isReady) return@prepare
                            _uiState.update { it.copy(isLoading = false, isReady = true, durationMs = player.durationMs()) }
                            player.play()
                            _uiState.update { it.copy(isPlaying = true) }
                            startProgressPolling(key)
                        }
                    )
                }
                .onFailure { error ->
                    _uiState.update {
                        if (it.key != key) it else it.copy(isLoading = false, errorMessage = error.message ?: "پخش ممکن نشد")
                    }
                }
        }
    }

    private fun startProgressPolling(key: String) {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive && _uiState.value.key == key) {
                _uiState.update {
                    it.copy(
                        isPlaying = player.isPlaying,
                        positionMs = player.currentPositionMs(),
                        durationMs = player.durationMs().takeIf { d -> d > 0L } ?: it.durationMs
                    )
                }
                delay(200)
            }
        }
    }

    override fun onCleared() {
        loadJob?.cancel()
        progressJob?.cancel()
        player.release()
        super.onCleared()
    }

    companion object {
        // The file's version is in the key, so a replaced take is fetched again.
        fun introKey(lesson: Lesson) = "intro/${lesson.id}/${lesson.introVersion}"
        fun sectionKey(section: LessonSection) = "section/${section.id}/${section.audioVersion}"
    }
}

/** What is playing, by its [key]; everything else on the page shows as stopped. */
data class ListenUiState(
    val key: String? = null,
    val isLoading: Boolean = false,
    val isReady: Boolean = false,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorMessage: String? = null
) {
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}
