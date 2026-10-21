package app.melova.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.melova.audio.playback.ReferenceAudioPlayer
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

/** Plays a lesson's overall teaching audio (the teacher explaining the whole lesson). */
class LessonIntroViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val lessonId: String = checkNotNull(savedStateHandle[ARG_LESSON_ID])
    private val repository: LessonRepository = RemoteLessonRepository()
    private val player = ReferenceAudioPlayer(application)

    private val _uiState = MutableStateFlow(LessonIntroUiState())
    val uiState: StateFlow<LessonIntroUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null
    private var isPrepared = false

    init {
        viewModelScope.launch {
            val lesson = runCatching { repository.lessons(getApplication()) }.getOrNull()
                ?.firstOrNull { it.id == lessonId }
            _uiState.update {
                it.copy(status = if (lesson?.hasIntro == true) IntroStatus.IDLE else IntroStatus.UNAVAILABLE)
            }
        }
    }

    fun togglePlayback() {
        when (_uiState.value.status) {
            IntroStatus.IDLE, IntroStatus.ERROR -> loadAndPlay()
            IntroStatus.READY -> {
                player.togglePlayPause()
                _uiState.update { it.copy(isPlaying = player.isPlaying) }
            }
            IntroStatus.LOADING, IntroStatus.UNAVAILABLE -> Unit
        }
    }

    fun seekTo(fraction: Float) {
        if (_uiState.value.status != IntroStatus.READY) return
        val durationMs = _uiState.value.durationMs.takeIf { it > 0L } ?: return
        val positionMs = (durationMs * fraction.coerceIn(0f, 1f)).toLong()
        player.seekTo(positionMs)
        _uiState.update { it.copy(positionMs = positionMs) }
    }

    fun skipBy(deltaMs: Long) {
        val state = _uiState.value
        if (state.status != IntroStatus.READY || state.durationMs <= 0L) return
        seekTo((state.positionMs + deltaMs).toFloat() / state.durationMs)
    }

    /** Called when the student leaves the lesson screen. */
    fun pause() {
        if (!player.isPlaying) return
        player.pause()
        _uiState.update { it.copy(isPlaying = false) }
    }

    private fun loadAndPlay() {
        _uiState.update { it.copy(status = IntroStatus.LOADING, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                val lesson = repository.lessons(getApplication()).first { it.id == lessonId }
                repository.introFile(getApplication(), lesson)
            }.onSuccess { file ->
                player.prepare(
                    file = file,
                    onFinished = {
                        player.pause()
                        player.seekTo(0L)
                        _uiState.update { it.copy(isPlaying = false, positionMs = 0L) }
                    },
                    onReady = {
                        if (isPrepared) return@prepare
                        isPrepared = true
                        _uiState.update {
                            it.copy(status = IntroStatus.READY, durationMs = player.durationMs())
                        }
                        player.play()
                        _uiState.update { it.copy(isPlaying = true) }
                        startProgressPolling()
                    }
                )
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        status = IntroStatus.ERROR,
                        errorMessage = error.message ?: "پخش آموزش ممکن نشد"
                    )
                }
            }
        }
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
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
        progressJob?.cancel()
        player.release()
        super.onCleared()
    }

    companion object {
        const val ARG_LESSON_ID = "lessonId"
    }
}

enum class IntroStatus { UNAVAILABLE, IDLE, LOADING, READY, ERROR }

data class LessonIntroUiState(
    val status: IntroStatus = IntroStatus.IDLE,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorMessage: String? = null
) {
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}
