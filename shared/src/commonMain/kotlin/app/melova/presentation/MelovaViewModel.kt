package app.melova.presentation

import app.melova.platform.SecurityException
import androidx.lifecycle.ViewModel
import app.melova.platform.appContext
import app.melova.platform.cacheDir
import androidx.lifecycle.viewModelScope
import app.melova.audio.playback.ExpectedSegment
import app.melova.audio.pitch.PitchAnalyzer
import app.melova.audio.playback.ReferenceAudioGenerator
import app.melova.audio.playback.ReferenceAudioPlayer
import app.melova.audio.capture.VocalRecorder
import app.melova.audio.scoring.VocalScorer
import app.melova.data.ScaleExercise
import app.melova.platform.persianDigits
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import app.melova.platform.LocalFile

class MelovaViewModel : ViewModel() {
    private val player = ReferenceAudioPlayer(appContext)
    private val recorder = VocalRecorder(appContext)
    private val pitchAnalyzer = PitchAnalyzer()
    private val vocalScorer = VocalScorer()

    private var currentExercise = ReferenceAudioGenerator.prepareExercise(
        appContext,
        ScaleExercise.MAJOR_ASCENDING
    )

    private val _uiState = MutableStateFlow(MelovaUiState())
    val uiState: StateFlow<MelovaUiState> = _uiState.asStateFlow()

    private var sessionJob: Job? = null
    private var progressJob: Job? = null

    init {
        applyExercise(currentExercise)
    }

    fun onListenClicked() {
        if (_uiState.value.mode == SessionMode.ANALYZING) return
        startOrResumePlayback()
    }

    fun togglePlayback() {
        if (_uiState.value.mode == SessionMode.ANALYZING) return

        when (_uiState.value.mode) {
            SessionMode.LISTENING -> {
                player.togglePlayPause()
                _uiState.update {
                    it.copy(
                        isPlaying = player.isPlaying,
                        statusMessage = if (player.isPlaying) {
                            "در حال پخش نمونه آواز..."
                        } else {
                            "پخش متوقف شد"
                        }
                    )
                }
                if (player.isPlaying) {
                    startProgressPolling(SessionMode.LISTENING)
                } else {
                    stopProgressPolling()
                }
            }
            SessionMode.MATCHING -> Unit
            else -> startOrResumePlayback()
        }
    }

    fun onMatchClicked() {
        if (_uiState.value.mode == SessionMode.ANALYZING) return
        startMatchSession()
    }

    fun onStopClicked() {
        sessionJob?.cancel()
        stopProgressPolling()
        player.stop()
        recorder.cancel()
        _uiState.update {
            it.copy(
                mode = SessionMode.IDLE,
                progress = 0f,
                isPlaying = false,
                statusMessage = "متوقف شد"
            )
        }
    }

    fun dismissResults() {
        _uiState.update {
            it.copy(
                mode = SessionMode.IDLE,
                scoreResult = null,
                progress = 0f,
                isPlaying = false,
                statusMessage = "آماده تمرین"
            )
        }
    }

    fun selectExercise(exercise: ScaleExercise) {
        if (_uiState.value.selectedExercise == exercise) return
        if (isBusy()) return

        player.stop()
        stopProgressPolling()
        currentExercise = ReferenceAudioGenerator.prepareExercise(appContext, exercise)
        applyExercise(currentExercise)
    }

    private fun applyExercise(generated: ReferenceAudioGenerator.GeneratedExercise) {
        _uiState.update {
            it.copy(
                referenceReady = true,
                selectedExercise = generated.exercise,
                exerciseTitle = generated.exercise.title,
                exerciseDescription = buildExerciseDescription(generated),
                durationSeconds = ((generated.durationMs + 999) / 1000).toInt(),
                noteCount = generated.noteCount,
                scoreResult = null,
                progress = 0f,
                isPlaying = false,
                mode = SessionMode.IDLE,
                statusMessage = "آماده تمرین"
            )
        }
    }

    private fun buildExerciseDescription(
        generated: ReferenceAudioGenerator.GeneratedExercise
    ): String {
        val durationText = formatDurationSeconds(generated.durationMs)
        return "${generated.exercise.description} این تمرین ${persianDigits(generated.noteCount.toString())} نت دارد و حدود $durationText طول می‌کشد."
    }

    private fun startOrResumePlayback() {
        if (_uiState.value.mode == SessionMode.LISTENING && player.canResume) {
            player.play()
            startProgressPolling(SessionMode.LISTENING)
            _uiState.update {
                it.copy(
                    isPlaying = true,
                    statusMessage = "در حال پخش نمونه آواز..."
                )
            }
            return
        }

        sessionJob?.cancel()
        player.stop()

        _uiState.update {
            it.copy(
                mode = SessionMode.LISTENING,
                progress = 0f,
                isPlaying = true,
                scoreResult = null,
                statusMessage = "در حال پخش نمونه آواز..."
            )
        }

        sessionJob = viewModelScope.launch {
            player.prepare(
                file = currentExercise.referenceFile,
                onFinished = {
                    stopProgressPolling()
                    _uiState.update {
                        it.copy(
                            mode = SessionMode.IDLE,
                            progress = 1f,
                            isPlaying = false,
                            statusMessage = "پایان پخش. حالا «تطبیق» را امتحان کنید."
                        )
                    }
                },
                onReady = {
                    player.play()
                    startProgressPolling(SessionMode.LISTENING)
                }
            )
        }
    }

    private fun startMatchSession() {
        sessionJob?.cancel()
        val recordingFile = LocalFile(cacheDir(appContext), "user_vocal.wav")

        _uiState.update {
            it.copy(
                mode = SessionMode.MATCHING,
                progress = 0f,
                isPlaying = true,
                scoreResult = null,
                statusMessage = "آماده باشید... همزمان با نمونه بخوانید"
            )
        }

        sessionJob = viewModelScope.launch {
            player.stop()
            var matchStarted = false
            player.prepare(
                file = currentExercise.referenceFile,
                onFinished = {},
                onReady = {
                    if (matchStarted) return@prepare
                    matchStarted = true
                    player.play()
                    startProgressPolling(SessionMode.MATCHING)
                }
            )

            val recordingDeferred = async {
                recorder.recordFor(
                    durationMs = currentExercise.durationMs,
                    outputFile = recordingFile
                )
            }

            try {
                recordingDeferred.await()
                stopProgressPolling()
                analyzeRecording(recordingFile, currentExercise.segments)
            } catch (_: SecurityException) {
                stopProgressPolling()
                player.stop()
                _uiState.update {
                    it.copy(
                        mode = SessionMode.IDLE,
                        progress = 0f,
                        isPlaying = false,
                        statusMessage = "برای تطبیق آواز، دسترسی میکروفون لازم است."
                    )
                }
            }
        }
    }

    private suspend fun analyzeRecording(
        recordingFile: LocalFile,
        expectedSegments: List<ExpectedSegment>
    ) {
        _uiState.update {
            it.copy(
                mode = SessionMode.ANALYZING,
                isPlaying = false,
                statusMessage = "در حال تحلیل آواز شما..."
            )
        }

        val sungFrames = pitchAnalyzer.analyzeFile(recordingFile)
        val score = vocalScorer.score(expectedSegments, sungFrames)

        _uiState.update {
            it.copy(
                mode = SessionMode.RESULT,
                progress = 1f,
                isPlaying = false,
                scoreResult = score,
                statusMessage = score.summary
            )
        }
    }

    private fun startProgressPolling(sessionMode: SessionMode) {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive && _uiState.value.mode == sessionMode) {
                val progress = player.currentProgress()
                _uiState.update { state ->
                    if (state.mode == sessionMode) {
                        state.copy(progress = progress)
                    } else {
                        state
                    }
                }
                if (player.hasEnded) {
                    break
                }
                delay(50L)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun isBusy(): Boolean {
        val mode = _uiState.value.mode
        return mode == SessionMode.LISTENING ||
            mode == SessionMode.MATCHING ||
            mode == SessionMode.ANALYZING
    }

    private fun formatDurationSeconds(durationMs: Long): String {
        val totalSeconds = ((durationMs + 999) / 1000).toInt()
        return if (totalSeconds < 60) {
            persianDigits("$totalSeconds ثانیه")
        } else {
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            persianDigits("$minutes:${seconds.toString().padStart(2, '0')} دقیقه")
        }
    }

    override fun onCleared() {
        stopProgressPolling()
        player.release()
        super.onCleared()
    }
}

enum class SessionMode {
    IDLE,
    LISTENING,
    MATCHING,
    ANALYZING,
    RESULT
}

data class MelovaUiState(
    val referenceReady: Boolean = false,
    val selectedExercise: ScaleExercise = ScaleExercise.MAJOR_ASCENDING,
    val exerciseTitle: String = "",
    val exerciseDescription: String = "",
    val durationSeconds: Int = 0,
    val noteCount: Int = 0,
    val mode: SessionMode = SessionMode.IDLE,
    val progress: Float = 0f,
    val isPlaying: Boolean = false,
    val statusMessage: String = "آماده تمرین",
    val scoreResult: VocalScorer.VocalScoreResult? = null
)
