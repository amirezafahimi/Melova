package com.example.melova

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.melova.audio.ExpectedSegment
import com.example.melova.audio.PitchAnalyzer
import com.example.melova.audio.ReferenceAudioGenerator
import com.example.melova.audio.ReferenceAudioPlayer
import com.example.melova.audio.VocalRecorder
import com.example.melova.audio.VocalScorer
import com.example.melova.data.ScaleExercise
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class MelovaViewModel(application: Application) : AndroidViewModel(application) {
    private val player = ReferenceAudioPlayer(application)
    private val recorder = VocalRecorder(application)
    private val pitchAnalyzer = PitchAnalyzer()
    private val vocalScorer = VocalScorer()

    private var currentExercise = ReferenceAudioGenerator.prepareExercise(
        application,
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
        startPlaybackOnly()
    }

    fun onMatchClicked() {
        if (_uiState.value.mode == SessionMode.ANALYZING) return
        startMatchSession()
    }

    fun onStopClicked() {
        sessionJob?.cancel()
        progressJob?.cancel()
        player.stop()
        recorder.cancel()
        _uiState.update {
            it.copy(
                mode = SessionMode.IDLE,
                progress = 0f,
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
                statusMessage = "آماده تمرین"
            )
        }
    }

    fun selectExercise(exercise: ScaleExercise) {
        if (_uiState.value.selectedExercise == exercise) return
        if (isBusy()) return

        currentExercise = ReferenceAudioGenerator.prepareExercise(getApplication(), exercise)
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
                mode = SessionMode.IDLE,
                statusMessage = "آماده تمرین"
            )
        }
    }

    private fun buildExerciseDescription(
        generated: ReferenceAudioGenerator.GeneratedExercise
    ): String {
        val durationText = formatDurationSeconds(generated.durationMs)
        return "${generated.exercise.description} این تمرین ${generated.noteCount} نت دارد و حدود $durationText طول می‌کشد."
    }

    private fun startPlaybackOnly() {
        sessionJob?.cancel()
        progressJob?.cancel()
        _uiState.update {
            it.copy(
                mode = SessionMode.LISTENING,
                progress = 0f,
                scoreResult = null,
                statusMessage = "در حال پخش نمونه آواز..."
            )
        }

        sessionJob = viewModelScope.launch {
            launchProgressTracking()
            player.play(
                file = currentExercise.referenceFile,
                onProgress = { progress ->
                    _uiState.update { state -> state.copy(progress = progress) }
                },
                onFinished = {
                    _uiState.update {
                        it.copy(
                            mode = SessionMode.IDLE,
                            progress = 1f,
                            statusMessage = "پایان پخش. حالا «تطبیق» را امتحان کنید."
                        )
                    }
                }
            )
        }
    }

    private fun startMatchSession() {
        sessionJob?.cancel()
        progressJob?.cancel()
        val recordingFile = File(getApplication<Application>().cacheDir, "user_vocal.wav")

        _uiState.update {
            it.copy(
                mode = SessionMode.MATCHING,
                progress = 0f,
                scoreResult = null,
                statusMessage = "آماده باشید... همزمان با نمونه بخوانید"
            )
        }

        sessionJob = viewModelScope.launch {
            launchProgressTracking()
            val recordingDeferred = async {
                recorder.recordFor(
                    durationMs = currentExercise.durationMs,
                    outputFile = recordingFile
                )
            }

            player.play(
                file = currentExercise.referenceFile,
                onProgress = { progress ->
                    _uiState.update { state -> state.copy(progress = progress) }
                },
                onFinished = {}
            )

            try {
                recordingDeferred.await()
                analyzeRecording(recordingFile, currentExercise.segments)
            } catch (_: SecurityException) {
                player.stop()
                _uiState.update {
                    it.copy(
                        mode = SessionMode.IDLE,
                        progress = 0f,
                        statusMessage = "برای تطبیق آواز، دسترسی میکروفون لازم است."
                    )
                }
            }
        }
    }

    private suspend fun analyzeRecording(
        recordingFile: File,
        expectedSegments: List<ExpectedSegment>
    ) {
        _uiState.update {
            it.copy(
                mode = SessionMode.ANALYZING,
                statusMessage = "در حال تحلیل آواز شما..."
            )
        }

        val sungFrames = pitchAnalyzer.analyzeFile(recordingFile)
        val score = vocalScorer.score(expectedSegments, sungFrames)

        _uiState.update {
            it.copy(
                mode = SessionMode.RESULT,
                progress = 1f,
                scoreResult = score,
                statusMessage = score.summary
            )
        }
    }

    private fun launchProgressTracking() {
        progressJob = viewModelScope.launch {
            val durationMs = currentExercise.durationMs.coerceAtLeast(1L)
            val startedAt = System.currentTimeMillis()
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startedAt
                val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                _uiState.update { state ->
                    if (state.mode == SessionMode.LISTENING || state.mode == SessionMode.MATCHING) {
                        state.copy(progress = progress)
                    } else {
                        state
                    }
                }
                if (progress >= 1f) break
                delay(100)
            }
        }
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
            "$totalSeconds ثانیه"
        } else {
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            "$minutes:${seconds.toString().padStart(2, '0')} دقیقه"
        }
    }

    override fun onCleared() {
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
    val statusMessage: String = "آماده تمرین",
    val scoreResult: VocalScorer.VocalScoreResult? = null
)
